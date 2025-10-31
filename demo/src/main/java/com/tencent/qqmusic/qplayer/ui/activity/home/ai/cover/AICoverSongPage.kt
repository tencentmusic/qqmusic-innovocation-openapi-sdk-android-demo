package com.tencent.qqmusic.qplayer.ui.activity.home.ai.cover

import android.graphics.Bitmap
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.ai.entity.AICoverDataInfo
import com.tencent.qqmusic.ai.entity.AICoverSongCreateType
import com.tencent.qqmusic.ai.entity.AITagData
import com.tencent.qqmusic.ai.function.base.IAIFunction
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.AIPersonalTimbrePage
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.AITimbreCreatePage
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.AITimbrePage
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.AIViewModel
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.QrCodeDialog
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.functionIndex
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val AITimbreTAG = "timbreCreate"
internal val AIPersonalTimbreTAG = "timbreRole"
internal val AISearchTAG = "serach"
internal val AIBuyTAG = "buy"
internal val AIVoucherTAG = "voucher"

internal val AIPersonalCreatePageTAG = "AIPersonalCreatePageTAG"
private var songMid: String? = ""
var buyAccDataInfo: AICoverDataInfo? = null

@Composable
fun AICoverSongPage() {
    val navController = rememberNavController()
    val aiViewModel : AIViewModel = viewModel()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { BasePage(aiViewModel, navController) }
        composable(AITimbreTAG) {
            AITimbreCreatePage {
                navController.popBackStack()
            }
        }
        composable(AIPersonalTimbreTAG) {
            AIPersonalTimbrePage(aiViewModel) {
                navController.popBackStack()
            }
        }
        composable(AISearchTAG) {
            AISearchPage(navController = navController) {
                navController.popBackStack()
            }
        }
        composable(AIPersonalCreatePageTAG) {
            AIPersonalCreatePage {
                navController.popBackStack()
            }
        }

        composable(AIBuyTAG) {
            AiCoverBuyPage(buyAccDataInfo) {
                navController.popBackStack()
            }
        }

        composable(AIVoucherTAG) {
            AIVoucherPage {
                navController.popBackStack()
            }
        }
    }

}

@Composable
private fun BasePage(aiViewModel : AIViewModel = viewModel(), navController: NavHostController) {

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            functionIndex.value = -1
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


    val saveableStateHolder = rememberSaveableStateHolder()
    saveableStateHolder.SaveableStateProvider(true) {
        Column(modifier = Modifier.fillMaxSize()) {
            AITimbrePage(navController = navController, aiViewModel = aiViewModel, onlyPersonal = true)

            Row (modifier = Modifier.padding(10.dp)) {
                Button(onClick = {
                    navController.navigate(AISearchTAG)
                }) {
                    Text(text = "搜索")
                }
                Button(modifier = Modifier.padding(10.dp, 0.dp), onClick = {
                    navController.navigate(AIVoucherTAG)
                }) {
                    Text(text = "优惠券")
                }
            }
            AIHotPage(navController = navController)
        }
    }
}


