package com.tencent.qqmusic.qplayer.ui.activity.search

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.model.LyricInfo
import com.tencent.qqmusic.openapisdk.model.SearchMVInfo
import com.tencent.qqmusic.openapisdk.model.SearchType
import com.tencent.qqmusic.openapisdk.model.Singer
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.ui.activity.LoadMoreItem
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderListPage
import com.tencent.qqmusic.qplayer.ui.activity.home.HOTKEY_TYPE_LIST
import com.tencent.qqmusic.qplayer.ui.activity.home.SearchViewModel
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumListPage
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListPage
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "SearchPage"
private val tabList = listOf(
    Pair("歌曲", SearchType.SONG),
    Pair("专辑", SearchType.ALBUM),
    Pair("歌单", SearchType.FOLDER),
    Pair("MV", SearchType.MV),
    Pair("歌词", SearchType.LYRIC),
    Pair("歌手", SearchType.SINGER),
    Pair("电台", SearchType.RADIO)
)
var searchInput by mutableStateOf("")
var currentTab by mutableStateOf(0)

enum class PageState {
    HOT_KEY,
    SMART_SEARCH,
    SEARCH_RESULT
}
val pageState = mutableStateOf(PageState.HOT_KEY)

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchPage(searchViewModel: SearchViewModel) {
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val smartKey = remember {
            SnapshotStateList<String>()
        }

        Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextField(value = searchInput,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    pageState.value = PageState.SEARCH_RESULT
                    searchViewModel.search(tabList[currentTab].second, searchInput)
                }),
                singleLine = true,
                textStyle = TextStyle(color = LocalContentColor.current, fontSize = 16.sp),
                placeholder = {
                    Text(text = "输入search关键字")
                }, onValueChange = {
                    pageState.value = PageState.SMART_SEARCH
                    searchInput = it
                    searchViewModel.smartSearchKey(searchInput) { list ->
                        Log.d(TAG, "SearchPage: ${list.size}")
                        smartKey.clear()
                        smartKey.addAll(list)
                    }
                }, modifier = Modifier.weight(1.0f, true),
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = {
                            searchInput = ""
                            pageState.value = PageState.HOT_KEY
                        }) {
                            Icon(Icons.Filled.Clear, "", tint = Color.Blue)
                        }
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    cursorColor = Color.Black,
                    disabledLabelColor = Color.Gray,
                    focusedIndicatorColor = Color.Blue,
                    unfocusedIndicatorColor = Color.Blue
                ),
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(modifier = Modifier.size(32.dp), onClick = {
                keyboard?.hide()
                pageState.value = PageState.SEARCH_RESULT
                searchViewModel.search(tabList[currentTab].second, searchInput)
            }) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Blue)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (pageState.value) {
            PageState.HOT_KEY -> {
                SearchHotKeyView(searchViewModel)
            }
            PageState.SMART_SEARCH -> {
                SmartSearchView(searchViewModel, smartKey)
            }
            PageState.SEARCH_RESULT -> {
                SearchResultTabs(searchViewModel)
            }
        }
    }
}

