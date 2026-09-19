package com.maisor.caleap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val Bg = Color(0xFFF7F7FB)
private val Ink = Color(0xFF151526)
private val Muted = Color(0xFF737487)
private val Purple = Color(0xFF6757F5)
private val PurpleSoft = Color(0xFFECE9FF)
private val Green = Color(0xFF16966D)
private val GreenSoft = Color(0xFFE5F7EF)
private val Blue = Color(0xFF347BC5)
private val BlueSoft = Color(0xFFEAF3FF)
private val Orange = Color(0xFFE58A2A)
private val OrangeSoft = Color(0xFFFFF1E1)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CaLeapApp() }
    }
}

private enum class Tab { HOME, FOOD, PROGRESS, INSIGHTS, PROFILE }

data class SavedMeal(val time: String, val foods: List<String>)

private class MealStore(context: Context) {
    private val prefs = context.getSharedPreferences("caleap_meals", Context.MODE_PRIVATE)
    fun save(foods: List<FoodItem>) {
        if (foods.isEmpty()) return
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val old = prefs.getString(day, "") ?: ""
        val entry = SimpleDateFormat("HH:mm", Locale.US).format(Date()) + "|" + foods.joinToString(",") { it.name }
        val next = if (old.isBlank()) entry else "$old\n$entry"
        prefs.edit().putString(day, next).apply()
    }
    fun today(): List<SavedMeal> {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return (prefs.getString(day, "") ?: "").lines().filter { it.isNotBlank() }.mapNotNull { row ->
            val parts = row.split("|", limit = 2)
            if (parts.size != 2) null else SavedMeal(parts[0], parts[1].split(",").filter { it.isNotBlank() })
        }.reversed()
    }
}

