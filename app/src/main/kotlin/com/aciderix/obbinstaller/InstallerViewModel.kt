package com.aciderix.obbinstaller

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Phase {
    Idle,
    Staging,
    Patching,
    InstallingApk,
    CopyingObb,
    Done,
    Error
}

data class UiState(
    val phase: Phase = Phase.Idle,
    val apk: FileSource? = null,
    val obb: FileSource? = null,
    val apkMeta: ApkMeta? = null,
    val progress: Float = 0f,
    val statusText: String = "",
    val errorText: String? = null,
    val canInstallUnknown: Boolean = true,
    val bundledApk: String? = null,
    val bundledObb: String? = null
)

class InstallerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        detectBundledAssets()
        refreshUnknownSourcesPermission()
    }

    private fun detectBundledAssets() {
        val ctx = getApplication<Application>()
        val assets = runCatching { ctx.assets.list("") ?: emptyArray() }.getOrDefault(emptyArray())
        val apk = assets.firstOrNull { it.endsWith(".apk", ignoreCase = true) }
        val obb = assets.firstOrNull { it.endsWith(".obb", ignoreCase = true) }
        _state.update { s ->
            s.copy(
                bundledApk = apk,
                bundledObb = obb,
                apk = apk?.let { FileSource.Asset(it) } ?: s.apk,
                obb = obb?.let { FileSource.Asset(it) } ?: s.obb
            )
        }
    }

    fun refreshUnknownSourcesPermission() {
        val ctx = getApplication<Application>()
        val ok = ctx.packageManager.canRequestPackageInstalls()
        _state.update { it.copy(canInstallUnknown = ok) }
    }

    fun unknownSourcesIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${getApplication<Application>().packageName}")
        }

    fun setApkUri(uri: Uri) {
        val ctx = getApplication<Application>()
        runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val name = resolveDisplayName(ctx, uri)
        _state.update { it.copy(apk = FileSource.UriSource(uri, name), errorText = null) }
    }

    fun setObbUri(uri: Uri) {
        val ctx = getApplication<Application>()
        runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val name = resolveDisplayName(ctx, uri)
        _state.update { it.copy(obb = FileSource.UriSource(uri, name), errorText = null) }
    }

    fun start() {
        val s = _state.value
        val apk = s.apk ?: return
        viewModelScope.launch {
            try {
                _state.update { it.copy(phase = Phase.Staging, progress = 0f, statusText = "Lecture de l'APK…", errorText = null) }
                val ctx = getApplication<Application>()
                val meta = ApkInstaller.stageAndReadMeta(ctx, apk) { p ->
                    _state.update { it.copy(progress = p) }
                }
                _state.update { it.copy(apkMeta = meta) }

                _state.update {
                    it.copy(
                        phase = Phase.Patching,
                        progress = 0f,
                        statusText = "Patch + signature de ${meta.packageName}…"
                    )
                }
                val patched = ApkResigner.patchAndResign(ctx, meta.cacheFile) { p ->
                    _state.update { it.copy(progress = p) }
                }
                val patchedMeta = meta.copy(cacheFile = patched)

                _state.update {
                    it.copy(
                        phase = Phase.InstallingApk,
                        progress = 0f,
                        statusText = "Installation de ${meta.packageName} (v${meta.versionName})…"
                    )
                }
                val result = ApkInstaller.install(ctx, patchedMeta) { p ->
                    _state.update { it.copy(progress = p) }
                }
                when (result) {
                    is InstallResult.Success -> {
                        if (s.obb != null) {
                            _state.update {
                                it.copy(
                                    phase = Phase.CopyingObb,
                                    progress = 0f,
                                    statusText = "Copie de l'OBB dans Android/obb/${meta.packageName}…"
                                )
                            }
                            val filename = s.obb.displayName.ifBlank { "main.${meta.versionCode}.${meta.packageName}.obb" }
                            val r = ObbCopier.copy(ctx, s.obb, meta.packageName, filename) { p ->
                                _state.update { it.copy(progress = p) }
                            }
                            if (r.isSuccess) {
                                _state.update { it.copy(phase = Phase.Done, statusText = "Terminé. Lance le jeu.") }
                            } else {
                                _state.update {
                                    it.copy(phase = Phase.Error,
                                        errorText = "Échec copie OBB : ${r.exceptionOrNull()?.message}")
                                }
                            }
                        } else {
                            _state.update { it.copy(phase = Phase.Done, statusText = "APK installée.") }
                        }
                    }
                    is InstallResult.Failure -> {
                        val hint = if (result.message.contains("INCOMPATIBLE", ignoreCase = true) ||
                                       result.message.contains("conflict", ignoreCase = true)) {
                            "\n→ Désinstalle d'abord toute version existante du jeu (Play Store ou autre) et réessaie."
                        } else ""
                        _state.update { it.copy(phase = Phase.Error, errorText = "Échec installation : ${result.message}$hint") }
                    }
                }
            } catch (t: Throwable) {
                _state.update { it.copy(phase = Phase.Error, errorText = t.message ?: "erreur inconnue") }
            }
        }
    }

    fun reset() {
        _state.update { UiState() }
        detectBundledAssets()
        refreshUnknownSourcesPermission()
    }
}
