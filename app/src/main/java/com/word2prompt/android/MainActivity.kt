package com.word2prompt.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.word2prompt.android.data.*
import com.word2prompt.android.ui.Word2PromptViewModel
import com.word2prompt.android.ui.theme.W2PTheme
import java.io.ByteArrayOutputStream
import kotlin.math.max

private enum class Screen { HOME, CREATE, PLAN, RESULT, LIBRARY, ACCOUNT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { W2PTheme { Word2PromptApp() } }
    }
}

@Composable
fun Word2PromptApp(vm: Word2PromptViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var showOnboarding by remember {
        mutableStateOf(!context.getSharedPreferences("w2p_ui", Context.MODE_PRIVATE).getBoolean("onboarding_complete", false))
    }
    var screen by remember { mutableStateOf(Screen.HOME) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showInspection by remember { mutableStateOf(false) }

    if (showOnboarding) {
        OnboardingScreen {
            context.getSharedPreferences("w2p_ui", Context.MODE_PRIVATE).edit().putBoolean("onboarding_complete", true).apply()
            showOnboarding = false
        }
        return
    }

    Scaffold(containerColor = Color(0xFFF6F7FB), topBar = {
        PremiumTopBar(state, onAccount = { screen = Screen.ACCOUNT })
    }, bottomBar = {
        PremiumBottomBar(screen) { screen = it }
    }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (screen) {
                Screen.HOME -> HomeScreen(state, onCreate = { screen = Screen.CREATE }, onAccount = { screen = Screen.ACCOUNT }, onPlans = { screen = Screen.ACCOUNT }, onTrend = { trend -> vm.startFromTrend(trend); screen = Screen.CREATE })
                Screen.CREATE -> CreateScreen(state, vm) { screen = Screen.PLAN }
                Screen.PLAN -> PlanScreen(state, vm) { screen = Screen.RESULT }
                Screen.RESULT -> ResultScreen(state, vm, showAdvanced, { showAdvanced = !showAdvanced }, showInspection, { showInspection = !showInspection }, context)
                Screen.LIBRARY -> LibraryScreen(state, vm, onCreate = { screen = Screen.CREATE }, onOpen = { screen = Screen.RESULT })
                Screen.ACCOUNT -> AccountScreen(state, vm)
            }
        }
    }
}

@Composable
private fun OnboardingScreen(onComplete: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple("From idea to creation", "Word2Prompt understands what you mean before it prepares the AI-ready work.", Icons.Default.AutoAwesome),
        Triple("Intelligence when it matters", "W2P can plan, ask focused questions, research when useful, check quality and prepare a supervised creation flow.", Icons.Default.Psychology),
        Triple("You stay in control", "Review the brief, prompt and quality signals before handing work to another AI or taking a consequential action.", Icons.Default.VerifiedUser)
    )
    val item = pages[page]
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101D45), Color(0xFF5C4DDA)))).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF4D74FF), Color(0xFF9B68FF)))), contentAlignment = Alignment.Center) {
                Icon(item.third, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(26.dp))
            Text("Word2Prompt", color = Color.White.copy(alpha = .72f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
            Text(item.first, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 36.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(item.second, color = Color.White.copy(alpha = .82f), fontSize = 15.sp, lineHeight = 23.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { pages.indices.forEach { i -> Box(Modifier.size(if (i == page) 24.dp else 7.dp, 7.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = if (i == page) .95f else .35f))) } }
            Spacer(Modifier.height(30.dp))
            Button(onClick = { if (page == pages.lastIndex) onComplete() else page++ }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF3346B8))) {
                Text(if (page == pages.lastIndex) "Get started" else "Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            if (page < pages.lastIndex) {
                TextButton(onClick = onComplete) { Text("Skip", color = Color.White.copy(alpha = .78f)) }
            }
        }
    }
}