@Composable
private fun CaLeapApp() {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var capture by remember { mutableStateOf(false) }
    var captureSession by remember { mutableStateOf(CaptureSessionCoordinator.begin()) }
    var onboarding by remember { mutableStateOf(true) }
    var onboardingStep by remember { mutableIntStateOf(0) }
    var selectedGoal by remember { mutableStateOf("Eat and live healthier") }
    var selectedDiet by remember { mutableStateOf("Balanced") }
    var selectedActivity by remember { mutableStateOf("Moderately active") }
    var plus by remember { mutableStateOf(false) }
    var understanding by remember { mutableStateOf(false) }
    var understandingType by remember { mutableStateOf("Photo") }
    var voiceText by remember { mutableStateOf("") }
    var foodAnalysis by remember { mutableStateOf(FoodAnalysis(source = "Photo")) }
    var selectedFoods by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    val foodVisionAnalyzer = remember { FoodVisionAnalyzer() }
    var foodResult by remember { mutableStateOf(false) }
    var savedMeal by remember { mutableStateOf(false) }
    var healthConnected by remember { mutableStateOf(false) }
    var familyOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    var timelineOpen by remember { mutableStateOf(false) }
    var healthSnapshot by remember { mutableStateOf(HealthSnapshot()) }
    var healthStatus by remember { mutableStateOf("Not connected") }
    val dailyContextInput = DailyContextInput(
        meals = 1,
        calories = 520.0,
        proteinGrams = 18.0,
        fibreGrams = 7.0,
        waterMl = 1200,
        steps = healthSnapshot.steps,
        sleepMinutes = healthSnapshot.sleepMinutes,
        weightKg = healthSnapshot.weightKg
    )
    val dailyInsight = remember(
        healthSnapshot.steps,
        healthSnapshot.sleepMinutes,
        healthSnapshot.weightKg
    ) { ContextEngine.evaluate(dailyContextInput) }
    val context = LocalContext.current
    val healthManager = remember { HealthConnectManager(context) }
    val scope = rememberCoroutineScope()
    val mealStore = remember { MealStore(context) }
    var savedMeals by remember { mutableStateOf(mealStore.today()) }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(healthManager.permissions)) {
            healthStatus = "Connected"
            scope.launch { healthSnapshot = healthManager.readToday() }
        } else {
            healthStatus = "Limited access"
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capture = false
            understandingType = "Photo"
            captureSession = CaptureSessionCoordinator.understanding(
                CaptureSessionCoordinator.preparing(captureSession, CaptureSource.CAMERA)
            )
            understanding = true
            foodVisionAnalyzer.analyze(context, bitmap) { result ->
                foodAnalysis = result
                selectedFoods = result.candidates
                captureSession = CaptureSessionCoordinator.vision(captureSession, result.labels, result.confidence)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            capture = false
            understandingType = "Photo"
            captureSession = CaptureSessionCoordinator.understanding(
                CaptureSessionCoordinator.preparing(captureSession, CaptureSource.GALLERY)
            )
            understanding = true
            runCatching {
                val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                foodVisionAnalyzer.analyze(context, bitmap) { result ->
                    foodAnalysis = result
                    selectedFoods = result.candidates
                    captureSession = CaptureSessionCoordinator.gallery(captureSession, result.labels, result.confidence)
                }
            }.onFailure {
                foodAnalysis = FoodAnalysis(source = "Photo", needsConfirmation = true)
                captureSession = captureSession.copy(stage = CaptureStage.ERROR, error = "The selected image could not be read.")
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) {
            voiceText = text
            capture = false
            understandingType = "Voice"
            captureSession = CaptureSessionCoordinator.understanding(
                CaptureSessionCoordinator.preparing(captureSession, CaptureSource.VOICE)
            )
            selectedFoods = IndianFoodCatalog.match(text)
            foodAnalysis = FoodAnalysis(
                source = "Voice",
                candidates = selectedFoods,
                confidence = if (selectedFoods.isNotEmpty()) 88 else null,
                needsConfirmation = true
            )
            captureSession = CaptureSessionCoordinator.voice(captureSession, text)
            understanding = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
        else Toast.makeText(context, "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show()
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Purple,
            background = Bg,
            surface = Color.White,
            onSurface = Ink,
            onBackground = Ink
        )
    ) {
        if (onboarding) {
            PremiumOnboarding(
                step = onboardingStep,
                goal = selectedGoal,
                diet = selectedDiet,
                activity = selectedActivity,
                onGoal = { selectedGoal = it },
                onDiet = { selectedDiet = it },
                onActivity = { selectedActivity = it },
                onNext = {
                    if (onboardingStep < 5) onboardingStep++ else onboarding = false
                },
                onBack = { if (onboardingStep > 0) onboardingStep-- },
                onSkip = { onboarding = false }
            )
            return@MaterialTheme
        }

        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavItem(Tab.HOME, tab, { tab = it }, Icons.Default.Home, "Home")
                    NavItem(Tab.FOOD, tab, { tab = it }, Icons.Default.Restaurant, "Food")
                    NavItem(Tab.PROGRESS, tab, { tab = it }, Icons.Default.ShowChart, "Progress")
                    NavItem(Tab.INSIGHTS, tab, { tab = it }, Icons.Default.AutoAwesome, "Insights")
                    NavItem(Tab.PROFILE, tab, { tab = it }, Icons.Default.Person, "Profile")
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { captureSession = CaptureSessionCoordinator.begin(); capture = true },
                    containerColor = Purple,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp)
                ) { Icon(Icons.Default.Add, "Show CaLeap") }
            }
        ) { padding ->
            when (tab) {
                Tab.HOME -> HomeScreen(
                    Modifier.padding(padding),
                    { captureSession = CaptureSessionCoordinator.begin(); capture = true },
                    healthSnapshot = healthSnapshot,
                    healthConnected = healthConnected,
                    mealsToday = savedMeals.size,
                    dailyInsight = dailyInsight
                )
                Tab.FOOD -> FoodScreen(Modifier.padding(padding), { capture = true })
                Tab.PROGRESS -> ProgressScreen(Modifier.padding(padding), healthSnapshot)
                Tab.INSIGHTS -> InsightsScreen(Modifier.padding(padding))
                Tab.PROFILE -> ProfileScreen(
                    Modifier.padding(padding),
                    { onboarding = true; onboardingStep = 0 },
                    { plus = true },
                    healthConnected = healthConnected,
                    healthStatus = healthStatus,
                    onHealthConnect = {
                        scope.launch {
                            val granted = healthManager.granted()
                            if (granted) {
                                healthConnected = true
                                healthStatus = "Connected"
                                healthSnapshot = healthManager.readToday()
                            } else {
                                healthPermissionLauncher.launch(healthManager.permissions)
                            }
                        }
                    },
                    onFamily = { familyOpen = true },
                    onReports = { reportOpen = true }
                )
            }
        }

        if (capture) CaptureSheet(
            onDismiss = { capture = false },
            onCamera = {
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraLauncher.launch(null)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onGallery = { galleryLauncher.launch("image/*") },
            onVoice = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell CaLeap what you ate or what happened.")
                }
                voiceLauncher.launch(intent)
            }
        )
        if (understanding) UnderstandingSheet(
            type = understandingType,
            session = captureSession,
            voiceText = voiceText,
            analysis = foodAnalysis,
            onDismiss = { understanding = false },
            onReview = {
                captureSession = CaptureSessionCoordinator.review(captureSession)
                understanding = false
                if (selectedFoods.isEmpty() && foodAnalysis.candidates.isNotEmpty()) selectedFoods = foodAnalysis.candidates
                foodResult = true
            }
        )
        if (foodResult) FoodResultSheet(
            saved = savedMeal,
            selectedFoods = selectedFoods,
            onDismiss = { foodResult = false },
            onSave = { foods ->
                selectedFoods = foods
                mealStore.save(foods)
                savedMeals = mealStore.today()
                captureSession = CaptureSessionCoordinator.saved(captureSession)
                savedMeal = true
            }
        )
        if (plus) PlusSheet { plus = false }
        if (familyOpen) FamilySheet { familyOpen = false }
        if (reportOpen) ReportSheet { reportOpen = false }
        if (timelineOpen) TimelineSheet { timelineOpen = false }
    }
}

