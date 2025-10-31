package com.tencent.qqmusic.qplayer.ui.activity.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.Category
import com.tencent.qqmusic.openapisdk.model.JumpInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.LoadMoreItem
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumListPage
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
            PerformanceHelper.monitorClick("LongAudioPage_LongAudioCategoryActivity_$tag")
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
                                vm.albums = emptyList() // 切换tab，默认清空二级列表
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
        if (categorys2.isNullOrEmpty()) {
            return
        }
        var selectType by remember { mutableIntStateOf(vm.getCurSortType()) }
        val pagerState = rememberPagerState(vm.initSid)
        val stateAdd = pagerState.hashCode()
        Log.i(TAG, "new secondPage: hash:$stateAdd")
        val composableScope = rememberCoroutineScope()
        // 获取当前的 fid 和 sid
        val fid = categorys.getOrNull(index)?.id ?: 0
        val sid = categorys2.getOrNull(pagerState.currentPage)?.id ?: 0
        // 使用 LaunchedEffect 和 debounce 来控制 fetchCategoryDetail 的调用
        LaunchedEffect(Pair(fid, sid)) {
            // 延迟 500ms
            delay(500)
            // 检查 fid 和 sid 是否仍然是当前的值
            if (fid == (categorys.getOrNull(index)?.id ?: 0) &&
                sid == (categorys2.getOrNull(pagerState.currentPage)?.id ?: 0)) {
                if ((vm.albums.isEmpty() && pagerState.currentPage == 0) || (fid != 0 && sid != 0)) {
                    vm.fetchCategoryDetail(fid, sid)
                    selectType = 0
                }
            }
        }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("最新", "最热").forEachIndexed { index, mode ->
                    Button(
                        onClick = { // 切换形态后刷新页面
                            selectType = index
                            composableScope.launch {
                                vm.fetchCategoryDetail(cateId = vm.getCurCateId(),
                                    subCateId = vm.getCurSucCateId(),
                                    sortType = index,
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (selectType == index) Color.Blue else Color.LightGray
                        )
                    ) {
                        Text(text = mode, color = Color.White)
                    }
                }
            }
            HorizontalPager(count = categorys2.size,
                state = pagerState,
                modifier = Modifier.fillMaxSize()) {
                val loadMoreState = vm.hasMoreState.collectAsState()
                Column(Modifier.fillMaxSize()) {
                    Text(text = "first tab index:${index}, second tab index:${pagerState.currentPage}")
                    AlbumListPage(albums = vm.albums, loadMoreItem = LoadMoreItem(loadMoreState, onLoadMore = {
                        vm.fetchCategoryDetail(fid, sid) // 翻页
                    }))
                }
            }
        }
    }



    class LongAudioViewModel : ViewModel() {
        var albums: List<Album> by mutableStateOf(emptyList())
        val _hasMoreStateFlow = MutableStateFlow(false)
        var hasMoreState: StateFlow<Boolean> = _hasMoreStateFlow.asStateFlow()
        private var curCateId = -1
        private var curSucCateId = -1
        private var curSortType:Int = 0
        var nextPage: Int = 0

        var initFid = 0
        var initSid = 0

        var categories: List<Category> by mutableStateOf(emptyList())

        fun getCurCateId(): Int = curCateId

        fun getCurSucCateId(): Int = curSucCateId

        fun getCurSortType(): Int = curSortType

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

        fun fetchCategoryDetail(cateId: Int, subCateId: Int, sortType:Int=0) {
            if (curCateId != cateId || curSucCateId != subCateId || sortType!=curSortType) {
                nextPage = 0
                _hasMoreStateFlow.update { false }
                albums = emptyList()
            }
            viewModelScope.launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi()
                    .fetchAlbumListOfLongAudioByCategory(listOf(cateId, subCateId), listOf(-1), page = nextPage, count = 60, sortType=sortType) {
                        albums = ArrayList<Album>(albums).also { newList->
                            newList.addAll(it.data ?: emptyList())
                        }
                        if (it.isSuccess()) {
                            curCateId = cateId
                            curSucCateId = subCateId
                            curSortType = sortType
                            _hasMoreStateFlow.update {_-> it.hasMore }
                            nextPage++
                        }
                    }
            }
        }

    }
}