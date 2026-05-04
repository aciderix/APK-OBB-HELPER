package com.aciderix.obbinstaller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aciderix.obbinstaller.ui.AboutScreen
import com.aciderix.obbinstaller.ui.HelpScreen
import com.aciderix.obbinstaller.ui.HomeScreen
import com.aciderix.obbinstaller.ui.HubColors
import com.aciderix.obbinstaller.ui.ObbInstallerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ObbInstallerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = HubColors.Background
                ) {
                    HubApp()
                }
            }
        }
    }
}

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    Home(R.string.tab_home, Icons.Filled.Home),
    About(R.string.tab_about, Icons.Filled.Info),
    Help(R.string.tab_help, Icons.Filled.HelpOutline)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HubApp(vm: InstallerViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    var current by rememberSaveable { mutableStateOf(Tab.Home) }

    val pickApk = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.setApkUri(it) }
    }
    val pickObb = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.setObbUri(it) }
    }
    val unknownSourcesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refreshUnknownSourcesPermission()
    }

    Scaffold(
        containerColor = HubColors.Background,
        topBar = { HubTopBar() },
        bottomBar = { HubBottomNav(current = current, onChange = { current = it }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = current,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab"
            ) { tab ->
                when (tab) {
                    Tab.Home -> HomeScreen(
                        state = state,
                        onPickApk = {
                            pickApk.launch(arrayOf(
                                "application/vnd.android.package-archive",
                                "application/octet-stream",
                                "*/*"
                            ))
                        },
                        onPickObb = {
                            pickObb.launch(arrayOf("application/octet-stream", "*/*"))
                        },
                        onStart = vm::start,
                        onReset = vm::reset,
                        onOpenUnknownSources = {
                            unknownSourcesLauncher.launch(vm.unknownSourcesIntent())
                        }
                    )
                    Tab.About -> AboutScreen()
                    Tab.Help -> HelpScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HubTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HubColors.SurfaceMuted)
                        .border(BorderStroke(1.dp, HubColors.Border), RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painter = painterResource(R.drawable.launcher_image), contentDescription = null)
                }
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = HubColors.Background,
            titleContentColor = HubColors.TextPrimary
        )
    )
}

@Composable
private fun HubBottomNav(current: Tab, onChange: (Tab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HubColors.Background)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(HubColors.Surface)
                .border(BorderStroke(1.dp, HubColors.Border), RoundedCornerShape(22.dp))
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { tab ->
                NavItem(
                    tab = tab,
                    selected = tab == current,
                    onClick = { onChange(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavItem(tab: Tab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val activeColor = if (selected) HubColors.Primary else HubColors.TextMuted
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (selected) HubColors.Primary.copy(alpha = 0.12f)
                    else androidx.compose.ui.graphics.Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(tab.icon, contentDescription = null, tint = activeColor, modifier = Modifier.size(20.dp))
        }
        Text(stringResource(tab.labelRes), style = MaterialTheme.typography.labelSmall, color = activeColor)
    }
}
