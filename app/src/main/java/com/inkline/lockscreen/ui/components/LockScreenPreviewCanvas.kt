package com.inkline.lockscreen.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.inkline.lockscreen.data.db.TaskEntity
import com.inkline.lockscreen.data.prefs.ThemeSettings
import com.inkline.lockscreen.wallpaper.InkLineBitmapRenderer
import java.time.LocalDate

/**
 * 1:1 live lock-screen preview rendered directly through [InkLineBitmapRenderer].
 * Contains ONLY the DAY and TASK OF THE DAY elements with thin decorative lines —
 * zero dots, circles, clocks, battery, network, weather, or unlock text.
 */
@Composable
fun InkLinePreviewCard(
    today: LocalDate,
    todayTask: TaskEntity?,
    settings: ThemeSettings,
    modifier: Modifier = Modifier
) {
    val previewBitmap = remember(today, todayTask, settings) {
        val previewWidthPx = 450
        val previewHeightPx = 975 // 9:19.5 phone aspect ratio
        val previewDensity = previewWidthPx / 390f
        InkLineBitmapRenderer.render(
            widthPx = previewWidthPx,
            heightPx = previewHeightPx,
            density = previewDensity,
            today = today,
            todayTask = todayTask,
            settings = settings
        ).asImageBitmap()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(settings.backgroundColor))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
    ) {
        Image(
            bitmap = previewBitmap,
            contentDescription = "clock.in lock screen preview",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
    }
}
