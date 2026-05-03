package com.aciderix.obbinstaller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    InstallerScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallerScreen(vm: InstallerViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    val pickApk = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.setApkUri(it) }
    }
    val pickObb = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.setObbUri(it) }
    }
    val unknownSourcesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refreshUnknownSourcesPermission()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("OBB Installer") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.canInstallUnknown) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Autorisation requise", fontWeight = FontWeight.Bold)
                        Text("Active \"Installer des applis inconnues\" pour cette app.", fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { unknownSourcesLauncher.launch(vm.unknownSourcesIntent()) }) {
                            Text("Ouvrir les paramètres")
                        }
                    }
                }
            }

            SourceCard(
                title = "APK",
                bundled = state.bundledApk,
                current = state.apk?.displayName,
                onPick = { pickApk.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*")) }
            )

            SourceCard(
                title = "OBB (optionnel)",
                bundled = state.bundledObb,
                current = state.obb?.displayName,
                onPick = { pickObb.launch(arrayOf("application/octet-stream", "*/*")) }
            )

            val canStart = state.apk != null && state.canInstallUnknown && state.phase in setOf(Phase.Idle, Phase.Done, Phase.Error)
            Button(
                onClick = { vm.start() },
                enabled = canStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when (state.phase) {
                        Phase.Idle, Phase.Done, Phase.Error ->
                            if (state.obb == null) "Installer l'APK" else "Installer APK + OBB"
                        Phase.Staging -> "Préparation…"
                        Phase.Patching -> "Patch + signature…"
                        Phase.InstallingApk -> "Installation…"
                        Phase.CopyingObb -> "Copie OBB…"
                    }
                )
            }

            if (state.phase in setOf(Phase.Staging, Phase.Patching, Phase.InstallingApk, Phase.CopyingObb)) {
                LinearProgressIndicator(
                    progress = { state.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.statusText.isNotEmpty()) {
                Text(state.statusText, fontSize = 14.sp)
            }
            state.errorText?.let { err ->
                Text(err, color = Color(0xFFFF6B6B), fontSize = 14.sp)
            }

            if (state.phase == Phase.Done) {
                OutlinedButton(onClick = { vm.reset() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Recommencer")
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Comment ça marche", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "L'app ré-écrit l'APK pour partager un UID Linux avec ce hub, ce qui lui " +
                        "permet d'écrire dans Android/obb/<package> sans SAF, sans Shizuku, sans root. " +
                        "Conséquence : la signature du jeu change, donc plus de mises à jour Play Store " +
                        "et anti-cheat en ligne KO. Pour les jeux solo, c'est transparent.",
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceCard(title: String, bundled: String?, current: String?, onPick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            when {
                bundled != null -> Text("Embarqué : $bundled", fontSize = 13.sp)
                current != null -> Text("Sélectionné : $current", fontSize = 13.sp)
                else -> Text("Aucun fichier sélectionné", fontSize = 13.sp, color = Color(0xFFAAAAAA))
            }
            if (bundled == null) {
                OutlinedButton(onClick = onPick, modifier = Modifier.align(Alignment.End)) {
                    Text(if (current == null) "Choisir…" else "Changer")
                }
            }
        }
    }
}