@Composable
private fun NavItem(
    target: Tab, current: Tab, onSelect: (Tab) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String
) {
    androidx.compose.material3.NavigationBarItem(
        selected = target == current,
        onClick = { onSelect(target) },
        icon = { Icon(icon, null) },
        label = { Text(label) }
    )
}

@Composable
private fun HomeScreen(modifier: Modifier, onCapture: () -> Unit, healthSnapshot: HealthSnapshot, healthConnected: Boolean, mealsToday: Int, dailyInsight: DailyInsight) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Good morning 👋", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Here's what matters about your day.", color = Muted)
                }
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(PurpleSoft),
                    contentAlignment = Alignment.Center
                ) { Text("R", color = Purple, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Ink),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Today's health picture", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Based on your recorded context", color = Color.White.copy(.68f), fontSize = 12.sp)
                        }
                        Box(
                            Modifier.size(58.dp).clip(CircleShape).background(Purple),
                            contentAlignment = Alignment.Center
                        ) { Text("82", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Your food and activity are on track. Hydration is the main thing to improve today.",
                        color = Color.White.copy(.88f),
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Context score • informational, not medical", color = Color.White.copy(.55f), fontSize = 10.sp)
                }
            }
        }

        item {
            Button(
                onClick = onCapture,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(19.dp)
            ) {
                Icon(Icons.Default.AddCircle, null)
                Spacer(Modifier.width(9.dp))
                Text("SHOW CALEAP", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Pill("1,680 kcal", PurpleSoft, Purple)
                Pill("82g protein", GreenSoft, Green)
                Pill("1.4L water", BlueSoft, Blue)
                Pill("6,240 steps", OrangeSoft, Orange)
            }
        }

        item {
            Card(shape = RoundedCornerShape(21.dp)) {
                Column(Modifier.padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FavoriteBorder, null, tint = Green)
                        Spacer(Modifier.width(9.dp))
                        Text("Health context", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(dailyInsight.headline + " " + dailyInsight.explanation, color = Ink)
                    Text("This is a wellness context statement, not a medical assessment.", color = Muted, fontSize = 10.sp)
                }
            }
        }

        item { Title("Today") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ContextCard("🍽️", "Food", "1,680", "of 2,350 kcal", Purple, Modifier.weight(1f))
                ContextCard("🥩", "Protein", "82 g", "of 120 g", Green, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ContextCard("🚶", "Activity", healthSnapshot.steps?.toString() ?: "—", if (healthConnected) "steps today" else "connect health", Orange, Modifier.weight(1f))
                ContextCard("😴", "Sleep", healthSnapshot.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "—", if (healthConnected) "recorded sleep" else "connect health", Blue, Modifier.weight(1f))
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = GreenSoft), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("✨ CaLeap noticed", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text("You usually drink less water on higher-activity days. Today you are already behind your usual pace.")
                    Spacer(Modifier.height(8.dp))
                    Text("Why this matters • Pattern from recorded data", color = Muted, fontSize = 10.sp)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(BlueSoft), contentAlignment = Alignment.Center) {
                        Text("💧", fontSize = 21.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("One small action", fontWeight = FontWeight.Bold)
                        Text("Drink one glass of water now.", color = Muted, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = {}, shape = RoundedCornerShape(13.dp)) { Text("Done") }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(21.dp)
            ) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(45.dp).clip(CircleShape).background(PurpleSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Timeline, null, tint = Purple)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Today's timeline", fontWeight = FontWeight.Bold)
                        Text("$mealsToday meal${if (mealsToday == 1) "" else "s"} logged • activity • sleep • hydration", color = Muted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
            }
        }

        item { Title("Quick view") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard("🍎", "Log food", onCapture, Modifier.weight(1f))
                QuickCard("🎤", "Voice log", onCapture, Modifier.weight(1f))
                QuickCard("📄", "Report", onCapture, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FoodScreen(modifier: Modifier, onCapture: () -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Text("Food", fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("See your food. Understand your nutrition.", color = Muted)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onCapture() },
                colors = CardDefaults.cardColors(containerColor = Purple),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(.16f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Show CaLeap your meal", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Photo • voice • label • recipe", color = Color.White.copy(.78f))
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(23.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("1,680 kcal", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text("of 2,350 kcal target", color = Muted)
                        }
                        Text("72%", color = Purple, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { .72f },
                        modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(9.dp)),
                        color = Purple,
                        trackColor = PurpleSoft
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Macro("Protein", "82g", "120g", Green)
                        Macro("Carbs", "196g", "285g", Purple)
                        Macro("Fat", "48g", "78g", Orange)
                        Macro("Fibre", "19g", "30g", Blue)
                    }
                }
            }
        }
        item { Title("Today's meals") }
        items(listOf(
            Triple("Breakfast", "2 idlis + sambar + coffee", "320–400 kcal"),
            Triple("Lunch", "Rice + dal + vegetables + curd", "520–650 kcal"),
            Triple("Snack", "Fruit + nuts", "220–280 kcal")
        )) { MealRow(it.first, it.second, it.third) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = OrangeSoft), shape = RoundedCornerShape(21.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("🎯 Nutrition focus", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Protein is the main gap today. CaLeap can suggest foods that fit your remaining context.")
                    TextButton(onClick = onCapture) { Text("Get a suggestion") }
                }
            }
        }
    }
}

