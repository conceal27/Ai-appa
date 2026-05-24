package com.ai.companion.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.companion.domain.model.ApiConfig
import com.ai.companion.domain.model.CharacterConfig
import com.ai.companion.domain.usecase.ChatUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val chatUseCases: ChatUseCases
) : ViewModel() {

    // 人设配置状态
    var characterName by mutableStateOf("")
    var characterPersonality by mutableStateOf("")
    var characterBackstory by mutableStateOf("")
    var characterSpeakingStyle by mutableStateOf("")
    var characterAvatar by mutableStateOf("")

    // API配置状态
    var deepSeekApiKey by mutableStateOf("")
    var volcengineAppId by mutableStateOf("")
    var volcengineAccessKey by mutableStateOf("")
    var volcengineSecretKey by mutableStateOf("")
    var doubaoImageKey by mutableStateOf("")

    // 测试状态
    var isTestingDeepSeek by mutableStateOf(false)
    var isTestingVolcengine by mutableStateOf(false)
    var isTestingDoubao by mutableStateOf(false)
    var deepSeekTestResult by mutableStateOf<Boolean?>(null)
    var volcengineTestResult by mutableStateOf<Boolean?>(null)
    var doubaoTestResult by mutableStateOf<Boolean?>(null)

    // UI状态
    var saveSuccess by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val characterConfig = chatUseCases.getCharacterConfig().first()
            characterName = characterConfig.name
            characterPersonality = characterConfig.personality
            characterBackstory = characterConfig.backstory
            characterSpeakingStyle = characterConfig.speakingStyle
            characterAvatar = characterConfig.avatar

            val apiConfig = chatUseCases.getApiConfig().first()
            deepSeekApiKey = apiConfig.deepSeekApiKey
            volcengineAppId = apiConfig.volcengineAppId
            volcengineAccessKey = apiConfig.volcengineAccessKey
            volcengineSecretKey = apiConfig.volcengineSecretKey
            doubaoImageKey = apiConfig.doubaoImageKey
        }
    }

    fun onCharacterNameChanged(value: String) { characterName = value }
    fun onCharacterPersonalityChanged(value: String) { characterPersonality = value }
    fun onCharacterBackstoryChanged(value: String) { characterBackstory = value }
    fun onCharacterSpeakingStyleChanged(value: String) { characterSpeakingStyle = value }
    fun onCharacterAvatarChanged(value: String) { characterAvatar = value }

    fun onDeepSeekApiKeyChanged(value: String) { 
        deepSeekApiKey = value 
        deepSeekTestResult = null
    }
    fun onVolcengineAppIdChanged(value: String) { 
        volcengineAppId = value 
        volcengineTestResult = null
    }
    fun onVolcengineAccessKeyChanged(value: String) { 
        volcengineAccessKey = value 
        volcengineTestResult = null
    }
    fun onVolcengineSecretKeyChanged(value: String) { 
        volcengineSecretKey = value 
        volcengineTestResult = null
    }
    fun onDoubaoImageKeyChanged(value: String) { 
        doubaoImageKey = value 
        doubaoTestResult = null
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                val characterConfig = CharacterConfig(
                    name = characterName,
                    personality = characterPersonality,
                    backstory = characterBackstory,
                    speakingStyle = characterSpeakingStyle,
                    avatar = characterAvatar
                )
                chatUseCases.saveCharacterConfig(characterConfig)

                val apiConfig = ApiConfig(
                    deepSeekApiKey = deepSeekApiKey,
                    volcengineAppId = volcengineAppId,
                    volcengineAccessKey = volcengineAccessKey,
                    volcengineSecretKey = volcengineSecretKey,
                    doubaoImageKey = doubaoImageKey
                )
                chatUseCases.saveApiConfig(apiConfig)

                saveSuccess = true
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }
    }

    fun testDeepSeekApi() {
        if (deepSeekApiKey.isBlank()) {
            errorMessage = "请先输入DeepSeek API Key"
            return
        }

        isTestingDeepSeek = true
        deepSeekTestResult = null

        viewModelScope.launch {
            try {
                val result = chatUseCases.testDeepSeekApi(deepSeekApiKey)
                deepSeekTestResult = result
            } catch (e: Exception) {
                deepSeekTestResult = false
                errorMessage = e.message
            } finally {
                isTestingDeepSeek = false
            }
        }
    }

    fun testVolcengineApi() {
        if (volcengineAppId.isBlank() || volcengineAccessKey.isBlank() || volcengineSecretKey.isBlank()) {
            errorMessage = "请完整填写火山引擎配置"
            return
        }

        isTestingVolcengine = true
        volcengineTestResult = null

        viewModelScope.launch {
            try {
                val result = chatUseCases.testVolcengineApi(
                    volcengineAppId,
                    volcengineAccessKey,
                    volcengineSecretKey
                )
                volcengineTestResult = result
            } catch (e: Exception) {
                volcengineTestResult = false
                errorMessage = e.message
            } finally {
                isTestingVolcengine = false
            }
        }
    }

    fun testDoubaoApi() {
        if (doubaoImageKey.isBlank()) {
            errorMessage = "请先输入豆包API Key"
            return
        }

        isTestingDoubao = true
        doubaoTestResult = null

        viewModelScope.launch {
            try {
                val result = chatUseCases.testDoubaoApi(doubaoImageKey)
                doubaoTestResult = result
            } catch (e: Exception) {
                doubaoTestResult = false
                errorMessage = e.message
            } finally {
                isTestingDoubao = false
            }
        }
    }

    fun clearSaveSuccess() {
        saveSuccess = false
    }

    fun clearError() {
        errorMessage = null
    }
}
