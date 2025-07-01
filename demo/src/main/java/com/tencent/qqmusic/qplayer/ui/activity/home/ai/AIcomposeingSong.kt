package com.tencent.qqmusic.qplayer.ui.activity.home.ai

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.ai.entity.AICreateSongRequest
import com.tencent.qqmusic.ai.entity.AICreateSongResponse
import com.tencent.qqmusic.ai.entity.AICreateTaskInfo
import com.tencent.qqmusic.ai.entity.AILyricInfo
import com.tencent.qqmusic.ai.entity.AIPayInfo
import com.tencent.qqmusic.ai.entity.AIPayOrderRequest
import com.tencent.qqmusic.ai.entity.AIPayOrderResp
import com.tencent.qqmusic.ai.entity.AIQueryStatusReq
import com.tencent.qqmusic.ai.entity.HotCreateWorkInfo
import com.tencent.qqmusic.ai.entity.QueryEditWorkStatusResp
import com.tencent.qqmusic.ai.entity.SongStyle
import com.tencent.qqmusic.ai.entity.VocalItem
import com.tencent.qqmusic.ai.function.base.AISceneType
import com.tencent.qqmusic.ai.function.base.IAIFunction
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.cover.AITimbreTAG
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusictvsdk.internal.lyric.LyricManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TAG = "AIComposingSongPage"

@Composable
fun AIComposingSongPage(backPrePage: () -> Unit) {
    val aiViewModel: AIViewModel = viewModel()
    var firstLoad by remember { mutableStateOf(true) }
    if (firstLoad) {
        firstLoad = false
        aiViewModel.getSongStyleList()
    }

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

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { composeSongPage(aiViewModel, navController) }
        composable(AITimbreTAG) {
            AITimbreCreatePage {
                navController.popBackStack()
            }
        }
    }
}