@Composable private fun Macro(name: String, value: String, target: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, color = Muted, fontSize = 10.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold)
        Text("/ $target", color = Muted, fontSize = 9.sp)
    }
}

@Composable private fun MealRow(title: String, detail: String, kcal: String) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(PurpleSoft), contentAlignment = Alignment.Center) {
                Text("🍛", fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, color = Muted, fontSize = 12.sp)
            }
            Text(kcal, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ProgressScreen(modifier: Modifier, healthSnapshot: HealthSnapshot) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Text("Progress", fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Your trends, habits and changes over time.", color = Muted)
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(19.dp)) {
                    Text("Weight", color = Muted)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(healthSnapshot.weightKg?.let { "%.1f".format(it) } ?: "—", fontSize = 35.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(5.dp))
                        Text("kg", color = Muted)
                        Spacer(Modifier.weight(1f))
                        Text("-1.4 kg", color = Green, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(14.dp))
                    TrendBars()
                    Spacer(Modifier.height(8.dp))
                    Text("Last 30 days • Goal 60 kg", color = Muted, fontSize = 11.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("🔥", "Avg calories", "2,040", Modifier.weight(1f))
                StatCard("🚶", "Today steps", healthSnapshot.steps?.toString() ?: "—", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("😴", "Sleep", healthSnapshot.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "—", Modifier.weight(1f))
                StatCard("💧", "Avg water", "1.9 L", Modifier.weight(1f))
            }
        }
        item { Title("Consistency") }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = PurpleSoft)) {
                Column(Modifier.padding(18.dp)) {
                    Text("🔥 8 day healthy-habit streak", color = Purple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text("You logged food, activity or sleep on each of the last 8 days.")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(7) { Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(Purple), contentAlignment = Alignment.Center) { Text("✓", color = Color.White, fontSize = 11.sp) } }
                        Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) { Text("8", color = Purple, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        item { ChangeRow("Weight", "63.6 kg", "-1.4 kg", Green); }
        item { ChangeRow("Activity", "7,820 avg steps", "+12%", Green); }
        item { ChangeRow("Sleep", "7h 04m", "+28 min", Green); }
    }
}

@Composable private fun TrendBars() {
    Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
        listOf(45,58,52,67,48,70,62,75,55,66,60,78,69,82,72,86).forEach { h ->
            Box(Modifier.weight(1f).fillMaxHeight(h / 100f).clip(RoundedCornerShape(6.dp)).background(PurpleSoft))
        }
    }
}

