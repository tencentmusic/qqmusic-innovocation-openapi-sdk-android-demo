package com.tencent.qqmusic.qplayer.ui.activity.home.ai

import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.ai.function.base.IAIFunction
import com.tencent.qqmusic.ai.entity.AICreateTaskInfo
import com.tencent.qqmusic.ai.entity.ComposePromptInfo
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.delay

private val TAG = "AIComposePage"

// ai作曲
@Composable
fun AIComposePage(backPrePage: () -> Unit) {
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: ")
            backPrePage.invoke()
            remove()
        }
    }

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(key1 = dispatcher) {
        dispatcher?.addCallback(callback)
        onDispose {
            callback.remove() // 移除回调
        }
    }

    var text by remember { mutableStateOf("一段以古筝为背景的适合打斗场面的音乐") }
    var sliderPosition by remember { mutableStateOf(15f) }
    val tags = remember { mutableStateListOf<String>() }
    val sentient = remember { mutableStateListOf<String>() }
    val tagMap = remember { mutableStateListOf<ComposePromptInfo>() }
    var showDialog by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf("") }
    val lastHintType = remember { mutableStateMapOf<String, String>() }
    var taskId by remember { mutableStateOf("") }
    var polling by remember { mutableStateOf(-1L) }
    var taskInfo by remember { mutableStateOf<AICreateTaskInfo?>(null) }

    LaunchedEffect(polling) {
        if (taskId.isNotBlank()) {
            delay(1000L)
            OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)?.queryAIComposeTaskInfo(listOf(taskId)) { taskInfoList ->
                if (taskInfoList.isSuccess()) {
                    if (taskInfoList.data != null && taskInfoList.data!!.isNotEmpty()) {
                        taskInfo = taskInfoList.data!![0]
                    }
                    if ((taskInfo?.taskStatus ?: 0) < 2) {
                        polling++
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E0E0))
            .padding(16.dp)
    ) {
        // TextField
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            placeholder = { Text("请输入提示语句") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = {
                if (sentient.isNotEmpty()) {
                    lastHintType.clear()
                    text = sentient.random()
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新提示语句")
                Spacer(modifier = Modifier.width(4.dp))
                Text("刷新提示语句")
            }
            TextButton(onClick = { text = "" }) {
                Text("清空")
            }

            // Generate Button
            Button(
                onClick = {
                    OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)?.generateSong(sliderPosition.toInt(), text) {
                        if (it.isSuccess()) {
                            UiUtils.showToast("开始生成成功")
                            it.data?.let { id ->
                                taskId = id
                                polling = 0
                            }
                        } else {
                            UiUtils.showToast("开始生成失败:${it.errorMsg}")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor =  Color(0xFF81C784)
                )
            ) {
                Text("开始生成", color = Color.White, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tags
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
             if (tags.isEmpty()) {
                 Chip("刷新提示词和提示语") {
                     OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)?.fetchPromptHintList {
                         if (it.isSuccess() && it.data != null) {
                             tagMap.addAll(it.data!!.second)
                             sentient.addAll(it.data!!.first)
                             tags.addAll(tagMap.map { map -> map.tagTypeName })
                         } else {
                             UiUtils.showToast("获取提示语句失败:${it.errorMsg}")
                         }
                     }
                 }
             } else {
                 tags.forEach { tag ->
                     Chip(tag) {
                         selectedTag = tag
                         showDialog = true
                     }
                 }
             }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Slider
        Text(
            text = "请选择生成音乐时长",
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 10f..30f,
            steps = 19,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("10S")
            Text("${sliderPosition.toInt()}S")
            Text("30S")
        }

        Spacer(modifier = Modifier.height(6.dp))
        taskInfo?.let { AICreateTaskInfoItem(it, scene = "2") }
    }

    if (showDialog) {
        SelectionDialog(
            tag = selectedTag,
            subTag = tagMap.firstOrNull { it.tagTypeName == selectedTag }?.tagNameList ?:  emptyList(),
            onDismiss = { showDialog = false },
            onSelect = { selectedText ->
                lastHintType[selectedTag] = selectedText
                text = lastHintType.values.joinToString(",")
                showDialog = false
            }
        )
    }
}

@Composable
fun Chip(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFBBDEFB)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color(0xFF0D47A1),
            fontSize = 14.sp
        )
    }
}

@Composable
fun SelectionDialog(tag: String, subTag: List<String>, onDismiss: () -> Unit, onSelect: (String) -> Unit) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "选择 $tag")
        },
        text = {
            Column {
                subTag.forEach { option ->
                    Text(
                        text = option,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                            }
                            .padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}