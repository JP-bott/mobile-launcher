package com.liquidlauncher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The artwork below is original and category-inspired, not copied from Apple assets.
// Unknown apps keep their own Android icons for identification.
data class IconLook(val symbol: String, val colors: List<Color>, val foreground: Color = Color.White)

object IconTheme {
    fun forApp(app: LauncherApp): IconLook? {
        val p = app.packageName.lowercase()
        val name = app.label.lowercase()
        return when {
            "dialer" in p || "phone" in name || "dialer" in name ->
                IconLook("☎", listOf(Color(0xFF54E36C), Color(0xFF20B747)))
            "messag" in p || "sms" in p || "messages" in name ->
                IconLook("●", listOf(Color(0xFF63E66E), Color(0xFF27B54B)))
            "camera" in p || name == "camera" ->
                IconLook("●", listOf(Color(0xFFFAFAFF), Color(0xFFE8EAF1)), Color(0xFF252B39))
            "settings" in p || name == "settings" ->
                IconLook("⚙", listOf(Color(0xFFB8BEC9), Color(0xFF6E7789)))
            "gallery" in p || "photos" in name || name == "gallery" ->
                IconLook("✿", listOf(Color(0xFFFDFDFF), Color(0xFFE8EDF8)), Color(0xFFEC658C))
            "calendar" in p || name == "calendar" ->
                IconLook("31", listOf(Color(0xFFFFFFFF), Color(0xFFF1F3F8)), Color(0xFF2C3442))
            "clock" in p || name == "clock" ->
                IconLook("◷", listOf(Color(0xFF303641), Color(0xFF11151C)))
            "calculator" in p || name == "calculator" ->
                IconLook("＋", listOf(Color(0xFF404650), Color(0xFF20252D)))
            "music" in name && "play" !in p ->
                IconLook("♫", listOf(Color(0xFFFF5B7E), Color(0xFFDA2E55)))
            "browser" in name || "chrome" in p || "firefox" in p ->
                IconLook("◎", listOf(Color(0xFF5BCAFA), Color(0xFF287ADE)))
            else -> null
        }
    }
}

@Composable
fun ThemedAppIcon(
    app: LauncherApp,
    iconSize: Int,
    themed: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape((iconSize * .25f).dp)
    val look = if (themed) IconTheme.forApp(app) else null
    Box(
        modifier.size(iconSize.dp).shadow(2.dp, shape).clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (look == null) {
            Image(
                bitmap = remember(app.key) { app.icon.bitmapForIcon(160).asImageBitmap() },
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.linearGradient(look.colors))
                    .border(.5.dp, Color.White.copy(alpha = .22f), shape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = look.symbol,
                    color = look.foreground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (iconSize * .48f).sp,
                    maxLines = 1
                )
            }
        }
    }
}
