package com.tencent.qqmusic.qplayer.ui.activity.login

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderPage
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MinePage"

@Composable
fun MinePage() {
    val activity = LocalContext.current as Activity

    val text = remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val hasLogin = OpenApiSDK.getLoginApi().hasLogin()
                    text.value = "是否登录 :\n" +
                            "$hasLogin"
                    if (hasLogin) {
                        OpenApiSDK.getOpenApi().fetchUserInfo {
                            if (it.isSuccess()) {
                                text.value = "是否登录 :\n" +
                                        "$hasLogin, 昵称：${it.data!!.nickName}"
                            }
                        }
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
        Text(text.value)

        Spacer(modifier = Modifier.height(16.dp))

        LoginButton(activity, text)

        Spacer(modifier = Modifier.height(16.dp))

        MineSongList(viewModel())
    }
}

@Composable
fun LoginButton(activity: Activity, info: MutableState<String>) {
    Column {
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
                    HomeViewModel.clearRequestState()
                }
            },
            modifier = Modifier.padding(8.dp)
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
                }
            }
        }, modifier = Modifier.padding(8.dp)) {
            Text(text = "QQ登录")
        }
        Button(onClick = {
            OpenApiSDK.getLoginApi().qqMusicLogin(activity) { b, msg ->
                if (b) {
                    Toast.makeText(activity, "登录成功", Toast.LENGTH_SHORT).show()

                    info.value = "QQ音乐已登录:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                } else {
                    Toast.makeText(activity, "登录失败: $msg", Toast.LENGTH_SHORT).show()
                }
                HomeViewModel.clearRequestState()
            }
        }, modifier = Modifier.padding(8.dp)) {
            Text(text = "QQ音乐登录")
        }
        Button(onClick = {
            OpenApiSDK.getLoginApi().qrCodeLogin(activity) { b, msg ->
                if (b) {
                    Toast.makeText(activity, "登录成功", Toast.LENGTH_SHORT).show()

                    info.value = "扫码已登录:\n${OpenApiSDK.getLoginApi().hasLogin()}"
                } else {
                    Toast.makeText(activity, "登录失败: $msg", Toast.LENGTH_SHORT).show()
                }
                HomeViewModel.clearRequestState()
            }
            info.value = "扫码登录结果:\n${OpenApiSDK.getLoginApi().hasLogin()}"
        }, modifier = Modifier.padding(16.dp)) {
            Text(text = "扫码登录")
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MineSongList(viewModel: HomeViewModel) {
    val pages = mutableListOf(
        "自建歌单",
        "收藏歌单",
        "最近播放"
    )

    val pagerState = rememberPagerState()
    val composableScope = rememberCoroutineScope()

    TabRow(
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
                FolderPage(viewModel.mineFolders)
            }
            1 -> {
                // 收藏歌单
                viewModel.fetchCollectedFolder()
                FolderPage(viewModel.favFolders)
            }
            2 -> {
                // 最近播放
                SongListPage(viewModel.pagingRecentSong())
            }
            else -> {
            }
        }
    }
}