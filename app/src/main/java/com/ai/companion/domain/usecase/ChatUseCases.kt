package com.ai.companion.domain.usecase

import com.ai.companion.domain.model.ApiConfig
import com.ai.companion.domain.model.CharacterConfig
import com.ai.companion.domain.model.ChatMessage
import com.ai.companion.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class ChatUseCases @Inject constructor(
    val getMessages: GetMessagesUseCase,
    val sendMessage: SendMessageUseCase,
    val deleteSession: DeleteSessionUseCase,
    val clearAllMessages: ClearAllMessagesUseCase,
    val getCharacterConfig: GetCharacterConfigUseCase,
    val saveCharacterConfig: SaveCharacterConfigUseCase,
    val getApiConfig: GetApiConfigUseCase,
    val saveApiConfig: SaveApiConfigUseCase,
    val testDeepSeekApi: TestDeepSeekApiUseCase,
    val testVolcengineApi: TestVolcengineApiUseCase,
    val testDoubaoApi: TestDoubaoApiUseCase,
    val getAllSessions: GetAllSessionsUseCase
)

class GetMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(sessionId: String): Flow<List<ChatMessage>> {
        return repository.getMessagesBySession(sessionId)
    }
}

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        content: String,
        sessionId: String,
        characterConfig: CharacterConfig,
        apiConfig: ApiConfig
    ): ChatMessage {
        return repository.sendMessage(content, sessionId, characterConfig, apiConfig)
    }
}

class DeleteSessionUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(sessionId: String) {
        repository.deleteSessionMessages(sessionId)
    }
}

class ClearAllMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke() {
        repository.deleteAllMessages()
    }
}

class GetCharacterConfigUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<CharacterConfig> {
        return repository.getCharacterConfig()
    }
}

class SaveCharacterConfigUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(config: CharacterConfig) {
        repository.saveCharacterConfig(config)
    }
}

class GetApiConfigUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<ApiConfig> {
        return repository.getApiConfig()
    }
}

class SaveApiConfigUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(config: ApiConfig) {
        repository.saveApiConfig(config)
    }
}

class TestDeepSeekApiUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(apiKey: String): Boolean {
        return repository.testDeepSeekApi(apiKey)
    }
}

class TestVolcengineApiUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(appId: String, accessKey: String, secretKey: String): Boolean {
        return repository.testVolcengineApi(appId, accessKey, secretKey)
    }
}

class TestDoubaoApiUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(apiKey: String): Boolean {
        return repository.testDoubaoApi(apiKey)
    }
}

class GetAllSessionsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<List<String>> {
        return repository.getAllSessions()
    }
}
