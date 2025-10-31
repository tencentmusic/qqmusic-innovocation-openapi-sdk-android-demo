package com.tencent.qqmusic.qplayer.ui.activity.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Switch
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.AuthType
import com.tencent.qqmusic.openapisdk.core.openapi.EditFolderParam
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.openapisdk.model.folder.FolderTagCategory
import com.tencent.qqmusic.openapisdk.model.folder.FolderTagItem
import com.tencent.qqmusic.qplayer.BaseFunctionManager
import com.tencent.qqmusic.qplayer.ui.activity.LoadMoreItem
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.PartnerLoginActivity
import com.tencent.qqmusic.qplayer.ui.activity.download.DownloadActivity
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderListPage
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVResDetailPage
import com.tencent.qqmusic.qplayer.ui.activity.person.MinePageNew
import com.tencent.qqmusic.qplayer.ui.activity.person.MineViewModel
import com.tencent.qqmusic.qplayer.ui.activity.search.singerPage
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumListPage
import com.tencent.qqmusic.qplayer.ui.activity.songlist.BuyAlbumPage
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListPage
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "MinePage"

@Composable
fun MinePage(homeViewModel: HomeViewModel) {
    val activity = LocalContext.current as Activity

    val loginText = remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current
    val mineViewModel by lazy { MineViewModel() }

    DisposableEffect(lifecycleOwner) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mineViewModel.updateData()
                }

                else -> {}
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MinePageNew(mineViewModel)
        Spacer(modifier = Modifier.height(10.dp))
        LoginBotton(activity, mineViewModel, loginText)
        Spacer(modifier = Modifier.height(10.dp))
        MineSongList(activity, homeViewModel)
    }
}

// 常驻按钮
@Composable
fun ResidentButton(activity: Activity){
    MiniTextButton(
        text = "第三方登录",
        modifier = Modifier.padding(0.dp),
        onClick = {
            activity.startActivity(Intent(activity, PartnerLoginActivity::class.java))
        }
    )

    MiniTextButton(
        text = "token登录",
        modifier = Modifier.padding(0.dp)
    ) {
        activity.startActivity(Intent(activity, TokenLoginActivity::class.java))
    }
}

