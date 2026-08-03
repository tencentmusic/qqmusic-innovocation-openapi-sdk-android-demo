package com.tencent.qqmusic.qplayer.ui.activity.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Card
import com.tencent.qqmusic.openapisdk.model.MusicSkillPlayItem
import com.tencent.qqmusic.openapisdk.model.MusicSkillRecommendation
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.openapisdk.model.StreamMusicSkillHistoryItem
import com.tencent.qqmusic.qplayer.ui.activity.home.SearchViewModel
import com.tencent.qqmusic.qplayer.ui.activity.songlist.PlayListParams
import com.tencent.qqmusic.qplayer.ui.activity.songlist.itemUI
import com.tencent.qqmusic.qplayer.utils.UiUtils

/**
 * 流式 AI 搜歌 Demo 页面
 * 测试 streamMusicSkill 接口：多轮对话、流式步骤/文本回调、歌曲列表展示、cancel 功能
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StreamMusicSkillPage(viewModel: SearchViewModel) {
    val keyboard = LocalSoftwareKeyboardController.current

    // 输入框文本
    var inputText by remember { mutableStateOf("") }

    // 从 ViewModel 收集状态
    val uiState by viewModel.streamSkillState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // ---- 输入区域 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入问题，如：推荐几首周杰伦的歌") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    keyboard?.hide()
                    if (inputText.isNotBlank() && !uiState.isLoading) {
                        viewModel.startStreamMusicSkill(inputText)
                        // 保留搜索词，不清空输入框
                    }
                }),
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清空", tint = Color.Gray)
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            // 发送 / 取消 按钮
            if (uiState.isLoading) {
                Button(
                    onClick = { viewModel.cancelStreamMusicSkill() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF5722))
                ) {
                    Text("取消", color = Color.White)
                }
            } else {
                IconButton(
                    onClick = {
                        keyboard?.hide()
                        if (inputText.isBlank()){
                            inputText = "我要听周杰伦、陶喆、蔡依林的歌，每位歌手各三首。"
                        }else{
                            viewModel.startStreamMusicSkill(inputText)
                            // 保留搜索词，不清空输入框
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "发送", tint = Color(0xFF1976D2))
                }
            }
        }

        // ---- 多轮对话历史条数提示 ----
        if (uiState.history.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已有 ${uiState.history.size} 条对话历史",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Button(
                    onClick = { viewModel.clearStreamHistory() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.LightGray)
                ) {
                    Text("清空历史", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        // ---- 结果区域 ----
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 加载中指示器
            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI 思考中...", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }

            // ---- 流式文本卡片（蓝色背景） ----
            if (uiState.streamingText.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = Color(0xFFE3F2FD)  // 蓝色
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("💬 AI 回复", fontSize = 12.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                                MsgTypeBadge(msgType = uiState.lastMsgType)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(uiState.streamingText, fontSize = 14.sp, color = Color(0xFF0D47A1))
                        }
                    }
                }
            }

            // ---- textAtEnd 卡片（绿色背景） ----
            if (!uiState.textAtEnd.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = Color(0xFFE8F5E9)  // 绿色
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "textAtEnd",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.textAtEnd?: "",
                                fontSize = 13.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }

            // ---- openApiSearchType 卡片（橙色背景） ----
            if (!uiState.openApiSearchType.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = Color(0xFFFFF3E0)  // 橙色
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "openApiSearchType",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val searchTypeDesc = when (uiState.openApiSearchType) {
                                "0" -> "0（未知）"
                                "1" -> "1（精确搜索）"
                                "2" -> "2（大模型搜索）"
                                "3" -> "3（问答模式）"
                                else -> uiState.openApiSearchType?:"模式异常".also {
                                    UiUtils.showToast("模式异常，请提单")
                                }
                            }
                            Text(
                                text = searchTypeDesc,
                                fontSize = 13.sp,
                                color = Color(0xFFBF360C),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }else if(uiState.songList.isNotEmpty() && uiState.openApiSearchType !in setOf("1","2")){
                // 只要有歌曲，那openApiSearchType一定有值
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = Color.Red
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "openApiSearchType",
                                fontSize = 11.sp,
                                color =Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text= "下发了歌曲openApiSearchType的值对不上",fontSize = 13.sp)
                        }
                    }
                }
            }

            // ---- recommentent 卡片（青色背景） ----
            val recommendation = uiState.musicSkillRecommendation
            if(recommendation != null){
                if ((recommendation.songCnt?:0) > 0 || (recommendation.suggestCnt?:0) > 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            backgroundColor = Color(0xFFE0F7FA)  // 青色
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "recommentent",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00695C),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                MusicSkillRecommendationDisplay(uiState = uiState, recommendation = recommendation)
                            }
                        }
                    }
                }
                if((recommendation.songCnt ?: 0) != uiState.songList.size){
                    // 只要有歌曲，那recommendation的songCnt一定有值
                    item {
                        Card(modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            backgroundColor = Color.Red){
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "recommentent",
                                    fontSize = 11.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text= "recommendation字段值异常，歌曲数量对不上！",fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ---- playInfoList 卡片（黄色背景） ----
            val playInfoList = uiState.playInfoList
            if (playInfoList.isNotEmpty()) {
                item {
                    PlayInfoListCard(playInfoList = playInfoList)
                }
            }

            // ---- miscInfo 卡片（紫色背景） ----
            if (!uiState.miscInfo.isNullOrEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = Color(0xFFF3E5F5)  // 紫色
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "miscInfo",
                                fontSize = 11.sp,
                                color = Color(0xFF6A1B9A),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            MiscInfoDisplay(miscInfo = uiState.miscInfo?: emptyMap())
                        }
                    }
                }
            }

            // 错误信息
            if (uiState.errorMsg.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = " ${uiState.errorMsg}",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }

            // 歌曲列表
            if (uiState.songList.isNotEmpty()) {
                item {
                    Text(
                        text = "🎵 为你找到 ${uiState.songList.size} 首歌曲",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(uiState.songList) { song ->
                    itemUI(PlayListParams(uiState.songList, song))
                }
            }
        }
    }
}

/**
 * msgType 标签组件，展示当前 chunk 的消息类型
 */
