package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.os.Bundle
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.model.SingerDetail
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.ui.activity.LoadMoreItem
import com.tencent.qqmusic.qplayer.ui.activity.loadMoreItemUI
import com.tencent.qqmusic.qplayer.ui.activity.player.FloatingPlayerPage
import com.tencent.qqmusic.qplayer.utils.UiUtils
import java.text.DateFormat
import java.util.Date

class CommonProfileActivity: ComponentActivity() {

    private val viewModel by lazy { ViewModelProvider(this)[CommonProfileViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = Bundle().apply {
            intent.extras?.let { putAll(it) }
            savedInstanceState?.let { putAll(it) }
        }
        viewModel.initWithArguments(args)
        setContent {
            Scaffold(
                bottomBar = {
                    FloatingPlayerPage()
                }
            ) {
                MainView(modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 70.dp))
            }
        }
    }

    private val titleState = mutableStateOf("")

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainView(modifier: Modifier) {
        LaunchedEffect(Unit) {
            viewModel.fetchData()
        }
        val tabs = remember {
            mutableStateListOf("歌曲")
        }
        val tabIndex = remember {
            mutableIntStateOf(0)
        }
        val songListState: State<List<SongInfo>?> = remember {
            viewModel.profileDataList(SongInfo::class.java) ?: mutableStateOf(null)
        }
        val songListHasMoreState: State<Boolean> = remember {
            viewModel.hasMore(SongInfo::class.java) ?: mutableStateOf(false)
        }
        val songTotalState: State<Int> = remember {
            viewModel.total(SongInfo::class.java) ?: mutableIntStateOf(0)
        }
        val albumListState: State<List<Album>?> = remember {
            viewModel.profileDataList(Album::class.java) ?: mutableStateOf(null)
        }
        val albumListHasMoreState: State<Boolean> = remember {
            viewModel.hasMore(Album::class.java) ?: mutableStateOf(false)
        }
        val albumTotalState: State<Int> = remember {
            viewModel.total(Album::class.java) ?: mutableIntStateOf(0)
        }
        if (viewModel.profileDataList(Album::class.java) != null) {
            tabs.add("专辑")
        }

        LazyColumn(modifier = modifier) {
            item {
                ProfileView()
            }

            stickyHeader {
                TabRow(selectedTabIndex = tabIndex.intValue, indicator = { tabPositions ->

                }) {
                    tabs.forEachIndexed { index, title ->
                        val extra = when (index) {
                            0 -> "(${songTotalState.value} 首)"
                            1 -> "(${albumTotalState.value} 张)"
                            else -> ""
                        }
                        Tab(selected = tabIndex.intValue == index, text = {
                            Text(text = "$title $extra")
                        }, selectedContentColor = Color.White, unselectedContentColor = Color.Gray, onClick = {
                            tabIndex.intValue = index
                        })
                    }
                }
                if (tabIndex.intValue == 0) {
                    StickPlayListHeader(viewModel, songListState)
                }
            }

            when (tabIndex.intValue) {
                0 -> {
                    val songs = songListState.value ?: emptyList()
                    itemsIndexed(songs) { index, song->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${index + 1}", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            itemUI(PlayListParams(songs, song, playListType = viewModel.playListType, playListTypeId = viewModel.playListTypeId))
                        }
                    }
                    loadMoreItemUI(songs.size, loadMoreItem = LoadMoreItem(songListHasMoreState, onLoadMore = {
                        viewModel.fetchMoreData(SongInfo::class.java)
                    }))
                }
                1 -> {
                    val albums = albumListState.value ?: emptyList()
                    items(albums) { album ->
                        albumItemUI(album)
                    }
                    loadMoreItemUI(albums.size, loadMoreItem = LoadMoreItem(albumListHasMoreState, onLoadMore = {
                        viewModel.fetchMoreData(Album::class.java)
                    }))
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    @Composable
    fun StickPlayListHeader(
        viewModel: CommonProfileViewModel,
        songListState: State<List<SongInfo>?>
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Spacer(modifier = Modifier.width(5.dp))
            OutlinedButton(modifier = Modifier.wrapContentSize(),
                border = BorderStroke(1.dp, brush = ButtonDefaults.outlinedBorder.brush),
                onClick = {
                    val songs = songListState.value ?: emptyList()
                    if (songs.isNotEmpty()) {
                        val ret = OpenApiSDK.getPlayerApi().playSongs(songs)
                        if (ret != PlayDefine.PlayError.PLAY_ERR_NONE) {
                            UiUtils.showToast("播放失败:ret:${ret}")
                        } else {
                            UiUtils.showToast("开始全部播放")
                        }
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "全部播放", fontSize = 16.sp)
                }
            }

            OutlinedButton(
                modifier = Modifier.wrapContentSize(),
                border = BorderStroke(1.dp, brush = ButtonDefaults.outlinedBorder.brush),
                onClick = {
                    val songs = songListState.value?.shuffled() ?: emptyList()
                    if (songs.isNotEmpty()) {
                        val ret = OpenApiSDK.getPlayerApi().appendSongToPlaylist(songs)
                        if (ret != PlayDefine.PlayError.PLAY_ERR_NONE) {
                            UiUtils.showToast("添加播放列表失败:ret:${ret}")
                        } else {
                            UiUtils.showToast("添加播放列表成功")
                        }
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "添加播放列表", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "添加播放列表", fontSize = 16.sp)
                }
            }

            EntityOrderView(viewModel, Album::class.java, SongInfo::class.java)
        }
    }

    @Composable
    private fun <T, R> EntityOrderView(
        viewModel: CommonProfileViewModel,
        typeToken: Class<T>,
        typeListToken: Class<R>
    ) {
        val typeState = viewModel.profileData(typeToken)
            ?: return

        val hasOrder = when(typeToken) {
            Album::class.java -> {
                (typeState.value as? Album).let { album->
                    album != null && !TextUtils.isEmpty(album.longAudioTag)
                }
            }
            else -> false
        }

        val orderState = remember {
            viewModel.order(typeListToken) ?: mutableIntStateOf(ORDER_UNDEFINE)
        }

        val selectOrderSheetState = remember { mutableStateOf(false) }
        if (orderState.value != ORDER_UNDEFINE && hasOrder) {
            OutlinedButton(modifier = Modifier.wrapContentSize(),
                border = BorderStroke(1.dp, brush = ButtonDefaults.outlinedBorder.brush),
                onClick = {
                    selectOrderSheetState.value = true
                }
            ) {
                val text = when(orderState.value) {
                    ORDER_DEFAULT -> "默认"
                    ORDER_REVERSE -> "倒序"
                    ORDER_POSITIVE -> "正序"
                    else -> "排序"
                }
                Text(text = text, fontSize = 16.sp)
            }
            if (selectOrderSheetState.value) {
                SelectOrderModelDialog(viewModel, selectOrderSheetState)
            }
        }
    }

    @Composable
    fun ProfileView() {
        val folderState = viewModel.profileData(Folder::class.java)
        if (folderState != null) {
            FolderProfileView(folderState)
        }

        val singerState = viewModel.profileData(SingerDetail::class.java)
        if (singerState != null) {
            SingerProfileView(singerState)
        }

        val albumState = viewModel.profileData(Album::class.java)
        if (albumState != null) {
            AlbumProfileView(albumState)
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    @Composable
    fun FolderProfileView(state: State<Folder?>)  {
        val folder = state.value
        titleState.value = "歌单详情"

        Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Image(
                    painter = rememberImagePainter(
                        data = folder?.picUrl ?: "",
                        builder = {
                            transformations(RoundedCornersTransformation())
                        }
                    ),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                )
            }

            Row {
                TabCell(text = "歌单id", 0.3f)
                TabCell(text = folder?.id ?: "", 0.7f)
            }
            Row {
                TabCell(text = "歌单名称", 0.3f)
                TabCell(text = folder?.name ?: "", 0.7f)
            }
            Row {
                TabCell(text = "创建者", 0.3f)
                TabCell(text = folder?.creator?.name ?: "", 0.7f)
            }
            Row {
                TabCell(text = "创建时间", 0.3f)
                TabCell(text = (DateFormat.getDateInstance().format(Date((folder?.createTime ?: 0).toLong()))), 0.7f)
            }
            Row {
                TabCell(text = "更新时间", 0.3f)
                TabCell(text = (DateFormat.getDateInstance().format(Date((folder?.updateTime ?: 0).toLong()))), 0.7f)
            }
            Row {
                TabCell(text = "收藏数量", 0.3f)
                TabCell(text = "${folder?.favNum}", 0.7f)
            }
            Row {
                TabCell(text = "播放数量", 0.3f)
                TabCell(text = "${folder?.listenNum}", 0.7f)
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    @Composable
    fun SingerProfileView(singer: State<SingerDetail?>) {
        titleState.value = "歌手详情"
        val singerWiki = singer.value?.wikiInfo

        val showMoreDescState = remember { mutableStateOf(false) }

        Column {
            Row {
                Image(painter = rememberImagePainter(singer.value?.singerPic ?: ""), contentDescription = "", modifier = Modifier.fillMaxWidth().height(300.dp))
            }

            Row {
                TabCell(text = "歌手id", 0.3f)
                TabCell(text = "${singer.value?.singerId}", 0.7f)
            }

            Row {
                TabCell(text = "歌手mid", 0.3f)
                TabCell(text = "${singer.value?.singerMid}", 0.7f)
            }

            Row {
                TabCell(text = "歌手名称", 0.3f)
                TabCell(text = singer.value?.singerName ?: "", 0.7f)
            }

            (singerWiki?.basicInfo?.entries?.toList() ?: emptyList()).forEach {
                Row {
                    TabCell(text = it.key, 0.3f)
                    TabCell(text = it.value, 0.7f)
                }
            }

            singerWiki?.groupListInfo?.forEach {
                it.entries.toList().forEach { entry ->
                    Row {
                        TabCell(text = entry.key, 0.3f)
                        TabCell(text = entry.value, 0.7f)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = "歌手简介", modifier = Modifier.wrapContentWidth(), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(modifier = Modifier.size(32.dp), onClick = {
                    showMoreDescState.value = !showMoreDescState.value
                }) {
                    if (showMoreDescState.value) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "", tint = Color.Blue)
                    } else {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "", tint = Color.Blue)
                    }
                }
            }
            if (showMoreDescState.value) {
                Row {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Text(text = singerWiki?.desc ?: "", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = if (showMoreDescState.value) Int.MAX_VALUE else 3)
                    }
                }
                Row {
                    Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
                }

                Row {
                    (singerWiki?.otherInfo?.entries?.toList() ?: emptyList()).forEach {
                        Column {
                            Text(text = "${it.key}  :  ${it.value}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            Divider(thickness = 1.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp, start = 10.dp, end = 20.dp))
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    @Composable
    fun AlbumProfileView(state: State<Album?>)  {
        val album = state.value
        titleState.value = "歌单详情"

        Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Image(
                    painter = rememberImagePainter(
                        data = album?.pic500x500 ?: "",
                        builder = {
                            transformations(RoundedCornersTransformation())
                        }
                    ),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                )
            }

            Row {
                TabCell(text = "专辑id", 0.3f)
                TabCell(text = album?.id ?: "", 0.7f)
            }
            Row {
                TabCell(text = "专辑名称", 0.3f)
                TabCell(text = album?.name ?: "", 0.7f)
            }

            Row {
                TabCell(text = "发布时间", 0.3f)
                TabCell(text = album?.publicTime, 0.7f)
            }
            Row {
                TabCell(text = "收藏数量", 0.3f)
                TabCell(text = "${album?.favCount}", 0.7f)
            }
            Row {
                TabCell(text = "播放数量", 0.3f)
                TabCell(text = "${album?.listenNum}", 0.7f)
            }
        }
    }

    @Composable
    fun SelectOrderModelDialog(viewModel: CommonProfileViewModel, sheetState: MutableState<Boolean>) {
        AlertDialog(
            title = {},
            text = {
                Column {
                    TextButton(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        onClick = {
                            viewModel.order(SongInfo::class.java, ORDER_POSITIVE)
                            sheetState.value = false
                        }
                    ) {
                        Text(text = "正序", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        onClick = {
                            viewModel.order(SongInfo::class.java, ORDER_REVERSE)
                            sheetState.value = false
                        }
                    ) {
                        Text(text = "倒序", fontSize = 16.sp)
                    }
                }
            },
            onDismissRequest = {
                sheetState.value = false
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

}