@Composable private fun InsightsScreen(modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Text("Insights", fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("What your data means, not just what it says.", color = Muted)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("✨ Ask CaLeap", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Ask about food, sleep, activity, routine or your progress.", color = Color.White.copy(.72f))
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("Ask a question") }
                }
            }
        }
        item { InsightCard("🍽️", "Food pattern", "Protein is strongest on days when breakfast includes eggs, dal or curd.", GreenSoft) }
        item { InsightCard("😴", "Sleep pattern", "Your recorded sleep is more consistent on days with an earlier dinner.", PurpleSoft) }
        item { InsightCard("💧", "Hydration", "You tend to drink less water on higher-activity days.", BlueSoft) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = OrangeSoft), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("🧠 Weekly summary", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("Activity improved, sleep became more consistent, and meal logging is more complete than last week.")
                    Spacer(Modifier.height(8.dp))
                    Text("AI-generated from recorded data • Not medical advice", color = Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable private fun InsightCard(icon: String, title: String, body: String, bg: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(21.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("$icon  $title", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(7.dp))
            Text(body, lineHeight = 20.sp)
            Spacer(Modifier.height(7.dp))
            Text("Pattern from your recorded context", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ProfileScreen(
    modifier: Modifier,
    onEdit: () -> Unit,
    onPlus: () -> Unit,
    healthConnected: Boolean,
    healthStatus: String,
    onHealthConnect: () -> Unit,
    onFamily: () -> Unit,
    onReports: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Profile", fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Your preferences and health context.", color = Muted)
        }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(PurpleSoft), contentAlignment = Alignment.Center) {
                        Text("R", color = Purple, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Your CaLeap profile", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Personal health context", color = Muted)
                    }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Purple), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(19.dp)) {
                    Text("✨ CaLeap Plus", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Deeper intelligence, reports and personalization.", color = Color.White.copy(.78f))
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onPlus, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Purple), shape = RoundedCornerShape(14.dp)) {
                        Text("View plans")
                    }
                }
            }
        }
        item { SettingRow(Icons.Default.Person, "Personal details", "Age, height, weight, goals", onEdit) }
        item { SettingRow(Icons.Default.Favorite, "Health preferences", "Diet, allergies and preferences", onEdit) }
        item { SettingRow(
            Icons.Default.Watch,
            "Connected health",
            if (healthConnected) "Connected • $healthStatus" else "Health Connect & devices",
            onHealthConnect
        ) }
        item { SettingRow(Icons.Default.Assessment, "Health reports", "Understand reports in plain language", onReports) }
        item { SettingRow(Icons.Default.People, "Family", "Profiles and sharing permissions", onFamily) }
        item { SettingRow(Icons.Default.Notifications, "Notifications", "Smart reminders and insights", {}) }
        item { SettingRow(Icons.Default.Lock, "Privacy & data", "Permissions, export and deletion", {}) }
        item { SettingRow(Icons.Default.Settings, "Appearance & settings", "Language, theme and app controls", {}) }
        item {
            Text(
                "CaLeap provides health and wellness information. It does not diagnose conditions, prescribe treatment, or replace professional medical care.",
                color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(vertical = 10.dp)
            )
        }
    }
}

