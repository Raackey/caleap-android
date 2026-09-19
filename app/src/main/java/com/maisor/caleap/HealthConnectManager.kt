package com.maisor.caleap

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class HealthSnapshot(
    val steps: Long? = null,
    val sleepMinutes: Long? = null,
    val weightKg: Double? = null
)

class HealthConnectManager(context: Context) {
    private val client = HealthConnectClient.getOrCreate(context)

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    suspend fun granted(): Boolean {
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun readToday(): HealthSnapshot {
        val zone = ZoneId.systemDefault()
        val start = ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val end = Instant.now()

        var steps: Long? = null
        var sleepMinutes: Long? = null
        var weightKg: Double? = null

        runCatching {
            val result = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            steps = result.records.sumOf { it.count }
        }

        runCatching {
            val result = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        start.minusSeconds(24 * 60 * 60),
                        end
                    )
                )
            )
            sleepMinutes = result.records.sumOf {
                java.time.Duration.between(it.startTime, it.endTime).toMinutes()
            }
        }

        runCatching {
            val result = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        start.minusSeconds(30L * 24 * 60 * 60),
                        end
                    )
                )
            )
            weightKg = result.records.maxByOrNull { it.time }?.weight?.inKilograms
        }

        return HealthSnapshot(steps, sleepMinutes, weightKg)
    }
}
