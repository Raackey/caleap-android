package com.maisor.caleap

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

private val Bg = Color(0xFFF7F7FB)
private val Ink = Color(0xFF171827)
private val Muted = Color(0xFF707083)
private val Purple = Color(0xFF6757F5)
private val PurpleSoft = Color(0xFFEDEAFF)
private val Green = Color(0xFF1C9A70)
private val GreenSoft = Color(0xFFE7F7F0)
private val Orange = Color(0xFFE88A2A)
private val OrangeSoft = Color(0xFFFFF1E2)

private enum class Tab { HOME, FOOD, PROGRESS, INSIGHTS, PROFILE }
private sealed interface AppScreen { data object Main : AppScreen; data object Camera : AppScreen; data class FoodResult(val labels: List<ImageLabel>) : AppScreen }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CaLeapPremiumApp() }
    }
}

@Composable
fun CaLeapPremiumApp() {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Main) }
    var showCapture by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showHealthConnect by remember { mutableStateOf(false) }
    var steps by remember { mutableLongStateOf(0L) }
    var sleepHours by remember { mutableDoubleStateOf(0.0) }

    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, background = Bg, surface = Color.White, onBackground = Ink, onSurface = Ink)) {
        when (val s = screen) {
            AppScreen.Main -> Scaffold(
                containerColor = Bg,
                floatingActionButton = {
                    FloatingActionButton(onClick = { showCapture = true }, containerColor = Purple, contentColor = Color.White, shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Show CaLeap")
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavItem(Tab.HOME, tab, { tab = it }, Icons.Default.Home, "Home")
                        NavItem(Tab.FOOD, tab, { tab = it }, Icons.Default.Restaurant, "Food")
                        NavItem(Tab.PROGRESS, tab, { tab = it }, Icons.Default.ShowChart, "Progress")
                        NavItem(Tab.INSIGHTS, tab, { tab = it }, Icons.Default.AutoAwesome, "Insights")
                        NavItem(Tab.PROFILE, tab, { tab = it }, Icons.Default.Person, "Profile")
                    }
                }
            ) { padding ->
                when (tab) {
                    Tab.HOME -> HomeScreen(Modifier.padding(padding), { showCapture = true }, { showOnboarding = true }, steps, sleepHours, { showHealthConnect = true })
                    Tab.FOOD -> FoodScreen(Modifier.padding(padding), { showCapture = true }, { showPaywall = true })
                    Tab.PROGRESS -> ProgressScreen(Modifier.padding(padding), { showPaywall = true })
                    Tab.INSIGHTS -> InsightsScreen(Modifier.padding(padding), { showPaywall = true })
                    Tab.PROFILE -> ProfileScreen(Modifier.padding(padding), { showOnboarding = true }, { showPaywall = true }, { showHealthConnect = true })
                }
            }
            AppScreen.Camera -> CameraScreen(
                onClose = { screen = AppScreen.Main },
                onCaptured = { labels -> screen = AppScreen.FoodResult(labels) }
            )
            is AppScreen.FoodResult -> FoodResultScreen(s.labels, onBack = { screen = AppScreen.Main }, onRetake = { screen = AppScreen.Camera })
        }

        if (showCapture) CaptureSheet(
            onDismiss = { showCapture = false },
            onPhoto = { showCapture = false; screen = AppScreen.Camera }
        )
        if (showPaywall) PaywallSheet { showPaywall = false }
        if (showOnboarding) OnboardingSheet { showOnboarding = false }
        if (showHealthConnect) HealthConnectSheet(
            onDismiss = { showHealthConnect = false },
            onSteps = { steps = 0L; showHealthConnect = false },
            onSleep = { sleepHours = 0.0; showHealthConnect = false }
        )
    }
}

@Composable private fun NavItem(target: Tab, current: Tab, onSelect: (Tab) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    NavigationBarItem(selected = current == target, onClick = { onSelect(target) }, icon = { Icon(icon, null) }, label = { Text(label) })
}