@Composable
private fun MsgTypeBadge(msgType: Int) {
    if (msgType == 0) return
    val (label, color) = when (msgType) {
        1    -> "用户" to Color(0xFF4CAF50)
        2    -> "AI助手" to Color(0xFF1976D2)
        else -> "msgType=$msgType" to Color.Gray
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

/**
 * MusicSkillRecommendation 展示组件
 * 展示 recommentent 字段的原字段名 + 字段类型名 + 值
 */
@Composable
private fun MusicSkillRecommendationDisplay(uiState:StreamSkillUiState, recommendation: MusicSkillRecommendation) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RecommendationRow("songCnt", "Int", recommendation.songCnt?.toString())
        RecommendationRow("suggestCnt", "Int", recommendation.suggestCnt?.toString())
        val playTypeDesc = when (recommendation.playType) {
            0 -> "0（插入列表）"
            1 -> "1（替换列表）"
            else -> recommendation.playType?.toString()
        }
        RecommendationRow("playType", "Int", playTypeDesc,
            onClick = {
                when(recommendation.playType){
                    0 -> OpenApiSDK.getPlayerApi().appendSongToPlaylist(uiState.songList)
                    1 -> OpenApiSDK.getPlayerApi().playSongs(uiState.songList)
                }
            })
    }
}

@Composable
private fun RecommendationRow(fieldName: String, typeName: String, value: String?, onClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().clickable(
        enabled = onClick != null) { onClick?.invoke().also { UiUtils.showToast("操作成功") } },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$fieldName: $typeName",
            fontSize = 11.sp,
            color = Color(0xFF00695C),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(text = "= ", fontSize = 11.sp, color = Color(0xFF004D40))
        Text(
            text = value ?: "null",
            fontSize = 11.sp,
            color = if (value != null) Color(0xFF004D40) else Color.Gray,
            fontWeight = FontWeight.Bold
        )
        // 添加手指图标（当有点击动作时显示）
        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "可点击",
                tint = Color.Blue,
                modifier = Modifier.size(14.dp)
            )
        }
    }
    Divider(color = Color(0xFF80CBC4), thickness = 0.5.dp)
}

/**
 * miscInfo 展示组件
 * 每一行格式：原字段名: String = 值
 */
@Composable
private fun MiscInfoDisplay(miscInfo: Map<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        miscInfo.forEach { (key, value) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // 字段名 + 类型名
                Text(
                    text = "$key: String",
                    fontSize = 11.sp,
                    color = Color(0xFF6A1B9A),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "= \"",
                    fontSize = 11.sp,
                    color = Color(0xFF4A148C)
                )
                Text(
                    text = value,
                    fontSize = 11.sp,
                    color = Color(0xFF4A148C),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "\"",
                    fontSize = 11.sp,
                    color = Color(0xFF4A148C)
                )
            }
            Divider(color = Color(0xFFCE93D8), thickness = 0.5.dp)
        }
    }
}

/**
 * playInfoList 卡片组件
 * - 前 3 首默认展示，超出部分点击"展开"按钮显示
 * - 每首歌只展示有值的字段
 */
