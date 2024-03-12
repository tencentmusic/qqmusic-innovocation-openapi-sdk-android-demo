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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.*
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.Category
import com.tencent.qqmusic.openapisdk.model.JumpInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 长音频分类
 */
class LongAudioCategoryActivity : ComponentActivity() {

    private val tag by lazy {
        intent.getStringExtra("tag") ?: ""
    }

    private val jumpInfo: JumpInfo? by lazy {
        intent.getParcelableExtra("jumpInfo")
    }

    companion object {

        private const val TAG = "CategoryActivity"

        @JvmStatic
        fun start(context: Context, tag: String, jumpInfo: JumpInfo?) {
            val starter = Intent(context, LongAudioCategoryActivity::class.java)
                .putExtra("tag", tag)
                .putExtra("jumpInfo", jumpInfo)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val vm: LongAudioViewModel by viewModels()
        setContent {
//            val vm: LongAudioViewModel = viewModel()
            Scaffold(topBar = { TopBar("长音频分类") }) {
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
        LaunchedEffect(Unit) {
            vm.fetchCategoryAudio()
        }
        val categorys = vm.categories
        if (categorys.isEmpty()) {
            Text(text = "kong")
            return
        } else {
            vm.initTab(tag, jumpInfo)
        }
        Log.i(TAG, "LongAudioScreen: init index:${vm.initFid}")
        val pagerState1 = rememberPagerState(initialPage = vm.initFid)

        val composableScope = rememberCoroutineScope()
        Column(modifier = Modifier.fillMaxSize()) {
            // 一级TAB
            ScrollableTabRow(
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
            LaunchedEffect(key1 = pagerState1) {
                delay(500)
                pagerState1.scrollToPage(vm.initFid)
            }
            HorizontalPager(count = categorys.size, state = pagerState1, modifier = Modifier.fillMaxSize()) {
                LongAudioSecondPage(categorys = categorys, index = pagerState1.currentPage, vm =vm)
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun LongAudioSecondPage(categorys: List<Category>, index: Int, vm: LongAudioViewModel) {
        val categorys2 = categorys.getOrNull(index)?.subCategory
        if (categorys2 == null || categorys2.isEmpty()) {
            return
        }
        val pagerState = rememberPagerState(vm.initSid)
        val stateAdd = pagerState.hashCode()
        Log.i(TAG, "new secondPage: hash:$stateAdd")
        val composableScope = rememberCoroutineScope()
        Column(Modifier.fillMaxSize()) {
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
                val fid = categorys.getOrNull(index)?.id ?: 0
                val sid = categorys2.getOrNull(pagerState.currentPage)?.id ?: 0
                LaunchedEffect(Pair(fid, sid)){
                    vm.fetchCategoryDetail(fid, sid)
                }
                Log.i(
                    TAG,
                    "first tab index:${index}, second tab index:${pagerState.currentPage} hash:${pagerState.hashCode()}"
                )
                Column(Modifier.fillMaxSize()) {
                    Text(text = "first tab index:${index}, second tab index:${pagerState.currentPage}")
                    AlbumPage(albums = vm.albums)
                }
            }
        }
    }



    class LongAudioViewModel : ViewModel() {
        var albums: List<Album> by mutableStateOf(emptyList())
        private var curCateId = -1
        private var curSucCateId = -1

        var initFid = 0
        var initSid = 0

        var categories: List<Category> by mutableStateOf(emptyList())

        fun initTab(tag: String, jumpInfo: JumpInfo?) {
            if (categories.isEmpty()) return
            if (tag.isNotEmpty()) {
                categories.forEachIndexed() { index, category ->
                    if (category.name == tag) {
                        initFid = index
                    }
                }
            } else if (jumpInfo != null) {
                jumpInfo.args?.getOrNull(0)?.intArrVal?.apply {
                    this.getOrNull(0)?.apply {
                        categories.forEachIndexed() { index, category ->
                            if (category.id == this) {
                                initFid = index
                            }
                        }
                    }
                    this.getOrNull(1)?.apply {
                        categories.getOrNull(initFid)?.subCategory?.forEachIndexed() { index, category ->
                            if (category.id == this) {
                                initSid = index
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "initFid:$initFid, initSid:$initSid")
        }

        fun fetchCategoryAudio() {
            if (categories.isEmpty().not()) return
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchCategoryOfLongAudio() {
                    categories = it.data ?: emptyList()
                }
            }
        }

        fun fetchCategoryDetail(cateId: Int, subCateId: Int) {
            if (curCateId == cateId && curSucCateId == subCateId) return
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi()
                    .fetchAlbumListOfLongAudioByCategory(listOf(cateId, subCateId), listOf(-1), count = 60) {
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