@Composable
fun HomeScreen(modifier: Modifier, onCapture: () -> Unit, onSetup: () -> Unit, steps: Long, sleepHours: Double, onHealth: () -> Unit) {
    val stepText = if (steps > 0) "${String.format("%,d", steps)}" else "6,240"
    val sleepText = if (sleepHours > 0) String.format("%.1f h", sleepHours) else "7h 12m"
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 22.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Good morning 👋", fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("Let's understand your day.", color = Muted, fontSize = 14.sp) }
                Box(Modifier.size(44.dp).clip(CircleShape).background(PurpleSoft), contentAlignment = Alignment.Center) { Text("R", fontWeight = FontWeight.Bold, color = Purple) }
            }
        }
        item { Button(onClick = onCapture, modifier = Modifier.fillMaxWidth().height(62.dp), shape = RoundedCornerShape(19.dp)) { Icon(Icons.Default.AddCircle, null); Spacer(Modifier.width(10.dp)); Text("SHOW CALEAP", fontSize = 17.sp, fontWeight = FontWeight.Bold) } }
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MiniPill("🔥 1,680 kcal", PurpleSoft, Purple); MiniPill("🥩 82g protein", GreenSoft, Green); MiniPill("💧 1.4L water", Color(0xFFEAF3FF), Color(0xFF3677C8)) } }
        item { SectionTitle("Today") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("🍽️", "Food", "1,680 kcal", "of 2,350", Modifier.weight(1f)); MetricCard("🥩", "Protein", "82 g", "of 120 g", Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("🚶", "Activity", stepText, "steps", Modifier.weight(1f)); MetricCard("😴", "Sleep", sleepText, "last night", Modifier.weight(1f)) } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = GreenSoft), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("✨", fontSize = 22.sp); Spacer(Modifier.width(8.dp)); Text("CaLeap understands", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
                    Spacer(Modifier.height(8.dp)); Text("Your activity is strong today. Your next meal could use more protein. Hydration is slightly behind your usual pattern.", lineHeight = 21.sp)
                    Spacer(Modifier.height(10.dp)); Text("Context-aware estimate • Review before acting", color = Muted, fontSize = 11.sp)
                }
            }
        }
        item { SectionTitle("One small action") }
        item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(46.dp).clip(CircleShape).background(PurpleSoft), contentAlignment = Alignment.Center) { Text("💧", fontSize = 22.sp) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Drink one glass of water", fontWeight = FontWeight.SemiBold); Text("A simple next step for today.", color = Muted, fontSize = 12.sp) }; OutlinedButton(onClick = {}, shape = RoundedCornerShape(14.dp)) { Text("Done") } } } }
        item { OutlinedButton(onClick = onHealth, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Watch, null); Spacer(Modifier.width(8.dp)); Text("Connect health data") } }
        item { OutlinedButton(onClick = onSetup, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("Personalize my goals") } }
    }
}

@Composable
fun CameraScreen(onClose: () -> Unit, onCaptured: (List<ImageLabel>) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var busy by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    imageCapture = capture
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(context as ComponentActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    } catch (_: Exception) { }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onClose, modifier = Modifier.background(Color.Black.copy(alpha=.45f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color.White) }
                    Surface(color = Color.Black.copy(alpha=.45f), shape = RoundedCornerShape(50)) { Text("Show CaLeap • Food", color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.size(48.dp))
                }
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Fit the whole meal in frame", color = Color.White, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    IconButton(
                        enabled = !busy,
                        onClick = {
                            val capture = imageCapture ?: return@IconButton
                            busy = true
                            val file = File(context.cacheDir, "caleap_${System.currentTimeMillis()}.jpg")
                            val options = ImageCapture.OutputFileOptions.Builder(file).build()
                            capture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                                override fun onError(exception: ImageCaptureException) { busy = false }
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    analyzeImage(file, context) { labels -> busy = false; onCaptured(labels) }
                                }
                            })
                        },
                        modifier = Modifier.size(78.dp).background(Color.White, CircleShape)
                    ) { Box(Modifier.size(62.dp).background(Purple, CircleShape)) }
                    Spacer(Modifier.height(18.dp))
                }
            }
        } else {
            Column(Modifier.align(Alignment.Center).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp)); Text("Camera access is needed to show CaLeap your meal.", color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp)); Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                TextButton(onClick = onClose) { Text("Cancel", color = Color.White) }
            }
        }
        if (busy) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.55f)), contentAlignment = Alignment.Center) { Card(shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(Modifier.height(12.dp)); Text("Understanding your food…", fontWeight = FontWeight.Bold); Text("Finding visible food signals", color = Muted, fontSize = 12.sp) } } }
    }
}

