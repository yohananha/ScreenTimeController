package com.screentime.mobile.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.screentime.shared.update.UpdateCheckResult
import com.screentime.shared.update.UpdateChecker
import com.screentime.shared.update.UpdateInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val versionName: String, val downloadUrl: String, val sizeBytes: Long) : UpdateUiState
    data class Downloading(val downloadId: Long) : UpdateUiState
    data class ReadyToInstall(val downloadId: Long) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val checker = UpdateChecker(assetName = "mobile-debug.apk")
    private val installer = UpdateInstaller(context)

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    fun checkForUpdate() {
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            val currentVersion = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "0"
            _state.value = when (val result = checker.check(currentVersion)) {
                is UpdateCheckResult.Available ->
                    UpdateUiState.Available(result.versionName, result.downloadUrl, result.sizeBytes)
                UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate
                is UpdateCheckResult.Error -> UpdateUiState.Failed(result.message)
            }
        }
    }

    fun startDownload(downloadUrl: String, versionName: String) {
        val id = installer.enqueueDownload(downloadUrl, "screentime-mobile-$versionName.apk")
        _state.value = UpdateUiState.Downloading(id)
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                if (installer.isDownloadComplete(id)) {
                    _state.value = UpdateUiState.ReadyToInstall(id)
                    break
                }
                delay(1_000)
            }
        }
    }

    fun canRequestInstall(): Boolean = installer.canRequestInstall()
    fun requestInstallPermissionIntent(): Intent = installer.requestInstallPermissionIntent()
    fun installIntent(downloadId: Long): Intent? = installer.installIntent(downloadId)

    fun dismiss() {
        pollJob?.cancel()
        _state.value = UpdateUiState.Idle
    }

    override fun onCleared() {
        pollJob?.cancel()
    }
}