// 登录栏
@Composable
fun LoginBotton(activity: Activity, mineViewModel: MineViewModel, info: MutableState<String>) {
    var isLogin by remember { mutableStateOf(false) }
    var isBind by remember { mutableStateOf(false) }
    val userInfo = mineViewModel.loginInfo.collectAsState()
    isLogin = userInfo.value != null
    isBind = userInfo.value != null && userInfo.value?.type == AuthType.PARTNER
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!isLogin) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniTextButton(text = "扫码登录"){
                    OpenApiSDK.getLoginApi().qrCodeLogin(activity) { b, msg ->
                        if (b) {
                            info.value = "扫码已登录:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                            mineViewModel.updateData()
                        } else {
                            Toast.makeText(activity, "登录失败: $msg", Toast.LENGTH_SHORT)
                                .show()
                        }
                        HomeViewModel.clearRequestState()
                        mineViewModel.updateData()
                    }
                    activity.startActivity(Intent(activity, OpiQRCodeActivity::class.java))
                    info.value = "扫码登录结果:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                }
                MiniTextButton(text = "QQ登录") {
                    OpenApiSDK.getLoginApi().qqLoginWeb(activity) { ret, msg ->
                        activity.runOnUiThread {
                            Log.i(TAG, "LoginPage: qq ret $ret, msg: $msg")
                            if (ret.not()) {
                                Toast.makeText(activity, "QQ登录失败: $msg", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            HomeViewModel.clearRequestState()
                            mineViewModel.updateData()
                            info.value = "QQ已登录:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                        }
                    }
                }
                MiniTextButton(text = "微信登录"){
                    OpenApiSDK.getLoginApi()
                        .wxLogin(activity, BaseFunctionManager.proxy.getWxAPPID()) { ret, msg ->
                            Log.i(TAG, "LoginPage: wechat ret $ret")
                            if (ret.not()) {
                                Toast.makeText(
                                    activity,
                                    "微信登录失败: $msg",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            info.value = "微信已登录:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                            mineViewModel.updateData()
                            HomeViewModel.clearRequestState()
                        }
                }
                MiniTextButton(text = "QQ音乐登录"){
                    OpenApiSDK.getLoginApi().qqMusicLogin(activity) { b, msg ->
                        if (b) {

                            info.value = "QQ音乐已登录:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                        } else {
                            Toast.makeText(activity, "登录失败: $msg", Toast.LENGTH_SHORT).show()
                        }
                        mineViewModel.updateData()
                        HomeViewModel.clearRequestState()
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly){
                val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
                PhoneLoginDialog(mineViewModel, showDialog = showDialog, setShowDialog = setShowDialog)
                MiniTextButton(text = "手机登录"){ setShowDialog(true) }
                val showOpenIdLoginDialog = remember { mutableStateOf(false) }
                MiniTextButton(text = "OpenId登录"){ showOpenIdLoginDialog.value = true }
                if (showOpenIdLoginDialog.value) {
                    OpenIdLoginDialog(mineViewModel, showOpenIdLoginDialog)
                }
                ResidentButton(activity)
            }
        }else{
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly){
                MiniTextButton(text = "退出登录") {
                    mineViewModel.logout()
                    OpenApiSDK.getPlayerApi().setPreferSongQuality(Quality.HQ)
                    HomeViewModel.clearRequestState()
                }
                ResidentButton(activity)
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MineSongList(activity: Activity, viewModel: HomeViewModel) {
    // 定义分类
    val categories = mapOf(
        "自建歌单" to listOf(
            "我喜欢",
            "自建歌单列表"
        ),
        "收藏" to listOf(
            "收藏歌单",
            "收藏专辑",
            "收藏的MV",
            "收藏有声单曲",
            "收藏播客单曲",
            "收藏有声专辑",
            "收藏有声播客"
        ),
        "最近播放" to listOf(
            "最近播放单曲",
            "最近播放专辑",
            "最近播放歌单",
            "最近播放长音频"
        ),
        "本地" to listOf(
            "已下载歌曲"
        ),
        "已购内容" to listOf(
            "已购数专",
            "已购单曲"
        ),
        "其他" to listOf(
            "订阅歌手",
            "其他端播放列表"
        )
    )

    // 当前选中的分类
    var selectedCategory by remember { mutableStateOf("自建歌单") }
    // 当前显示的页面列表
    val currentPages by remember(selectedCategory) {
        derivedStateOf { categories[selectedCategory] ?: emptyList() }
    }
    // 避免多次创建PagingSource
    val orderedSingerPagingSource by lazy { viewModel.pagingCollectedSinger() }

    val pagerState = rememberPagerState()
    val composableScope = rememberCoroutineScope()

    Column {
        // 分类选择行
        ScrollableTabRow(
            selectedTabIndex = categories.keys.indexOf(selectedCategory),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.pagerTabIndicatorOffset(
                        pagerState = pagerState, // 使用同一个pagerState实例
                        tabPositions = tabPositions
                    )
                )
            }
        ) {
            categories.keys.forEachIndexed { index, category ->
                Tab(
                    text = { Text(text = category) },
                    selected = selectedCategory == category,
                    onClick = {
                        selectedCategory = category
                        composableScope.launch {
                            pagerState.scrollToPage(0)
                        }
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.Gray
                )
            }
        }

        // 页面内容Tab
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                )
            }
        ) {
            currentPages.forEachIndexed { index, title ->
                Tab(
                    text = { Text(text = title) },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        composableScope.launch {
                            pagerState.scrollToPage(index)
                        }
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.Gray
                )
            }
        }

        HorizontalPager(
            count = currentPages.size,
            state = pagerState
        ) { page ->
            // 原有页面内容保持不变...
            when (currentPages[page]) {
                "我喜欢" -> {
                    LaunchedEffect(Unit) {
                        viewModel.fetchMyLikeSong()
                    }
                    val songs = remember { viewModel.songOfMyLike }
                    val loadMoreItem = LoadMoreItem(viewModel.songOfMyLikeHasMore) {
                        viewModel.fetchMyLikeSong()
                    }
                    SongListPage(
                        songs = songs.value,
                        needPlayer = false,
                        loadMoreItem = loadMoreItem
                    )
                }

                "自建歌单列表" -> {
                    viewModel.fetchMineFolder()
                    Column(modifier = Modifier.fillMaxSize()) {
                        NewFolder(viewModel)
                        FolderListPage(viewModel.mineFolders)
                    }
                }

                "收藏的MV" -> {
                    viewModel.fetchFavMVList()
                    MVResDetailPage(viewModel.mvFavList)
                }

                "收藏专辑" -> {
                    viewModel.fetchCollectedAlbum()
                    AlbumListPage(viewModel.favAlbums)
                }

                "收藏歌单" -> {
                    viewModel.fetchCollectedFolder()
                    FolderListPage(viewModel.favFolders)
                }

                "最近播放单曲" -> {
                    SongListPage(viewModel.pagingRecentSong())
                }

                "最近播放专辑" -> {
                    viewModel.fetchRecentAlbums()
                    AlbumListPage(albums = viewModel.recentAlbums)
                }

                "最近播放歌单" -> {
                    // 最近播放（歌单）
                    viewModel.fetchRecentFolders()
                    FolderListPage(folders = viewModel.recentFolders)
                }

                "最近播放长音频" -> {
                    viewModel.fetchRecentLongRadios()
                    AlbumListPage(albums = viewModel.recentLongRadio)
                }

                "已购数专" -> {
                    LaunchedEffect(Unit) {
                        viewModel.fetchBuyRecordOfAlbum()
                    }
                    val payedAlbums = remember { viewModel.albumOfRecord }
                    val loadMoreItem = LoadMoreItem(viewModel.albumOfRecordHasMore) {
                        viewModel.fetchBuyRecordOfAlbum()
                    }
                    BuyAlbumPage(albums = payedAlbums.value, loadMoreItem = loadMoreItem)
                }

                "已购单曲" -> {
                    LaunchedEffect(Unit) {
                        viewModel.fetchBuyRecordOfSong()
                    }
                    val payedSong = remember { viewModel.songOfRecord }
                    val loadMoreItem = LoadMoreItem(viewModel.songOfRecordHasMore) {
                        viewModel.fetchBuyRecordOfSong()
                    }
                    SongListPage(songs = payedSong.value, needPlayer = false, loadMoreItem = loadMoreItem)
                }

                "收藏有声单曲" -> {
                    SongListPage(viewModel.pagingLongAudioSong(1))
                }

                "收藏播客单曲" -> {
                    SongListPage(viewModel.pagingLongAudioSong(2))
                }

                "收藏有声专辑" -> {
                    viewModel.fetchCollectedAlbum(1)
                    AlbumListPage(viewModel.favAlbums)
                }

                "收藏有声播客" -> {
                    viewModel.fetchCollectedAlbum(2)
                    AlbumListPage(viewModel.favAlbums)
                }

                "订阅歌手" -> {
                    singerPage(orderedSingerPagingSource)
                }

                "已下载歌曲" -> {
                    TextButton(onClick = {
                        activity.startActivity(
                            Intent(
                                activity,
                                DownloadActivity::class.java
                            ).apply {
                                putExtra(DownloadActivity.FROM_DOWNLOAD_SONG_PAGE, true)
                            })
                    }) {
                        Text(text = "前往已下载歌曲")
                    }
                }

                "其他端播放列表" -> {
                    viewModel.fetchOtherFlatPlayList()
                    Column {
                        val songOfOther = viewModel.songOfOther
                        if (songOfOther.playList.isNullOrEmpty()) {
                            Text(text = "未获取到其他平台的数据")
                        } else {
                            Text(text = "来自${songOfOther.getPlatformString()}平台的${songOfOther.playList?.size}首歌(${songOfOther.suggestShowText})")
                        }
                        songOfOther.playList?.let {
                            SongListPage(songs = it, needPlayer = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewFolder(viewModel: HomeViewModel) {
    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp).clickable {
                setShowDialog(true)
            }
    ) {
        FolderDialog(viewModel, showDialog = showDialog, isCreate = true, setShowDialog = setShowDialog)
        Icon(Icons.Filled.Add, "新建歌单",
            modifier = Modifier.size(30.dp))
        Text(text = "点击创建新个人歌单")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderDialog(
    viewModel: HomeViewModel,
    showDialog: Boolean,
    isCreate: Boolean,
    dissId: String = "",
    setShowDialog: (Boolean) -> Unit,
) {
    if (!showDialog) return

    val context = LocalContext.current
    var imagePath by remember { mutableStateOf("") }
    var prevImageUrl by remember { mutableStateOf(imagePath) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File.createTempFile(
                    "local_image_",
                    ".${context.contentResolver.getType(uri)?.substringAfterLast('/')}",
                    context.cacheDir
                )
                inputStream?.use { it.copyTo(file.outputStream()) }
                imagePath = file.absolutePath
                prevImageUrl = "file://$imagePath"
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    var foldName by rememberSaveable { mutableStateOf("") }
    var folderDesc by rememberSaveable { mutableStateOf("") }
    var isPublic by rememberSaveable { mutableStateOf(true) }

    var tagCategories by remember { mutableStateOf(listOf<FolderTagCategory>()) }
    var selectedTags by rememberSaveable { mutableStateOf<List<FolderTagItem>>(emptyList()) }

    Dialog(onDismissRequest = {
        Log.d(TAG, "FolderDialog Dismiss Request")
        setShowDialog(false)
    }) {
        LazyColumn (
            modifier = Modifier
                .background(Color.White)
                .padding(5.dp)
        ) {
            item {
                OutlinedTextField(
                    value = foldName,
                    onValueChange = { foldName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入歌单名字") }
                )
                OutlinedTextField(
                    value = folderDesc,
                    onValueChange = { folderDesc = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入歌单描述") }
                )

                Text("选择标签:", modifier = Modifier.padding(top = 8.dp))

                Button(
                    onClick = {
                        OpenApiSDK.getOpenApi().getEditFolderTags {
                            if (it.isSuccess()) {
                                tagCategories = it.data ?: emptyList()
                            }
                        }
                    }
                ) {
                    Text("加载标签列表")
                }
            }
            items(tagCategories.size) {
                val category = tagCategories[it]
                Text(
                    text = category.categoryName ?: "",
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    category.tagList?.forEach { tag ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable {
                                    selectedTags = if (selectedTags.contains(tag)) {
                                        selectedTags - tag
                                    } else {
                                        selectedTags + tag
                                    }
                                }
                                .padding(2.dp)
                        ) {
                            Checkbox(
                                checked = selectedTags.contains(tag),
                                onCheckedChange = {
                                    selectedTags = if (it) {
                                        selectedTags + tag
                                    } else {
                                        selectedTags - tag
                                    }
                                }
                            )
                            Text(text = tag.tagName ?: "", modifier = Modifier.padding(start = 3.dp))
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("选择封面")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(text = "公开歌单", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { setShowDialog(false) }) {
                        Text("取消")
                    }

                    OutlinedButton(onClick = {
                        if (foldName.isEmpty()) return@OutlinedButton
                        val folderParam = EditFolderParam(
                            folderName = foldName,
                            folderDesc = folderDesc,
                            folderId = dissId,
                            folderTagList = selectedTags,
                            folderPicPath = imagePath,
                            isPublic = isPublic
                        )
                        if (isCreate) {
                            OpenApiSDK.getOpenApi().createFolder(folderParam) {
                                if (it.isSuccess()) {
                                    setShowDialog(false)
                                    HomeViewModel.clearRequestState()
                                    viewModel.fetchMineFolder()
                                } else {
                                    foldName = ""
                                }
                            }
                        } else {
                            OpenApiSDK.getOpenApi().editFolder(folderParam) {
                                if (it.isSuccess()) {
                                    setShowDialog(false)
                                    HomeViewModel.clearRequestState()
                                    viewModel.fetchMineFolder()
                                } else {
                                    foldName = ""
                                }
                            }
                        }
                    }) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
fun OpenIdLoginDialog(mineViewModel: MineViewModel, showDialog: MutableState<Boolean>) {
    var openId by rememberSaveable {
        mutableStateOf("")
    }
    var accessToken by rememberSaveable {
        mutableStateOf("")
    }
    Dialog(onDismissRequest = {
        showDialog.value = false
    }) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(5.dp)
        ) {
            OutlinedTextField(
                value = openId,
                onValueChange = {
                    openId = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "请输入OpenId") }
            )
            OutlinedTextField(
                value = accessToken,
                onValueChange = {
                    accessToken = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "请输入AccessToken") }
            )
            OutlinedButton(onClick = {
                OpenApiSDK.getLoginApi()
                    .openIdLogin(MustInitConfig.APP_ID, openId, accessToken) { suc, msg ->
                        UiUtils.showToast("登录结果：$suc $msg")
                        if (suc) {
                            mineViewModel.updateData()
                        }
                    }
                showDialog.value = false
            }) {
                Text(text = "登录")
            }
        }
    }
}

@Composable
fun PhoneLoginDialog(model: MineViewModel, showDialog: Boolean, setShowDialog: (Boolean) -> Unit) {
    if (!showDialog) return

    val activity = LocalContext.current as Activity
    var phoneNum by rememberSaveable { mutableStateOf("") }
    var verifyCode by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = {
        Log.d(TAG, "FolderDialog Dismiss Request")
        setShowDialog(false)
    }) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(5.dp)
        ) {
            OutlinedTextField(
                value = phoneNum,
                onValueChange = {
                    phoneNum = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入11位手机号") }
            )

            OutlinedButton(onClick = {
                Log.d(TAG, "phoneNum: $phoneNum")
                if (phoneNum.isEmpty()) return@OutlinedButton

                OpenApiSDK.getLoginApi().sendPhoneVerificationCode(phoneNum) { b, msg, info ->
                    if (b == 0) {
                        UiUtils.showToast("获取验证码成功")
                    } else {
                        UiUtils.showToast("获取失败:$msg")
                    }
                }
            }) {
                Text("获取验证码")
            }

            OutlinedTextField(
                value = verifyCode,
                onValueChange = {
                    verifyCode = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入验证码") }
            )

            OutlinedButton(onClick = {
                Log.d(TAG, "phoneNum: $phoneNum， verifyCode:$verifyCode")
                if (phoneNum.isEmpty()) return@OutlinedButton
                if (verifyCode.isEmpty()) return@OutlinedButton

                OpenApiSDK.getLoginApi().phoneLogin(
                    phoneNum,
                    verifyCode,
                    activity
                ) { code, msg, phoneLoginInfo ->
                    if (code == 1) {
                        UiUtils.showToast("需要绑定Q音账号")
                        phoneLoginInfo?.apply {
                            PhoneLoginQRCodeActivity.start(
                                activity,
                                phoneLoginInfo.phoneToken,
                                phoneLoginInfo.authCode,
                                phoneLoginInfo.qrCode
                            )
                        }
                    } else {
                        UiUtils.showToast("登录失败:$msg")
                    }
                    model.updateData()
                    HomeViewModel.clearRequestState()
                }
            }) {
                Text("验证码登录")
            }

            OutlinedButton(onClick = { setShowDialog(false) }) {
                Text("取消")
            }
        }
    }
}

@Composable
fun MiniTextButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.width(IntrinsicSize.Max),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.White,
            contentColor = Color.Black
        ),
        border = BorderStroke(1.dp, Color.Black),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(1.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
        )
    }
}