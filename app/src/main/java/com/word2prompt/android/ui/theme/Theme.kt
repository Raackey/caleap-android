package com.word2prompt.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val W2PBlue = Color(0xFF355CFF)
private val W2PPurple = Color(0xFF7B4DFF)
private val Scheme = lightColorScheme(primary = W2PBlue, secondary = W2PPurple, tertiary = Color(0xFF5D63E8), background = Color(0xFFF6F7FB), surface = Color.White)

@Composable
fun W2PTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography().copy(titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold), bodyLarge = Typography().bodyLarge.copy(lineHeight = Typography().bodyLarge.lineHeight)), content = content)
}
