package com.tencent.qqmusic.qplayer.ui.activity.home

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.rememberImagePainter
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.hologram.HologramManager
import com.tencent.qqmusic.openapisdk.hologram.service.IFireEyeXpmService
import com.tencent.qqmusic.openapisdk.model.AreaShelf
import com.tencent.qqmusic.openapisdk.model.Category
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.audio.LongAudioCategoryActivity
import com.tencent.qqmusic.qplayer.ui.activity.audio.LongAudioModuleContentActivity
import com.tencent.qqmusic.qplayer.ui.activity.audio.LongAudioRankActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Created by tannyli on 2023/8/1.
 * Copyright (c) 2023 TME. All rights reserved.
 */

private const val TAG = "LongAudioPage"

@OptIn(ExperimentalPagerApi::class)
@Composable
fun LongAudioPage(homeViewModel: HomeViewModel) {
    homeViewModel.fetchLongAudioCategoryPages()
    var categories = homeViewModel.longAudioCategoryPages

    if (categories.isEmpty()) {
        Log.i(TAG, "LongAudioPage categories.isEmpty()")
        return
    }

    val pagerState = rememberPagerState()
    val composableScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 一级TAb
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                )
            },
            modifier = Modifier
                .fillMaxWidth(),
            backgroundColor = colorResource(id = R.color.purple_500)
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    text = { Text(text = category.name) },
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
            count = categories.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
//                .clickable {
//                Log.i(TAG, "一级tab click")
//            }
        ) {
            HologramManager.getService(IFireEyeXpmService::class.java)?.monitorXpmEvent(
                IFireEyeXpmService.XpmEvent.PAGE_SCROLL, "LongAudioPage_TabLevel1_${pagerState.currentPage}"
            )
            LongAudioCategorySecondPage(categories, pagerState.currentPage, homeViewModel)
    }
}}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun LongAudioCategorySecondPage(categories: List<Category>, index: Int, homeViewModel: HomeViewModel) {
    val activity = LocalContext.current as Activity
    val pagerState2 = rememberPagerState()
    val composableScope = rememberCoroutineScope()
    val subCategoryes = categories.getOrNull(index)?.subCategory?.getOrNull(pagerState2.currentPage)
    if (subCategoryes == null) {
        Log.i(TAG, "LongAudioPage subCategoryes.isEmpty()")
        return
    }

    val curCategory = categories.getOrNull(index)
    if (curCategory == null) {
        Log.i(TAG, "LongAudioPage subCategoryes.isEmpty()")
        return
    }
    Column(Modifier.fillMaxSize()) {
        // 二级筛选
        ScrollableTabRow(
            selectedTabIndex = pagerState2.currentPage,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.pagerTabIndicatorOffset(pagerState2, tabPositions),
                    color = Color.Red,
                    height = 2.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth(),
            backgroundColor = colorResource(id = R.color.purple_200)
        ) {

            curCategory.subCategory!!.forEachIndexed { index, category ->
                Tab(
                    text = { Text(text = category.name) },
                    selected = pagerState2.currentPage == index,
                    onClick = {
                        composableScope.launch(Dispatchers.Main) {
//                            select2 = (index)
                            pagerState2.scrollToPage(index)
                        }
                    },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.Gray
                )
            }
        }

        HorizontalPager(
            count = curCategory.subCategory!!.size,
            state = pagerState2,
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    Log.i(TAG, "一级tab click")
                }
        ) { page ->
            Log.i(TAG, "long audio: current index $index, index2:${pagerState2.currentPage}")
            HologramManager.getService(IFireEyeXpmService::class.java)?.monitorXpmEvent(
                IFireEyeXpmService.XpmEvent.PAGE_SCROLL, "LongAudioPage_TabLevel2_${pagerState2.currentPage}"
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp),horizontalArrangement = Arrangement.Center) {
                    Button(onClick = {
                        LongAudioRankActivity.start(activity, categories.getOrNull(index)?.name ?: "")
                    }, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp)) {
                        Text(text = "排行榜")
                    }

                    Button(onClick = {
                        LongAudioCategoryActivity.start(activity, categories.getOrNull(index)?.name ?: "", null)
                    }, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp)) {
                        Text(text = "分类")
                    }
                }
                categories.getOrNull(index)?.let {
                    val key1 = it.id
                    val key2 = it.subCategory?.getOrNull(pagerState2.currentPage)?.id ?: 0
                    val flow = remember(key1 = key1, key2 = key2) {
                        homeViewModel.pagingCategoryPageDetail(key1, key2)
                    }
                    LongAudioCategoryPageDetail(flow, categoryId = key1, key2, homeViewModel = homeViewModel)
                }
            }
        }
    }
}


