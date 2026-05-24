package com.ai.companion.domain.model

data class CharacterConfig(
    val name: String = "",
    val personality: String = "",
    val backstory: String = "",
    val speakingStyle: String = "",
    val avatar: String = ""
) {
    fun toPrompt(): String {
        return buildString {
            if (name.isNotEmpty()) append("你的名字是：$name\n")
            if (personality.isNotEmpty()) append("性格特点：$personality\n")
            if (backstory.isNotEmpty()) append("背景故事：$backstory\n")
            if (speakingStyle.isNotEmpty()) append("说话风格：$speakingStyle\n")
            append("请严格按照以上设定与用户交流。")
        }
    }

    fun isEmpty(): Boolean {
        return name.isEmpty() && personality.isEmpty() && backstory.isEmpty() && speakingStyle.isEmpty()
    }
}

data class ApiConfig(
    val deepSeekApiKey: String = "",
    val volcengineAppId: String = "",
    val volcengineAccessKey: String = "",
    val volcengineSecretKey: String = "",
    val doubaoImageKey: String = ""
) {
    fun isDeepSeekConfigured(): Boolean = deepSeekApiKey.isNotBlank()
    fun isVolcengineConfigured(): Boolean = volcengineAppId.isNotBlank() && 
            volcengineAccessKey.isNotBlank() && 
            volcengineSecretKey.isNotBlank()
    fun isDoubaoConfigured(): Boolean = doubaoImageKey.isNotBlank()
}
