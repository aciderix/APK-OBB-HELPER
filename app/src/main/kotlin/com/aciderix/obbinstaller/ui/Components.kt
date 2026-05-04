package com.aciderix.obbinstaller.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FileSourceCard(
    title: String,
    statusLabel: String,
    isSelected: Boolean,
    icon: ImageVector,
    pickHint: String,
    formatHint: String,
    onPick: () -> Unit,
    enabled: Boolean,
    canChange: Boolean
) {
    val transition = rememberInfiniteTransition(label = "card-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card-pulse"
    )
    val borderColor = if (isSelected) HubColors.BorderActive
                      else HubColors.Border.copy(alpha = pulse * 0.6f + 0.2f)
    val accent = if (isSelected) HubColors.Primary else HubColors.AccentSoft

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(HubColors.Surface)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, accent.copy(alpha = 0.4f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) HubColors.Primary else HubColors.TextSecondary
                    )
                }
            }

            if (canChange) {
                PickerSlot(onClick = onPick, enabled = enabled, hint = pickHint, format = formatHint)
            }
        }
    }
}

@Composable
private fun PickerSlot(onClick: () -> Unit, enabled: Boolean, hint: String, format: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HubColors.SurfaceMuted)
            .border(BorderStroke(1.dp, HubColors.Border), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = HubColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(hint, style = MaterialTheme.typography.labelLarge, color = HubColors.Primary)
            }
            Text(format, style = MaterialTheme.typography.labelSmall, color = HubColors.TextMuted)
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    body: String,
    icon: ImageVector,
    accent: Color = HubColors.Primary
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HubColors.Surface)
            .border(BorderStroke(1.dp, HubColors.Border), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = accent)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