@Composable
private fun composeSongPage(viewModel: AIViewModel, navController: NavController) {

    val activity = LocalContext.current as Activity

    var vocal = remember {
        mutableStateOf<VocalItem?>(null)
    }

    val songStyleList = viewModel.songStyleList
    val hotWorkList = viewModel.hotAiCreateSongList
    var sameStyleTaskId: String? = null
    val aiFunction = OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)

    var keyWord by remember { mutableStateOf(TextFieldValue("")) }

    var selectedSongStyleId by remember {
        mutableStateOf("")
    }

    var taskId by remember { mutableStateOf("") }
    var polling by remember { mutableStateOf(-1L) }
    var taskInfo by remember { mutableStateOf<AICreateTaskInfo?>(null) }
    var taskStatus by remember { mutableStateOf(0) }
    val image = remember { mutableStateOf(ImageBitmap(1, 1)) }

    var consoleMessage by remember { mutableStateOf("") }
    var createSongResp by remember { mutableStateOf<AICreateSongResponse?>(null) }
    var payOrderResp by remember { mutableStateOf<AIPayOrderResp?>(null) }
    val qrCodeImage = remember { mutableStateOf<ImageBitmap?>(null) }
    var selectedIndex by remember { mutableStateOf(0) }
    var showExpanded by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }

    LaunchedEffect(polling) {
        if (taskId.isNotBlank()) {
            delay(5000L)
            OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)?.queryCreateTaskStatus(
                AIQueryStatusReq(
                    AISceneType.AI_SCENE_TYPE_CREATE_SONG,
                    taskId
                )
            ) { info ->
                if (info.isSuccess()) {
                    if (info.data == 2) {
                        OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)
                            ?.queryAICreateSongTaskInfo(listOf(taskId)) { taskInfoList ->
                                if (taskInfoList.isSuccess()) {
                                    if (taskInfoList.data != null && taskInfoList.data!!.isNotEmpty()) {
                                        taskInfo = taskInfoList.data!![0]
                                    }
                                    taskId = ""
                                }
                            }
                    }
                    taskStatus = info.data ?: 0
                    if ((info.data ?: 0) == 1) {
                        polling++
                    }
                } else {
                    UiUtils.showToast("查询失败：${info.errorMsg}")
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TextField(
                value = keyWord,
                label = {
                    Text(text = "输入灵感词或歌词")
                },
                onValueChange = {
                    keyWord = it
                },
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            )

            Row {
                Button(onClick = {
                    aiFunction?.getAIRandomPrompt {
                        keyWord = TextFieldValue((it.data?.randomOrNull() ?: "") as String)
                    }
                }) {
                    Text(text = "AI随机灵感")
                }
            }
        }

        item {
            Text(text = "选择歌曲风格")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                songStyleList.forEach {
                    SongStyleItem(it, selectedSongStyleId) {
                        selectedSongStyleId = it.styleMid ?: ""
                    }
                }
                Image(
                    bitmap = image.value,
                    null,
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        item {
            Text(text = "选择音色")
            AITimbrePage(navController = navController) {
                vocal.value = it
            }
        }

        item {
            ScrollableTextArea(consoleMessage)
            Row {
                Button(onClick = {
                    if (selectedSongStyleId.isEmpty() || vocal.value == null) {
                        Toast.makeText(activity, "请先选择歌曲风格和音色", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        vocal.value?.let { vocalItem ->
                            aiFunction?.createSong(
                                AICreateSongRequest(
                                    prompt = keyWord.text,
                                    songStyleId = selectedSongStyleId,
                                    timbreItem = vocalItem,
                                    sameStyleTaskId = sameStyleTaskId
                                )
                            ) {
                                val createResult = if (it.isSuccess() && it.taskId != null) {
                                    taskId = it.taskId!!
                                    createSongResp = it
                                    showDropdownMenu = createSongResp?.produceInfoList.isNullOrEmpty().not()
                                    "生成成功！任务id为${it.taskId}"
                                } else {
                                    showDropdownMenu = false
                                    "生成失败！错误信息：${it.errorMsg}"
                                }
                                val consoleMessageNew = StringBuilder(consoleMessage)
                                consoleMessageNew.appendLine(createResult)
                                consoleMessage = consoleMessageNew.toString()
                                sameStyleTaskId = null
                                AppScope.launchUI {
                                    Toast.makeText(activity, createResult, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                    }
                }) {
                    Text(text = "1.一键生成")
                }
                Spacer(modifier = Modifier.width(2.dp))

            }
        }

        item {
            if (showDropdownMenu){
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()) {
                    TextField(
                        value = createSongResp?.produceInfoList?.get(selectedIndex)?.produceName
                            ?: "无",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { showExpanded = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 下拉菜单
                    DropdownMenu(
                        expanded = showExpanded,
                        onDismissRequest = { showExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        createSongResp?.produceInfoList?.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedIndex = index
                                    showExpanded = false
                                }
                            ) {
                                Text(option.produceName?:"null-$index")
                            }
                        }
                    }
                }
                createSongResp?.produceInfoList?.get(selectedIndex)?.let { produceInfo ->
                    Text(
                        "商品信息:\n" +
                                "Id:${produceInfo.produceId}\n" +
                                "Name:${produceInfo.produceName}\n" +
                                "Price:${(produceInfo.producePrice?.toFloat() ?: 0f) / 100}元"
                    )
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    if (selectedSongStyleId.isEmpty() || vocal.value == null) {
                        Toast.makeText(activity, "请先选择歌曲风格和音色", Toast.LENGTH_SHORT)
                            .show()
                        return@Button
                    }
                    if (createSongResp?.payment != true) {
                        val consoleMessageNew = StringBuilder(consoleMessage)
                        consoleMessageNew.appendLine("无需付费")
                        consoleMessage = consoleMessageNew.toString()
                        return@Button
                    }
                    val produceInfo = createSongResp?.produceInfoList?.get(selectedIndex)
                    aiFunction?.createSongPayOrder(
                        AIPayOrderRequest(
                            scene = AISceneType.AI_SCENE_TYPE_CREATE_SONG,
                            produceId = produceInfo?.produceId,
                            taskId = createSongResp?.taskId,
                            orderInfo = produceInfo?.orderInfo
                        )
                    ) { resp ->
                        val uploadMessage = if (resp.isSuccess()) {
                            val asImageBitmap =
                                UiUtils.generateQRCode(resp.paymentURL)?.asImageBitmap()
                            asImageBitmap?.let {
                                qrCodeImage.value = asImageBitmap
                            }
                            payOrderResp = resp
                            MLog.i(TAG, "createSongPayOrder: ${resp.paymentURL}")
                            "createSongPayOrder: ${resp}"
                        } else {
                            "createSongPayOrder: ${resp.errorMsg}"
                        }

                        val consoleMessageNew = StringBuilder(consoleMessage)
                        consoleMessageNew.appendLine(uploadMessage)
                        consoleMessage = consoleMessageNew.toString()
                    }
                }) {
                    Text(text = "2.创建支付订单")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Image(
                    bitmap = qrCodeImage.value ?: ImageBitmap(1, 1),
                    null,
                    modifier = Modifier.size(100.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    val orderId = payOrderResp?.orderId
                    if (orderId != null) {
                        aiFunction?.fetchAIPayStatus(orderId) { resp ->
                            val uploadMessage = if (resp.isSuccess()) {
                                val msg = if (resp.state == 3) {
                                    polling++
                                    "已支付"
                                } else "等待支付"
                                "fetchAIPayStatus: $msg"
                            } else {
                                "fetchAIPayStatus: ${resp.errorMsg}"
                            }
                            val consoleMessageNew = StringBuilder(consoleMessage)
                            consoleMessageNew.appendLine(uploadMessage)
                            consoleMessage = consoleMessageNew.toString()
                        }
                    }
                }) {
                    Text(text = "3.查询支付状态")
                }
                if (taskId.isNotBlank()) {
                    when (taskStatus) {
                        0 -> Text(text = "等待中")
                        1 -> Text(text = "进行中")
                        2 -> Text(text = "已完成")
                        3 -> Text(text = "生成失败")
                    }
                }
            }
        }

        item {
            taskInfo?.let { AICreateTaskInfoItem(it, scene = "1") }
        }

        item {
            Divider()
            Button(onClick = {
                viewModel.getHotAICreateSongs()
            }) {
                Text(text = "获取热门作品")
            }
        }
        items(hotWorkList) {
            HotCreateSongItem(it) {
                keyWord = TextFieldValue((it.prompt ?: "") as String)
                sameStyleTaskId = it.taskId
            }
        }
    }
}

@Composable
fun SongStyleItem(songStyle: SongStyle, selectedId: String, onClick: () -> Unit) {
    val textColor = if (selectedId == songStyle.styleMid) {
        Color.Green
    } else {
        Color.Black
    }
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable {
                onClick.invoke()
            }) {
        Image(painter = rememberImagePainter(songStyle.iconUrl ?: ""), "")
        Text(text = songStyle.styleName ?: "", color = textColor)
        Text(text = songStyle.title ?: "", color = textColor)
    }
}

@Composable
fun HotCreateSongItem(item: HotCreateWorkInfo, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable {
            onClick.invoke()
        }) {
        Image(
            painter = rememberImagePainter(item.coverUrl ?: ""),
            "",
            modifier = Modifier.size(50.dp)
        )
        Column(modifier = Modifier.widthIn(max = 200.dp)) {
            Text(text = item.songName ?: "", maxLines = 1)
            Text(text = item.userInfo?.userName ?: "", maxLines = 1)
            Text(text = "tag:${item.tag?.type}", maxLines = 1)
            Text(text = "灵感词：${item.prompt}", maxLines = 1)
            Text(text = "播放量：" + item.playCount?.toString(), maxLines = 1)
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                onClick.invoke()
            }) {
            Text(text = "一键同款")
        }
    }
}

@Composable
fun EditAiCreateSongWorkPage(taskId: String, scene: Int) {
    val activity = LocalContext.current as Activity
    val aiFunction = OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)

    var loading by remember { mutableStateOf(false) }
    var firstLoading by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    var taskInfo: AICreateTaskInfo? by remember { mutableStateOf(null) }
    var reEditLyricResult: String? by remember { mutableStateOf("") }
    var reEditReqSuc by remember { mutableStateOf(false) }
    var queryLyricCreateStatusRet by remember { mutableStateOf("") }
    var queryLyricCreateStatusSuc by remember { mutableStateOf(false) }
    var editWorkRet by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var oldLyricStr: String? by remember { mutableStateOf("") }
    var showPublishDialog by remember { mutableStateOf(false) }
    var payInfo: AIPayInfo? by remember { mutableStateOf(null) }
    var songName: String? by remember { mutableStateOf(null) }
    var editNewSongName: String? by remember { mutableStateOf("") }
    var hasLyric: Boolean by remember { mutableStateOf(false) }
    var editNewLyric: AILyricInfo? by remember {
        mutableStateOf(null)
    }

    if (firstLoading) {
        firstLoading = false
        loading = true
        aiFunction?.queryTaskInfo(listOf(taskId).associateWith { scene.toString() }) {
            loading = false
            taskInfo = it.data?.getOrNull(0)
            songName = taskInfo?.songName
            oldLyricStr = taskInfo?.lyricInfo?.lyricList?.map {
                it.lyricStr
            }?.joinToString(separator = "\n")
            hasLyric = taskInfo?.aiPlayInfo?.lyrics?.isNotEmpty() == true
        }
    }

    fun queryLyricStatus() {
        aiFunction?.queryReCreateLyricStatus(reEditLyricResult ?: "") {
            queryLyricCreateStatusRet = if (it.isSuccess()) {
                if (it.data?.state == 3) {
                    editNewLyric = it.data?.Lyrics
                } else if (it.data?.state == 1) {
                    scope.launch(Dispatchers.IO) {
                        delay(1000)
                        queryLyricStatus()
                    }
                }

                "生成状态：${it.data?.state}，歌词：\n${
                    it.data?.Lyrics?.lyricList?.map {
                        it.lyricStr
                    }?.joinToString(separator = "\n")
                }"
            } else {
                "查询失败"
            }
        }
    }

    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    } else if (taskInfo == null) {
        Text(text = "获取任务详情失败")
    } else if (showPublishDialog) {
        SaveEditDialog(taskInfo = taskInfo, payInfo = payInfo) {
            showPublishDialog = false
        }
    } else {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Text(text = ("歌曲名：" + taskInfo?.songName))
            Text(
                text = "是否有QRC歌词：$hasLyric，QRC歌词是\n ${
                    LyricManager.instance.getStrWithBase64Safely(
                        taskInfo?.aiPlayInfo?.lyrics
                    )
                }"
            )
            Text(text = ("当前歌词：\n$oldLyricStr"))

            Button(onClick = {
                aiFunction?.getAIReCreateLyric(taskInfo?.taskId ?: "") {
                    reEditReqSuc = it.isSuccess()
                    reEditLyricResult = if (it.isSuccess()) {
                        it.data
                    } else {
                        "请求失败，ret: ${it.errorMsg}"
                    }
                }
            }) {
                Text(text = "AI重新填词(图片做歌不支持)")
            }

            Text(text = "重新填词请求结果：$reEditLyricResult")

            if (reEditReqSuc) {
                Button(onClick = {
                    queryLyricStatus()
                }) {
                    Text(text = "轮询歌词生成状态")
                }

                Text(text = ("新歌词：\n$queryLyricCreateStatusRet"))
            }

            Text(modifier = Modifier.padding(top = 10.dp), text = "编辑歌曲名")
            TextField(
                value = editNewSongName ?: "",
                label = {
                    Text(text = "输入新的歌曲名，当前名称：${songName}")
                },
                onValueChange = {
                    editNewSongName = it
                },
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
            )

            Button(onClick = {
                taskInfo?.songName = songName
                val finalSongName = if (editNewSongName?.equals(songName) == true) {
                    null
                } else {
                    editNewSongName
                }
                val finalTask = if (editNewLyric == null) {
                    null
                } else {
                    taskInfo?.lyricInfo = editNewLyric
                    taskInfo
                }
                aiFunction?.saveEdit(taskInfo?.taskId ?: "", songName = finalSongName, finalTask) {
                    editWorkRet = if (it.isSuccess()) {
                        if (it.data?.isFree == true) {
                            "编辑成功，此次操作免费"
                        } else {
                            "编辑成功！任务id：${it.data?.payUrl}"
                        }
                    } else {
                        "编辑失败！错误码：${it.ret}， ${it.errorMsg}"
                    }

                    payInfo = it.data
                    if (it.isSuccess() && it.data?.payUrl?.isNotEmpty() == true) {
                        showPublishDialog = true
                    }
                }
            }) {
                Text(text = "保存编辑")
            }

            if (editWorkRet.isNotEmpty()) {
                Text(text = editWorkRet)
            }
        }
    }
}

@Composable
fun SaveEditDialog(taskInfo: AICreateTaskInfo?, payInfo: AIPayInfo?, onDismissRequest: () -> Unit) {
    if (taskInfo == null || payInfo == null) {
        return
    }
    val aiFunction = OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)

    var loading by remember { mutableStateOf(false) }
    var firstLoad by remember { mutableStateOf(true) }
    var queryOrderStatus by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

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
        aiFunction?.queryEditTaskStatus(payInfo?.orderId ?: "", callback)
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
            title = { Text("保存编辑作品：${taskInfo.songName}") },
            text = {
                Column {
                    Text(text = "保存编辑作品")
                    val text = "订单号：${payInfo?.orderId}\n价格：${payInfo?.price}"
                    Text(text = text)
                    val bitmap = UiUtils.generateQRCode(payInfo?.payUrl)?.asImageBitmap()
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