@Composable
private fun PremiumTopBar(state: W2PState, onAccount: () -> Unit) {
    Surface(color = Color(0xFFF6F7FB)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFF315BFF), Color(0xFF7C4DFF)))), contentAlignment = Alignment.Center) {
                Text("W2P", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("Word2Prompt", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("AI Intent & Creation Assistant", fontSize = 10.sp, color = Color(0xFF727B90))
            }
            Surface(shape = RoundedCornerShape(50), color = Color.White, shadowElevation = 1.dp, modifier = Modifier.clickable(onClick = onAccount)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF52607A), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (state.session.authenticated) "Account" else "Sign in", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PremiumBottomBar(screen: Screen, onNavigate: (Screen) -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            PremiumNavItem("Home", Icons.Default.Home, screen == Screen.HOME) { onNavigate(Screen.HOME) }
            PremiumNavItem("Create", Icons.Default.AutoAwesome, screen == Screen.CREATE) { onNavigate(Screen.CREATE) }
            PremiumNavItem("Library", Icons.Default.Folder, screen == Screen.LIBRARY) { onNavigate(Screen.LIBRARY) }
            PremiumNavItem("Account", Icons.Default.Person, screen == Screen.ACCOUNT) { onNavigate(Screen.ACCOUNT) }
        }
    }
}

@Composable
private fun RowScope.PremiumNavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = if (selected) Color(0xFF4B55E8) else Color(0xFF7D879A), modifier = Modifier.size(21.dp))
        Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) Color(0xFF4B55E8) else Color(0xFF7D879A))
    }
}

@Composable
private fun HomeScreen(state: W2PState, onCreate: () -> Unit, onAccount: () -> Unit, onPlans: () -> Unit, onTrend: (TrendingItem) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF17295D), Color(0xFF5A4ED4), Color(0xFF7A4DFF)))).padding(22.dp)) {
            Column {
                Text(if (state.session.authenticated) "Welcome back${state.session.user?.fullName?.let { if (it.isNotBlank()) ", $it" else "" } ?: ""}" else "Welcome to Word2Prompt", color = Color.White.copy(alpha = .78f), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("Turn what you mean into what AI understands.", color = Color.White, fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onCreate, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF263B9E)), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(7.dp)); Text("Create with W2P", fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(16.dp))
        UsageCard(state, onAccount = onAccount, onPlans = onPlans)
        Spacer(Modifier.height(16.dp))
        Text("Create anything", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickCard("Image", Icons.Default.Image, "Visual creation", Modifier.weight(1f), onCreate)
            QuickCard("Video", Icons.Default.Movie, "Stories & clips", Modifier.weight(1f), onCreate)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickCard("Writing", Icons.Default.EditNote, "Content & copy", Modifier.weight(1f), onCreate)
            QuickCard("Code", Icons.Default.Code, "Build & fix", Modifier.weight(1f), onCreate)
        }
        if (state.trends.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trending creation ideas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Live", color = Color(0xFF4F55E8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            state.trends.take(3).forEach { trend ->
                TrendingCard(trend) { onTrend(trend) }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        SectionCard("The W2P loop", "Intent → Plan → Research when needed → Creative Brief → Prompt → Create → Evaluate → Improve")
        Spacer(Modifier.height(12.dp))
        SectionCard("Built on V8.3 Dynamic Intelligence", "Android uses the same W2P backend intelligence as the web product. No provider secrets or GPU dependency are placed in the app.")
    }
}

@Composable
private fun TrendingCard(trend: TrendingItem, onCreate: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onCreate), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFF0EEFF)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF5A50D6), modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(trend.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(trend.category, color = Color(0xFF5A50D6), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(trend.hook, color = Color(0xFF737D90), fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp), maxLines = 2)
            }
            Text("${trend.score}", color = Color(0xFF4F55E8), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun UsageCard(state: W2PState, onAccount: () -> Unit, onPlans: () -> Unit) {
    val u = state.usage
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (u.subscriptionPlan != null) "Pro plan" else if (state.session.authenticated) "Free account" else "Guest trial", fontWeight = FontWeight.Bold)
                    Text(if (u.subscriptionPlan != null) "Unlimited prompt usage is managed by your active subscription." else "${u.freeRemaining} free prompts remaining", color = Color(0xFF737D90), fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
                Text(if (u.subscriptionPlan != null) "PRO" else "FREE", color = Color(0xFF5A50D6), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onAccount, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Text(if (state.session.authenticated) "Account" else "Sign in") }
                Button(onClick = onPlans, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Text(if (u.subscriptionPlan != null) "Manage plan" else "Go Pro") }
            }
        }
    }
}

