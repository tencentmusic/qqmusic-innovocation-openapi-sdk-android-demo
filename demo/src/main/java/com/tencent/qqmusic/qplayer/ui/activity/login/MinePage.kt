package com.tencent.qqmusic.qplayer.ui.activity.login

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.PartnerLoginActivity
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderPage
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumPage
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MinePage"

@Composable
fun MinePage() {
    val activity = LocalContext.current as Activity

    val loginText = remember { mutableStateOf("") }
    val userInfoText = remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current
    val homeViewModel by lazy {
        HomeViewModel()
    }

    var loginCommon = "未知"
    var loginThird = "未知"
    var vipTxt = "未知"
    var vipLevel = "未知"
    val tipFormat = "普通登录：%s，第三方账号登录：%s\nVIP：%s\n会员等级：%s"

    homeViewModel.loginState.observe(lifecycleOwner, Observer {
        loginCommon = it.first.toString()
        loginThird = it.second.toString()
        loginText.value = String.format(tipFormat, loginCommon, loginThird, vipTxt, vipLevel)
    })

    homeViewModel.userInfo.observe(lifecycleOwner, Observer {
        userInfoText.value = it?.let { userInfo ->
            "昵称：${userInfo.nickName} id:${OpenApiSDK.getLoginApi().getUserOpenId()}"
        } ?: ""
    })

    DisposableEffect(lifecycleOwner) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    homeViewModel.fetchUserLoginStatus()

                    OpenApiSDK.getOpenApi().fetchGreenMemberInformation {
                        if (it.isSuccess()) {
                            val vipInfo = it.data
                            if (vipInfo != null) {
                                if (vipInfo.greenVipFlag != 1 && vipInfo.superGreenVipFlag != 1 && vipInfo.hugeVipFlag != 1) {
                                    vipTxt = "非vip"
                                }
                                if (vipInfo.greenVipFlag == 1) {
                                    vipTxt = "绿钻：${vipInfo.greenVipStartTime}-${vipInfo.greenVipEndTime}"
                                }
                                if (vipInfo.superGreenVipFlag == 1) {
                                    vipTxt = "豪华绿钻：${vipInfo.superGreenVipStartTime}-${vipInfo.superGreenVipEndTime}"
                                }
                                if (vipInfo.hugeVipFlag == 1) {
                                    vipTxt = "超级会员：${vipInfo.hugeVipStartTime}-${vipInfo.hugeVipEndTime}"
                                }
                                vipLevel = vipInfo.vipLevel.toString() + "级"
                            }
                        } else {
                            vipTxt = "获取vip信息失败"
                        }
                        loginText.value = String.format(tipFormat, loginCommon, loginThird, vipTxt, vipLevel)
                    }
                }
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
        loginStatusButton(activity, homeViewModel, loginText, userInfoText)

        Spacer(modifier = Modifier.height(16.dp))

        LoginButton(activity, homeViewModel, loginText)

        Spacer(modifier = Modifier.height(16.dp))

        MineSongList(viewModel())
    }
}

@Composable
fun loginStatusButton(activity: Activity, vm: HomeViewModel, text: MutableState<String>, userInfoText: MutableState<String>) {
    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var isLogin by remember {
            mutableStateOf(false)
        }
        vm.loginState.observe(LocalLifecycleOwner.current, Observer {
            isLogin = it.first
        })
        Row {
           Text(text = text.value)
        }
        Row {
            if (isLogin) {
                Button(onClick = {
                    vm.logout()
                    Global.getPlayerModuleApi().setPreferSongQuality(Quality.HQ)
                    HomeViewModel.clearRequestState()
                }, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp)) {
                    Text(text = "退出登录")
                }
            }
        }
        if (isLogin) {
            Text(text = userInfoText.value)
        }
    }
}

