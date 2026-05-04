package com.aciderix.obbinstaller.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aciderix.obbinstaller.Phase
import com.aciderix.obbinstaller.R
import com.aciderix.obbinstaller.UiState

@Composable
fun HomeScreen(
    state: UiState,
    onPickApk: () -> Unit,
    onPickObb: () -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onOpenUnknownSources: () -> Unit
) {
    val ctx = LocalContext.current
    val canStart = state.apk != null && state.canInstallUnknown &&
        state.phase in setOf(Phase.Idle, Phase.Done, Phase.Error)
    val isRunning = state.phase in setOf(Phase.Staging, Phase.Patching, Phase.InstallingApk)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AnimatedVisibility(
            visible = !state.canInstallUnknown,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            UnknownSourcesCard(onOpenSettings = onOpenUnknownSources)
        }

        Stagger(visible = visible, index = 0) {
            FileSourceCard(
                title = stringResource(R.string.card_apk),
                statusLabel = state.apk?.let {
                    when {
                        state.bundledApk != null -> stringResource(R.string.bundled_label, it.displayName)
                        else -> stringResource(R.string.selected_label, it.displayName)
                    }
                } ?: stringResource(R.string.no_file_selected),
                isSelected = state.apk != null,
                icon = Icons.Outlined.Android,
                pickHint = stringResource(R.string.pick_file),
                formatHint = stringResource(R.string.format_apk),
                onPick = onPickApk,
                enabled = !isRunning,
                canChange = state.bundledApk == null
            )
        }

        Stagger(visible = visible, index = 1) {
            FileSourceCard(
                title = stringResource(R.string.card_obb),
                statusLabel = state.obb?.let {
                    when {
                        state.bundledObb != null -> stringResource(R.string.bundled_label, it.displayName)
                        else -> stringResource(R.string.selected_label, it.displayName)
                    }
                } ?: stringResource(R.string.no_file_selected),
                isSelected = state.obb != null,
                icon = Icons.Outlined.Inventory,
                pickHint = stringResource(R.string.pick_file),
                formatHint = stringResource(R.string.format_obb),
                onPick = onPickObb,
                enabled = !isRunning,
                canChange = state.bundledObb == null
            )
        }

        Stagger(visible = visible, index = 2) {
            InstallButton(
                isWithObb = state.obb != null,
                phase = state.phase,
                progress = state.progress,
                enabled = canStart,
                onClick = onStart
            )
        }

        AnimatedVisibility(
            visible = isRunning || state.statusText.isNotEmpty() || state.errorText != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            StatusBlock(state = state)
        }

        AnimatedVisibility(visible = state.phase == Phase.Done) {
            androidx.compose.material3.OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.restart)) }
        }

        Stagger(visible = visible, index = 3) {
            InfoCard(
                title = stringResource(R.string.how_it_works_title),
                body = stringResource(R.string.how_it_works_body),
                icon = Icons.Outlined.Info
            )
        }
        Stagger(visible = visible, index = 4) {
            InfoCard(
                title = stringResource(R.string.caveat_title),
                body = stringResource(R.string.caveat_body),
                icon = Icons.Outlined.WarningAmber,
                accent = HubColors.Warn
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Stagger(visible: Boolean, index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 380, delayMillis = index * 70)) +
                slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = tween(durationMillis = 480, delayMillis = index * 70, easing = FastOutSlowInEasing)
                ),
        exit = fadeOut()
    ) { content() }
}

@Composable
private fun UnknownSourcesCard(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HubColors.Surface)
            .border(BorderStroke(1.dp, HubColors.Warn.copy(alpha = 0.5f)), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = HubColors.Warn,
                    modifier = Modifier.size(18.dp)
                )
                Text(stringResource(R.string.unknown_sources_required), style = MaterialTheme.typography.titleSmall, color = HubColors.Warn)
            }
            Text(stringResource(R.string.unknown_sources_subtitle), style = MaterialTheme.typography.bodyMedium)
            androidx.compose.material3.Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}

@Composable
private fun InstallButton(
    isWithObb: Boolean,
    phase: Phase,
    progress: Float,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "btn-glow")
    val glow by transition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "btn-glow"
    )
    val scale = if (enabled) glow * 0.02f + 0.98f else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HubColors.Surface)
            .border(BorderStroke(1.dp, HubColors.Border), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(14.dp))
                .background(HubColors.InstallGradient)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 16.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = phaseIcon(phase),
                    contentDescription = null,
                    tint = HubColors.Background,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = phaseButtonLabel(phase, isWithObb),
                    style = MaterialTheme.typography.titleMedium,
                    color = HubColors.Background,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
        if (isWithObb && phase == Phase.Idle) {
            Text(
                stringResource(R.string.install_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = HubColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (phase in setOf(Phase.Staging, Phase.Patching, Phase.InstallingApk)) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                color = HubColors.Primary,
                trackColor = HubColors.Border
            )
        }
    }
}

@Composable
private fun phaseIcon(phase: Phase) = when (phase) {
    Phase.Done -> Icons.Outlined.CheckCircle
    Phase.Error -> Icons.Outlined.ErrorOutline
    Phase.Staging, Phase.Patching, Phase.InstallingApk -> Icons.Filled.Bolt
    else -> Icons.Filled.Download
}

@Composable
private fun phaseButtonLabel(phase: Phase, isWithObb: Boolean): String = when (phase) {
    Phase.Idle, Phase.Done, Phase.Error ->
        if (isWithObb) stringResource(R.string.install_apk_obb) else stringResource(R.string.install_apk)
    Phase.Staging -> stringResource(R.string.phase_staging)
    Phase.Patching -> stringResource(R.string.phase_patching)
    Phase.InstallingApk -> stringResource(R.string.phase_installing, "")
}

@Composable
private fun StatusBlock(state: UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.statusText.isNotEmpty() && state.errorText == null) {
            Text(
                state.statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.phase == Phase.Done) HubColors.Primary else HubColors.TextSecondary
            )
        }
        state.errorText?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(HubColors.Danger.copy(alpha = 0.10f))
                    .border(BorderStroke(1.dp, HubColors.Danger.copy(alpha = 0.4f)), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(err, style = MaterialTheme.typography.bodyMedium, color = HubColors.Danger)
            }
        }
    }
}