@Composable
fun SmartSearchView(searchViewModel: SearchViewModel, smartKeyList: SnapshotStateList<String>) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(3),
    ) {
        items(smartKeyList)  { key->
            Box(contentAlignment = Alignment.Center, modifier = Modifier
                .height(40.dp)
                .wrapContentWidth()
                .clickable {
                    searchInput = key
                    searchViewModel.search(tabList[currentTab].second, searchInput)
                    pageState.value = PageState.SEARCH_RESULT
                }) {
                Text(text = key)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchHotKeyView(searchViewModel: SearchViewModel) {
    LaunchedEffect(Unit) {
        searchViewModel.hotkey()
    }
    val hotKeyListState = searchViewModel.hotkeyStateFlow.collectAsState()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        val hotTypes = hotKeyListState.value.keys.toList()
        items(hotTypes) { hotTypeKey->
            val hotList = hotKeyListState.value[hotTypeKey] ?: emptyList()
            Card(shape = RoundedCornerShape(8.dp), backgroundColor = Color.White) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.width(150.dp).fillParentMaxHeight().padding(8.dp)) {
                    stickyHeader {
                        Text(text = HOTKEY_TYPE_LIST[hotTypeKey], fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.fillParentMaxWidth().background(Color.White))
                    }
                    items(hotList) { hotKey->
                        Text(text = hotKey.vecTitle.joinToString(" "),
                            fontSize = 12.sp,
                            color = Color.Black,
                            modifier = Modifier.combinedClickable(onClick = {
                                searchInput = hotKey.query
                                pageState.value = PageState.SEARCH_RESULT
                                searchViewModel.search(tabList[currentTab].second, hotKey.query)
                            }))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SearchResultTabs(viewModel: SearchViewModel) {
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
        PerformanceHelper.monitorPageScroll("SearchResult_${tabList[pagerState.currentPage].second}")

        when (tabList[pagerState.currentPage].second) {
            SearchType.SONG -> {
                val dataState = viewModel.dataListFlow<SongInfo>(SearchType.SONG).collectAsState()
                val loadMoreState = LoadMoreItem(needLoadMore = viewModel.loadMoreStateFlow(SearchType.SONG).collectAsState(), onLoadMore = {
                    viewModel.loadMore(SearchType.SONG)
                })
                SongListPage(dataState.value, needPlayer = false, loadMoreItem = loadMoreState)
            }

            SearchType.ALBUM -> {
                val dataState = viewModel.dataListFlow<Album>(SearchType.ALBUM).collectAsState()
                val loadMoreState = LoadMoreItem(needLoadMore = viewModel.loadMoreStateFlow(SearchType.ALBUM).collectAsState(), onLoadMore = {
                    viewModel.loadMore(SearchType.ALBUM)
                })
                AlbumListPage(dataState.value, loadMoreItem = loadMoreState)
            }

            SearchType.FOLDER -> {
                val dataState = viewModel.dataListFlow<Folder>(SearchType.FOLDER).collectAsState()
                val loadMoreState = LoadMoreItem(needLoadMore = viewModel.loadMoreStateFlow(SearchType.FOLDER).collectAsState(), onLoadMore = {
                    viewModel.loadMore(SearchType.FOLDER)
                })
                FolderListPage(dataState.value, loadMore = loadMoreState)
            }

            SearchType.SINGER -> {
                val dataState = viewModel.dataListFlow<Singer>(SearchType.SINGER).collectAsState()
                val loadMoreState = LoadMoreItem(needLoadMore = viewModel.loadMoreStateFlow(SearchType.SINGER).collectAsState(), onLoadMore = {
                    viewModel.loadMore(SearchType.SINGER)
                })
                singerPage(list = dataState.value, loadMoreItem = loadMoreState)
            }

            SearchType.LYRIC -> {
                val dataState = viewModel.dataListFlow<LyricInfo>(SearchType.LYRIC).collectAsState()
                val loadMoreState = LoadMoreItem(needLoadMore = viewModel.loadMoreStateFlow(SearchType.LYRIC).collectAsState(), onLoadMore = {
                    viewModel.loadMore(SearchType.LYRIC)
                })
                LyricPage(list = dataState.value, loadMoreItem = loadMoreState)
            }

            SearchType.MV -> {
                val dataState = viewModel.dataListFlow<SearchMVInfo>(SearchType.MV).collectAsState()
                val loadMoreState = LoadMoreItem(needLoadMore = viewModel.loadMoreStateFlow(SearchType.MV).collectAsState(), onLoadMore = {
                    viewModel.loadMore(SearchType.MV)
                })
                MVPage(list = dataState.value, loadMoreItem = loadMoreState)
            }

            SearchType.RADIO -> {
                val dataState = viewModel.dataListFlow<Album>(SearchType.RADIO).collectAsState()
                val loadMoreState = LoadMoreItem(needLoadMore = viewModel.loadMoreStateFlow(SearchType.RADIO).collectAsState(), onLoadMore = {
                    viewModel.loadMore(SearchType.RADIO)
                })
                AlbumListPage(dataState.value, loadMoreItem = loadMoreState)
            }
        }
    }
}