@Composable
fun LoginButton(activity: Activity, vm: HomeViewModel, info: MutableState<String>) {
    var isLogin by remember {
        mutableStateOf(false)
    }
    var isBind by remember {
        mutableStateOf(false)
    }
    vm.loginState.observe(LocalLifecycleOwner.current, Observer {
        isLogin = it.first
        isBind = it.second
    })
    Column (modifier = Modifier.fillMaxWidth()){
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ){
            Button(
                onClick = {
                    OpenApiSDK.getLoginApi().wxLogin(activity, MustInitConfig.WX_APP_ID) { ret, msg ->
                        Log.i(TAG, "LoginPage: wechat ret $ret")
                        if (ret) {
                            Toast.makeText(activity, "微信登录成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, "微信登录失败: $msg", Toast.LENGTH_SHORT).show()
                        }

                        info.value = "微信已登录:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                        vm.fetchUserLoginStatus()
                        HomeViewModel.clearRequestState()
                    }
                },
                modifier = Modifier.padding(0.dp)
            ) {
                Text(text = "微信登录")
            }
            Button(onClick = {
                OpenApiSDK.getLoginApi().qqLoginWeb(activity) { ret, msg ->
                    activity.runOnUiThread {
                        Log.i(TAG, "LoginPage: qq ret $ret, msg: $msg")
                        if (ret) {
                            Toast.makeText(activity, "QQ登录成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, "QQ登录失败: $msg", Toast.LENGTH_SHORT).show()
                        }
                        HomeViewModel.clearRequestState()

                        info.value = "QQ已登录:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                        vm.fetchUserLoginStatus()
                    }
                }
            }, modifier = Modifier.padding(0.dp)) {
                Text(text = "QQ登录")
            }
            Button(onClick = {
                OpenApiSDK.getLoginApi().qqMusicLogin(activity) { b, msg ->
                    if (b) {
                        Toast.makeText(activity, "登录成功", Toast.LENGTH_SHORT).show()

                        info.value = "QQ音乐已登录:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                        vm.fetchUserLoginStatus()
                    } else {
                        Toast.makeText(activity, "登录失败: $msg", Toast.LENGTH_SHORT).show()
                    }
                    HomeViewModel.clearRequestState()
                }
            }, modifier = Modifier.padding(0.dp)) {
                Text(text = "QQ音乐登录")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ){
            Button(onClick = {
                OpenApiSDK.getLoginApi().qrCodeLogin(activity) { b, msg ->
                    if (b) {
                        Toast.makeText(activity, "登录成功", Toast.LENGTH_SHORT).show()

                        info.value = "扫码已登录:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                        vm.fetchUserLoginStatus()
                    } else {
                        Toast.makeText(activity, "登录失败: $msg", Toast.LENGTH_SHORT).show()
                    }
                    HomeViewModel.clearRequestState()
                }
                info.value = "扫码登录结果:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                vm.fetchUserLoginStatus()
            }, modifier = Modifier.padding(0.dp)) {
                Text(text = "扫码登录")
            }
            Button(
                modifier = Modifier.padding(0.dp),
                onClick = {
                    activity.startActivity(Intent(activity, PartnerLoginActivity::class.java))
                }
            ) {
                Text(text = "第三方登录")
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MineSongList(viewModel: HomeViewModel) {
    val pages = mutableListOf(
        "自建歌单",
        "收藏歌单",
        "收藏专辑",
        "最近播放单曲",
        "最近播放专辑",
        "最近播放歌单",
        "最近播放长音频"
    )

    val pagerState = rememberPagerState()
    val composableScope = rememberCoroutineScope()

    ScrollableTabRow(
        // Our selected tab is our current page
        selectedTabIndex = pagerState.currentPage,
        // Override the indicator, using the provided pagerTabIndicatorOffset modifier
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
            )
        }
    ) {
        // Add tabs for all of our pages
        pages.forEachIndexed { index, title ->
            Tab(
                text = { Text(text = title) },
                selected = pagerState.currentPage == index,
                onClick = {
                    composableScope.launch(Dispatchers.Main) {
                        pagerState.scrollToPage(index)
                    }
                },
                selectedContentColor = Color.White,
                unselectedContentColor = Color.Gray
            )
        }
    }

    HorizontalPager(
        count = pages.size,
        state = pagerState
    ) { page ->
        val index = pagerState.currentPage
        Log.i(TAG, "MineSongList: current index $index")

        when (index) {
            0 -> {
                // 自建歌单
                viewModel.fetchMineFolder()
                Column(modifier = Modifier.fillMaxSize()) {
                    NewFolder(viewModel)
                    FolderPage(viewModel.mineFolders)
                }
            }
            1 -> {
                // 收藏歌单
                viewModel.fetchCollectedFolder()
                FolderPage(viewModel.favFolders)
            }
            2 -> {
                // 收藏专辑
                viewModel.fetchCollectedAlbum()
                AlbumPage(viewModel.favAlbums)
            }
            3 -> {
                // 最近播放（单曲）
                SongListPage(viewModel.pagingRecentSong())
            }
            4 -> {
                // 最近播放（专辑）
                viewModel.fetchRecentAlbums()
                AlbumPage(albums = viewModel.recentAlbums)
            }
            5 -> {
                // 最近播放（歌单）
                viewModel.fetchRecentFolders()
                FolderPage(folders = viewModel.recentFolders)
            }
            6 -> {
                // 最近播放（长音频）
                viewModel.fetchRecentLongRadios()
                AlbumPage(albums = viewModel.recentLongRadio)
            }
        }
    }
}

@Composable
fun NewFolder(viewModel: HomeViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
//        val showDialog = remember { mutableStateOf(false) }
//        val showDialog by remember { mutableStateOf(true) }
        FolderDialog(viewModel, showDialog = showDialog, setShowDialog = setShowDialog)
        Image(
            painter = rememberImagePainter(""),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .padding(2.dp)
                .clickable {
                    setShowDialog(true)
                }
        )
        Column(modifier = Modifier.clickable { setShowDialog(true) }) {
            Text(text = "Click to Create New Folder")
            Text(text = "点击创建新个人歌单")
        }
    }
}

@Composable
fun FolderDialog(viewModel: HomeViewModel, showDialog: Boolean, setShowDialog: (Boolean) -> Unit) {
    if (!showDialog) return

    var foldName by rememberSaveable { mutableStateOf("") }
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
                value = foldName,
                onValueChange = {
                    foldName = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入歌单名字") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = { setShowDialog(false) }) {
                    Text("取消")
                }

                OutlinedButton(onClick = {
                    Log.d(TAG, "New Folder: $foldName")
                    if (foldName.isEmpty()) return@OutlinedButton

                    com.tencent.qqmusic.openapisdk.business_common.Global.getOpenApi().createFolder(foldName) {
                        if (it.isSuccess()) {
                            Log.d(TAG, "succeeded to create folder: $foldName. ret: $it")
                            setShowDialog(false)
                            HomeViewModel.clearRequestState()
                            viewModel.fetchMineFolder()
                            return@createFolder
                        } else {
                            Log.w(TAG, "failed to create folder: $foldName. ret: $it")
                            foldName = ""
                        }
                    }
                }) {
                    Text("确定")
                }
            }
        }
    }
}