package com.tencent.qqmusic.qplayer.ui.activity.home.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.ai.entity.AICreateImageSongRequest
import com.tencent.qqmusic.ai.entity.AICreateSongResponse
import com.tencent.qqmusic.ai.entity.AICreateTaskInfo
import com.tencent.qqmusic.ai.entity.AIPayOrderRequest
import com.tencent.qqmusic.ai.entity.AIPayOrderResp
import com.tencent.qqmusic.ai.entity.AIQueryStatusReq
import com.tencent.qqmusic.ai.entity.GetSongStyleReq
import com.tencent.qqmusic.ai.entity.PicSongStyle
import com.tencent.qqmusic.ai.function.base.AISceneType
import com.tencent.qqmusic.ai.function.base.IAIFunction
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.cover.AITimbreTAG
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.CountDownLatch

private val TAG = "AIComposingSongPage"

@Composable
fun AIImageSongPage(backPrePage: () -> Unit) {
    val aiViewModel: AIViewModel = viewModel()
    var firstLoad by remember { mutableStateOf(true) }
    if (firstLoad) {
        firstLoad = false
        aiViewModel.getImageSongStyleList(GetSongStyleReq(type = 1))
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
    val songStyleList = viewModel.songImageStyleList
    val hotWorkList = viewModel.hotAiCreateSongList
    var sameStyleTaskId: String? = null
    val aiFunction = OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)

    var keyWord by remember { mutableStateOf(TextFieldValue("")) }

    var selectedSongStyleId by remember {
        mutableStateOf(15)
    }

    val context = LocalContext.current
    var taskInfo by remember { mutableStateOf<AICreateTaskInfo?>(null) }
    var consoleMessage by remember { mutableStateOf("") }
    var createSongResp by remember { mutableStateOf<AICreateSongResponse?>(null) }
    var payOrderResp by remember { mutableStateOf<AIPayOrderResp?>(null) }
    val qrCodeImage = remember { mutableStateOf<ImageBitmap?>(null) }
    // 图片
    var imagePath by remember { mutableStateOf("https://music-file6.y.qq.com/iotsdkmksong/u/oKSkNKEioKEqNv/124ab/e81090d807544b1e6a0e28d942eb4113f05a2335_1347a.png") } // 实际上传用的path
    var prevImageUrl by remember { mutableStateOf(imagePath) } // 预览图url
    var showImageDialog by remember { mutableStateOf(false) }
    var editableUrl by remember { mutableStateOf(prevImageUrl) }

    // 图片点击弹窗，在线图片下载
    if (showImageDialog) {
        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            title = { Text("图片URL") },
            text = {
                TextField(
                    value = editableUrl,
                    onValueChange = { newUrl ->
                        editableUrl = newUrl
                        downloadImage(newUrl, context,
                            onSuccess = { tempFile ->
                                imagePath = tempFile.absolutePath
                                prevImageUrl = "file://$imagePath"
                                editableUrl = prevImageUrl
                                consoleMessage += "\n图片下载成功: $prevImageUrl"
                            },
                            onError = { errorMsg ->
                                consoleMessage += "\n$errorMsg"
                            }
                        )
                    },
                    label = { Text("编辑图片地址") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            buttons = {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            showImageDialog = false
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("确定")
                    }
                    Button(onClick = { showImageDialog = false }) {
                        Text("取消")
                    }
                }
            }
        )
    }

    // 相册选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val consoleMessageNew = StringBuilder(consoleMessage)
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File.createTempFile(
                    "local_image_",
                    ".${context.contentResolver.getType(uri)?.substringAfterLast('/')}",
                    context.cacheDir
                )
                inputStream?.use { it.copyTo(file.outputStream()) }
                // 立即更新本地预览
                imagePath = file.absolutePath
                prevImageUrl = "file://$imagePath"  // 仅用于预览
                consoleMessageNew.appendLine("imagePath=${imagePath}")
            } catch (e: Exception) {
                consoleMessageNew.appendLine("图片选择错误: ${e.message}")
            }
            consoleMessage = consoleMessageNew.toString()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // URL输入和图片预览区域
            Column {
                Box {
                    Text(text = "日志:")
                    ScrollableTextArea(consoleMessage)
                    IconButton(
                        onClick = { consoleMessage = "" },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Filled.Clear, "清空日志")
                    }
                }
                Row {
                    Image(
                        painter = rememberImagePainter(prevImageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .padding(4.dp)
                            .clickable {
                                editableUrl = prevImageUrl
                                showImageDialog = true
                            }
                    )
                    Column(modifier = Modifier.padding(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text("换本地图片")
                            }

                            Button(onClick = {
                                aiFunction?.uploadAIImage(imagePath, callback = { info ->
                                    val uploadMessage = if (info.isSuccess()) {
                                        "upload success: ${info.url}"
                                    } else {
                                        "upload failed: ${info.errorMsg}"
                                    }
                                    prevImageUrl = info.url.toString()
                                    val consoleMessageNew = StringBuilder(consoleMessage)
                                    consoleMessageNew.appendLine(uploadMessage)
                                    consoleMessage = consoleMessageNew.toString()
                                })
                            }) {
                                Text(text = "1.开始上传")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(onClick = {
                                aiFunction?.createAIImageCheckTask(
                                    AIQueryStatusReq(
                                        AISceneType.AI_SCENE_TYPE_IMAGE_SONG_CHECK,
                                        id = prevImageUrl
                                    ),
                                    callback = { info ->
                                        val uploadMessage = if (info.isSuccess()) {
                                            "createAIImageCheckTask: success $info"
                                        } else {
                                            "createAIImageCheckTask: ${info.errorMsg}"
                                        }
                                        val consoleMessageNew = StringBuilder(consoleMessage)
                                        consoleMessageNew.appendLine(uploadMessage)
                                        consoleMessage = consoleMessageNew.toString()
                                    })
                            }) {
                                Text(text = "2.审核")
                            }
                            Button(onClick = {
                                aiFunction?.queryCreateTaskStatus(
                                    AIQueryStatusReq(
                                        AISceneType.AI_SCENE_TYPE_IMAGE_SONG_CHECK,
                                        id = prevImageUrl
                                    ),
                                    callback = { info ->
                                        val uploadMessage = if (info.isSuccess()) {
                                            val text =
                                                when(info.data){
                                                    1 -> "审核中"
                                                    2 -> "已完成"
                                                    1001 -> "图片违规"
                                                    else -> "异常失败"
                                                }
                                            "queryCreateTaskStatus: $text (${info.data})"
                                        } else {
                                            "queryCreateTaskStatus: ${info.errorMsg}"
                                        }
                                        val consoleMessageNew = StringBuilder(consoleMessage)
                                        consoleMessageNew.appendLine(uploadMessage)
                                        consoleMessage = consoleMessageNew.toString()
                                    })
                            }) {
                                Text(text = "3.查询审核状态")
                            }
                        }
                        Button(onClick = {
                            aiFunction?.createAIImageCheckTask(
                                AIQueryStatusReq(
                                    AISceneType.AI_SCENE_TYPE_IMAGE_SONG_CHECK,
                                    id = prevImageUrl
                                ),
                                callback = { info ->
                                    val consoleMessageNew = StringBuilder(consoleMessage)
                                    if (info.isSuccess()) {
                                        consoleMessageNew.appendLine("createAIImageCheckTask: success $info")
                                        runBlocking {
                                            var status:Int? = null
                                            for (times in 0..10){
                                                val countDownLatch = CountDownLatch(1)
                                                aiFunction.queryCreateTaskStatus(
                                                    req = AIQueryStatusReq(
                                                        scene = AISceneType.AI_SCENE_TYPE_IMAGE_SONG_CHECK,
                                                        id = prevImageUrl
                                                    ), callback = { result ->
                                                        status = result.data
                                                        countDownLatch.countDown()
                                                    }
                                                )
                                                countDownLatch.await()
                                                val text =
                                                    when(status){
                                                        1 -> "审核中"
                                                        2 -> "已完成"
                                                        1001 -> "图片违规"
                                                        else -> "异常失败"
                                                    }
                                                consoleMessageNew.appendLine("审核状态:$text")
                                                if (status!=1) break
                                                delay(500)
                                            }
                                        }
                                    } else {
                                        consoleMessageNew.appendLine("createAIImageCheckTask: ${info.errorMsg}")
                                    }
                                    consoleMessage = consoleMessageNew.toString()
                                })
                        }){
                            Text(text = "2+3.提交审核并轮询状态")
                        }
                    }

                }
            }
        }

        item {
            Text(text = "选择歌曲风格")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                songStyleList.forEach {
                    SongImageStyleItem(it, selectedSongStyleId) {
                        selectedSongStyleId = it.id ?: 0
                    }
                }
            }
            Button(onClick = {
                aiFunction?.createAIImageSong(
                    AICreateImageSongRequest(
                        picType = selectedSongStyleId,
                        imgurl = prevImageUrl
                    ),
                    callback = { info ->
                        val uploadMessage = if (info.isSuccess()) {
                            createSongResp = info
                            "createAIImageSong: ${createSongResp?.taskId}\n" +
                                    "是否付费:${createSongResp?.payment}\n"+
                                    "商品信息:${createSongResp?.produceInfoList}"

                        } else {
                            "createAIImageSong: ${info.errorMsg}"
                        }
                        val consoleMessageNew = StringBuilder(consoleMessage)
                        consoleMessageNew.appendLine(uploadMessage)
                        consoleMessage = consoleMessageNew.toString()
                    })
            }) {
                Text(text = "4.一键生成")
            }

        }

        item {
            Row {
                Button(onClick = {
                    val produceInfo = createSongResp?.produceInfoList?.firstOrNull()
                    aiFunction?.createSongPayOrder(
                        AIPayOrderRequest(
                            scene = AISceneType.AI_SCENE_TYPE_IMAGE_SONG,
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
                            "createSongPayOrder: ${resp.orderId}"
                        } else {
                            "createSongPayOrder: ${resp.errorMsg}"
                        }

                        val consoleMessageNew = StringBuilder(consoleMessage)
                        consoleMessageNew.appendLine(uploadMessage)
                        consoleMessage = consoleMessageNew.toString()
                    }
                }) {
                    Text(text = "5.创建支付订单")
                }
                Spacer(modifier = Modifier.width(2.dp))
                Image(
                    bitmap = qrCodeImage.value ?: ImageBitmap(1, 1),
                    null,
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        item {
            Row {
                Button(onClick = {
                    val orderId = payOrderResp?.orderId
                    if (orderId != null) {
                        aiFunction?.fetchAIPayStatus(orderId) { resp ->
                            val uploadMessage = if (resp.isSuccess()) {
                                val msg = if (resp.state == 3) "已支付" else "等待支付"
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
                    Text(text = "6.查询支付状态")
                }
                Spacer(modifier = Modifier.width(2.dp))

                Button(onClick = {
                    aiFunction?.queryCreateTaskStatus(
                        AIQueryStatusReq(
                            AISceneType.AI_SCENE_TYPE_IMAGE_SONG,
                            id = createSongResp?.taskId ?: ""
                        )
                    ) { info ->
                        val uploadMessage = if (info.isSuccess()) {
                            val text = if (info.data == 2) "已完成" else "正在生成"
                            "queryCreateTaskStatus: $text"
                        } else {
                            "queryCreateTaskStatus: ${info.errorMsg}"
                        }
                        val consoleMessageNew = StringBuilder(consoleMessage)
                        consoleMessageNew.appendLine(uploadMessage)
                        consoleMessage = consoleMessageNew.toString()
                    }
                }) {
                    Text(text = "7.查询任务状态")
                }
            }
        }


//        item {
//            taskInfo?.let { AICreateTaskInfoItem(it, scene = "1") }
//        }

//        item {
//            Divider()
//            Row {
//                Button(onClick = {
//                    viewModel.getHotAICreateImageSongs()
//                }) {
//                    Text(text = "获取热门作品")
//                }
//                Spacer(modifier = Modifier.width(2.dp))

//                Button(onClick = {
//                    aiFunction?.getAIHotSongTag(aiTabDataReq = AITabDataReq(AISceneType.AI_SCENE_TYPE_IMAGE_SONG)) { info ->
//                        val uploadMessage = if (info.isSuccess()) {
//                            "getAIHotSongTag: $info"
//                        } else {
//                            "getAIHotSongTag: ${info.errorMsg}"
//                        }
//                        val consoleMessageNew = StringBuilder(consoleMessage)
//                        consoleMessageNew.appendLine(uploadMessage)
//                        consoleMessage = consoleMessageNew.toString()
//                    }
//                }) {
//                    Text(text = "获取Tab列表")
//                }
//                Spacer(modifier = Modifier.width(2.dp))
//
//                Button(onClick = {
//                    aiFunction?.getAISongByTagName("热门", 10, "0") { info ->
//                        val uploadMessage = if (info.isSuccess()) {
//                            "getAISongByTagName: $info"
//                        } else {
//                            "getAISongByTagName: ${info.errorMsg}"
//                        }
//                        val consoleMessageNew = StringBuilder(consoleMessage)
//                        consoleMessageNew.appendLine(uploadMessage)
//                        consoleMessage = consoleMessageNew.toString()
//                    }
//                }) {
//                    Text(text = "获取Tab列表")
//                }
//            }
//        }
//        items(hotWorkList) {
//            HotCreateSongItem(it) {
//                keyWord = TextFieldValue((it.prompt ?: "") as String)
//                sameStyleTaskId = it.taskId
//            }
//        }
    }
}

@Composable
fun SongImageStyleItem(songStyle: PicSongStyle, selectedId: Int, onClick: () -> Unit) {
    val textColor = if (selectedId == songStyle.id) {
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
        Text(text = songStyle.name ?: "", color = textColor)
        Text(text = songStyle.title ?: "", color = textColor)
    }


}


@Composable
fun ScrollableTextArea(consoleMessage: String) {
    Box(
        modifier = Modifier
            .background(Color.White) // 背景色
            .size(width = 800.dp, height = 200.dp) // 固定大小
            .padding(8.dp) // 内边距
    ) {
        Text(
            text = consoleMessage,
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(rememberScrollState()) // 垂直滚动
                .wrapContentSize(),
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = Color.Black,
            maxLines = Int.MAX_VALUE, // 允许无限行，但通过滚动显示
            overflow = TextOverflow.Visible // 文本超出时显示滚动条
        )
    }
}

// 下载网络图片的方法
fun downloadImage(
    url: String,
    context: Context,
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val tempFile = File.createTempFile(
                "downloaded_image_",
                ".png",
                context.cacheDir
            )
            OkHttpClient().newCall(
                Request.Builder().url(url).build()
            ).execute().use { response ->
                if (response.isSuccessful) {
                    tempFile.outputStream().use { output ->
                        response.body?.byteStream()?.copyTo(output)
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess(tempFile)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("下载失败: HTTP ${response.code}")
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("下载异常: ${e.message}")
            }
        }
    }
}

@Composable
@Preview(showBackground = true, device = "id:pixel_5")
fun AIImageSongPagePreview() {
    AIImageSongPage(backPrePage = {})
}