package com.tencent.qqmusic.qplayer.ui.activity.home.ai


import android.graphics.Bitmap
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.google.gson.Gson
import com.tencent.qqmusic.ai.entity.AICreateTaskInfo
import com.tencent.qqmusic.ai.entity.AIPayInfo
import com.tencent.qqmusic.ai.entity.QueryEditWorkStatusResp
import com.tencent.qqmusic.ai.function.base.AISceneType
import com.tencent.qqmusic.ai.function.base.IAICommon.OnPlayListener
import com.tencent.qqmusic.ai.function.base.IAIFunction
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine.PlayState
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

@Composable
fun AISongRecordPage(backPrePage: () -> Unit) {
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
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
    val aiFunction = OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)

    val records = remember { mutableStateListOf<AICreateTaskInfo>() }
    var isLoading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(false) }
    var scene by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf(0) }
    var nextStart by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var isEditClick by remember { mutableStateOf(false) }
    var showPublishDialog by remember { mutableStateOf(false) }
    var editTask: AICreateTaskInfo? by remember { mutableStateOf(null) }
    Log.d("wmy", "AISongRecordPage: $aiFunction")
    val loadFirstFun = {
        isLoading = true
        aiFunction?.fetchAIWorkList(scene, "", 20) {
            if (it.isSuccess()) {
                records.clear()
                nextStart = it.data?.nextStart ?: ""
                records.addAll(it.data?.taskList ?: emptyList())
                hasMore = it.hasMore
            } else {
                UiUtils.showToast("未获取到数据：${it.errorMsg}")
            }
            isLoading = false
        }
    }

    val loadPublishFirstFun = {
        isLoading = true
        aiFunction?.fetchPublishCreateSongList( "", 20) {
            if (it.isSuccess()) {
                records.clear()
                nextStart = it.data?.nextStart ?: ""
                records.addAll(it.data?.taskList ?: emptyList())
                hasMore = it.hasMore
            } else {
                UiUtils.showToast("未获取到数据：${it.errorMsg}")
            }
            isLoading = false
        }
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    val options = listOf("全部(灵感+图片)", "已发布", "AI作曲")

    // 图片做歌和灵感做歌都可编辑、发布
    if (isEditClick && AISceneType.isCanEditor(scene)) {
        editTask?.taskId?.let {
            EditAiCreateSongWorkPage(it, scene)
        }
    } else if (showPublishDialog && AISceneType.isCanPublish(scene)) {
        editTask?.let {
            PublishDialog(it) {
                showPublishDialog = false
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE0F7FA))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 文本框+下拉箭头
                Box(modifier = Modifier.weight(2f)) {
                    TextField(
                        value = options[selectedIndex],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { expanded = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 下拉菜单
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        options.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedIndex = index
                                    expanded = false
                                }
                            ) {
                                Text(option)
                            }
                        }
                    }
                }
                Spacer(Modifier.padding(2.dp))
                // 查询按钮（保持原有功能）
                Button(
                    onClick = {
                        when (selectedIndex) {
                            0 -> {
                                // 灵感做歌+图片做歌
                                scene = AISceneType.AI_SCENE_TYPE_CREATE_SONG
                                status = 0
                                loadFirstFun.invoke()
                            }
                            1 -> {
                                // 已发布-灵感做歌
                                scene = AISceneType.AI_SCENE_TYPE_CREATE_SONG
                                status = 1
                                loadPublishFirstFun.invoke()
                            }
                            2 -> {
                                // AI作曲
                                scene = AISceneType.AI_SCENE_TYPE_COMPOSE
                                status = 0
                                loadFirstFun.invoke()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("查询")
                }
            }

            Text(
                text = "历史生成记录",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyColumn (
                state = listState,
            ){
                items(records) { record ->
                    AICreateTaskInfoItem(record, scene.toString()) {
                        editTask = record
                        when (it) {
                            2 -> {
                                aiFunction?.deleteAIComposeTask(
                                    scene.toString(),
                                    listOf(record.taskId ?: "")
                                ) { ret ->
                                    if (ret.isSuccess()) {
                                        UiUtils.showToast("删除成功")
                                        records.remove(record)
                                    } else {
                                        UiUtils.showToast("删除失败:${ret.errorMsg}")
                                    }
                                }
                            }
                            0 -> {
                                isEditClick = true
                            }
                            else -> {
                                // 发布
                                showPublishDialog = true
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    if (isLoading) {
                        records.clear()
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }
            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                    .collect { visibleItems ->
                        if (visibleItems.isNotEmpty() && visibleItems.last().index == records.size - 1 && hasMore) {
                            // 用户已经滚动到列表的底部
                            if (!isLoading) {
                                isLoading = true
                                // 加载更多数据
                                if (status == 1) {
                                    aiFunction?.fetchPublishCreateSongList(nextStart, 20) {
                                            if (it.isSuccess()) {
                                                nextStart = it.data?.nextStart ?: ""
                                                records.addAll(it.data?.taskList ?: emptyList())
                                                hasMore = it.hasMore
                                            }
                                            isLoading = false
                                        }
                                } else {
                                    aiFunction?.fetchAIWorkList(scene, nextStart, 20) {
                                            if (it.isSuccess()) {
                                                nextStart = it.data?.nextStart ?: ""
                                                records.addAll(it.data?.taskList ?: emptyList())
                                                hasMore = it.hasMore
                                            }
                                            isLoading = false
                                        }
                                }
                            }
                        }
                    }
            }
        }
    }
}

@Composable
fun PublishDialog(taskInfo: AICreateTaskInfo, onDismissRequest: () -> Unit) {
    var loading by remember { mutableStateOf(false) }
    var payInfo: AIPayInfo? by remember { mutableStateOf(null) }
    var requestSuc by remember { mutableStateOf(false) }
    var firstLoad by remember { mutableStateOf(true) }
    var queryOrderStatus by remember { mutableStateOf("") }
    val aiFunction = OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)

    val scope = rememberCoroutineScope()

    if (firstLoad) {
        firstLoad = false
        loading = true
        aiFunction?.publishWork(taskInfo.taskId ?: "") {
            loading = false
            requestSuc = it.isSuccess()
            payInfo = it.data
        }
    }

    fun queryOrderState() {
        val callback: (OpenApiResponse<QueryEditWorkStatusResp>) -> Unit = {
            queryOrderStatus = if (it.isSuccess()) {
                "订单状态：${it.data?.state}-${it.data?.stateMsg}"
            } else {
                "查询失败：${it.errorMsg}"
            }
            if (it.isSuccess()) {
                if (it.data?.state != 3 && it.data?.state != 4) {
                    scope.launch(Dispatchers.IO) {
                        delay(1000)
                        queryOrderState()
                    }
                } else if (it.data?.state == 3) {
                    queryOrderStatus = "发布成功！"
                }
            }
        }
        aiFunction?.queryPublishTaskStatus(payInfo?.orderId ?: "", callback)
    }

    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    } else {
        AlertDialog(
            modifier = Modifier.height(400.dp),
            onDismissRequest = onDismissRequest,
            title = { Text("发布作品：${taskInfo.songName}") },
            text = {
                Column {
                    Text(text = "发布作品")
                    val text = if (requestSuc) {
                        "订单号：${payInfo?.orderId}\n价格：${payInfo?.price}"
                    } else {
                        "请求失败！"
                    }
                    Text(text = text)
                    if (requestSuc) {
                        if (payInfo?.isFree == true) {
                            Text("此次操作免费，作品发布成功！")
                        } else {
                            val payUrl = payInfo?.payUrl
                            if (payUrl.isNullOrEmpty()) {
                                Text("支付链接为空！")
                            } else {
                                val bitmap = UiUtils.generateQRCode(payUrl)?.asImageBitmap()
                                if (bitmap != null) {
                                    Image(bitmap = bitmap, "")
                                    Button(
                                        onClick = {
                                            queryOrderState()
                                        }
                                    ) {
                                        Text("轮询订单状态")
                                    }
                                    Text(text = queryOrderStatus)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onDismissRequest.invoke()
                }) {
                    Text("确定")
                }
            }
        )
    }
}

fun Int?.getTaskStatusInfo(): String {
    return when (this) {
        0 -> "等待中"
        1 -> "进行中"
        2 -> "已完成"
        3 -> "任务失败"
        else -> "未知状态"
    }
}

@Composable
fun AICreateTaskInfoItem(record: AICreateTaskInfo, scene: String, onItemIconClick: ((type: Int) -> Unit)? = null) {
    var showDialog by remember { mutableStateOf<Bitmap?>(null) }
    var showTaskInfoDialog by remember { mutableStateOf(false) }
    var playStateRes by remember { mutableStateOf(R.drawable.ic_state_paused) }
    var playState by remember { mutableStateOf(PlayState.MEDIAPLAYER_STATE_IDLE) }
    var downloadTaskId by remember { mutableStateOf<Int?>(null) }
    val aiFunction = OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)
    var rememberPlayTime by remember { mutableStateOf(0F) }
    var rememberDuration by remember { mutableStateOf(0) }
    val aiViewModel: AIViewModel = viewModel()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showTaskInfoDialog = true
            }
            .padding(8.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                record.coverUrl?.takeIf { it.isNotBlank() }?.let {
                    Image(
                        painter = rememberImagePainter(it),
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .padding(2.dp)
                    )
                }
                val publishStatusText = if (record.publishStatus != 0) {
                    "发布状态：" + record.publishStatus?.toString()
                } else {
                    ""
                }
                Text(text = "${record.taskId} 创作时间： ${record.createTime} 时长：${(record.duration ?: 0)}s 状态 ${record.taskStatus.getTaskStatusInfo()} ${publishStatusText}", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = record.prompt ?: "")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if ((record.taskStatus ?: 0) < 2) {
                    LinearProgressIndicator(
                        progress = (record.progress ?: 0)/100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if ((record.taskStatus ?: 0) == 2) {
                    if (scene != "2") {
                        Button(onClick = {
                            onItemIconClick?.invoke(0)
                        }) {
                            Text(text = "编辑")
                        }

                        if (record.publishStatus == 0) {
                            Button(onClick = {
                                onItemIconClick?.invoke(1)
                            }) {
                                Text(text = "发布")
                            }
                        }

                        if (record.publishStatus != 3) {
                            Button(onClick = {
                                onItemIconClick?.invoke(2)
                            }) {
                                Text(text = "删除")
                            }
                        }

                        record.taskId?.let { taskId ->
                            Button(onClick = {
                                aiFunction?.shareWork(scene, taskId) { resp ->
                                    if (resp.isSuccess()) {
                                        resp.data?.shareUrl?.let { url ->
                                            AppScope.launchIO {
                                                val bitmap =
                                                    UiUtils.generateQRCode(url)
                                                AppScope.launchUI {
                                                    if (bitmap != null) {
                                                        showDialog = bitmap
                                                    } else {
                                                        UiUtils.showToast("二维码生成失败")
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        UiUtils.showToast("获取分享链接失败:${resp.errorMsg}")
                                    }
                                }
                            }) {
                                Text(text = "分享")
                            }
                        }
                    }

                    IconButton(onClick = {
                        when (playState) {
                            PlayState.MEDIAPLAYER_STATE_ERROR,
                            PlayState.MEDIAPLAYER_STATE_STOPPED,
                            PlayState.MEDIAPLAYER_STATE_PLAYBACKCOMPLETED,
                            PlayState.MEDIAPLAYER_STATE_END,
                            PlayState.MEDIAPLAYER_STATE_IDLE -> {
                                aiViewModel.playTask(record, object : OnPlayListener {
                                        override fun onDownloadErr() {
                                            UiUtils.showToast("下载失败")
                                            playStateRes = R.drawable.ic_state_paused
                                        }

                                        override fun onPlayStateChange(state: Int) {
                                            playState = state
                                            if (state == PlayState.MEDIAPLAYER_STATE_STARTED) {
                                                playStateRes = R.drawable.ic_state_playing
                                                return
                                            } else if (state == PlayState.MEDIAPLAYER_STATE_PLAYBACKCOMPLETED || state == PlayState.MEDIAPLAYER_STATE_STOPPED || state == PlayState.MEDIAPLAYER_STATE_PAUSED || state == PlayState.MEDIAPLAYER_STATE_END) {
                                                playStateRes = R.drawable.ic_state_paused
                                                return
                                            } else if (state != PlayState.MEDIAPLAYER_STATE_ERROR) {
                                                return
                                            }
                                            UiUtils.showToast("播放失败")
                                            playStateRes = R.drawable.ic_state_paused
                                        }

                                    override fun onPlayProgressChange(
                                        curPlayTime: Int,
                                        duration: Int
                                    ) {
                                        rememberPlayTime = curPlayTime.toFloat()
                                        rememberDuration = duration
                                    }

                                    override fun onQueryErr(msg: String) {
                                            UiUtils.showToast("播放失败：$msg")
                                        }

                                    })
                            }
                            PlayState.MEDIAPLAYER_STATE_STARTED -> {
                                aiViewModel.pause()
                            }
                            PlayState.MEDIAPLAYER_STATE_PAUSED -> {
                                aiViewModel.resume()
                            }
                            else -> {
                                aiViewModel.stopPlayCoverLink()
                                playStateRes = R.drawable.ic_state_paused
                            }
                        }
                    }) {
                        Image(
                            painter = painterResource(id = playStateRes),
                            contentDescription = "播放"
                        )
                    }
                    if (scene == "2") {
                        IconButton(onClick = {
                            aiFunction?.getAIComposeTaskDownloadUrl(record) {
                                if (it.isSuccess()) {
                                    it.data?.let { url ->
                                        AppScope.launchIO {
                                            val bitmap = UiUtils.generateQRCode(url)
                                            AppScope.launchUI {
                                                if (bitmap != null) {
                                                    showDialog = bitmap
                                                } else {
                                                    UiUtils.showToast("二维码生成失败")
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    UiUtils.showToast("获取下载链接失败:${it.errorMsg}")
                                }
                            }
                        }) {
                            Image(
                                painter = painterResource(id = R.drawable.icon_player_download_light),
                                contentDescription = "下载"
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 播放控制
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = PlayerObserver.convertTime(ceil(rememberPlayTime / 1000f).toLong()),
                    fontFamily = FontFamily.Monospace
                )

                Slider(
                    enabled = playStateRes == R.drawable.ic_state_playing,
                    value = rememberPlayTime,
                    valueRange = 0f..rememberDuration.toFloat(),
                    onValueChange = { newValue ->
                        rememberPlayTime = newValue
                    },
                    onValueChangeFinished = {
                        aiViewModel.seek(rememberPlayTime.toInt())
                    },
                    modifier = Modifier
                        .weight(1f, true)
                        .padding(horizontal = 10.dp)
                )

                Text(
                    text = PlayerObserver.convertTime(ceil(rememberDuration / 1000f).toLong()),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    if (showDialog != null) {
        QrCodeDialog(showDialog,
                onDismiss = { showDialog = null })
    }
    if (showTaskInfoDialog) {
        TextDialog( Gson().toJson(record),
                onDismiss = { showTaskInfoDialog = false })
    }
}

@Composable
fun QrCodeDialog(img: Bitmap?, onDismiss: () -> Unit, url: String? = null) {
    img ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (url.isNullOrEmpty()) "二维码" else "url=$url")
        },
        text = {
            Image(
                bitmap = img.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(300.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确认")
            }
        }
    )
}
@Composable
fun TextDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "详情")
        },
        text = {Text(text = text, modifier = Modifier.verticalScroll(rememberScrollState()))},
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确认")
            }
        }
    )
}

fun AISceneType.isCanEditor(scene: Int):Boolean{
    return scene in listOf(AI_SCENE_TYPE_CREATE_SONG, AI_SCENE_TYPE_IMAGE_SONG)
}

fun AISceneType.isCanPublish(scene: Int):Boolean{
    return isCanEditor(scene)
}