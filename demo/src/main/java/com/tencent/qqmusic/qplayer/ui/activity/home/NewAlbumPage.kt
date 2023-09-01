package com.tencent.qqmusic.qplayer.ui.activity.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.CoroutineScope

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

private const val TAG = "HomePage"

@Composable
fun NewAlbumPage(homeViewModel: HomeViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        albumPageTabs(homeViewModel = homeViewModel,)
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalPagerApi::class)
@Composable
fun albumPageTabs(homeViewModel: HomeViewModel) {

    val pages = homeViewModel.areaId.map { it.name }
    // 添加地区信息到列表中

    val pagerState = rememberPagerState()
    val composableScope = rememberCoroutineScope()

    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
            )
        }
    )
    {
        pages.forEachIndexed { index, _ ->
            Tab(
                text = { Text(text = pages[index].toString()) },
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
        Log.i(TAG, "NewAlbumPage: current index $index")
        when (index) {
            0 -> {

                LaunchedEffect(Unit ){
                    homeViewModel.fetchNewAlbum(1, null)
                }
                MyUI(homeViewModel)
                SelectTypeView(homeViewModel = homeViewModel, 1 )

            }
            1 -> {
                LaunchedEffect(Unit) {

                        homeViewModel.fetchNewAlbum(2, type = null)

                }
                MyUI(homeViewModel)
                SelectTypeView(homeViewModel = homeViewModel, 2 )
            }
            2 -> {
                LaunchedEffect(Unit) {

                        homeViewModel.fetchNewAlbum(3, type = null)

                }
                MyUI(homeViewModel)
                SelectTypeView(homeViewModel = homeViewModel, 3 )

            }
            3 -> {
                LaunchedEffect(Unit) {

                        homeViewModel.fetchNewAlbum(4, type = null)

                }
                MyUI(homeViewModel)
                SelectTypeView(homeViewModel = homeViewModel, 4 )
            }
            4 -> {
                LaunchedEffect(Unit) {

                        homeViewModel.fetchNewAlbum(5, type = null)

                }
                MyUI(homeViewModel)
                SelectTypeView(homeViewModel = homeViewModel, 5 )
            }
            5 -> {
                LaunchedEffect(Unit) {

                        homeViewModel.fetchNewAlbum(6, type = null)

                }
                MyUI(homeViewModel)
                SelectTypeView(homeViewModel = homeViewModel, 6 )
            }
        }
    }
}


@Composable
fun MyUI(homeViewModel: HomeViewModel){
    val activity = LocalContext.current as Activity
    val albums = homeViewModel.newAlbums
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(albums.size) { index ->
            val album = albums.getOrNull(index) ?: return@items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        activity.startActivity(
                            Intent(activity, SongListActivity::class.java)
                                .putExtra(SongListActivity.KEY_ALBUM_ID, album.id)
                        )
                    }
            ) {
                Image(
                    painter = rememberImagePainter(album.pic),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .padding(2.dp)
                )
                Column {
                    Text(text = album.name)
                    Text(text = album.singers?.get(0)?.name ?:"")
                    Text(text = album.publicTime?:"")
                }
            }
        }
    }


}




@Composable
fun SelectTypeView(homeViewModel: HomeViewModel,areaId:Int) {
    val typeList = mutableListOf(
        "全部",
        "单曲",
        "EP",
        "录音室专辑",
        "现场专辑",
    )
    val isClick = rememberSaveable { mutableStateOf(false) }
    val selectType = rememberSaveable { mutableStateOf("筛选") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Button(
            onClick = {isClick.value = !isClick.value},
            content = {
                Text(text = selectType.value)
            },
        )
        DropdownMenu(
            expanded = isClick.value,
            modifier = Modifier.fillMaxWidth(),
            onDismissRequest = {},
            content = {
                typeList.forEach {
                    DropdownMenuItem(
                        onClick = {
                            isClick.value = !isClick.value
                            selectType.value = it
                            val myScope = CoroutineScope(Dispatchers.Main)
                            when(it){
                                "全部" ->{
                                    homeViewModel.fetchNewAlbum(areaId, type = null)
                                    myScope.launch {
                                        homeViewModel.fetchNewAlbum(areaId, type = null)
                                    }
                                }
                                "单曲" ->{
                                    // 根据单曲类型进行筛选逻
                                    homeViewModel.fetchNewAlbum(areaId, type = 10)
                                    myScope.launch {
                                        homeViewModel.fetchNewAlbum(areaId, type = 10)
                                    }
                                }
                                "EP" ->{
                                    // 根据EP类型进行筛选逻辑
                                    homeViewModel.fetchNewAlbum(areaId,11)
                                    myScope.launch {
                                        homeViewModel.fetchNewAlbum(areaId, type = 11)
                                    }
                                }
                                "录音室专辑" ->{
                                    // 根据录音室专辑类型进行筛选逻辑
                                    homeViewModel.fetchNewAlbum(areaId,0)
                                    myScope.launch {
                                        homeViewModel.fetchNewAlbum(areaId, type = 0)
                                    }
                                }
                                "现场专辑" ->{
                                    // 根据现场专辑类型进行筛选逻辑
                                    homeViewModel.fetchNewAlbum(areaId,1)
                                    myScope.launch {
                                        homeViewModel.fetchNewAlbum(areaId, type = 1)
                                    }

                                }
                            }
                        },
                        content = {
                            Text(text = it)
                        }
                    )
                }
            },
        )
    }
}




