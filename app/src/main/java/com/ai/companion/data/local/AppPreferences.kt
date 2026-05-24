package com.ai.companion.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences @Inject constructor(private val context: Context) {

    // API Keys - 所有密钥都从本地存储读取，无默认值无硬编码
    private object PreferencesKeys {
        // DeepSeek V4
        val DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key")

        // 火山引擎
        val VOLCENGINE_APP_ID = stringPreferencesKey("volcengine_app_id")
        val VOLCENGINE_ACCESS_KEY = stringPreferencesKey("volcengine_access_key")
        val VOLCENGINE_SECRET_KEY = stringPreferencesKey("volcengine_secret_key")

        // 豆包
        val DOUBAO_IMAGE_KEY = stringPreferencesKey("doubao_image_key")

        // 人设配置
        val CHARACTER_NAME = stringPreferencesKey("character_name")
        val CHARACTER_PERSONALITY = stringPreferencesKey("character_personality")
        val CHARACTER_BACKSTORY = stringPreferencesKey("character_backstory")
        val CHARACTER_SPEAKING_STYLE = stringPreferencesKey("character_speaking_style")
        val CHARACTER_AVATAR = stringPreferencesKey("character_avatar")
    }

    // DeepSeek
    val deepSeekApiKey: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.DEEPSEEK_API_KEY] }

    suspend fun setDeepSeekApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEEPSEEK_API_KEY] = key
        }
    }

    // 火山引擎
    val volcengineAppId: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.VOLCENGINE_APP_ID] }

    suspend fun setVolcengineAppId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOLCENGINE_APP_ID] = id
        }
    }

    val volcengineAccessKey: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.VOLCENGINE_ACCESS_KEY] }

    suspend fun setVolcengineAccessKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOLCENGINE_ACCESS_KEY] = key
        }
    }

    val volcengineSecretKey: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.VOLCENGINE_SECRET_KEY] }

    suspend fun setVolcengineSecretKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOLCENGINE_SECRET_KEY] = key
        }
    }

    // 豆包
    val doubaoImageKey: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.DOUBAO_IMAGE_KEY] }

    suspend fun setDoubaoImageKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DOUBAO_IMAGE_KEY] = key
        }
    }

    // 人设配置
    val characterName: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.CHARACTER_NAME] }

    suspend fun setCharacterName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHARACTER_NAME] = name
        }
    }

    val characterPersonality: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.CHARACTER_PERSONALITY] }

    suspend fun setCharacterPersonality(personality: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHARACTER_PERSONALITY] = personality
        }
    }

    val characterBackstory: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.CHARACTER_BACKSTORY] }

    suspend fun setCharacterBackstory(backstory: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHARACTER_BACKSTORY] = backstory
        }
    }

    val characterSpeakingStyle: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.CHARACTER_SPEAKING_STYLE] }

    suspend fun setCharacterSpeakingStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHARACTER_SPEAKING_STYLE] = style
        }
    }

    val characterAvatar: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.CHARACTER_AVATAR] }

    suspend fun setCharacterAvatar(avatar: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHARACTER_AVATAR] = avatar
        }
    }

    // 清空所有配置
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
