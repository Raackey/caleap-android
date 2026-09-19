package com.maisor.caleap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFFF8FAFC)
private val Ink = Color(0xFF172033)
private val Muted = Color(0xFF667085)
private val Accent = Color(0xFF635BFF)
private val Mint = Color(0xFFE8F7F1)
private val Lavender = Color(0xFFF0EDFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CaLeapApp() }
    }
}

@Composable
fun CaLeapApp() {
    var showSheet by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(0) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent,
            background = Bg,
            surface = Color.White,
            onBackground = Ink,
            onSurface = Ink
        )
    ) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    val items = listOf(
                        "Home" to Icons.Default.Home,
                        "Timeline" to Icons.Default.Timeline,
                        "Family" to Icons.Default.People,
                        "Profile" to Icons.Default.Person
                    )
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selected == index,
                            onClick = { selected = index },
                            icon = { Icon(item.second, null) },
                            label = { Text(item.first) }
                        )
                    }
                }
            }
        ) { padding ->
            when (selected) {
                0 -> HomeScreen(Modifier.padding(padding), onShow = { showSheet = true })
                1 -> TimelineScreen(Modifier.padding(padding))
                2 -> FamilyScreen(Modifier.padding(padding))
                else -> ProfileScreen(Modifier.padding(padding))
            }
        }

        if (showSheet) {
            ShowCaLeapSheet(onDismiss = { showSheet = false })
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier, onShow: () -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Good morning 👋", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("How can I help?", color = Muted, fontSize = 15.sp)
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Lavender),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FavoriteBorder, null, tint = Accent)
                }
            }
        }

        item {
            Button(
                onClick = onShow,
                modifier = Modifier.fillMaxWidth().height(62.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.AddCircle, null)
                Spacer(Modifier.width(10.dp))
                Text("SHOW CALEAP", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }

        item { SectionTitle("Today") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("🍽️", "Food", "Not logged", Modifier.weight(1f))
                MetricCard("🚶", "Activity", "6,240 steps", Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("😴", "Sleep", "7h 12m", Modifier.weight(1f))
                MetricCard("💧", "Water", "1.4 L", Modifier.weight(1f))
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Mint),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("🧠 Today's Insight", fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your activity is looking good today. Add a protein-rich food to your next meal.",
                        color = Ink,
                        lineHeight = 21.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("AI estimate • Personalised", color = Muted, fontSize = 12.sp)
                }
            }
        }

        item { SectionTitle("Quick actions") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("📸", "Scan", onShow, Modifier.weight(1f))
                QuickAction("🎤", "Tell", onShow, Modifier.weight(1f))
                QuickAction("📄", "Report", onShow, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MetricCard(icon: String, title: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
fun QuickAction(icon: String, label: String, action: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = action,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 20.sp)
            Text(label, fontSize = 12.sp)
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowCaLeapSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("What do you want CaLeap to understand?", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("Show it something. CaLeap will turn it into useful information.", color = Muted)

            val actions = listOf(
                "📸" to "Photo",
                "🎤" to "Voice",
                "📄" to "Report",
                "🍎" to "Food",
                "❤️" to "Health",
                "💊" to "Medicine",
                "🏃" to "Activity"
            )
            actions.forEach { (icon, label) ->
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("$icon  $label", fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
fun TimelineScreen(modifier: Modifier) {
    val events = listOf(
        "08:20" to "Morning walk • 2,140 steps",
        "09:05" to "Breakfast • Not analysed yet",
        "11:30" to "Water • 500 ml",
        "13:10" to "Activity synced from Health Connect"
    )
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Timeline", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        item { Text("Your day, in one simple stream.", color = Muted) }
        items(events) { (time, event) ->
            Card(shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(time, color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp))
                    Text(event)
                }
            }
        }
    }
}

@Composable
fun FamilyScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("CaLeap Family", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Shared habits with private health profiles.", color = Muted)
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Family Table", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Dinner was shared by 3 family members.")
                Text("Shared meals this week: 4", color = Muted)
            }
        }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PersonAdd, null)
            Spacer(Modifier.width(8.dp))
            Text("Add family member")
        }
    }
}

@Composable
fun ProfileScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Personalisation", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Food preferences")
                Text("Activity goals")
                Text("Health data permissions")
                Text("Family sharing")
            }
        }
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("Privacy & permissions")
        }
    }
}