@Composable
fun LongAudioCategoryPageDetail(flow: Flow<PagingData<AreaShelf>>? = null, categoryId: Int, subCategoryId: Int, homeViewModel: HomeViewModel) {
//    val executedOnce = remember("$categoryId*$subCategoryId") { mutableStateOf(false) }
//    var flow: Flow<PagingData<AreaShelf>>? = null
//    if (!executedOnce.value) {
//        // 只需执行一次的操作
//        Log.i(TAG, "This will be executed only once")
//        executedOnce.value = true
//        flow = homeViewModel.pagingCategoryPageDetail(categoryId, subCategoryId)
//    }
    if (flow == null) return
//    val flow = remember(key1 = categoryId, key2 = subCategoryId) {
//        homeViewModel.pagingCategoryPageDetail(categoryId, subCategoryId)
//    }
    val shelfes = flow.collectAsLazyPagingItems()
    Log.i(TAG, "SLongAudioCategoryPageDetail:categoryId:$categoryId, subCategoryId:$subCategoryId count: ${shelfes.itemCount}")
    if (shelfes.itemCount == 0) return
    val activity = LocalContext.current as Activity
    val width = UiUtils.getDisplayWidth(activity)
    val scrollState = rememberLazyListState()
    if (scrollState.isScrollInProgress) {
        DisposableEffect(key1 = Unit) {
            HologramManager.getService(IFireEyeXpmService::class.java)?.monitorXpmEvent(
                IFireEyeXpmService.XpmEvent.LIST_SCROLL, "LongAudioCategoryPageDetail_${categoryId}_${subCategoryId}", 1
            )
            onDispose {
                HologramManager.getService(IFireEyeXpmService::class.java)?.monitorXpmEvent(
                    IFireEyeXpmService.XpmEvent.LIST_SCROLL, "LongAudioCategoryPageDetail_${categoryId}_${subCategoryId}", 0
                )
            }
        }
    }
    LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize()) {
        this.items(shelfes) { shelf ->
            shelf?: return@items
            val albums = shelf.shelfItems.map { it.album }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = shelf.shelfTitle,
                    color = Color.Black,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomStart)
                )
                Text(
                    text = "更多",
                    color = Color.Black,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.BottomEnd)
                        .clickable {
                            if (shelf.jumpInfo?.interfaceName?.isNullOrEmpty() == true) {
                                Toast
                                    .makeText(activity, "interfaceName为空", Toast.LENGTH_SHORT)
                                    .show()
                                return@clickable
                            }
                            shelf.jumpInfo?.interfaceName?.apply {
                                if (this.equals("fetchCategoryPageModuleContentLongAudio")) {
                                    LongAudioModuleContentActivity.start(
                                        activity,
                                        shelf.jumpInfo?.args?.getOrNull(0)?.intVal ?: 0,
                                        shelf.shelfTitle
                                    )
                                } else if (this.equals("fetchAlbumListOfLongAudioByCategory")) {
                                    LongAudioCategoryActivity.start(activity, "", shelf.jumpInfo)
                                } else {
                                    Toast
                                        .makeText(
                                            activity,
                                            "interfaceName不支持",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            }
                        }
                )
            }
            FlowRow {
                repeat(albums.size) {
                    val album = albums.getOrNull(it) ?: return@repeat
                    val padding = 10.dp
                    val itemWidth = (UiUtils.px2dp(width.toFloat()).dp - padding * 5) / 4
                    Column(modifier = Modifier
                        .wrapContentSize()
                        .padding(padding)
                        .clickable {
                            HologramManager.getService(IFireEyeXpmService::class.java)?.monitorXpmEvent(
                                IFireEyeXpmService.XpmEvent.CLICK, "LongAudioPage_AlbumActivity"
                            )
                            activity.startActivity(
                                Intent(activity, AlbumActivity::class.java)
                                    .putExtra(AlbumActivity.KEY_ALBUM_ID, album.id)
                            )
                        }) {
                        Text(
                            modifier = Modifier
                                .width(itemWidth)
                                .wrapContentSize()
                                .padding(10.dp),
                            text = album.name,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Image(
                            painter = rememberImagePainter(album.pic),
                            contentDescription = null,
                            modifier = Modifier
                                .width(itemWidth)
                                .height(itemWidth)
                                .padding(padding),

                            )
                        Text(
                            modifier = Modifier
                                .wrapContentSize()
                                .width(itemWidth)
                                .padding(10.dp),
                            text = "${(album.listenNum ?: 0) / 10000}万",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )

                    }

                }
            }
        }
    }
}