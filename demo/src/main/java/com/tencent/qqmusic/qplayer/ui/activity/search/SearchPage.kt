package com.tencent.qqmusic.qplayer.ui.activity.search

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.openapisdk.hologram.HologramManager
import com.tencent.qqmusic.openapisdk.hologram.service.IFireEyeXpmService
import com.tencent.qqmusic.openapisdk.model.SearchType
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderPage
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumPage
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SearchPage"
private val tabList = listOf(
    Pair("歌曲", SearchType.SONG),
    Pair("专辑", SearchType.ALBUM),
    Pair("歌单", SearchType.FOLDER),
    Pair("MV", SearchType.MV),
    Pair("歌词", SearchType.LYRIC),
    Pair("歌手", SearchType.SINGER)
)
var searchInput by mutableStateOf("")
var currentTab by mutableStateOf(0)

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchPage(homeViewModel: HomeViewModel) {
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val smartKey = remember {
            SnapshotStateList<String>()
        }


        TextField(value = searchInput,
            placeholder = {
                Text(text = "输入search关键字")
            }, onValueChange = {
                searchInput = it
                homeViewModel.smartSearchKey(searchInput) { list ->
                    Log.d(TAG, "SearchPage: ${list.size}")
                    smartKey.clear()
                    smartKey.addAll(list)
                }
            }, modifier = Modifier.width(300.dp)
        )


        LazyVerticalGrid(
            modifier = Modifier.height(100.dp),
            cells = GridCells.Fixed(3),
        ) {
            items(smartKey) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier
                    .height(40.dp)
                    .wrapContentWidth()
                    .clickable {
                        keyboard?.hide()
                        searchInput = it
                        homeViewModel.search(tabList[currentTab].second, searchInput)
                    }) {
                    Text(text = it)
                }
            }
        }

        Button(onClick = {
            keyboard?.hide()
            homeViewModel.search(tabList[currentTab].second, searchInput)
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
    val pages = tabList.map { it.first }

    val pagerState = rememberPagerState()
    val composableScope = rememberCoroutineScope()

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
                        currentTab = index
                        viewModel.search(tabList[index].second, searchInput)
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
    ) {

        HologramManager.getService(IFireEyeXpmService::class.java)?.monitorXpmEvent(
            IFireEyeXpmService.XpmEvent.PAGE_SCROLL,
            "SearchReult_${tabList[pagerState.currentPage].second}"
        )

        val data = viewModel.searchResult.collectAsState().value
        when (tabList[pagerState.currentPage].second) {
            SearchType.SONG -> {
                SongListPage(data?.songList ?: emptyList(), needPlayer = false)
            }

            SearchType.ALBUM -> {
                AlbumPage(data?.albumList ?: emptyList())
            }

            SearchType.FOLDER -> {
                FolderPage(data?.folderList ?: emptyList())
            }

            SearchType.SINGER -> {
                singerPage(list = data?.singerList ?: emptyList())
            }

            SearchType.LYRIC -> {
                LyricPage(list = data?.lyricInfoList ?: emptyList())
            }

            SearchType.MV -> {
                MVPage(list = data?.mvList ?: emptyList())
            }
        }
    }
}
