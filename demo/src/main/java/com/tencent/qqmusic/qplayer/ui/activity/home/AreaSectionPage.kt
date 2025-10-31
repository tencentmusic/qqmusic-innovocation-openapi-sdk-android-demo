package com.tencent.qqmusic.qplayer.ui.activity.home

import android.content.Intent
import android.util.Log
import androidx.activity.*
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.model.AreaId
import com.tencent.qqmusic.qplayer.ui.activity.home.area.AreaViewModel
import com.tencent.qqmusic.qplayer.ui.activity.player.voyage.PlayerVoyageActivity

private const val TAG = "AreaSectionPage"

@Composable
fun AreaSectionPage(homeViewModel: HomeViewModel) {
    AreaScreen(homeViewModel)
}

var areaIndex = mutableStateOf(-1)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AreaScreen(homeViewModel: HomeViewModel) {
    val pageMap = mapOf<String, @Composable () -> Unit>(
        "臻品乐航专区" to @Composable { VoyagePage(homeViewModel) },
        "Hires专区" to @Composable { AreaSectionDetailPageWithTitle("Hires专区",AreaId.AreaHires, homeViewModel) },
        "Dolby专区" to @Composable { AreaSectionDetailPageWithTitle("Dolby专区",AreaId.AreaDolby, homeViewModel) },
        "臻品全景声专区" to @Composable { AreaSectionDetailPageWithTitle("臻品全景声专区",AreaId.AreaGalaxy, homeViewModel) },
        "Wanos专区" to @Composable { AreaSectionDetailPageWithTitle("Wanos专区",AreaId.Wanos, homeViewModel) },
        "黑胶专区" to @Composable { AreaSectionDetailPageWithTitle("黑胶专区",AreaId.Vinly, homeViewModel) },
        "臻品母带专区" to @Composable { AreaSectionDetailPageWithTitle("臻品母带专区",AreaId.Master, homeViewModel) },
        "场景歌单" to @Composable { categoryFoldersPageWithTitle("场景歌单",homeViewModel = homeViewModel, true) },
        "新碟" to @Composable { NewAlbumPage(areaViewModel = AreaViewModel()) },
        "小宇宙" to @Composable { AreaSectionDetailPageWithTitle("小宇宙",AreaId.XiaoYuZhou, homeViewModel) },
        "DTS专区" to @Composable { AreaSectionDetailPageWithTitle("DTS专区",AreaId.DTS, homeViewModel) }
    )

    val pages = pageMap.keys.toList()

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
            homeViewModel.cleanData()
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
                    Box(
                        contentAlignment = Alignment.Center, modifier = Modifier
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

        else -> {
            pages.getOrNull(areaIndex.value)?.let { pageName ->
                pageMap[pageName]?.invoke()
            } ?: Box {}
        }
    }


}