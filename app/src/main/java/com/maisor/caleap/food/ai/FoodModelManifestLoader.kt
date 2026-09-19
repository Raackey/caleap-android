package com.maisor.caleap.food.ai

import android.content.Context
import org.json.JSONObject

object FoodModelManifestLoader {
    fun load(context: Context, assetName: String): TfliteModelManifest? {
        return try {
            val text = context.assets.open(assetName).bufferedReader().use { it.readText() }
            val o = JSONObject(text)
            val labelsJson = o.getJSONArray("outputLabels")
            val labels = buildList {
                for (i in 0 until labelsJson.length()) add(labelsJson.getString(i))
            }
            TfliteModelManifest(
                modelId = o.getString("modelId"),
                modelVersion = o.getString("modelVersion"),
                modelAsset = o.getString("modelAsset"),
                license = o.getString("license"),
                inputWidth = o.getInt("inputWidth"),
                inputHeight = o.getInt("inputHeight"),
                mean = listOf(o.getJSONArray("mean").getDouble(0).toFloat(),
                              o.getJSONArray("mean").getDouble(1).toFloat(),
                              o.getJSONArray("mean").getDouble(2).toFloat()),
                std = listOf(o.getJSONArray("std").getDouble(0).toFloat(),
                             o.getJSONArray("std").getDouble(1).toFloat(),
                             o.getJSONArray("std").getDouble(2).toFloat()),
                outputLabels = labels,
                topK = o.optInt("topK", 5)
            )
        } catch (_: Exception) {
            null
        }
    }
}