@Composable
private fun PlayInfoListCard(
    playInfoList: List<com.tencent.qqmusic.openapisdk.model.MusicSkillPlayItem?>
) {
    val collapseThreshold = 3
    var expanded by remember { mutableStateOf(false) }
    val visibleList = if (expanded || playInfoList.size <= collapseThreshold) {
        playInfoList
    } else {
        playInfoList.take(collapseThreshold)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(0xFFFFFDE7)  // 黄色
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "playInfo（共 ${playInfoList.size} 首）",
                fontSize = 11.sp,
                color = Color(0xFFF57F17),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            visibleList.forEachIndexed { index, playInfo ->
                Text(
                    text = "[第 ${index + 1} 首]",
                    fontSize = 11.sp,
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (playInfo != null) {
                    MusicSkillPlayItemDisplay(playInfo = playInfo)
                } else {
                    Text(text = "null", fontSize = 11.sp, color = Color.Gray)
                }
                if (index < visibleList.lastIndex) {
                    Divider(
                        color = Color(0xFFFFE082),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            // 展开 / 收起 按钮
            if (playInfoList.size > collapseThreshold) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (expanded) "▲ 收起（仅显示前 $collapseThreshold 首）"
                           else "▼ 展开剩余 ${playInfoList.size - collapseThreshold} 首",
                    fontSize = 11.sp,
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * MusicSkillPlayItem 展示组件
 * 只展示有值（非 null）的字段，格式：字段名: 类型 = 值
 */
@Composable
private fun MusicSkillPlayItemDisplay(playInfo: com.tencent.qqmusic.openapisdk.model.MusicSkillPlayItem) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        playInfo.uuid?.let { PlayInfoRow("uuid", "Int", it.toString()) }
        playInfo.previousTips?.let { PlayInfoRow("previousTips", "String", it) }
        playInfo.speakCommand?.let { PlayInfoRow("speakCommand", "String", it) }
        playInfo.hitStatus?.let {
            val desc = when (it) {
                0 -> "0（默认）"; 1 -> "1（精确命中）"; 2 -> "2（非精确命中）"; else -> it.toString()
            }
            PlayInfoRow("hitStatus", "Int", desc)
        }
        playInfo.originalTag?.let {
            val desc = when (it) {
                0 -> "0（未知）"; 1 -> "1（原唱）"; 2 -> "2（翻唱）"; else -> it.toString()
            }
            PlayInfoRow("originalTag", "Int", desc)
        }
        playInfo.albumNameAlias?.let { PlayInfoRow("albumNameAlias", "String", it) }
        playInfo.songNameAlias?.let { PlayInfoRow("songNameAlias", "String", it) }
        playInfo.singerNameAlias?.let { PlayInfoRow("singerNameAlias", "String", it) }
        playInfo.description?.let { PlayInfoRow("description", "String", it) }
    }
}

@Composable
private fun PlayInfoRow(fieldName: String, typeName: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$fieldName: $typeName = ",
            fontSize = 11.sp,
            color = Color(0xFFF57F17),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = Color(0xFFE65100),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
    Divider(color = Color(0xFFFFE082), thickness = 0.5.dp)
}

/**
 * 流式 AI 搜歌 UI 状态
 */
data class StreamSkillUiState(
    /** 是否正在加载（轮询中） */
    val isLoading: Boolean = false,
    /** 流式回复文本（agent-text，流式累积） */
    val streamingText: String = "",
    /** 最终歌曲列表 */
    val songList: List<SongInfo> = emptyList(),
    /** 错误信息 */
    val errorMsg: String = "",
    /** 多轮对话历史 */
    val history: List<StreamMusicSkillHistoryItem> = emptyList(),
    /**
     * 最近一次 onTextUpdate 的 msgType：
     * 1=用户, 2=AI助手, 3=AI助手兜底, 1001=重置, 1002=平滑重置
     */
    val lastMsgType: Int = 0,
    /** 附加信息 Map，后台 miscInfo 字段原始下发内容 */
    val miscInfo: Map<String, String>? = null,
    /** 结束文本介绍，对应协议 textAtEnd 字段 */
    val textAtEnd: String? = null,
    /** 搜索类型：0=未知, 1=精确搜索, 2=大模型搜索，对应协议 openApiSearchType 字段 */
    val openApiSearchType: String? = null,
    /** 音乐技能推荐内容，对应协议 musicContent.recommentent 字段 */
    val musicSkillRecommendation: MusicSkillRecommendation? = null,
    /** 歌曲播放信息列表，对应每首歌曲的 playInfo 字段 */
    val playInfoList: List<MusicSkillPlayItem?> = emptyList()
)