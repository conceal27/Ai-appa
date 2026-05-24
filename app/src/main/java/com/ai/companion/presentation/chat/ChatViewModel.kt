package com.ai.companion.presentation.chat

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.companion.domain.model.ApiConfig
import com.ai.companion.domain.model.CharacterConfig
import com.ai.companion.domain.model.ChatMessage
import com.ai.companion.domain.model.SenderType
import com.ai.companion.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val TAG = "ChatViewModel"
    }

    var messageText by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isStreaming by mutableStateOf(false)
        private set

    val sessionId: String = savedStateHandle.get<String>("sessionId")
        ?: UUID.randomUUID().toString()

    val messages: Flow<List<ChatMessage>> = chatRepository.getMessagesBySession(sessionId)

    private var characterConfig: CharacterConfig = CharacterConfig()
    private var apiConfig: ApiConfig = ApiConfig()
    private var sendMessageJob: Job? = null

    init {
        loadConfigs()
    }

    private fun loadConfigs() {
        viewModelScope.launch {
            chatRepository.getCharacterConfig().collect { config ->
                characterConfig = config
            }
        }
        viewModelScope.launch {
            chatRepository.getApiConfig().collect { config ->
                apiConfig = config
            }
        }
    }

    fun onMessageTextChanged(text: String) {
        messageText = text
    }

    fun sendMessage() {
        if (messageText.isBlank()) return
        if (sendMessageJob?.isActive == true) return

        val content = messageText
        messageText = ""
        isLoading = true
        isStreaming = true
        errorMessage = null

        sendMessageJob = viewModelScope.launch {
            try {
                chatRepository.sendMessage(
                    content = content,
                    sessionId = sessionId,
                    characterConfig = characterConfig,
                    apiConfig = apiConfig
                )
                Log.d(TAG, "Message sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                errorMessage = e.message ?: "发送失败，请重试"

                // 恢复用户输入
                messageText = content
            } finally {
                isLoading = false
                isStreaming = false
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }

    /**
     * 清除会话，开始新对话
     */
    fun clearSession() {
        viewModelScope.launch {
            chatRepository.deleteSessionMessages(sessionId)
        }
    }

    /**
     * 取消正在发送的消息
     */
    fun cancelSending() {
        sendMessageJob?.cancel()
        isLoading = false
        isStreaming = false
    }

    override fun onCleared() {
        super.onCleared()
        sendMessageJob?.cancel()
    }
}
