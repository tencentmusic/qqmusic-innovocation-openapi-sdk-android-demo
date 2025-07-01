package com.tencent.qqmusic.qplayer.ui.activity.musichall

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Singer
import com.tencent.qqmusic.qplayer.ui.activity.search.plachImageID
import com.tencent.qqmusic.qplayer.ui.activity.songlist.CommonProfileActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Create by tinguo on 2025/4/22
 * CopyWrite (c) 2025 TME. All rights reserved.
 */

@Composable
fun SingerListPage() {
    val coroutineScope = rememberCoroutineScope()
    val singerListViewModel:SingerListViewModel = viewModel()
    val areaIndex = remember { mutableIntStateOf(0) }
    val sexIndex = remember { mutableIntStateOf(0) }
    val genreIndex = remember { mutableIntStateOf(0) }
    val indexIndex = remember { mutableIntStateOf(0) }

    val singerListState = singerListViewModel.singerListState.collectAsState()

    val filterFlow = remember(singerListViewModel) {
        val transform =
            { area: SingerListViewModel.Area, sex: SingerListViewModel.Sex, genre: SingerListViewModel.Genre, index: SingerListViewModel.Index ->
                SingerListFilter(area, sex, genre, index)
            }
        combine(
            singerListViewModel.areaState,
            singerListViewModel.sexState,
            singerListViewModel.genreState,
            singerListViewModel.indexState,
            transform = transform
        )
    }

    LaunchedEffect(Unit) {
        filterFlow.collect { filter->
            areaIndex.intValue = SingerListViewModel.Area.entries.indexOf(filter.area)
            sexIndex.intValue = SingerListViewModel.Sex.entries.indexOf(filter.sex)
            genreIndex.intValue = SingerListViewModel.Genre.entries.indexOf(filter.genre)
            indexIndex.intValue = SingerListViewModel.Index.entries.indexOf(filter.index)

            singerListViewModel.loadSingerList(filter)
        }
    }

    singerListViewModel.loadSingerList(null)

    val showFilter = remember { mutableStateOf(false) }

    Column {
        MusicHallSingerFilterBar(showFilter, areaIndex, sexIndex, genreIndex, indexIndex, singerListViewModel)
        LazyVerticalGrid(columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(singerListState.value) { singer->
                MusicHallSingerItem(singer)
            }
        }
    }
}