@Composable private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, action: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { action() }, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Purple)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(sub, color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable private fun ContextCard(icon: String, title: String, value: String, sub: String, color: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(19.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(5.dp))
            Text(title, color = Muted, fontSize = 12.sp)
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(sub, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable private fun StatCard(icon: String, title: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(5.dp))
            Text(title, color = Muted, fontSize = 11.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable private fun QuickCard(icon: String, title: String, action: () -> Unit, modifier: Modifier) {
    Card(modifier.clickable { action() }, shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 21.sp)
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable private fun Pill(text: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(text, color = fg, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun Title(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable private fun ChangeRow(title: String, value: String, change: String, color: Color) {
    Card(shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(value, color = Muted, fontSize = 11.sp)
            }
            Text(change, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureSheet(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onVoice: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("What do you want CaLeap to understand?", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Show it something. CaLeap turns it into useful context.", color = Muted)

            CaptureAction("📸", "Take a food photo", "Camera", onCamera)
            CaptureAction("🖼️", "Choose from gallery", "Photo", onGallery)
            CaptureAction("🎤", "Tell CaLeap by voice", "Voice", onVoice)
            CaptureAction("🧾", "Health report", "Coming next", onDismiss)
            CaptureAction("💊", "Medicine information", "Coming next", onDismiss)
            CaptureAction("🏃", "Activity", "Coming next", onDismiss)

            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun CaptureAction(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(PurpleSoft),
                contentAlignment = Alignment.Center
            ) { Text(icon, fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnderstandingSheet(
    type: String,
    session: CaptureSession,
    voiceText: String,
    analysis: FoodAnalysis,
    onDismiss: () -> Unit,
    onReview: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("CaLeap is understanding…", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                if (type == "Voice") "We heard: “$voiceText”" else "Analyzing what you showed CaLeap.",
                color = Muted,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(18.dp))

            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text("Capture pipeline", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PipelineStep("SEE", session.stage != CaptureStage.CAPTURING)
                        PipelineStep("UNDERSTAND", session.coreResult != null)
                        PipelineStep("CONFIRM", session.stage == CaptureStage.REVIEW || session.stage == CaptureStage.SAVED)
                        PipelineStep("SAVE", session.stage == CaptureStage.SAVED)
                    }
                    if (session.error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(session.error, color = Orange, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = PurpleSoft), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (analysis.source == "Voice" && analysis.candidates.isNotEmpty()) "Foods understood" else "Visual understanding", color = Purple, fontWeight = FontWeight.Bold)
                            Text(
                                if (analysis.confidence != null) "Confidence ${analysis.confidence}% • confirm before saving" else "Food-specific confidence is not available yet",
                                color = Muted, fontSize = 11.sp
                            )
                        }
                        Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                            Text("Review", color = Purple, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (analysis.candidates.isNotEmpty()) {
                        analysis.candidates.take(5).forEach { item ->
                            DetectedItem(item.name, item.unit, "${item.nutrition.kcalMin}–${item.nutrition.kcalMax} kcal")
                        }
                        val total = IndianFoodCatalog.combine(analysis.candidates)
                        Spacer(Modifier.height(8.dp))
                        Text("Estimated range: ${total.kcalMin}–${total.kcalMax} kcal", fontWeight = FontWeight.Bold)
                    } else {
                        Text("CaLeap can see the image, but it will not invent a food name or nutrition value. Confirm the food items in the next step.", lineHeight = 19.sp)
                        if (analysis.labels.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("General visual labels: ${analysis.labels.joinToString(", ")}", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("CaLeap uses ranges and asks for confirmation when the available information is not food-specific enough. Portions can be corrected before saving.", color = Muted, fontSize = 11.sp)

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onReview,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(15.dp)
                ) { Text("Review & edit") }
                Button(
                    onClick = onReview,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(15.dp)
                ) { Text("Continue") }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun PipelineStep(label: String, active: Boolean) {
    Surface(
        color = if (active) GreenSoft else Bg,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Green else Muted
        )
    }
}

@Composable
private fun DetectedItem(name: String, portion: String, kcal: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(portion, color = Muted, fontSize = 11.sp)
        }
        Text(kcal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodResultSheet(
    saved: Boolean,
    selectedFoods: List<FoodItem>,
    onDismiss: () -> Unit,
    onSave: (List<FoodItem>) -> Unit
) {
    var confirmedFoods by remember(selectedFoods) { mutableStateOf(selectedFoods) }
    var portions by remember(selectedFoods) { mutableStateOf(selectedFoods.associate { it.name to 1 }) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                Text("Meal review", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Correct anything CaLeap got wrong before saving.", color = Muted)
                Spacer(Modifier.height(15.dp))
            }

            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("🍛 Meal confirmation", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Confirm what is actually in the meal before saving.", color = Muted, fontSize = 11.sp)
                        Spacer(Modifier.height(14.dp))

                        if (confirmedFoods.isEmpty()) {
                            Text("No food-specific match was confirmed yet.", color = Orange, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text("Choose only what is actually present. This is the confirmation step that prevents CaLeap from inventing nutrition.", color = Muted, fontSize = 12.sp)
                        } else {
                            confirmedFoods.forEach { item ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text(item.unit, color = Muted, fontSize = 11.sp)
                                    IconButton(onClick = { confirmedFoods = confirmedFoods.filterNot { it.name == item.name }
                                        portions = portions - item.name }) {
                                        Icon(Icons.Default.Close, "Remove")
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Add / correct food", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(7.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            IndianFoodCatalog.items.forEach { item ->
                                FilterChip(
                                    selected = confirmedFoods.any { it.name == item.name },
                                    onClick = {
                                        confirmedFoods = if (confirmedFoods.any { it.name == item.name }) {
                                            confirmedFoods.filterNot { it.name == item.name }
                                        } else {
                                            confirmedFoods + item
                                        }
                                        portions = if (portions.containsKey(item.name)) portions else portions + (item.name to 1)
                                    },
                                    label = { Text(item.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(14.dp))
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Portion check", fontWeight = FontWeight.Bold)
                        Text("Adjust the serving count before saving. CaLeap keeps the nutrition as a range.", color = Muted, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        confirmedFoods.forEach { item ->
                            EditableFoodRow(
                                name = item.name,
                                count = portions[item.name] ?: 1,
                                unit = "serving",
                                onMinus = { portions = portions + (item.name to ((portions[item.name] ?: 1) - 1).coerceAtLeast(1)) },
                                onPlus = { portions = portions + (item.name to ((portions[item.name] ?: 1) + 1).coerceAtMost(3)) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(14.dp))
                Card(colors = CardDefaults.cardColors(containerColor = PurpleSoft), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Estimated nutrition", color = Purple, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        val weighted = confirmedFoods.flatMap { item -> List((portions[item.name] ?: 1).coerceIn(1, 3)) { item } }
                        val total = IndianFoodCatalog.combine(weighted)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            NutritionMini("Calories", "${total.kcalMin}–${total.kcalMax}")
                            NutritionMini("Protein", "${total.proteinMin.roundToInt()}–${total.proteinMax.roundToInt()}g")
                            NutritionMini("Carbs", "${total.carbsMin.roundToInt()}–${total.carbsMax.roundToInt()}g")
                            NutritionMini("Fibre", "${total.fibreMin.roundToInt()}–${total.fibreMax.roundToInt()}g")
                        }
                        Spacer(Modifier.height(9.dp))
                        Text("Range depends on portion size and recipe. Values are estimates, not medical measurements.", color = Muted, fontSize = 10.sp)
                    }
                }
            }

            item {
                Spacer(Modifier.height(14.dp))
                Card(colors = CardDefaults.cardColors(containerColor = GreenSoft), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(17.dp)) {
                        Text("🧠 CaLeap context", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(if (confirmedFoods.isEmpty()) "Confirm the food items to let CaLeap calculate nutrition and connect the meal to your daily context." else "This meal is now confirmed enough for CaLeap to calculate a nutrition range and connect it to your daily context.")
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                if (saved) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) { Text("✓ Saved to today's timeline") }
                } else {
                    Button(
                        onClick = {
                            val weighted = confirmedFoods.flatMap { item -> List((portions[item.name] ?: 1).coerceIn(1, 3)) { item } }
                            onSave(weighted)
                        },
                        enabled = confirmedFoods.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Save meal to today") }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "You can correct the meal later. Saved values are estimates, not medical measurements.",
                    color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EditableFoodRow(
    name: String,
    count: Int,
    unit: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text("Estimated portion", color = Muted, fontSize = 10.sp)
        }
        IconButton(onClick = onMinus, enabled = count > 0) {
            Icon(Icons.Default.RemoveCircleOutline, null)
        }
        Text("$count $unit", fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 64.dp), textAlign = TextAlign.Center)
        IconButton(onClick = onPlus) {
            Icon(Icons.Default.AddCircleOutline, null, tint = Purple)
        }
    }
}

@Composable
private fun NutritionMini(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(label, color = Muted, fontSize = 9.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlusSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✨", fontSize = 35.sp)
            Text("CaLeap Plus", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Understand your health, not just track it.", color = Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            listOf(
                "Unlimited AI food understanding",
                "Personalized nutrition suggestions",
                "Advanced progress and trends",
                "Health report intelligence",
                "Ask CaLeap",
                "Health story and deeper insights",
                "Family intelligence"
            ).forEach {
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp)); Text(it)
                }
            }
            Spacer(Modifier.height(15.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Start Plus") }
            TextButton(onClick = onDismiss) { Text("Continue with free plan") }
            Text("Pricing is a product-validation decision and can change before launch.", color = Muted, fontSize = 10.sp)
            Spacer(Modifier.height(10.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilySheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Family", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Connect healthy routines without exposing private health information by default.", color = Muted)
            Spacer(Modifier.height(15.dp))
            FamilyMember("You", "Private health profile", true)
            FamilyMember("Family member", "Meal & habit sharing only", false)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Manage family")
            }
            Spacer(Modifier.height(12.dp))
            Text("Sharing should be explicit and revocable.", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun FamilyMember(name: String, detail: String, owner: Boolean) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(if (owner) PurpleSoft else GreenSoft), contentAlignment = Alignment.Center) {
                Icon(if (owner) Icons.Default.Person else Icons.Default.People, null, tint = if (owner) Purple else Green)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Muted, fontSize = 11.sp)
            }
            if (owner) Text("Owner", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            else Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Health reports", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("CaLeap can explain information in plain language without pretending to diagnose.", color = Muted)
            Spacer(Modifier.height(15.dp))
            ReportCard("Blood report", "Upload a PDF or photo", "Explain • compare • ask")
            ReportCard("Lab history", "Compare previous and current values", "Trend • context")
            ReportCard("Health document", "Store important documents", "Timeline • access")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Choose a report")
            }
            Spacer(Modifier.height(10.dp))
            Text("CaLeap does not diagnose conditions or change medication doses.", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ReportCard(title: String, sub: String, action: String) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(45.dp).clip(RoundedCornerShape(13.dp)).background(BlueSoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Description, null, tint = Blue)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(sub, color = Muted, fontSize = 11.sp)
            }
            Text(action, color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text("Today's timeline", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Everything CaLeap understands about your day.", color = Muted)
                Spacer(Modifier.height(15.dp))
            }
            item { TimelineEvent("08:10", "Breakfast", "2 idlis + sambar + coffee", "Food", Purple) }
            item { TimelineEvent("10:30", "Walking", "2,140 steps", "Activity", Orange) }
            item { TimelineEvent("13:20", "Lunch", "Rice + dal + vegetables + curd", "Food", Purple) }
            item { TimelineEvent("15:40", "Hydration", "1.4 L total so far", "Water", Blue) }
            item { TimelineEvent("Yesterday 23:05", "Sleep", "7h 12m", "Sleep", Green) }
            item {
                Spacer(Modifier.height(10.dp))
                Card(colors = CardDefaults.cardColors(containerColor = PurpleSoft), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(17.dp)) {
                        Text("✨ Context", color = Purple, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("Your activity is higher than your usual morning pace. Hydration is the main gap currently.")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEvent(time: String, title: String, detail: String, type: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(58.dp)) {
            Text(time, color = Muted, fontSize = 9.sp)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        }
        Card(Modifier.weight(1f), shape = RoundedCornerShape(17.dp)) {
            Column(Modifier.padding(13.dp)) {
                Row {
                    Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(type, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Text(detail, color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PremiumOnboarding(
    step: Int,
    goal: String,
    diet: String,
    activity: String,
    onGoal: (String) -> Unit,
    onDiet: (String) -> Unit,
    onActivity: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val titles = listOf(
        "Let's make CaLeap yours",
        "What matters most to you?",
        "Tell us about your food",
        "Understand your routine",
        "Connect your health",
        "Your CaLeap baseline is ready"
    )
    val subtitles = listOf(
        "A few basics help CaLeap personalize your experience. You can change them later.",
        "Choose the outcome you want CaLeap to help you work toward.",
        "Your food preferences help us make suggestions that actually fit your life.",
        "Your routine gives context to food, sleep and activity.",
        "Connect only the data you are comfortable sharing.",
        "CaLeap now has a starting context. You can improve it over time."
    )

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CaLeap", color = Purple, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSkip) { Text("Skip") }
            }

            Spacer(Modifier.height(22.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / 6f },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(7.dp)),
                color = Purple, trackColor = PurpleSoft
            )
            Spacer(Modifier.height(30.dp))

            Text(titles[step], fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(subtitles[step], color = Muted, lineHeight = 20.sp)

            Spacer(Modifier.height(26.dp))

            when (step) {
                0 -> {
                    InputLike("Age", "29 years")
                    InputLike("Height", "165 cm")
                    InputLike("Current weight", "63.6 kg")
                }
                1 -> {
                    listOf("Eat and live healthier", "Boost energy and mood", "Stay motivated and consistent", "Feel better about my body").forEach {
                        Choice(it, goal == it) { onGoal(it) }
                    }
                }
                2 -> {
                    listOf("Balanced", "Vegetarian", "High-protein", "South Indian / regional", "Other").forEach {
                        Choice(it, diet == it) { onDiet(it) }
                    }
                }
                3 -> {
                    listOf("Mostly sitting", "Lightly active", "Moderately active", "Very active").forEach {
                        Choice(it, activity == it) { onActivity(it) }
                    }
                }
                4 -> {
                    ConnectCard("Health Connect", "Steps, activity and sleep", true)
                    ConnectCard("Wearable", "Optional device connection", false)
                    ConnectCard("Notifications", "Reminders and daily intelligence", true)
                }
                5 -> {
                    Card(colors = CardDefaults.cardColors(containerColor = GreenSoft), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("✓ Your baseline is ready", color = Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            BaselineRow("Goal", goal)
                            BaselineRow("Food preference", diet)
                            BaselineRow("Activity", activity)
                            BaselineRow("Daily target", "Personalized from your profile")
                            BaselineRow("Context", "Food + routine + activity + sleep")
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("CaLeap will ask for more only when it can improve an answer.", color = Muted, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Back") }
                }
                Button(onClick = onNext, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(if (step == 5) "Enter CaLeap" else "Continue")
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Your data stays under your control. Health information should be shared intentionally.",
                color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable private fun InputLike(label: String, value: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = Muted, fontSize = 11.sp)
                Text(value, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable private fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (selected) PurpleSoft else Color.White),
        border = if (selected) ButtonDefaults.outlinedButtonBorder else null,
        shape = RoundedCornerShape(17.dp)
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(if (selected) Purple else Color(0xFFE9E9EF)),
                contentAlignment = Alignment.Center
            ) { if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable private fun ConnectCard(title: String, subtitle: String, enabled: Boolean) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(if (enabled) GreenSoft else PurpleSoft), contentAlignment = Alignment.Center) {
                Icon(if (enabled) Icons.Default.Check else Icons.Default.Add, null, tint = if (enabled) Green else Purple)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Text(if (enabled) "Connected" else "Later", color = if (enabled) Green else Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun BaselineRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = Muted, modifier = Modifier.width(110.dp), fontSize = 11.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}
