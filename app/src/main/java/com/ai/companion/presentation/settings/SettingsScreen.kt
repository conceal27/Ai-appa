package com.ai.companion.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ai.companion.presentation.base.BackgroundGray
import com.ai.companion.presentation.base.BackgroundWhite
import com.ai.companion.presentation.base.PrimaryGreen
import com.ai.companion.presentation.base.TextPrimary
import com.ai.companion.presentation.base.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 人设设置
        SectionTitle(title = "人设设置")
        CharacterSettings(viewModel = viewModel)

        Spacer(modifier = Modifier.height(24.dp))

        // API配置
        SectionTitle(title = "API 配置")
        ApiSettings(viewModel = viewModel)

        Spacer(modifier = Modifier.height(32.dp))

        // 保存按钮
        Button(
            onClick = viewModel::saveSettings,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("保存所有设置")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 成功提示
        if (viewModel.saveSuccess) {
            Text(
                text = "设置已保存 ✓",
                color = PrimaryGreen,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearSaveSuccess()
            }
        }

        // 错误提示
        viewModel.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearError()
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun CharacterSettings(viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundWhite, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        ConfigTextField(
            value = viewModel.characterName,
            onValueChange = viewModel::onCharacterNameChanged,
            label = "角色名称",
            placeholder = "例如：小助手、小明"
        )

        Spacer(modifier = Modifier.height(12.dp))

        ConfigTextField(
            value = viewModel.characterPersonality,
            onValueChange = viewModel::onCharacterPersonalityChanged,
            label = "性格描述",
            placeholder = "例如：温柔体贴、幽默风趣、充满正能量",
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        ConfigTextField(
            value = viewModel.characterBackstory,
            onValueChange = viewModel::onCharacterBackstoryChanged,
            label = "背景故事",
            placeholder = "例如：我是你最好的朋友，一直在你身边陪伴你...",
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(12.dp))

        ConfigTextField(
            value = viewModel.characterSpeakingStyle,
            onValueChange = viewModel::onCharacterSpeakingStyleChanged,
            label = "语气风格",
            placeholder = "例如：使用可爱的语气，喜欢用表情符号",
            maxLines = 3
        )
    }
}

@Composable
fun ApiSettings(viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundWhite, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // DeepSeek
        ApiConfigGroup(
            title = "DeepSeek V4",
            description = "用于聊天对话",
            apiKey = viewModel.deepSeekApiKey,
            onApiKeyChanged = viewModel::onDeepSeekApiKeyChanged,
            label = "API Key",
            isTesting = viewModel.isTestingDeepSeek,
            testResult = viewModel.deepSeekTestResult,
            onTestClick = viewModel::testDeepSeekApi
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 火山引擎
        Text(
            text = "火山引擎",
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
        Text(
            text = "用于ASR语音识别、TTS语音合成、图片识别",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        ConfigTextField(
            value = viewModel.volcengineAppId,
            onValueChange = viewModel::onVolcengineAppIdChanged,
            label = "APP ID"
        )
        Spacer(modifier = Modifier.height(8.dp))
        ConfigTextField(
            value = viewModel.volcengineAccessKey,
            onValueChange = viewModel::onVolcengineAccessKeyChanged,
            label = "Access Key ID"
        )
        Spacer(modifier = Modifier.height(8.dp))
        ConfigTextField(
            value = viewModel.volcengineSecretKey,
            onValueChange = viewModel::onVolcengineSecretKeyChanged,
            label = "Secret Access Key"
        )
        Spacer(modifier = Modifier.height(8.dp))

        TestButton(
            isTesting = viewModel.isTestingVolcengine,
            testResult = viewModel.volcengineTestResult,
            onClick = viewModel::testVolcengineApi
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 豆包图像生成
        ApiConfigGroup(
            title = "豆包图像生成",
            description = "用于AI绘图",
            apiKey = viewModel.doubaoImageKey,
            onApiKeyChanged = viewModel::onDoubaoImageKeyChanged,
            label = "API Key",
            isTesting = viewModel.isTestingDoubao,
            testResult = viewModel.doubaoTestResult,
            onTestClick = viewModel::testDoubaoApi
        )
    }
}

@Composable
fun ApiConfigGroup(
    title: String,
    description: String,
    apiKey: String,
    onApiKeyChanged: (String) -> Unit,
    label: String,
    isTesting: Boolean,
    testResult: Boolean?,
    onTestClick: () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = TextPrimary
    )
    Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
    )
    Spacer(modifier = Modifier.height(8.dp))
    ConfigTextField(
        value = apiKey,
        onValueChange = onApiKeyChanged,
        label = label
    )
    Spacer(modifier = Modifier.height(8.dp))
    TestButton(
        isTesting = isTesting,
        testResult = testResult,
        onClick = onTestClick
    )
}

@Composable
fun ConfigTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        placeholder = { Text(placeholder, color = TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryGreen,
            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = PrimaryGreen
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun TestButton(
    isTesting: Boolean,
    testResult: Boolean?,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onClick,
            enabled = !isTesting,
            colors = ButtonDefaults.buttonColors(
                containerColor = BackgroundGray,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = PrimaryGreen,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("测试中...")
            } else {
                Text("测试连接")
            }
        }

        testResult?.let { result ->
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (result) "✓ 连接成功" else "✗ 连接失败",
                color = if (result) PrimaryGreen else MaterialTheme.colorScheme.error
            )
        }
    }
}