@Composable
private fun AIHotPage(aiViewModel: AIViewModel = viewModel(), navController: NavHostController?) {
    LaunchedEffect(key1 = 1) {
        aiViewModel.getAITagList()
    }

    val currentTag = remember {
        mutableStateOf<AITagData?>(null)
    }

    val currentSubTag = remember {
        mutableStateOf<AITagData?>(null)
    }

    val currentPage = remember { mutableStateOf(0) } // 用于记录当前页码

    Column(modifier = Modifier.padding(top = 10.dp)) {
        LazyRow {
            aiViewModel.aiCoverTagListData.forEach {
                if (currentTag.value == null) {
                    currentTag.value = it
                    val subs = it.subTags
                    if (subs?.isNotEmpty() == true) {
                        currentSubTag.value = subs[0]
                    }
                    aiViewModel.getAICoverSongByTag(it.tabId ?: 0, it.subTags?.getOrNull(0)?.tabId, 0)
                }
                item {
                    Box(modifier = Modifier
                        .padding(start = 10.dp, end = 10.dp)
                        .clickable {
                            currentTag.value = it
                            currentPage.value = 0 // 重置页码
                            val subID = if (it.subTags.isNullOrEmpty()) null else it.subTags?.getOrNull(0)?.tabId
                            aiViewModel.getAICoverSongByTag(currentTag.value?.tabId ?: 0, subID, 0)
                        }) {
                        Text(text = it.tabName ?: "")
                    }
                }
            }
        }
        if (currentTag.value != null) {
            LazyRow {
                currentTag.value?.subTags?.forEach {
                    item {
                        Box(modifier = Modifier
                            .padding(start = 10.dp, end = 10.dp, top = 20.dp)
                            .clickable {
                                currentSubTag.value = it
                                currentPage.value = 0 // 重置页码
                                aiViewModel.getAICoverSongByTag(currentTag.value?.tabId ?: 0, it.tabId, 0)
                            }) {
                            Text(text = it.tabName ?: "")
                        }
                    }
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            aiViewModel.aiCoverSongList.forEach {
                item {
                    AiSongItem(it, false, navController)
                }
            }
            // 添加翻页item
            item {
                Box(modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .clickable {
                        val page = (aiViewModel.passBackIndex["getAICoverSongByTag"] ?: "0").ifBlank { "0" }
                        currentPage.value = if (page.toInt() < 0) 0 else page.toInt()
                        aiViewModel.getAICoverSongByTag(currentTag.value?.tabId ?: 0, currentSubTag.value?.tabId, currentPage.value)
                    }) {
                    val nextIndex = aiViewModel.passBackIndex["getAICoverSongByTag"]
                    if (nextIndex.isNullOrEmpty()) {
                        Text(text = "已经是最后一页")
                    } else {
                        Text(
                            text = "第${currentPage.value}页,点击翻页->", fontSize = 18.sp,
                            color = Color.Blue
                        )
                    }
                }
            }
        }

    }
}

@Composable
fun AISearchPage(aiViewModel: AIViewModel = viewModel(), navController: NavHostController?, backPrePage: () -> Unit) {
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
    val searchWord = remember {
        mutableStateOf("")
    }
    val currentPage = remember { mutableStateOf(0) } // 用于记录当前页码
    Column(modifier = Modifier.fillMaxSize()) {
        TextField(value = searchWord.value, onValueChange = {
            searchWord.value = it
        }, label = { Text(text = "搜索") }, leadingIcon = @Composable {// 设置左边图标
            Image(
                imageVector = Icons.Filled.Search,
                contentDescription = "search", //image的无障碍描述
            )
        }, modifier = Modifier.fillMaxWidth())

        LaunchedEffect(searchWord.value) {
            currentPage.value = 0
            aiViewModel.passBackIndex.remove("getSearchSongList")
            aiViewModel.getSearchResultByWord(searchWord.value, 0)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            aiViewModel.aiSearchCoverSongList.forEach {
                item {
                    AiSongItem(it, navController = navController)
                }
            }
            // 添加翻页item
            item {
                Box(modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .clickable {
                        currentPage.value = aiViewModel.aiSearchNext ?: 0
                        aiViewModel.getSearchResultByWord(searchWord.value, currentPage.value)
                    }) {
                    if (aiViewModel.passBackIndex["getSearchSongList"] == "-1") {
                        Text(text = "已经是最后一页")
                    } else {
                        Text(text = "点击翻页->", fontSize = 18.sp, color = Color.Blue)
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun AiSongItem(aiCoverDataInfo: AICoverDataInfo, isPersonal: Boolean = false, navController: NavHostController? = null) {
    val aiViewModel: AIViewModel = viewModel()
    ConstraintLayout(
        modifier = Modifier
            .padding(top = 10.dp, start = 10.dp)
            .fillMaxSize()
    ) {
        val (songCover, songTitle, label, singerName, createWork, userIcons, tryCreate, personalData) = createRefs()
        Image(
            painter = rememberImagePainter(aiCoverDataInfo.accInfo?.cover),
            "",
            modifier = Modifier
                .width(60.dp)
                .height(60.dp)
                .zIndex(1f)
                .clip(CircleShape)
                .constrainAs(songCover) {
                    start.linkTo(parent.start)

                    top.linkTo(parent.top)
                }
        )


        //歌曲名
        Text(text = aiCoverDataInfo.accInfo?.songName ?: "", modifier = Modifier.constrainAs(songTitle) {
            top.linkTo(songCover.top)
            start.linkTo(songCover.end, margin = 10.dp)
        })
        // 歌手名
        Text(
            text = (if (isPersonal) aiCoverDataInfo.userInfo?.userName else aiCoverDataInfo.accInfo?.singer) ?: "",
            modifier = Modifier.constrainAs(singerName) {
                top.linkTo(songTitle.bottom)
                start.linkTo(songTitle.start, margin = 2.dp)
            })


        Text(
            text = aiCoverDataInfo.accInfo?.operationLabel ?: "",
            modifier = Modifier.constrainAs(label) {
                top.linkTo(singerName.bottom)
                start.linkTo(singerName.start, margin = 2.dp)
            })

        if (isPersonal.not()) {
            LazyRow(modifier = Modifier
                .clickable {
                    songMid = aiCoverDataInfo.accInfo?.songMid
                    navController?.navigate(AIPersonalCreatePageTAG)
                }
                .constrainAs(userIcons) {
                    start.linkTo(singerName.start)
                    bottom.linkTo(parent.bottom)
                }) {
                aiCoverDataInfo.makeInfo?.makeUserInfo?.take(3)?.forEach {
                    item {
                        Image(
                            painter = rememberImagePainter(it.userIcon),
                            "",
                            modifier = Modifier
                                .width(20.dp)
                                .height(20.dp)
                                .zIndex(1f)
                                .clip(CircleShape)
                        )
                    }
                }
                item {
                    Text(
                        text = aiCoverDataInfo.makeInfo?.makeCnt?.toString() ?: "",
                        modifier = Modifier
                            .clickable {
                                songMid = aiCoverDataInfo.accInfo?.songMid
                                navController?.navigate(AIPersonalCreatePageTAG)
                            }
                    )
                }


                item {
                    Text(
                        "${if (aiCoverDataInfo.collectInfo?.bCollected != 0) "已" else "未"}收藏",
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .clickable {
                                aiViewModel.operaWorkCollect(aiCoverDataInfo.accInfo?.songMid, null, aiCoverDataInfo.collectInfo?.bCollected == 0)
                            })
                }
            }

        } else {
            Column(modifier = Modifier.constrainAs(personalData) {
                start.linkTo(singerName.start)
                bottom.linkTo(parent.bottom)
            }) {

                val text = when (aiCoverDataInfo.type) {
                    1 -> "片段"
                    2 -> "全曲"
                    16 -> "专业"
                    64 -> "至臻"
                    else -> "未知"
                }
                Text("作品品质 $text")

                Text(
                    "创作时间 ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date((aiCoverDataInfo.createTime ?: 0) * 1000L))
                    }"
                )
                Row {
                    Text("${if (aiCoverDataInfo.collectInfo?.bCollected != 0) "已" else "未"}收藏", modifier = Modifier.clickable {
                        aiViewModel.operaWorkCollect(null, aiCoverDataInfo.ugcId, aiCoverDataInfo.collectInfo?.bCollected == 0)
                    })
                    Text("点赞数量 ${aiCoverDataInfo.likeInfo?.likeCount}", modifier = Modifier.padding(start = 10.dp))
                    Text("播放数量 ${aiCoverDataInfo.listenCnt?.listenCnt}", modifier = Modifier.padding(start = 10.dp))
                    Text("${if (aiCoverDataInfo.likeInfo?.bliked != 0) "已" else "未"}点赞", modifier = Modifier
                        .padding(start = 10.dp)
                        .clickable {
                            aiViewModel.likeAiCoverSong(aiCoverDataInfo.ugcId, aiCoverDataInfo.likeInfo?.bliked == 0)
                        })

                }
            }
        }
        Box(modifier = Modifier.constrainAs(createWork) {
            top.linkTo(parent.top)
            end.linkTo(parent.end)
            bottom.linkTo(parent.bottom)
        }) {
            TryAndGeneratePage(aiCoverDataInfo, isPersonal, navController = navController)
        }
    }
}


@Composable
fun AIPersonalCreatePage(aiViewModel: AIViewModel = viewModel(), navController: NavHostController? = null, backPrePage: () -> Unit) {
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

    LaunchedEffect(Unit) {
        aiViewModel.getPersonalCreateData(songMid ?: "", "")
    }

    LazyColumn {
        aiViewModel.aiPersonalCoverSongList.forEach {
            item {
                AiSongItem(it, isPersonal = true)
            }
        }
        // 添加翻页item
        item {
            Box(modifier = Modifier
                .padding(start = 20.dp, end = 20.dp)
                .clickable {
                    val page = aiViewModel.passBackIndex["getAICoverPersonalCreateData"] ?: ""
                    aiViewModel.getPersonalCreateData(songMid ?: "", page)
                }) {
                if (aiViewModel.passBackIndex["getAICoverPersonalCreateData"].isNullOrEmpty()) {
                    Text(text = "已经是最后一页")
                } else {
                    Text(text = "点击翻页->", fontSize = 18.sp, color = Color.Blue)
                }
            }
        }
    }
}


@Composable
fun TryAndGeneratePage(
    dataInfo: AICoverDataInfo,
    isPersonal: Boolean = true,
    aiViewModel: AIViewModel = viewModel(),
    navController: NavHostController? = null
) {
    val scope = rememberCoroutineScope()
    val tryStatus = remember { mutableStateOf(-1) }
    var showDialog by remember { mutableStateOf<Bitmap?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            aiViewModel.stopPlayCoverLink()
        }
    }

    Column(modifier = Modifier) {
        if (isPersonal.not()) {
            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    var audioStatus = -1
                    while (audioStatus == -1 || audioStatus == 2) {
                        aiViewModel.getTryLink(dataInfo.accInfo?.songMid ?: "", AICoverSongCreateType.SEG) {
                            audioStatus = it.audioStatus ?: -1
                            tryStatus.value = audioStatus
                            if (audioStatus == 3) {
                                aiViewModel.playLink(it.audioLink ?: "", dataInfo.accInfo?.songMid, dataInfo.ugcId, null)

                            }
                        }
                        delay(1000)
                    }
                }
            }) {
                if (tryStatus.value == 2) {
                    Text("试听生成中")
                } else {
                    Text("试听")
                }
            }
        }
        Button(onClick = {
            aiViewModel.getWorkLink(songMid = dataInfo.accInfo?.songMid, ugcId = dataInfo.ugcId ?: "") { url ->
                url?.let {
                    if (isPersonal) {
                        aiViewModel.playLink(it.audioLink ?: "", dataInfo.accInfo?.songMid, dataInfo.ugcId, null)

                    } else {
                        aiViewModel.playLink(it.audioLink ?: "", null, null, null)
                    }
                }
            }
        }) {
            Text("播放")
        }

        Button({
            buyAccDataInfo = dataInfo
            navController?.navigate(AIBuyTAG)
        }) {
            Text("生成")
        }
        dataInfo.ugcId?.takeIf { it.isNotEmpty() }?.let { ugcId ->
            Button({
                OpenApiSDK.getAIFunctionApi(IAIFunction::class.java)?.shareWork("3", ugcId) { resp ->
                    if (resp.isSuccess()) {
                        resp.data?.shareUrl?.let { url ->
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
                        UiUtils.showToast("获取分享链接失败:${resp.errorMsg}")
                    }
                }
            }) {
                Text("分享")
            }
        }

        if (showDialog != null) {
            QrCodeDialog(showDialog,
                onDismiss = { showDialog = null })
        }
    }

}


