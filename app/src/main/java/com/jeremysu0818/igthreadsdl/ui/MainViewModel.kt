package com.jeremysu0818.igthreadsdl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremysu0818.igthreadsdl.AppGraph
import com.jeremysu0818.igthreadsdl.data.resolver.UrlNormalizer
import com.jeremysu0818.igthreadsdl.domain.download.DownloadRecord
import com.jeremysu0818.igthreadsdl.domain.download.DownloadStatus
import com.jeremysu0818.igthreadsdl.domain.model.MediaManifest
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import android.content.Context
import com.jeremysu0818.igthreadsdl.ui.theme.ThemeMode

import com.jeremysu0818.igthreadsdl.i18n.AppLanguage
import com.jeremysu0818.igthreadsdl.i18n.AppStrings
import com.jeremysu0818.igthreadsdl.i18n.LanguageManager

enum class MainTab {
    DOWNLOAD,
    QUEUE,
    HISTORY,
    SETTINGS,
}

data class MainUiState(
    val tab: MainTab = MainTab.DOWNLOAD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val input: String = "",
    val isResolving: Boolean = false,
    val manifest: MediaManifest? = null,
    val selectedIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
    val records: List<DownloadRecord> = emptyList(),
) {
    val strings: AppStrings
        get() = LanguageManager.getStrings(appLanguage)
}

class MainViewModel : ViewModel() {
    private val resolverRepository = AppGraph.resolverRepository
    private val downloadRepository = AppGraph.downloadRepository
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()
    private var lastAutomaticUrl: String? = null

    init {
        loadThemeMode()
        loadAppLanguage()
        viewModelScope.launch {
            downloadRepository.records.collect { records ->
                _state.update { it.copy(records = records) }
            }
        }
    }

    private fun loadThemeMode() {
        runCatching {
            val prefs = AppGraph.application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val savedName = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
            val mode = ThemeMode.valueOf(savedName ?: ThemeMode.SYSTEM.name)
            _state.update { it.copy(themeMode = mode) }
        }
    }

    private fun loadAppLanguage() {
        runCatching {
            val language = LanguageManager.getSavedLanguage(AppGraph.application)
            _state.update { it.copy(appLanguage = language) }
        }
    }

    fun selectThemeMode(mode: ThemeMode) {
        _state.update { it.copy(themeMode = mode) }
        runCatching {
            val prefs = AppGraph.application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putString("theme_mode", mode.name).apply()
        }
    }

    fun selectAppLanguage(language: AppLanguage) {
        _state.update { it.copy(appLanguage = language) }
        runCatching {
            LanguageManager.saveLanguage(AppGraph.application, language)
        }
    }

    fun selectTab(tab: MainTab) {
        _state.update { it.copy(tab = tab) }
    }

    fun updateInput(value: String) {
        _state.update {
            it.copy(
                input = value,
                errorMessage = null,
                noticeMessage = null,
            )
        }
    }

    fun parse() {
        parseText(_state.value.input)
    }

    fun receiveExternalText(
        text: String,
        autoResolve: Boolean,
    ) {
        val url = UrlNormalizer.extractSupportedUrl(text)
        if (url == null) {
            if (text.isNotBlank() && autoResolve) {
                _state.update {
                    it.copy(errorMessage = it.strings.msgInvalidClipboard)
                }
            }
            return
        }
        if (autoResolve && url == lastAutomaticUrl) return
        lastAutomaticUrl = if (autoResolve) url else lastAutomaticUrl
        _state.update {
            it.copy(
                tab = MainTab.DOWNLOAD,
                input = url,
                noticeMessage = if (autoResolve) it.strings.msgLinkCapturedResolving else null,
            )
        }
        if (autoResolve) parseText(url)
    }

    fun toggleSelection(id: String) {
        _state.update { current ->
            val selected = current.selectedIds.toMutableSet()
            if (!selected.add(id)) selected.remove(id)
            current.copy(selectedIds = selected, errorMessage = null)
        }
    }

    fun selectAll(selected: Boolean) {
        _state.update { current ->
            current.copy(
                selectedIds = if (selected) {
                    current.manifest?.items?.map { it.id }?.toSet().orEmpty()
                } else {
                    emptySet()
                },
            )
        }
    }

    fun downloadSelected() {
        val current = _state.value
        val manifest = current.manifest ?: return
        val items = manifest.items.filter { it.id in current.selectedIds }
        if (items.isEmpty()) {
            _state.update { it.copy(errorMessage = it.strings.msgSelectAtLeastOne) }
            return
        }
        viewModelScope.launch {
            val records = downloadRepository.enqueue(manifest, items)
            val accepted = records.count { it.status != DownloadStatus.FAILED }
            val failed = records.size - accepted
            _state.update {
                val s = it.strings
                val msg = when {
                    accepted > 0 && failed > 0 -> String.format(s.msgDownloadJobsMixed, accepted, failed)
                    accepted > 0 -> String.format(s.msgDownloadJobCreated, accepted)
                    else -> String.format(s.msgDownloadJobFailed, failed)
                }
                it.copy(
                    tab = MainTab.QUEUE,
                    noticeMessage = msg,
                    errorMessage = null,
                )
            }
        }
    }

    fun cancel(managerId: Long) {
        viewModelScope.launch { downloadRepository.cancel(managerId) }
    }

    fun retry(managerId: Long) {
        viewModelScope.launch {
            val record = downloadRepository.retry(managerId)
            _state.update {
                it.copy(
                    tab = MainTab.QUEUE,
                    noticeMessage = if (record != null) it.strings.msgRetryCreated else it.strings.msgRetryNotFound,
                )
            }
        }
    }

    fun delete(managerId: Long) {
        viewModelScope.launch {
            val deleted = downloadRepository.delete(managerId)
            _state.update {
                it.copy(noticeMessage = if (deleted) it.strings.msgDeleteFileAndRecord else it.strings.msgDeleteRecordOnly)
            }
        }
    }

    fun open(record: DownloadRecord) {
        if (!downloadRepository.open(record)) {
            _state.update { it.copy(errorMessage = it.strings.msgCannotOpenFile) }
        }
    }

    fun share(record: DownloadRecord) {
        if (!downloadRepository.share(record)) {
            _state.update { it.copy(errorMessage = it.strings.msgCannotShareFile) }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(errorMessage = null, noticeMessage = null) }
    }

    private fun parseText(text: String) {
        if (text.isBlank()) {
            _state.update { it.copy(errorMessage = it.strings.msgPleasePasteLink) }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isResolving = true,
                    manifest = null,
                    selectedIds = emptySet(),
                    errorMessage = null,
                )
            }
            when (val result = resolverRepository.resolve(text)) {
                is ResolverResult.Success -> {
                    _state.update {
                        it.copy(
                            isResolving = false,
                            input = result.manifest.sourceUrl,
                            manifest = result.manifest,
                            selectedIds = result.manifest.items.map { item -> item.id }.toSet(),
                            errorMessage = null,
                            noticeMessage = if (result.manifest.isPartial) {
                                it.strings.msgPartialManifest
                            } else {
                                String.format(it.strings.msgGotMediaItems, result.manifest.items.size)
                            },
                        )
                    }
                }
                is ResolverResult.Failure -> {
                    _state.update {
                        it.copy(
                            isResolving = false,
                            manifest = null,
                            selectedIds = emptySet(),
                            errorMessage = result.error.userMessage(it.strings),
                            noticeMessage = null,
                        )
                    }
                }
            }
        }
    }
}
