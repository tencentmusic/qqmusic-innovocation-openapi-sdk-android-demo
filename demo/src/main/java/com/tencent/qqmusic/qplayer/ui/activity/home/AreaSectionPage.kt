package com.tencent.qqmusic.qplayer.ui.activity.home

import android.util.Log
import androidx.activity.*
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
//import androidx.compose.foundation.lazy.GridCells
//import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tencent.qqmusic.openapisdk.model.AreaId
import com.tencent.qqmusic.qplayer.ui.activity.home.area.AreaViewModel
import com.tencent.qqmusic.qplayer.ui.activity.main.AreaSectionDetailPage

private const val TAG = "AreaSectionPage"

@Composable
fun AreaSectionPage(homeViewModel: HomeViewModel) {
    areaScreen(homeViewModel)
}

var areaIndex = mutableStateOf(-1)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun areaScreen(homeViewModel: HomeViewModel) {

    val countState = rememberUpdatedState(areaIndex.value)

    val pages = mutableListOf(
        "Hires专区",
        "Dolby专区",
        "臻品全景声专区",
        "Wanos专区",
        "黑胶专区",
        "臻品母带专区",
        "场景歌单",
        "新碟",
        "小宇宙",
        "DTS专区"
    )

    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: ")
            areaIndex.value = -1
            remove()
        }
    }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(key1 = dispatcher) {
        onDispose {
            areaIndex.value = -1
            callback.remove() // 移除回调
        }
    }

    when (areaIndex.value) {
        -1 -> {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(2),
            ) {
                items(pages) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier
                        .height(100.dp)
                        .clickable {
                            areaIndex.value = pages.indexOf(it)
                            dispatcher?.addCallback(callback)
                        }) {
                        Text(text = it)
                    }
                }
            }
        }

        0 -> {
            AreaSectionDetailPage(AreaId.AreaHires, homeViewModel)
        }
        1 -> {
            AreaSectionDetailPage(AreaId.AreaDolby, homeViewModel)
        }
        2 -> {
            AreaSectionDetailPage(AreaId.AreaGalaxy, homeViewModel)
        }
        3 -> {
            AreaSectionDetailPage(AreaId.Wanos, homeViewModel)
        }
        4 -> {
            AreaSectionDetailPage(AreaId.Vinly, homeViewModel)
        }
        5 -> {
            AreaSectionDetailPage(AreaId.Master, homeViewModel)
        }
        6 -> {
            categoryFoldersPage(homeViewModel = homeViewModel, true)
        }
        7 -> {
            NewAlbumPage(areaViewModel = AreaViewModel())
        }
        8 -> {
            AreaSectionDetailPage(AreaId.XiaoYuZhou, homeViewModel)
        }
        9 -> {
            AreaSectionDetailPage(AreaId.DTS, homeViewModel)
        }
    }


}