@Composable
fun MusicHallSingerFilterBar(
    showFilter: MutableState<Boolean>,
    areaIndex: MutableIntState,
    sexIndex: MutableIntState,
    genreIndex: MutableIntState,
    indexIndex: MutableIntState,
    singerListViewModel: SingerListViewModel
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "筛选")
        Spacer(modifier = Modifier.width(3.dp))
        val res = if (showFilter.value) {
            Icons.Default.KeyboardArrowUp
        } else {
            Icons.Default.KeyboardArrowDown
        }
        IconButton(onClick = {
            showFilter.value = !showFilter.value
        }) {
            Icon(imageVector = res, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
    if (showFilter.value) {
        Spacer(modifier = Modifier.height(10.dp))
        Row {
            Text(text = "地区")
            Spacer(modifier = Modifier.width(10.dp))
            ScrollableTabRow(areaIndex.intValue, backgroundColor = Color.Transparent) {
                SingerListViewModel.Area.entries.forEachIndexed { _, entry ->
                    Tab(
                        modifier = Modifier.width(48.dp),
                        selected = singerListViewModel.areaState.value == entry,
                        onClick = {
                            singerListViewModel.updateArea(entry)
                        },
                        text = {
                            Text(entry.title, style = MaterialTheme.typography.body1)
                        }
                    )
                }
            }
        }
        Divider(modifier = Modifier.fillMaxWidth().height(.5.dp))
        Row {
            Text(text = "性别")
            Spacer(modifier = Modifier.width(10.dp))
            ScrollableTabRow(sexIndex.intValue, backgroundColor = Color.Transparent) {
                SingerListViewModel.Sex.entries.forEachIndexed { _, entry ->
                    Tab(
                        modifier = Modifier.width(48.dp),
                        selected = singerListViewModel.sexState.value == entry,
                        onClick = {
                            singerListViewModel.updateSex(entry)
                        },
                        text = {
                            Text(entry.title, style = MaterialTheme.typography.body1)
                        }
                    )
                }
            }
        }
        Divider(modifier = Modifier.fillMaxWidth().height(.5.dp))
        Row {
            Text(text = "流派")
            Spacer(modifier = Modifier.width(10.dp))
            ScrollableTabRow(genreIndex.intValue, backgroundColor = Color.Transparent) {
                SingerListViewModel.Genre.entries.forEachIndexed { _, entry ->
                    Tab(
                        modifier = Modifier.width(48.dp),
                        selected = singerListViewModel.genreState.value == entry,
                        onClick = {
                            singerListViewModel.updateGenre(entry)
                        },
                        text = {
                            Text(entry.title, style = MaterialTheme.typography.body1)
                        }
                    )
                }
            }
        }
        Divider(modifier = Modifier.fillMaxWidth().height(.5.dp))
        Row {
            Text(text = "索引")
            Spacer(modifier = Modifier.width(10.dp))
            ScrollableTabRow(indexIndex.intValue, backgroundColor = Color.Transparent) {
                SingerListViewModel.Index.entries.forEachIndexed { _, entry ->
                    Tab(
                        modifier = Modifier.width(48.dp),
                        selected = singerListViewModel.indexState.value == entry,
                        onClick = {
                            singerListViewModel.updateIndex(entry)
                        },
                        text = {
                            Text(entry.name, style = MaterialTheme.typography.body1)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalCoilApi::class, ExperimentalFoundationApi::class)
@Composable
fun MusicHallSingerItem(singer: Singer) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.combinedClickable(onClick = {
            context.startActivity(
                Intent(context, CommonProfileActivity::class.java)
                    .putExtra(SongListActivity.KEY_SINGER_ID, singer.id)
            )
        })) {
        Image(
            painter = rememberImagePainter(singer.singerPic150x150 ?: plachImageID,
                builder = {
                    crossfade(false)
                    placeholder(plachImageID)
                }),
            contentDescription = "",
            modifier = Modifier
                .padding(start = 10.dp)
                .size(60.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Text(text = singer.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.body1)
        }
    }
}

data class SingerListFilter(
    val area: SingerListViewModel.Area,
    val sex: SingerListViewModel.Sex,
    val genre: SingerListViewModel.Genre,
    val index: SingerListViewModel.Index
)

class SingerListViewModel: ViewModel() {

    //区域：-100:全部, 200:内地, 2:港台, 3:韩国, 4:日本, 5:欧美
    enum class Area(val title: String, val value: Int) {
        ALL("全部", -100),
        MAINLAND("内地",200),
        HONGKONG("港台", 2),
        KOREA("韩国", 3),
        JAPAN("日本", 4),
        EUROPE("欧美", 5),
    }

    // 性别：-100:全部, 0:男, 1:女, 2:组合
    enum class Sex(val title: String,val value: Int) {
        ALL("全部", -100),
        MALE("男", 0),
        FEMALE("女", 1),
        GROUP("组合", 2),
    }

    // 流派：-100:全部, 1:流行, 2:摇滚, 3:民谣, 4:电子, 5:爵士, 6:嘻哈, 8:R&B, 9:轻音乐, 10:民歌, 14:古典, 20:蓝调, 25:乡
    enum class Genre(val title: String, val value: Int) {
        ALL("全部", -100),
        POP("流行", 1),
        ROCK("摇滚", 2),
        FOLK("民谣", 3),
        ELECTRONIC("电子", 4),
        JAZZ("爵士", 5),
        HIP_HOP("嘻哈", 6),
        RNB("R&B", 8),
        LIGHT_MUSIC("轻音乐", 9),
        BLUES("蓝调", 7),
        SOUL("灵魂", 8),
        TRADITIONAL("民歌", 10),
        CLASSICAL("古典", 14),
        BLUE("蓝调", 20),
        COUNTRY("乡村", 25)
    }

    // 排序：热门, A-Z
    enum class Index(val value: String) {
        HOT("hot"), A("A"), B("B"), C("C"), D("D"), E("E"), F("F"), G("G"), H("H"), I("I"), J("J"), K("K"), L("L"), M("M"), N("N"), O("O"), P("P"), Q("Q"), R("R"), S("S"), T("T"), U("U"), V("V"), W("W"), X("X"), Y("Y"), Z("Z")
    }

    val DEFAULT_FILTER = SingerListFilter(Area.ALL, Sex.ALL, Genre.ALL, Index.HOT)

    private val _areaState: MutableStateFlow<Area> = MutableStateFlow(DEFAULT_FILTER.area)
    val areaState: StateFlow<Area> = _areaState.asStateFlow()

    private val _sexState: MutableStateFlow<Sex> = MutableStateFlow(DEFAULT_FILTER.sex)
    val sexState: StateFlow<Sex> = _sexState.asStateFlow()

    private val _genreState: MutableStateFlow<Genre> = MutableStateFlow(DEFAULT_FILTER.genre)
    val genreState: StateFlow<Genre> = _genreState.asStateFlow()

    private val _indexState: MutableStateFlow<Index> = MutableStateFlow(DEFAULT_FILTER.index)
    val indexState: StateFlow<Index> = _indexState.asStateFlow()

    private val _singerListState: MutableStateFlow<List<Singer>> = MutableStateFlow(emptyList())
    val singerListState: StateFlow<List<Singer>> = _singerListState.asStateFlow()

    private val hasMoreState: MutableState<Boolean> = mutableStateOf(false)
    val hasMore: State<Boolean> = hasMoreState

    private var nextPage = 0 // 接口尚未提供分页

    private var loadJob: Job? = null

    fun loadSingerList(filter: SingerListFilter?) {
        _singerListState.update { emptyList() }
        loadJob?.cancel()

        val singerFilter = filter ?: DEFAULT_FILTER
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchHotSingerListWithFilter(
                singerFilter.area.value,
                singerFilter.sex.value,
                singerFilter.genre.value,
                singerFilter.index.value,
                bigResponse = 0
            ) { resp->
                if (resp.isSuccess()) {
                    _singerListState.update { resp.data ?: emptyList() }
                    hasMoreState.value = resp.hasMore
                } else {
                    MLog.i("SingerListViewModel", "loadSingerList failed:$resp")
                }
            }
        }
    }

    fun updateArea(area: Area) {
        _areaState.update { area }
    }

    fun updateSex(sex: Sex) {
        _sexState.update { sex }
    }

    fun updateGenre(genre: Genre) {
        _genreState.update { genre }
    }

    fun updateIndex(index: Index) {
        _indexState.update { index }
    }

}