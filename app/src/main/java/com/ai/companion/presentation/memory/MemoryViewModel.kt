package com.ai.companion.presentation.memory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.companion.domain.model.ChatMessage
import com.ai.companion.domain.usecase.ChatUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val chatUseCases: ChatUseCases
) : ViewModel() {

    val allSessions: Flow<List<String>> = chatUseCases.getAllSessions()

    var showClearDialog by mutableStateOf(false)
        private set

    fun showClearConfirmation() {
        showClearDialog = true
    }

    fun dismissClearDialog() {
        showClearDialog = false
    }

    fun clearAllMemory() {
        viewModelScope.launch {
            chatUseCases.clearAllMessages()
        }
        showClearDialog = false
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatUseCases.deleteSession(sessionId)
        }
    }

    fun exportMemory() {
        // 导出功能可以后续实现
    }
}
