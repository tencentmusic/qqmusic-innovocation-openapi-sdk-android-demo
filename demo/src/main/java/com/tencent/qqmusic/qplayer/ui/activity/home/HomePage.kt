package com.tencent.qqmusic.qplayer.ui.activity.home

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.core.player.playlist.MusicPlayList
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderActivity
import com.tencent.qqmusic.qplayer.ui.activity.home.ai.AIFunctionPage
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVFunctionPage
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver.isRadio
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

private const val TAG = "HomePage"

@Composable
fun HomePage(homeViewModel: HomeViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        homePageTabs(homeViewModel = homeViewModel)
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun homePageTabs(homeViewModel: HomeViewModel) {
    val pages = mutableListOf(
        "分类歌单", "首页推荐", "视频", "AI功能", "排行榜",
        "专区", "长音频",
    )

    val pagerState = rememberPagerState()
    val composableScope = rememberCoroutineScope()

    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
            )
        }
    ) {
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
        Log.i(TAG, "HomePage: current index $index")

        PerformanceHelper.monitorPageScroll("Home_${index}")

        when (index) {
            1 -> {
                HomeRecommendPage(homeViewModel)
            }
            0 -> {
                categoryFoldersPage(homeViewModel)
            }

            2 -> {
                MVFunctionPage()
            }

            3 -> {
                AIFunctionPage()
            }

            4 -> {
                rankPage(homeViewModel)
            }

            5 -> {
                AreaSectionPage(homeViewModel)
            }

            6 -> {
                LongAudioPage(homeViewModel = homeViewModel)
            }
        }
    }
}


@Composable
fun categoryFoldersPage(homeViewModel: HomeViewModel, fetchSceneSongList: Boolean = false,observer: PlayerObserver = PlayerObserver) {
    val activity = LocalContext.current as Activity
    if (fetchSceneSongList) {
        homeViewModel.fetchSceneCategory()
    }
    var categories =
        if (fetchSceneSongList) homeViewModel.sceneCategories else homeViewModel.categories
    val scrollState = rememberLazyListState()
    PerformanceHelper.MonitorListScroll(scrollState = scrollState, location = "CategoryFolderPage")
    LazyColumn(state = scrollState) {
        items(categories.count()) { it ->
            val topCategory = categories.getOrNull(it) ?: return@items
            val subCategories = topCategory.subCategory ?: emptyList()
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = topCategory.name,
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }
            FlowRow {
                repeat(subCategories.size) {
                    val sub = subCategories.getOrNull(it) ?: return@repeat
                    val subId = sub.id
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(16.dp)
                            .clickable {
                                if (subId == MusicPlayList.PLAY_LIST_RADIO_TYPE) {
                                    OpenApiSDK
                                        .getPlayerApi()
                                        .playRadio(autoPlay = true)
                                    return@clickable
                                }
                                val location = "CategoryFolderPage_" + if (fetchSceneSongList) {
                                    "SongListActivity"
                                } else {
                                    "FolderActivity"
                                }
                                PerformanceHelper.monitorClick(location)
                                if (fetchSceneSongList) {
                                    activity.startActivity(
                                        Intent(activity, SongListActivity::class.java)
                                            .putIntegerArrayListExtra(
                                                SongListActivity.KEY_CATEGORY_IDS,
                                                arrayListOf(topCategory.id, subId)
                                            )
                                    )
                                } else {
                                    activity.startActivity(
                                        Intent(activity, FolderActivity::class.java)
                                            .putIntegerArrayListExtra(
                                                FolderActivity.KEY_CATEGORY_IDS,
                                                arrayListOf(topCategory.id, subId)
                                            )
                                    )
                                }

                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRadio && subId == MusicPlayList.PLAY_LIST_RADIO_TYPE) "电台:${PlayerObserver.currentSong?.songName}" else sub.name,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}
