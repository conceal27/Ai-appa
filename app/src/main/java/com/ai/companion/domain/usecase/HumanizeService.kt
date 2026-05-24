package com.ai.companion.domain.usecase

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class HumanizeService @Inject constructor() {
    companion object {
        private const val TAG = "HumanizeService"

        // 需要过滤的AI典型话术
        private val AI_PHRASES = listOf(
            "作为AI",
            "作为一个AI",
            "作为人工智能",
            "抱歉，我不能",
            "作为语言模型",
            "我是一个AI",
            "我没有感情",
            "我没有意识",
            "根据我的编程",
            "很抱歉，我无法",
            "我作为AI",
            "抱歉，作为一个AI",
            "作为一个人工智能"
        )

        // 语气词库
        private val MOOD_WORDS = listOf(
            "哦", "嗯", "啊", "呢", "吧", "呀", "啦", "嘛", "哈", "嘿嘿", "哇"
        )

        // 口语化连接词
        private val FILLER_WORDS = listOf(
            "那个", "怎么说呢", "其实吧", "说实话", "对了", "话说回来"
        )

        // 句末语气词
        private val ENDING_PARTICLES = listOf(
            "~", "~", "~", "~", "~", "!", "", "", "", "", ""
        )

        // 停顿符号
        private val PAUSE_SYMBOLS = listOf(
            "...", "......", "…", "…"
        )
    }

    /**
     * 对AI回复进行人性化处理
     */
    fun humanizeResponse(
        original: String,
        characterConfig: com.ai.companion.domain.model.CharacterConfig,
        enablePause: Boolean = true,
        enableMood: Boolean = true,
        maxLength: Int = 500
    ): String {
        var result = original

        // 1. 过滤AI话术
        result = filterAIPhrases(result)

        // 2. 控制长度
        if (result.length > maxLength) {
            result = truncateToNeatly(result, maxLength)
        }

        // 3. 添加口语化元素
        if (enableMood) {
            result = addMoodWords(result)
        }

        // 4. 模拟停顿
        if (enablePause) {
            result = addRandomPauses(result)
        }

        // 5. 根据人设调整语气
        result = applyPersonalityStyle(result, characterConfig)

        return result.trim()
    }

    /**
     * 过滤AI典型话术
     */
    private fun filterAIPhrases(text: String): String {
        var result = text

        // 移除AI身份相关表述
        AI_PHRASES.forEach { phrase ->
            result = result.replace(phrase, "", ignoreCase = true)
        }

        // 处理"抱歉"开头的句子改为更自然表达
        result = result.replace("抱歉，", "不好意思，")
            .replace("很抱歉，", "不好意思，")

        return result
    }

    /**
     * 整齐地截断文本，在句子边界截断
     */
    private fun truncateToNeatly(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text

        val punctuations = listOf("。", "！", "？", "!", "?", ".", "\n")
        
        // 在最大长度往前找最近的标点
        var endIndex = maxLength
        for (i in maxLength downTo maxLength / 2) {
            val char = text[i]
            if (char.toString() in punctuations) {
                endIndex = i + 1
                break
            }
        }

        var truncated = text.take(endIndex).trim()
        
        // 如果最后没有结束标点，添加省略号
        if (!punctuations.any { truncated.endsWith(it) }) {
            truncated += "..."
        }

        return truncated
    }

    /**
     * 添加语气词
     */
    private fun addMoodWords(text: String): String {
        val sentences = text.split(Regex("(?<=[。！？!?\n])")).filter { it.isNotBlank() }
        
        if (sentences.isEmpty()) return text
        
        val result = mutableListOf<String>()
        sentences.forEachIndexed { index, sentence ->
            var modified = sentence
            
            // 对短句更可能添加语气词
            if (sentence.length in 5..50 && Random.nextFloat() < 0.3f) {
                modified = addMoodWordToSentence(sentence)
            }
            
            // 偶尔在句首添加填充词
            if (index > 0 && Random.nextFloat() < 0.15f) {
                modified = "${FILLER_WORDS.random()}，$modified"
            }
            
            result.add(modified)
        }

        return result.joinToString("")
    }

    /**
     * 给单个句子添加语气词
     */
    private fun addMoodWordToSentence(sentence: String): String {
        // 已经有语气词就不添加了
        if (MOOD_WORDS.any { sentence.contains(it) }) return sentence

        val trimmed = sentence.trim()
        
        // 问句不添加
        if (trimmed.endsWith("?") || trimmed.endsWith("？")) return sentence

        // 在末尾添加语气词
        val mood = MOOD_WORDS.random()
        val punctuation = if (trimmed.endsWith("。")) {
            trimmed.removeSuffix("。") + "$mood。"
        } else if (trimmed.endsWith("!")) {
            trimmed.removeSuffix("!") + "$mood！"
        } else if (trimmed.endsWith("！")) {
            trimmed.removeSuffix("！") + "$mood！"
        } else {
            "$trimmed$mood。"
        }

        return if (Random.nextFloat() < 0.3f) {
            punctuation.dropLast(1) + ENDING_PARTICLES.random()
        } else {
            punctuation
        }
    }

    /**
     * 添加随机停顿
     */
    private fun addRandomPauses(text: String): String {
        if (text.length < 30) return text

        var result = text
        val pausePositions = mutableListOf<Int>()

        // 找逗号、句号的位置
        result.forEachIndexed { index, c ->
            if ((c == '，' || c == ',' || c == '。' || c == ' ') 
                && index > 10 
                && index < result.length - 10) {
                if (Random.nextFloat() < 0.1f) {
                    pausePositions.add(index)
                }
            }
        }

        // 倒序插入，避免索引问题
        pausePositions.sortedDescending().forEach { pos ->
            val pause = PAUSE_SYMBOLS.random()
            result = result.take(pos) + pause + result.drop(pos)
        }

        return result
    }

    /**
     * 根据人设配置调整语气风格
     */
    private fun applyPersonalityStyle(
        text: String,
        characterConfig: com.ai.companion.domain.model.CharacterConfig
    ): String {
        var result = text
        val style = characterConfig.speakingStyle.lowercase()

        when {
            // 可爱风格
            style.contains("可爱") || style.contains("萌") -> {
                result = makeCute(result)
            }
            // 温柔风格
            style.contains("温柔") -> {
                result = makeGentle(result)
            }
            // 幽默风格
            style.contains("幽默") || style.contains("搞笑") -> {
                result = makeHumorous(result)
            }
            // 正式/专业风格
            style.contains("正式") || style.contains("专业") -> {
                result = makeFormal(result)
            }
            // 活泼风格
            style.contains("活泼") -> {
                result = makeLively(result)
            }
        }

        return result
    }

    /**
     * 可爱风格处理
     */
    private fun makeCute(text: String): String {
        var result = text
        // 添加表情符号
        val cuteEmojis = listOf("🥰", "😊", "☺️", "😆", "💖", "✨")
        if (Random.nextFloat() < 0.4f) {
            result += " " + cuteEmojis.random()
        }
        // 增加叠词
        result = result.replace("很可爱", "好可爱好可爱")
        return result
    }

    /**
     * 温柔风格处理
     */
    private fun makeGentle(text: String): String {
        var result = text
        // 句首添加温柔开头
        val gentleStarts = listOf("嗯~", "嗯...", "好的呢", "")
        if (!result.length < 50 && Random.nextFloat() < 0.2f) {
            result = gentleStarts.random() + result
        }
        return result
    }

    /**
     * 幽默风格处理
     */
    private fun makeHumorous(text: String): String {
        var result = text
        val humorousEndings = listOf("哈哈", "嘿嘿", "")
        if (Random.nextFloat() < 0.3f) {
            result += " " + humorousEndings.random()
        }
        return result
    }

    /**
     * 正式风格处理
     */
    private fun makeFormal(text: String): String {
        // 移除口语化，保持正式
        return text
            .replace("嗯", "")
            .replace("哦", "")
            .replace("呀", "")
            .replace("啦", "")
            .replace("~", "")
            .trim()
    }

    /**
     * 活泼风格处理
     */
    private fun makeLively(text: String): String {
        var result = text
        val livelyEmojis = listOf("✨", "🌟", "💫", "🎉", "")
        if (Random.nextFloat() < 0.3f) {
            result += " " + livelyEmojis.random()
        }
        return result
    }

    /**
     * 计算打字延迟（模拟真实打字速度）
     */
    fun calculateTypingDelay(textLength: Int): Long {
        // 假设平均打字速度：每秒 3-5 个中文字符
        val charsPerSecond = Random.nextFloat() * 2 + 3
        val baseDelay = (textLength / charsPerSecond * 1000).toLong()

        // 添加随机额外延迟
        val extraDelay = Random.nextLong() % 1000

        // 思考延迟
        val thinkingDelay = if (textLength > 50) Random.nextLong() % 2000 else 0

        return baseDelay + extraDelay + thinkingDelay
    }
}
