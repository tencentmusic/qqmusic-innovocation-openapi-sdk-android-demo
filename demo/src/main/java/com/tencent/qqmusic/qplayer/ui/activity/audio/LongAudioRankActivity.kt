package com.tencent.qqmusic.qplayer.ui.activity.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.*
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.Category
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumListPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 长音频排行榜
 */
class LongAudioRankActivity : ComponentActivity() {

    private val tag by lazy {
        intent.getStringExtra("tag")
    }

    companion object {

        private const val TAG = "LongAudioRankActivity"

        @JvmStatic
        fun start(context: Context, tag: String) {
            val starter = Intent(context, LongAudioRankActivity::class.java)
                .putExtra("tag", tag)
            context.startActivity(starter)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val vm: LongAudioViewModel by viewModels()
        setContent {
//            val vm: LongAudioViewModel = viewModel()
            Scaffold(topBar = { TopBar("长音频排行榜") },
                modifier = Modifier.semantics{ testTagsAsResourceId=true }) {
                val vm: LongAudioViewModel = viewModel()
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(text = "加载中")
                    LongAudioScreen(vm)
                }
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun LongAudioScreen(vm: LongAudioViewModel) {
        vm.fetchRankCategory()
        val categorys = vm.categories
        if (categorys.isEmpty()) {
            return
        }

        val pagerState1 = rememberPagerState(0)
        val composableScope = rememberCoroutineScope()
        Column(modifier = Modifier.fillMaxSize()) {
            // 一级TAB
            TabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = pagerState1.currentPage,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.pagerTabIndicatorOffset(pagerState1, tabPositions)
                    )
                },
                backgroundColor = colorResource(id = R.color.purple_500)
            ) {
                categorys.forEachIndexed { index, category ->
                    Tab(
                        text = { Text(text = category.name) },
                        selected = pagerState1.currentPage == index,
                        onClick = {
                            composableScope.launch(Dispatchers.Main) {
                                pagerState1.scrollToPage(index)
                            }
                        },
                        selectedContentColor = Color.White,
                        unselectedContentColor = Color.Gray
                    )
                }
            }
            HorizontalPager(count = categorys.size, state = pagerState1, modifier = Modifier.fillMaxSize()) {
                LongAudioSecondPage(categorys = categorys, index = pagerState1.currentPage, vm =vm)
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun LongAudioSecondPage(categorys: List<Category>, index: Int, vm: LongAudioViewModel) {
        val categorys2 = categorys.getOrNull(index)?.subCategory ?: return

        var initFid = 0
        if (tag?.isNotEmpty() == true) {
            categorys2.forEachIndexed() { index, category ->
                if (category.name == tag) {
                    initFid = index
                }
            }
        }
        val pagerState = rememberPagerState(initFid)
        LaunchedEffect(Unit) {
            if (pagerState.pageCount == 0) {
                initFid = 0
            }
            pagerState.scrollToPage(initFid)
        }
        val stateAdd = pagerState.hashCode()
        Log.i(TAG, "new secondPage: hash:$stateAdd, initFid:$initFid")
        val composableScope = rememberCoroutineScope()

        Column(Modifier.fillMaxSize()) {
            if (categorys2.isNotEmpty()) {
                // 二级TAB
                // 二级筛选
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colorResource(id = R.color.purple_200)
                ) {
                    categorys2.forEachIndexed { index, category ->
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
                HorizontalPager(count = categorys2.size,
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()) {
                    LaunchedEffect(Unit){
                        vm.fetchRankDetail(
                            categorys.getOrNull(index)?.id ?: 0,
                            categorys2.getOrNull(pagerState.currentPage)?.id ?: 0
                        )
                    }
                    Log.i(
                        TAG,
                        "first tab index:${index}, second tab index:${pagerState.currentPage} hash:${pagerState.hashCode()}"
                    )
                    Column(Modifier.fillMaxSize()) {
                        Text(text = "first tab index:${index}, second tab index:${pagerState.currentPage}")
                        AlbumListPage(albums = vm.albums)
                    }
                }
            } else {
                OneTabRankPage(vm, categorys, index, categorys2, pagerState)
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    private fun OneTabRankPage(
        vm: LongAudioViewModel,
        categorys: List<Category>,
        index: Int,
        categorys2: List<Category>,
        pagerState: PagerState
    ) {
        val id = categorys.getOrNull(index)?.id ?: 0
        LaunchedEffect(index) {
            vm.fetchRankDetail(
                id,
                categorys2.getOrNull(pagerState.currentPage)?.id ?: 0
            )
        }
        Log.i(
            TAG,
            "first tab index:${index}, second tab index:${pagerState.currentPage} hash:${pagerState.hashCode()}"
        )
        Column(Modifier.fillMaxSize()) {
            Text(text = "first tab index:${index}, second tab index null")
            AlbumListPage(albums = vm.albums)
        }
    }


    class LongAudioViewModel : ViewModel() {
        var categories: List<Category> by mutableStateOf(emptyList())
        var albums: List<Album> by mutableStateOf(emptyList())
        private var curCateId = -1
        private var curSucCateId = -1

        fun fetchRankCategory() {
            if (categories.isEmpty().not()) return
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchCategoryOfRankLongAudio {
                    categories = it.data ?: emptyList()
                }
            }
        }

        fun fetchRankDetail(cateId: Int, subCateId: Int) {
            if (curCateId == cateId && curSucCateId == subCateId) return
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi()
                    .fetchAlbumListOfRankLongAudioByCategory(listOf(cateId, subCateId)) {
                        albums = it.data ?: emptyList()
                        if (it.isSuccess()) {
                            curCateId = cateId
                            curSucCateId = subCateId
                        }
                    }
            }
        }

    }
}