@Composable
private fun QuickCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, null, tint = Color(0xFF4F55E8), modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp)); Text(title, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 11.sp, color = Color(0xFF7C8597), modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun CreateScreen(state: W2PState, vm: Word2PromptViewModel, openPlan: () -> Unit) {
    val context = LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uriToDataUrl(context, uri)?.let { vm.setSourceImage(it.first, it.second) }
    }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("CREATE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("What do you want to create?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("W2P understands the intent before generating the AI-ready creation package.", color = Color(0xFF737D90), fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(14.dp)) {
                OutlinedTextField(value = state.idea, onValueChange = { vm.setIdea(it); vm.suggest(it.takeLast(24)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 145.dp), placeholder = { Text("Example: Create a cinematic forest trekking image for children") }, label = { Text("Your requirement") }, shape = RoundedCornerShape(18.dp))
                if (state.commandSuggestions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp)); Text("Smart commands", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    state.commandSuggestions.take(4).forEach { c -> Text("/${c.name}  ${c.description}", modifier = Modifier.fillMaxWidth().clickable { vm.setIdea("/${c.name} ") }.padding(vertical = 7.dp), color = Color(0xFF4B55E8), fontSize = 12.sp) }
                }
                Spacer(Modifier.height(12.dp))
                Text("Inputs", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) {
                    OutlinedButton(onClick = { imageLauncher.launch("image/*") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text(if (state.sourceImageName == null) "Add source image" else "Replace image", fontSize = 11.sp)
                    }
                    if (state.sourceImageName != null) {
                        OutlinedButton(onClick = { vm.clearSourceImage() }, shape = RoundedCornerShape(13.dp)) { Icon(Icons.Default.Close, null, modifier = Modifier.size(17.dp)); Text("Remove", fontSize = 11.sp) }
                    }
                }
                state.sourceImageName?.let { Text("Source of truth: $it", color = Color(0xFF4F55E8), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)) }
                Spacer(Modifier.height(12.dp)); Text("Output", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); ChipWrap(listOf("Let W2P choose", "Image", "Video", "Writing", "Document", "Code"), state.output) { vm.setOutput(it) }
                Spacer(Modifier.height(10.dp)); Text("Destination", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); ChipWrap(listOf("Let W2P choose", "ChatGPT", "Gemini", "Grok", "Perplexity", "Image AI tool", "Video AI tool"), state.destination) { vm.setDestination(it) }
                Spacer(Modifier.height(14.dp)); Button(onClick = { vm.buildPlan(openPlan) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Understand & Plan", fontWeight = FontWeight.Bold) }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
    }
}

@Composable
private fun ChipWrap(options: List<String>, selected: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        options.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { row.forEach { value -> AssistChip(onClick = { onPick(value) }, label = { Text(value, fontSize = 11.sp) }, leadingIcon = if (value == selected) ({ Icon(Icons.Default.Check, null, modifier = Modifier.size(15.dp)) }) else null) } } }
    }
}

@Composable
private fun PlanScreen(state: W2PState, vm: Word2PromptViewModel, openResult: () -> Unit) {
    val plan = state.plan
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        PipelineStatusCard(state)
        Spacer(Modifier.height(10.dp))
        Text("INTENT ENGINE 2.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp)); Text(plan?.taskType?.replaceFirstChar { it.uppercase() } ?: "Planning", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(plan?.goal ?: "Build a plan from the customer requirement.", color = Color(0xFF737D90))
        state.intentAnalysis?.let { a ->
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Intent: ${a.intentName.replace('_',' ')}", fontWeight = FontWeight.SemiBold)
                        Text("${a.confidence}% confidence", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                    }
                    Text("Requirement completeness: ${a.completeness}%", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Text("Output: ${a.briefOutput} • Destination: ${a.briefDestination}", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                    if (a.researchNeeded) Text("Research: ${a.researchReason}", color = Color(0xFF4F55E8), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
        if (state.smartQuestions.isNotEmpty()) { Spacer(Modifier.height(14.dp)); SectionTitle("Important questions"); state.smartQuestions.forEach { q -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(14.dp)) { Text(q.question, fontWeight = FontWeight.SemiBold); Text(q.reason, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp)); Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { q.options.take(3).forEach { option -> AssistChip(onClick = { vm.setAnswer(q.key, option) }, label = { Text(option, fontSize = 11.sp) }) } } } } } }
        Spacer(Modifier.height(12.dp)); plan?.capabilities?.let { CapabilityRow(it) }; Spacer(Modifier.height(12.dp)); plan?.researchAngles?.takeIf { it.isNotEmpty() }?.let { SectionCard("Multi-angle research", it.joinToString(" • ")); Spacer(Modifier.height(12.dp)) }
        plan?.nodes?.forEachIndexed { i, n -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(14.dp)) { Text("${i + 1}. ${n.title}", fontWeight = FontWeight.SemiBold); Text(n.action, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)); if (n.requiresApproval) Text("Approval checkpoint", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) } } }
        Spacer(Modifier.height(14.dp)); Button(onClick = { vm.buildResult(openResult) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Run Intelligence & Build Result", fontWeight = FontWeight.Bold) }; state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
    }
}

@Composable
private fun PipelineStatusCard(state: W2PState) {
    val active = state.pipelineStage != PipelineStage.IDLE
    val title = when (state.pipelineStage) {
        PipelineStage.CHECKING_USAGE -> "Checking access"
        PipelineStage.BUILDING_CONTEXT -> "Understanding intent"
        PipelineStage.RESEARCHING -> "Research decision"
        PipelineStage.BUILDING_CREATIVE_PLAN -> "Planning creation"
        PipelineStage.GENERATING_PROMPT -> "Creating AI-ready work"
        PipelineStage.QUALITY_CHECK -> "Quality check"
        PipelineStage.READY -> "Ready for review"
        PipelineStage.BLOCKED -> "Action required"
        PipelineStage.FAILED -> "Fallback result"
        else -> "W2P pipeline"
    }
    if (active) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (state.pipelineStage == PipelineStage.READY) Color(0xFFEFFBF4) else Color.White)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (state.pipelineStage == PipelineStage.READY) Icons.Default.CheckCircle else Icons.Default.AutoAwesome, null, tint = if (state.pipelineStage == PipelineStage.READY) Color(0xFF1F9D62) else Color(0xFF4F55E8), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); state.pipelineElapsedMs?.let { Text("${it / 1000.0}s", fontSize = 10.sp, color = Color.Gray) }
                }
                if (state.pipelineMessage.isNotBlank()) Text(state.pipelineMessage, color = Color(0xFF737D90), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable
private fun ResultScreen(state: W2PState, vm: Word2PromptViewModel, advanced: Boolean, toggleAdvanced: () -> Unit, inspection: Boolean, toggleInspection: () -> Unit, context: Context) {
    val prompt = state.finalPrompt
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) uriToDataUrl(context, uri)?.let { vm.inspectResult(it.first, it.second) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text("AI-READY RESULT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(if (state.promptMeta?.mode == "model") "Model optimized" else "Ready", fontWeight = FontWeight.SemiBold) }; AssistChip(onClick = toggleAdvanced, label = { Text(if (advanced) "Hide intelligence" else "More intelligence") }, leadingIcon = { Icon(Icons.Default.Tune, null) }) }
        Spacer(Modifier.height(8.dp)); Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.fillMaxSize().padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text("Your prompt", fontWeight = FontWeight.SemiBold); Text("AI-ready", fontSize = 11.sp, color = Color.Gray) }; Spacer(Modifier.height(8.dp)); Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { Text(prompt, fontSize = 15.sp, lineHeight = 26.sp) } } }
        Spacer(Modifier.height(8.dp)); Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { OutlinedButton(onClick = { copyToClipboard(context, prompt) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(4.dp)); Text("Copy") }; OutlinedButton(onClick = { shareText(context, prompt) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(4.dp)); Text("Share") }; OutlinedButton(onClick = { vm.saveCurrentToLibrary() }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.BookmarkAdd, null); Spacer(Modifier.width(4.dp)); Text("Save") } }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { OutlinedButton(onClick = vm::improve, modifier = Modifier.weight(1f)) { Icon(Icons.Default.AutoFixHigh, null); Spacer(Modifier.width(4.dp)); Text("Improve") }; Button(onClick = { vm.createJob() }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Create") } }
        }
        Spacer(Modifier.height(6.dp)); OutlinedButton(onClick = toggleInspection, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Visibility, null); Spacer(Modifier.width(6.dp)); Text(if (inspection) "Hide Result Inspection" else "Inspect Generated Result") }
        OutlinedButton(onClick = vm::runGhostCheck, modifier = Modifier.fillMaxWidth()) { Text("Ghost Check • Find hidden failures") }
        if (inspection) { Card(Modifier.fillMaxWidth().padding(top = 6.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(12.dp)) { Text("Result Intelligence", fontWeight = FontWeight.SemiBold); Text("Upload the generated image and W2P will compare it against the original requirement.", color = Color.Gray, fontSize = 12.sp); Spacer(Modifier.height(8.dp)); Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text(if (state.inspectionBusy) "Inspecting…" else "Choose generated image") }; state.resultIntelligence?.let { r -> Spacer(Modifier.height(8.dp)); Text("${r.overall.replace('_', ' ').uppercase()}${r.score?.let { " • $it/100" } ?: ""}", fontWeight = FontWeight.Bold); Text(r.summary, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)); if (r.mismatches.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text("Verified mismatches", fontWeight = FontWeight.SemiBold); r.mismatches.take(5).forEach { Text("• $it", fontSize = 12.sp) }; Spacer(Modifier.height(6.dp)); Button(onClick = { vm.autoFixFromInspection() }, modifier = Modifier.fillMaxWidth()) { Text("Auto-Fix Verified Mismatches") } } } } } }
        state.ghostCheck?.let { g ->
            Spacer(Modifier.height(6.dp)); Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(12.dp)) { Text("Ghost Check", fontWeight = FontWeight.SemiBold); Text(g.status.replace('_',' ').uppercase(), fontWeight = FontWeight.Bold, color = if (g.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error); g.blockers.take(3).forEach { Text("• $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }; g.warnings.take(3).forEach { Text("• $it", fontSize = 12.sp, color = Color.Gray) }; OutlinedButton(onClick = vm::runGhostCheck, modifier = Modifier.fillMaxWidth()) { Text("Run Ghost Check Again") } } }
        }
        if (advanced) { Spacer(Modifier.height(6.dp)); state.promptQuality?.let { q -> SectionCard("Prompt quality", "${q.grade} • ${q.score}/100\n" + q.issues.take(3).joinToString("\n") { "• $it" }) }; state.research?.takeIf { it.needed }?.let { r -> Spacer(Modifier.height(6.dp)); SectionCard("Research", "${r.confidence} confidence\n${r.topics.take(5).joinToString(" • ")}") }; state.creationPlan?.let { c -> Spacer(Modifier.height(6.dp)); SectionCard("Creation Intelligence", "${c.readiness} • next: ${c.nextAction}") } }
        state.creationStatus?.let { Text("Creation job: $it", modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.primary) }; state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
    }
}

@Composable
private fun LibraryScreen(state: W2PState, vm: Word2PromptViewModel, onCreate: () -> Unit, onOpen: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("LIBRARY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("Your creation workspace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Saved W2P results stay available on this device until you remove app data.", color = Color(0xFF737D90), fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        if (state.library.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, null, tint = Color(0xFF5961E9), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(10.dp)); Text("Your library is ready", fontWeight = FontWeight.Bold)
                    Text("Save a result from the workspace and it will appear here.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(14.dp)); Button(onClick = onCreate, shape = RoundedCornerShape(14.dp)) { Text("Create something") }
                }
            }
        } else {
            state.library.forEach { item ->
                Card(Modifier.fillMaxWidth().padding(bottom = 9.dp).clickable { vm.openLibraryItem(item) ; onOpen() }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, null, tint = Color(0xFF5A50D6), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                            Text(item.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(item.quality, color = Color(0xFF5A50D6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(item.preview, color = Color(0xFF737D90), fontSize = 12.sp, lineHeight = 18.sp, maxLines = 4, modifier = Modifier.padding(top = 8.dp))
                        Text(item.output + " • " + item.destination, color = Color(0xFF8A92A2), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountScreen(state: W2PState, vm: Word2PromptViewModel) {
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var passwordVisible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("ACCOUNT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(if (state.session.authenticated) "Your Word2Prompt account" else "Create your Word2Prompt account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(if (state.session.authenticated) state.session.user?.email.orEmpty() else "Save your workspace, unlock your free allowance and manage your plan.", color = Color(0xFF737D90))
        Spacer(Modifier.height(16.dp))
        if (!state.session.authenticated) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(16.dp)) {
                Text("Email account", fontWeight = FontWeight.Bold); Text("Use the same W2P account across the web and Android.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                Spacer(Modifier.height(12.dp)); OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name (optional)") }, singleLine = true); Spacer(Modifier.height(8.dp)); OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true); Spacer(Modifier.height(8.dp)); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password (8+ characters)") }, singleLine = true, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
                Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { Button(onClick = { vm.login(email, password) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Text("Sign in") }; OutlinedButton(onClick = { vm.register(email, password, name) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) { Text("Create account") } }
                Spacer(Modifier.height(10.dp)); Text("Google sign-in is already supported by the W2P web account system. Native Google credential wiring will be added only after the Android OAuth client ID is configured; no fake credential flow is used here.", color = Color(0xFF7A8496), fontSize = 11.sp)
            } }
        } else {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(16.dp)) { Text(state.session.user?.fullName?.ifBlank { "W2P member" } ?: "W2P member", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text(state.session.user?.email.orEmpty(), color = Color.Gray, fontSize = 12.sp); Spacer(Modifier.height(14.dp)); Text("Free prompts remaining: ${state.usage.freeRemaining}"); Text("Paid credits: ${state.usage.paidCredits}", modifier = Modifier.padding(top = 5.dp)); Text(if (state.usage.subscriptionPlan != null) "Active plan: ${state.usage.subscriptionPlan}" else "No active subscription", modifier = Modifier.padding(top = 5.dp)); Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") } } }
        }
        Spacer(Modifier.height(16.dp)); Text("Plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
        PlanCard("Free", "₹0", "10 prompts after account creation", false) {}
        Spacer(Modifier.height(8.dp)); PlanCard("Pro Monthly", "₹49/month launch offer", "Higher usage, premium creation workflow and plan entitlement.", true) { if (state.session.authenticated) vm.createBillingOrder("monthly") else vm.login(email, password) }
        Spacer(Modifier.height(8.dp)); PlanCard("Pro Yearly", "₹499/year launch offer", "Annual plan with the same premium workspace entitlement.", true) { if (state.session.authenticated) vm.createBillingOrder("yearly") else vm.login(email, password) }
        Spacer(Modifier.height(8.dp)); PlanCard("Prompt Credit", "₹1", "Add one paid prompt credit when you need it.", false) { if (state.session.authenticated) vm.createBillingOrder("payg_prompt") else vm.login(email, password) }
        state.billingMessage?.let { Spacer(Modifier.height(12.dp)); SectionCard("Billing status", it) }
        state.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(16.dp)); SectionCard("Payment safety", "The Android app creates an order through the W2P backend. It never contains provider secret keys. The current V8.3.2 backend exposes the billing order contract, but production provider checkout/webhook verification must be configured before real money is accepted.")
    }
}

@Composable
private fun PlanCard(title: String, price: String, body: String, featured: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (featured) Color(0xFFF1F0FF) else Color.White)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(price, color = Color(0xFF4F55E8), fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 3.dp)) }; if (featured) Text("PRO", color = Color(0xFF5A50D6), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp) }; Text(body, color = Color(0xFF737D90), fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp)); Spacer(Modifier.height(10.dp)); Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) { Text(if (featured) "Choose Pro" else "Add credit") } } }
}

@Composable private fun SectionTitle(text: String) { Text(text, fontWeight = FontWeight.SemiBold) }
@Composable private fun CapabilityRow(items: List<String>) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { items.take(3).forEach { AssistChip(onClick = {}, label = { Text(it, fontSize = 11.sp) }) } } }
@Composable private fun SectionCard(title: String, body: String) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(body, color = Color.Gray, modifier = Modifier.padding(top = 4.dp), lineHeight = 18.sp) } } }
private fun copyToClipboard(context: Context, text: String) { val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cm.setPrimaryClip(ClipData.newPlainText("Word2Prompt", text)) }

private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(send, "Share from Word2Prompt"))
}

private fun uriToDataUrl(context: Context, uri: Uri): Pair<String, String>? = runCatching {
    val input = context.contentResolver.openInputStream(uri) ?: return null; val source = input.use { BitmapFactory.decodeStream(it) ?: return null }; val scale = minOf(1f, 1400f / max(source.width, source.height).toFloat()); val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true) else source; val out = ByteArrayOutputStream(); bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, out); "data:image/jpeg;base64,${Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)}" to (uri.lastPathSegment ?: "generated-result.jpg")
}.getOrNull()
