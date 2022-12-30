package com.tencent.qqmusic.qplayer.ui.activity.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderPage
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumPage
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SearchPage"

@Composable
fun SearchPage(homeViewModel: HomeViewModel) {
    var searchInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(value = searchInput,
            placeholder = {
                Text(text = "输入search关键字")
            }, onValueChange = {
            searchInput = it
        }, modifier = Modifier.width(300.dp))

        Button(onClick = {
            homeViewModel.searchInput = searchInput
            homeViewModel.searchSong()
            homeViewModel.searchFolder()
            homeViewModel.searchAlbum()

        }, modifier = Modifier.padding(16.dp)) {
            Text("发起搜索")
        }

        Spacer(modifier = Modifier.height(16.dp))

        SearchResultTabs(viewModel = homeViewModel)
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SearchResultTabs(viewModel: HomeViewModel) {
    val pages = mutableListOf(
        "单曲",
        "歌单",
        "专辑"
    )

    val pagerState = rememberPagerState()
    val composableScope = rememberCoroutineScope()
    var songList: List<SongInfo> = emptyList()

    TabRow(
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
        when (index) {
            0 -> {
                SongListPage(viewModel.searchSongs)
            }
            1 -> {
                FolderPage(viewModel.searchFolders)
            }
            2 -> {
                AlbumPage(viewModel.searchAlbums)
            }
            else -> {
            }
        }
    }
}
