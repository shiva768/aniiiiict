package com.zelretch.aniiiiict.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelretch.aniiiiict.domain.sync.LibrarySyncService
import com.zelretch.aniiiiict.domain.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val librarySyncService: LibrarySyncService
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = librarySyncService.status
        .stateIn(viewModelScope, SharingStarted.Eagerly, SyncStatus.Idle)

    fun syncLibrary() {
        viewModelScope.launch {
            librarySyncService.sync()
        }
    }
}