private fun analyzeImage(file: File, context: Context, onDone: (List<ImageLabel>) -> Unit) {
    try {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.55f).build())
        labeler.process(image).addOnSuccessListener { labels -> onDone(labels.take(8)) }.addOnFailureListener { onDone(emptyList()) }
    } catch (_: Exception) { onDone(emptyList()) }
}

@Composable
fun FoodResultScreen(labels: List<ImageLabel>, onBack: () -> Unit, onRetake: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(Bg).padding(20.dp), contentPadding = PaddingValues(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }; Column { Text("Food understanding", fontSize = 25.sp, fontWeight = FontWeight.Bold); Text("AI-assisted visual analysis", color = Muted, fontSize = 12.sp) } } }
        item { Card(colors = CardDefaults.cardColors(containerColor = PurpleSoft), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Text("What CaLeap sees", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(8.dp)); Text(if (labels.isEmpty()) "No reliable visual labels were found. Retake the photo with the meal clearly visible." else labels.joinToString(" • ") { it.text }, lineHeight = 22.sp); Spacer(Modifier.height(8.dp)); Text("This is image labeling, not a nutrition diagnosis. Nutrition values require food-specific models/database data and portion confirmation.", color = Muted, fontSize = 11.sp) } } }
        item { SectionTitle("Detected signals") }
        if (labels.isEmpty()) item { Text("Try a brighter, wider photo with less background.", color = Muted) }
        items(labels) { label ->
            Card(shape = RoundedCornerShape(17.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, tint = Green); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(label.text, fontWeight = FontWeight.SemiBold); Text("Confidence ${(label.confidence * 100).toInt()}%", color = Muted, fontSize = 12.sp) }; Text("Review", color = Purple, fontSize = 12.sp) } }
        }
        item { SectionTitle("Next step") }
        item { Card(colors = CardDefaults.cardColors(containerColor = GreenSoft), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp)) { Text("Portion + nutrition", fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text("CaLeap should ask only what changes the nutrition estimate, such as serving size or recipe details."); Spacer(Modifier.height(8.dp)); Text("Example: Was this about 1 cup or 2 cups?", color = Purple, fontWeight = FontWeight.SemiBold) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("Retake") }; Button(onClick = onBack, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("Save draft") } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectSheet(onDismiss: () -> Unit, onSteps: () -> Unit, onSleep: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Not connected") }
    var stepValue by remember { mutableLongStateOf(0L) }
    var sleepValue by remember { mutableDoubleStateOf(0.0) }
    val permissions = remember { setOf(HealthPermission.getReadPermission(StepsRecord::class), HealthPermission.getReadPermission(SleepSessionRecord::class)) }
    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        status = if (granted.containsAll(permissions)) "Connected" else "Some permissions were not granted"
        if (granted.containsAll(permissions)) scope.launch {
            val result = HealthDataReader(context).readToday()
            stepValue = result.steps
            sleepValue = result.sleepHours
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Connect your health data", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("CaLeap can use Health Connect data only after you grant permission. You can revoke access later.", color = Muted)
            Text("Status: $status", color = if (status == "Connected") Green else Muted, fontWeight = FontWeight.SemiBold)
            if (stepValue > 0 || sleepValue > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("🚶", "Steps", String.format("%,d", stepValue), Modifier.weight(1f)); StatCard("😴", "Sleep", String.format("%.1f h", sleepValue), Modifier.weight(1f)) }
            }
            Button(onClick = {
                val client = HealthConnectClient.getOrCreate(context)
                scope.launch {
                    try { launcher.launch(permissions.toTypedArray().toSet()); status = "Requesting permission…" }
                    catch (_: Exception) { status = "Health Connect is unavailable or needs an update" }
                }
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Connect Health Connect") }
            OutlinedButton(onClick = { openHealthConnect(context) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Open Health Connect") }
            Text("CaLeap reads only the health categories needed for the current feature. It does not upload raw Health Connect data to a server in this V4 prototype.", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun openHealthConnect(context: Context) {
    try { context.startActivity(Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")) } catch (_: Exception) { }
}

private data class HealthSnapshot(val steps: Long, val sleepHours: Double)
private class HealthDataReader(private val context: Context) {
    suspend fun readToday(): HealthSnapshot {
        val client = HealthConnectClient.getOrCreate(context)
        val zone = ZoneId.systemDefault(); val start = ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone).toInstant(); val end = Instant.now()
        var steps = 0L
        var sleepHours = 0.0
        try {
            val stepRecords = client.readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end))).records
            steps = stepRecords.sumOf { it.count }
        } catch (_: Exception) { }
        try {
            val sleepRecords = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(start.minus(1, ChronoUnit.DAYS), end))).records
            sleepHours = sleepRecords.sumOf { java.time.Duration.between(it.startTime, it.endTime).toMinutes() } / 60.0
        } catch (_: Exception) { }
        return HealthSnapshot(steps, sleepHours)
    }
}

@Composable fun FoodScreen(modifier: Modifier, onCapture: () -> Unit, onPremium: () -> Unit) { LazyColumn(modifier.fillMaxSize().padding(horizontal=20.dp), contentPadding=PaddingValues(top=22.dp,bottom=100.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) { item { Text("Food", fontSize=28.sp,fontWeight=FontWeight.Bold); Text("See your food. Understand your nutrition.",color=Muted) }; item { Card(Modifier.fillMaxWidth().clickable{onCapture()},shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Purple)){ Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha=.18f)),contentAlignment=Alignment.Center){Icon(Icons.Default.CameraAlt,null,tint=Color.White)};Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text("Scan a meal",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold);Text("Photo • Voice • Label • Recipe",color=Color.White.copy(alpha=.8f))};Icon(Icons.Default.ChevronRight,null,tint=Color.White)}}}; item{NutritionSummary()}; item{SectionTitle("Today's meals")}; items(listOf(Triple("Breakfast","2 idlis + sambar + coffee","320–400 kcal"),Triple("Lunch","Rice + dal + vegetables + curd","520–650 kcal"),Triple("Snack","Fruit + nuts","220–280 kcal"))){MealRow(it.first,it.second,it.third)}; item{Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=OrangeSoft)){Column(Modifier.padding(18.dp)){Text("🎯 Nutrition focus",fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));Text("Protein is the main gap today. CaLeap can suggest foods that fit your remaining calories.");TextButton(onClick=onPremium){Text("Explore personalized suggestions")}}}} } }
@Composable private fun NutritionSummary(){Card(shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("1,680",fontSize=30.sp,fontWeight=FontWeight.Bold);Text("kcal eaten",color=Muted)};Column(horizontalAlignment=Alignment.End){Text("2,350",fontWeight=FontWeight.Bold);Text("daily target",color=Muted,fontSize=12.sp)}};Spacer(Modifier.height(16.dp));LinearProgressIndicator(progress={.715f},modifier=Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),color=Purple,trackColor=PurpleSoft);Spacer(Modifier.height(16.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Macro("Protein","82g","120g",Green);Macro("Carbs","196g","285g",Purple);Macro("Fat","48g","78g",Orange);Macro("Fibre","19g","30g",Color(0xFF3A83B7))}}}
@Composable private fun Macro(name:String,value:String,target:String,color:Color){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(name,fontSize=11.sp,color=Muted);Text(value,fontWeight=FontWeight.Bold,color=color);Text("/ $target",fontSize=10.sp,color=Muted)}}
@Composable private fun MealRow(title:String,detail:String,calories:String){Card(shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(PurpleSoft),contentAlignment=Alignment.Center){Text("🍛",fontSize=22.sp)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold);Text(detail,color=Muted,fontSize=12.sp)};Text(calories,fontWeight=FontWeight.SemiBold,fontSize=12.sp)}}}
@Composable fun ProgressScreen(modifier: Modifier,onPremium:()->Unit){LazyColumn(modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(top=22.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){item{Text("Progress",fontSize=28.sp,fontWeight=FontWeight.Bold);Text("Track the changes that matter to you.",color=Muted)};item{Card(shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp)){Text("Weight",color=Muted);Row(verticalAlignment=Alignment.Bottom){Text("63.6",fontSize=34.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.width(6.dp));Text("kg",color=Muted);Spacer(Modifier.weight(1f));Text("-1.4 kg",color=Green,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(12.dp));SimpleChart();Spacer(Modifier.height(10.dp));Text("Last 30 days • Goal 60 kg",color=Muted,fontSize=11.sp)}}};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){StatCard("🔥","Avg calories","2,040",Modifier.weight(1f));StatCard("🚶","Avg steps","7,820",Modifier.weight(1f))}};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){StatCard("😴","Avg sleep","7h 04m",Modifier.weight(1f));StatCard("💧","Avg water","1.9 L",Modifier.weight(1f))}};item{Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=PurpleSoft)){Column(Modifier.padding(18.dp)){Text("🔒 Advanced progress",fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));Text("Compare weeks, view nutrition trends, understand plateaus and get personalized progress insights.");TextButton(onClick=onPremium){Text("Unlock Plus")}}}};item{SectionTitle("Recent changes")};item{ChangeRow("Weight","63.6 kg","-1.4 kg",Green)};item{ChangeRow("Activity","7,820 avg steps","+12%",Green)};item{ChangeRow("Sleep","7h 04m","+28 min",Green)}}}
@Composable private fun SimpleChart(){Row(Modifier.fillMaxWidth().height(100.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf(62,68,58,74,52,66,48,60,44,54,39,47,35,42,31,38).forEach{Box(Modifier.weight(1f).fillMaxHeight(it/100f).clip(RoundedCornerShape(6.dp)).background(PurpleSoft))}}}
@Composable private fun ChangeRow(title:String,current:String,change:String,color:Color){Card(shape=RoundedCornerShape(16.dp)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.SemiBold);Text(current,color=Muted,fontSize=12.sp)};Text(change,color=color,fontWeight=FontWeight.Bold)}}}
@Composable fun InsightsScreen(modifier:Modifier,onPremium:()->Unit){LazyColumn(modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(top=22.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){item{Text("Insights",fontSize=28.sp,fontWeight=FontWeight.Bold);Text("What your data means, not just what it says.",color=Muted)};item{Card(shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=Ink)){Column(Modifier.padding(20.dp)){Text("✨ Ask CaLeap",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(7.dp));Text("Ask about your food, activity, sleep, routine or progress.",color=Color.White.copy(alpha=.75f));Spacer(Modifier.height(15.dp));OutlinedButton(onClick=onPremium,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White)){Text("Ask a question")}}}};item{InsightCard("🍽️","Food pattern","Your protein intake is strongest on days when you include eggs, dal or curd at breakfast.",GreenSoft)};item{InsightCard("😴","Sleep pattern","Your recorded sleep is more consistent on days with earlier dinner times.",PurpleSoft)};item{InsightCard("💧","Hydration","You tend to drink less water on higher-activity days.",Color(0xFFEAF3FF))};item{Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=OrangeSoft)){Column(Modifier.padding(18.dp)){Text("🧠 Weekly summary",fontWeight=FontWeight.Bold);Spacer(Modifier.height(7.dp));Text("Activity improved, sleep became more consistent, and your meal logging is more complete than last week.");Spacer(Modifier.height(8.dp));Text("AI-generated from recorded data • Not medical advice",color=Muted,fontSize=11.sp)}}}}}
@Composable private fun InsightCard(icon:String,title:String,text:String,bg:Color){Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=bg)){Column(Modifier.padding(18.dp)){Text("$icon  $title",fontWeight=FontWeight.Bold,fontSize=17.sp);Spacer(Modifier.height(7.dp));Text(text,lineHeight=20.sp);Spacer(Modifier.height(8.dp));Text("See evidence →",color=Muted,fontSize=12.sp)}}}
@Composable fun ProfileScreen(modifier:Modifier,onSetup:()->Unit,onPremium:()->Unit,onHealth:()->Unit){LazyColumn(modifier.fillMaxSize().padding(horizontal=20.dp),contentPadding=PaddingValues(top=22.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("Profile",fontSize=28.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(14.dp));Card(shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(60.dp).clip(CircleShape).background(PurpleSoft),contentAlignment=Alignment.Center){Text("R",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Purple)};Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text("Your CaLeap profile",fontWeight=FontWeight.Bold,fontSize=17.sp);Text("Personal health context",color=Muted)};IconButton(onClick=onSetup){Icon(Icons.Default.Edit,null)}}}};item{Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Purple)){Column(Modifier.padding(18.dp)){Text("CaLeap Plus",color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold);Text("Unlock deeper health intelligence.",color=Color.White.copy(alpha=.8f));Spacer(Modifier.height(10.dp));Button(onClick=onPremium,colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Purple),shape=RoundedCornerShape(14.dp)){Text("View plans")}}}};item{SettingsRow(Icons.Default.Person,"Personal details","Age, height, weight, goals",onSetup)};item{SettingsRow(Icons.Default.Favorite,"Health preferences","Diet, allergies, preferences",onSetup)};item{SettingsRow(Icons.Default.Watch,"Connected health","Health Connect & devices",onHealth)};item{SettingsRow(Icons.Default.People,"Family","Profiles and sharing permissions",{})};item{SettingsRow(Icons.Default.Notifications,"Notifications","Smart reminders and insights",{})};item{SettingsRow(Icons.Default.Lock,"Privacy & data","Permissions, export and deletion",{})};item{SettingsRow(Icons.Default.Settings,"Settings","Language, appearance and app settings",{})};item{Text("CaLeap is a health information and wellness companion. It does not diagnose conditions or replace professional medical care.",color=Muted,fontSize=11.sp,lineHeight=16.sp,modifier=Modifier.padding(vertical=12.dp))}}}
@Composable private fun SettingsRow(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,subtitle:String,action:()->Unit){Card(Modifier.fillMaxWidth().clickable{action()},shape=RoundedCornerShape(17.dp)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Purple);Spacer(Modifier.width(13.dp));Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.SemiBold);Text(subtitle,color=Muted,fontSize=12.sp)};Icon(Icons.Default.ChevronRight,null,tint=Muted)}}}
@Composable private fun MetricCard(icon:String,title:String,value:String,sub:String,modifier:Modifier){Card(modifier,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(16.dp)){Text(icon,fontSize=21.sp);Spacer(Modifier.height(7.dp));Text(title,fontWeight=FontWeight.SemiBold);Text(value,fontWeight=FontWeight.Bold,fontSize=17.sp);Text(sub,color=Muted,fontSize=11.sp)}}}
@Composable private fun StatCard(icon:String,title:String,value:String,modifier:Modifier){Card(modifier,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Text(icon,fontSize=21.sp);Spacer(Modifier.height(7.dp));Text(title,color=Muted,fontSize=12.sp);Text(value,fontWeight=FontWeight.Bold,fontSize=18.sp)}}}
@Composable private fun MiniPill(text:String,bg:Color,fg:Color){Surface(color=bg,shape=RoundedCornerShape(50)){Text(text,modifier=Modifier.padding(horizontal=12.dp,vertical=7.dp),color=fg,fontSize=11.sp,fontWeight=FontWeight.SemiBold)}}
@Composable private fun SectionTitle(text:String){Text(text,fontSize=18.sp,fontWeight=FontWeight.Bold)}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CaptureSheet(onDismiss:()->Unit,onPhoto:()->Unit){ModalBottomSheet(onDismissRequest=onDismiss){Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("What do you want CaLeap to understand?",fontSize=21.sp,fontWeight=FontWeight.Bold);Text("Show it something. CaLeap turns it into useful context.",color=Muted);val actions=listOf("📸" to "Photo / Scan food","🎤" to "Tell CaLeap by voice","🧾" to "Health report","🍎" to "Food / recipe","💊" to "Medicine information","🏃" to "Activity","📄" to "Health document");actions.forEach{(icon,label)->OutlinedButton(onClick=if(label.startsWith("Photo"))onPhoto else onDismiss,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp)){Text(icon,fontSize=18.sp);Spacer(Modifier.width(10.dp));Text(label)}};Spacer(Modifier.height(12.dp))}}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PaywallSheet(onDismiss:()->Unit){ModalBottomSheet(onDismissRequest=onDismiss){Column(Modifier.fillMaxWidth().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("✨",fontSize=34.sp);Text("CaLeap Plus",fontSize=28.sp,fontWeight=FontWeight.Bold);Text("Understand your health, not just track it.",color=Muted,textAlign=TextAlign.Center);Spacer(Modifier.height(18.dp));listOf("Unlimited AI food understanding","Personalized nutrition suggestions","Advanced progress & trends","Health report intelligence","Ask CaLeap","Health story & deeper insights","Family intelligence").forEach{Row(Modifier.fillMaxWidth().padding(vertical=5.dp)){Icon(Icons.Default.CheckCircle,null,tint=Green,modifier=Modifier.size(20.dp));Spacer(Modifier.width(10.dp));Text(it)}};Spacer(Modifier.height(18.dp));Button(onClick=onDismiss,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)){Text("Start Plus")};TextButton(onClick=onDismiss){Text("Continue with free plan")};Text("Pricing will be finalized after product validation.",color=Muted,fontSize=10.sp);Spacer(Modifier.height(12.dp))}}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun OnboardingSheet(onDismiss:()->Unit){var page by remember{mutableIntStateOf(0)};val titles=listOf("Make CaLeap yours","Your food & activity goals","Your health context","Ready to understand your day");val bodies=listOf("Tell CaLeap the basics once. We use them to personalize your experience.","Set a goal without turning your life into a spreadsheet.","Add only the information you are comfortable connecting.","Show CaLeap what matters. Let AI turn it into useful understanding.");ModalBottomSheet(onDismissRequest=onDismiss){Column(Modifier.fillMaxWidth().padding(22.dp)){Text("CaLeap",color=Purple,fontWeight=FontWeight.Bold);Spacer(Modifier.height(14.dp));Text(titles[page],fontSize=27.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(7.dp));Text(bodies[page],color=Muted,lineHeight=20.sp);Spacer(Modifier.height(20.dp));when(page){0->SetupField("Your goal","Lose weight • Maintain • Build healthy habits");1->SetupField("Daily target","2,350 kcal • Personalized later");2->SetupField("Diet preference","Balanced • Vegetarian • Other");3->Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=GreenSoft)){Column(Modifier.padding(18.dp)){Text("✓ Your profile is ready",fontWeight=FontWeight.Bold);Text("You can change any preference later.")}}};Spacer(Modifier.height(24.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){if(page>0)OutlinedButton(onClick={page--},modifier=Modifier.weight(1f)){Text("Back")};Button(onClick={if(page<3){ {page++} } else onDismiss},modifier=Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(16.dp)){Text(if(page<3)"Continue" else "Finish")}};Spacer(Modifier.height(18.dp))}}}
@Composable private fun SetupField(title:String,value:String){Card(shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(17.dp)){Text(title,color=Muted,fontSize=12.sp);Spacer(Modifier.height(5.dp));Text(value,fontWeight=FontWeight.SemiBold)}}}
