package com.aciderix.obbinstaller.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aciderix.obbinstaller.R

@Composable
fun AboutScreen() {
    val ctx = LocalContext.current
    val versionName = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(HubColors.Surface)
                .border(BorderStroke(1.dp, HubColors.Border), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HubColors.SurfaceMuted)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painter = painterResource(R.drawable.launcher_image))
                }
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.about_version, versionName),
                        style = MaterialTheme.typography.bodySmall,
                        color = HubColors.TextMuted
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(HubColors.Surface)
                .border(BorderStroke(1.dp, HubColors.Border), RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.about_intro), style = MaterialTheme.typography.bodyLarge)
        }

        SectionCard(title = stringResource(R.string.about_features_title), icon = androidx.compose.material.icons.Icons.Outlined.AutoAwesome) {
            BulletItem(stringResource(R.string.about_feature_1))
            BulletItem(stringResource(R.string.about_feature_2))
            BulletItem(stringResource(R.string.about_feature_3))
            BulletItem(stringResource(R.string.about_feature_4))
        }

        SectionCard(title = stringResource(R.string.about_compat_title), icon = androidx.compose.material.icons.Icons.Outlined.Devices) {
            Text(stringResource(R.string.about_compat_body), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun HelpScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.help_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        FaqItem(stringResource(R.string.help_q1), stringResource(R.string.help_a1))
        FaqItem(stringResource(R.string.help_q2), stringResource(R.string.help_a2))
        FaqItem(stringResource(R.string.help_q3), stringResource(R.string.help_a3))
        FaqItem(stringResource(R.string.help_q4), stringResource(R.string.help_a4))
        FaqItem(stringResource(R.string.help_q5), stringResource(R.string.help_a5))
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    body: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HubColors.Surface)
            .border(BorderStroke(1.dp, HubColors.Border), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                        .background(HubColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = HubColors.Primary, modifier = Modifier.size(16.dp))
                }
                Text(title, style = MaterialTheme.typography.titleSmall, color = HubColors.Primary)
            }
            body()
        }
    }
}

@Composable
private fun BulletItem(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier.size(6.dp).clip(CircleShape).background(HubColors.Primary)
                .offset(y = 8.dp)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = HubColors.TextPrimary)
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HubColors.Surface)
            .border(BorderStroke(1.dp, HubColors.Border), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (expanded) 8.dp else 0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(question, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = HubColors.TextSecondary
                )
            }
            if (expanded) {
                Text(answer, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Image(painter: androidx.compose.ui.graphics.painter.Painter) {
    androidx.compose.foundation.Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
    )
}
