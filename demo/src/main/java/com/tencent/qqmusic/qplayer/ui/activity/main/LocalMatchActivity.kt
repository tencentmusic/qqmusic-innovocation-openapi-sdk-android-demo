package com.tencent.qqmusic.qplayer.ui.activity.main

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.autofill.FieldClassification.Match
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.SongID3Info
import com.tencent.qqmusic.openapisdk.core.other.MatchLocalSongInfo

/**
 * Created by denzelzhou on 2025/11/15.
 */
class LocalMatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SongMatchDebugScreen()
        }
    }
}

@Composable
fun SongMatchDebugScreen(viewModel: SongMatchDebugViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "本地歌曲匹配调试",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 输入区域
        InputSection(viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // 输出区域
        OutputSection(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputSection(viewModel: SongMatchDebugViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "输入区域",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 手动输入模式
            ManualInputSection(viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            // 文件选择模式
            FileSelectionSection(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputSection(viewModel: SongMatchDebugViewModel) {
    var songName by remember { mutableStateOf("") }
    var singer by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }

    Column {
        Text(
            text = "手动输入 ID3 信息",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = songName,
            onValueChange = { songName = it },
            label = { Text("歌曲名称") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = singer,
            onValueChange = { singer = it },
            label = { Text("歌手名称") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = album,
            onValueChange = { album = it },
            label = { Text("专辑名称") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Button(
            onClick = {
                viewModel.matchByManualInput(
                    songName = songName,
                    singer = singer,
                    album = album
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = songName.isNotEmpty()
        ) {
            Text("开始匹配")
        }
    }
}

@Composable
fun FileSelectionSection(viewModel: SongMatchDebugViewModel) {
    var hasStoragePermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.matchByFileUri(it) }
    }

    Column {
        Text(
            text = "文件选择",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (!hasStoragePermission) {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        if (Build.VERSION.SDK_INT >= 36)
                            Manifest.permission.READ_MEDIA_AUDIO
                        else
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("请求存储权限")
            }
        } else {
            Button(
                onClick = { filePickerLauncher.launch("audio/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("选择音频文件")
            }
        }
    }
}

@Composable
fun OutputSection(viewModel: SongMatchDebugViewModel) {
    val matchResults = viewModel.matchResults
    val isLoading = viewModel.isLoading.value

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "匹配结果",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (matchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无匹配结果", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matchResults) { result ->
                        MatchResultItem(result)
                    }
                }
            }
        }
    }
}

@Composable
fun MatchResultItem(result: MatchResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isSuccess) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = if (result.isSuccess) "✅ 匹配成功" else "❌ 匹配失败",
                style = MaterialTheme.typography.titleSmall,
                color = if (result.isSuccess) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            SelectionContainer {
                Text(
                    text = "输入: ${result.inputInfo}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (result.isSuccess) {
                SelectionContainer {
                    Text(
                        text = "匹配到: ${result.matchedSong?.songInfo}}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                SelectionContainer {
                    Text(
                        text = "错误: ${result.errorMessage ?: "未知错误"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ViewModel
class SongMatchDebugViewModel : androidx.lifecycle.ViewModel() {
    private val _matchResults = mutableStateListOf<MatchResult>()
    val matchResults: List<MatchResult> get() = _matchResults

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun matchByManualInput(songName: String, singer: String, album: String) {
        _isLoading.value = true
        OpenApiSDK.getLocalSongAPI()?.matchSongsByID3(
            listOf(
                SongID3Info(
                    songName = songName,
                    singerName = singer,
                    albumName = album
                )
            )
        ) { response ->
            _matchResults.clear()
            _matchResults.add(
                0, MatchResult(
                    inputInfo = "手动输入: $songName - $singer - $album",
                    isSuccess = response.isSuccess(),
                    matchedSong = response.data?.firstOrNull(),
                    errorMessage = response.errorMsg,
                    timestamp = System.currentTimeMillis()
                )
            )
            _isLoading.value = false
        }
    }

    fun matchByFileUri(uri: Uri) {
        _isLoading.value = true
        OpenApiSDK.getLocalSongAPI()?.matchSongByFingerPrint(songUri = uri) { response ->
            _matchResults.clear()
            _matchResults.add(
                0, MatchResult(
                    inputInfo = "选择文件: ${uri.path}",
                    isSuccess = response.isSuccess(),
                    matchedSong = MatchLocalSongInfo(null, response.isSuccess().toString(), response.data?.firstOrNull()),
                    errorMessage = response.errorMsg,
                    timestamp = System.currentTimeMillis()
                )
            )
            _isLoading.value = false
        }
        // 解析文件并调用匹配接口
        // 实现文件解析逻辑，然后调用 MatchLocalSongManager
    }
}

// 数据类
data class MatchResult(
    val inputInfo: String,
    val isSuccess: Boolean,
    val matchedSong: MatchLocalSongInfo? = null,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)