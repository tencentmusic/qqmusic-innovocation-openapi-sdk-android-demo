package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.model.PlayParam
import com.tencent.qqmusic.openapisdk.model.RecentPlayType
import com.tencent.qqmusic.openapisdk.model.SingerDetail
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.playerinsight.util.coverErrorCode
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.core.player.playlist.MusicPlayList
import com.tencent.qqmusic.qplayer.core.player.proxy.FromInfo
import com.tencent.qqmusic.qplayer.ui.activity.LoadMoreItem
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.loadMoreItemUI
import com.tencent.qqmusic.qplayer.ui.activity.login.FolderDialog
import com.tencent.qqmusic.qplayer.ui.activity.login.WebViewActivity
import com.tencent.qqmusic.qplayer.ui.activity.main.CopyableText
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.player.FloatingPlayerPage
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import com.tencent.qqmusic.qplayer.utils.UiUtils
import java.text.DateFormat
import java.util.Date

class CommonProfileActivity: ComponentActivity() {

    private val viewModel by lazy { ViewModelProvider(this)[CommonProfileViewModel::class.java] }

    private val homeViewModel: HomeViewModel by viewModels()

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = Bundle().apply {
            intent.extras?.let { putAll(it) }
            savedInstanceState?.let { putAll(it) }
        }
        viewModel.initWithArguments(args)
        setContent {
            Scaffold(
                topBar = { TopBar() },
                modifier = Modifier.semantics{ testTagsAsResourceId=true },
                bottomBar = { FloatingPlayerPage() }) {
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
        val tabIndex = remember { mutableIntStateOf(0) } // 0:歌曲，1:歌单/专辑
        val showEditorView = remember { mutableStateOf(false) } // 编辑模式
        val showAddSongDialog = remember { mutableStateOf(false) } // 添加歌曲对话框
        val showDialogState = remember {
            mutableStateOf(ShowDialogState(showEditorView.value, showAddSongDialog.value))
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
                    StickPlayListHeader(
                        viewModel = viewModel,
                        songListState = songListState,
                        showDialogState = showDialogState.value){ showState->
                        showEditorView.value = showState.showEditor
                        showAddSongDialog.value = showState.showAddSong
                    }
                    if (showEditorView.value){
                        EditorView(viewModel = viewModel,
                            songs = songListState.value ?: emptyList()){
                            showEditorView.value = false
                            viewModel.clearSelection()
                        }
                    }
                    if (showAddSongDialog.value){
                        AddSongDialog(viewModel = viewModel){
                            showAddSongDialog.value=false
                        }
                    }
                }
            }

            when (tabIndex.intValue) {
                0 -> {
                    // 歌曲列表
                    val songs = songListState.value ?: emptyList()
                    itemsIndexed(songs) { index, song->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showEditorView.value) {
                                // 批量操作
                                Checkbox(
                                    checked = viewModel.selectedSongs.contains(song),
                                    onCheckedChange = { viewModel.toggleSongSelection(song) }
                                )
                            }
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

    @OptIn(ExperimentalCoilApi::class)
    @Composable
    fun EditorView(
        viewModel: CommonProfileViewModel,
        songs: List<SongInfo>,
        callback: () -> Unit
    ) {
        var checked by remember { mutableStateOf(false) }
        Row {
            // 全选按钮
            Checkbox(
                checked = checked,
                onCheckedChange = {
                    if (checked){
                        viewModel.clearSelection()
                    }else{
                        viewModel.selectAllSongs(songs = songs)
                    }
                    checked = checked.not()
                }
            )
            Row(modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.End) {
                // 删除按钮
                IconButton(
                    onClick = {
                        viewModel.deleteSelectedSongs()
                        HomeViewModel.myFolderRequested = false
                        callback.invoke()
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "删除")
                }

                IconButton(
                    onClick = {
                        val ret = OpenApiSDK.getPlayerApi().appendSongToPlaylist(viewModel.selectedSongs)
                        HomeViewModel.myFolderRequested = false
                        if (ret != PlayDefine.PlayError.PLAY_ERR_NONE) {
                            UiUtils.showToast("添加播放列表失败:ret:${ret}")
                        } else {
                            UiUtils.showToast("添加播放列表成功")
                        }
                    }
                ) {
                    Icon(
                        painter = rememberImagePainter(R.drawable.action_icon_play_next),
                        contentDescription = "添加播放列表"
                    )
                }
            }

        }
    }

    data class ShowDialogState(var showEditor: Boolean, var showAddSong: Boolean)

    @OptIn(ExperimentalCoilApi::class)
    @Composable
    fun StickPlayListHeader(
        viewModel: CommonProfileViewModel,
        songListState: State<List<SongInfo>?>,
        showDialogState: ShowDialogState,
        callback: (ShowDialogState) -> Unit
    ) {
        val isCurrentPlayList = PlayerObserver.currentPlayList.let { playListState->
            playListState.first == viewModel.playListType && playListState.second == viewModel.playListTypeId
        }
        val isPlaying = PlayerObserver.currentState == PlayDefine.PlayState.MEDIAPLAYER_STATE_STARTED
        Row(modifier = Modifier.background(color = Color.White).fillMaxWidth()) {
            OutlinedButton(modifier = Modifier.wrapContentSize(),
                border = BorderStroke(0.dp, Color.Transparent),
                onClick = {
                    startPlayAndReportRecentPlay(viewModel, songListState)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val text = if (isCurrentPlayList) {
                        if (isPlaying) "暂停" else "继续"
                    } else {
                        "全部"
                    }
                    val icon = if (isCurrentPlayList && isPlaying) {
                        R.drawable.icon_pause
                    } else {
                        R.drawable.icon_play
                    }
                    Icon(painter = painterResource(id = icon), contentDescription = text,  modifier = Modifier.size(30.dp))
                    Text(text = text, fontSize = 14.sp)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.End) {
                if (!showDialogState.showEditor){
                    IconButton(
                        onClick = {
                            val songs = songListState.value?.shuffled() ?: emptyList()
                            if (songs.isNotEmpty()){
                                val ret = OpenApiSDK.getPlayerApi().appendSongToPlaylist(songs)
                                if (ret != PlayDefine.PlayError.PLAY_ERR_NONE) {
                                    UiUtils.showToast("添加播放列表失败:ret:${ret}")
                                } else {
                                    UiUtils.showToast("添加播放列表成功")
                                }
                            }else{
                                UiUtils.showToast("无歌曲")
                            }
                        }
                    ) {
                        Icon(
                            painter = rememberImagePainter(R.drawable.action_icon_play_next),
                            contentDescription = "添加播放列表",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(onClick = {
                        callback.invoke(showDialogState.apply {
                            showAddSong=showAddSong.not()
                            showEditor=false
                        })
                    }) {
                        Icon(painter = rememberImagePainter(R.drawable.folder_action_icon_add_song),
                            contentDescription = "添加歌曲",
                            modifier = Modifier.size(30.dp))
                    }
                }

                IconButton(onClick = {
                    callback.invoke(showDialogState.apply {
                        showEditor=showEditor.not()
                        showAddSong=false
                    })
                    viewModel.clearSelection()
                }) {
                    Icon(painter = rememberImagePainter(R.drawable.action_icon_folder_multi_opt),
                        contentDescription = "批量操作",
                        modifier = Modifier.size(30.dp))
                }
            }
            EntityOrderView(viewModel, Album::class.java, SongInfo::class.java)
        }
    }

    private fun startPlayAndReportRecentPlay(
        viewModel: CommonProfileViewModel,
        songListState: State<List<SongInfo>?>
    ) {
        val playListState = PlayerObserver.currentPlayList
        val isCurrentPlayList = playListState.first == viewModel.playListType && playListState.second == viewModel.playListTypeId
        if (isCurrentPlayList) {
            if (OpenApiSDK.getPlayerApi().isPlaying()) {
                OpenApiSDK.getPlayerApi().pause()
            } else if (OpenApiSDK.getPlayerApi().isPause()) {
                OpenApiSDK.getPlayerApi().resume()
            } else {
                OpenApiSDK.getPlayerApi().play(FromInfo.FROM_NORMAL)
            }
            return
        }

        val songs = songListState.value ?: emptyList()
        if (songs.isNotEmpty()) {
            val playParam = PlayParam(
                playListType = viewModel.playListType,
                playListTypeId = viewModel.playListTypeId,
                songList = songs,
                playMode = OpenApiSDK.getPlayerApi().getPlayMode(),
                startPlay = true
            )
            val ret = OpenApiSDK.getPlayerApi().playSongs(playParam)
            if (ret != PlayDefine.PlayError.PLAY_ERR_NONE) {
                UiUtils.showPlayErrToast(ret,songs.firstOrNull())
                return
            } else {
                UiUtils.showToast("开始全部播放")
                reportRecentPlay()
            }
        }
    }

    private fun reportRecentPlay() {
        val resType = when (viewModel.playListType) {
            MusicPlayList.PLAY_LIST_ALBUM_TYPE -> RecentPlayType.ALBUM
            MusicPlayList.PLAY_LIST_FOLDER_TYPE -> RecentPlayType.FOLDER
            else -> return
        }
        OpenApiSDK.getOpenApi().reportRecentPlay("${viewModel.playListTypeId}", resType) { resp->
            Log.i("playlistHeader", "reportRecentPlay resp: $resp")
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
        val activity = LocalContext.current as Activity
        var showEditorDialog by remember { mutableStateOf(false) } // 歌单编辑

        val folder = state.value
        titleState.value = "歌单详情"

        Column(modifier = Modifier) {
            Box {
                Image(
                    painter = rememberImagePainter(
                        data = folder?.picUrl ?: "",
                        builder = {
                            transformations(RoundedCornersTransformation())
                        }
                    ),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(100.dp).clickable{
                        folder?.picUrl?.let {
                            WebViewActivity.start(activity, it)
                        }
                    }
                )
                IconButton(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color = Color.White.copy(alpha = 0.8f))
                        .align(Alignment.BottomEnd), onClick = {
                            if (folder?.ownerFlag!=1){
                                UiUtils.showToast("非自建歌单，可能无法编辑")
                            }
                        showEditorDialog = true
                    }) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑歌单")
                }
            }
            CopyableText(title="歌单id", content = folder?.id)
            CopyableText("歌单名称",folder?.name)
            var showDetailDialog by remember { mutableStateOf(false) }
            Row {
                if ((folder?.introduction?.length ?: 0) > 15 && showDetailDialog.not()){
                    CopyableText("歌单描述",
                        folder?.introduction?.substring(0,15)+"...",
                        folder?.introduction)
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "查看全部",
                        modifier = Modifier.clickable{
                            showDetailDialog = true
                        })
                }else{
                    CopyableText("歌单描述",
                        folder?.introduction,
                        folder?.introduction)
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "收起",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable{ showDetailDialog = false })
                }
            }
            CopyableText("创建者",folder?.creator?.name)
            CopyableText("创建时间",(DateFormat.getDateInstance().format(Date((folder?.createTime ?: 0).toLong()))))
            CopyableText("更新时间",(DateFormat.getDateInstance().format(Date((folder?.updateTime ?: 0).toLong()))))
            CopyableText("收藏数量",UiUtils.getFormatNumber(folder?.favNum))
            CopyableText("播放数量",UiUtils.getFormatNumber(folder?.listenNum))
        }

        if (showEditorDialog) {
            FolderDialog(homeViewModel, showEditorDialog, isCreate = false, dissId = viewModel.playListTypeId.toString()) {
                showEditorDialog = false
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
            Image(painter = rememberImagePainter(singer.value?.singerPic ?: ""),
                contentDescription = "",
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                contentScale = ContentScale.Fit)
            CopyableText("歌手id", singer.value?.singerId.toString())
            CopyableText("歌手mid", singer.value?.singerMid)
            CopyableText("歌手名称", singer.value?.singerName)


            (singerWiki?.basicInfo?.entries?.toList() ?: emptyList()).forEach {
                CopyableText(it.key, it.value)
            }

            singerWiki?.groupListInfo?.forEach {
                it.entries.toList().forEach { entry ->
                    CopyableText(entry.key, entry.value)
                }
            }

            Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = "歌手简介:", modifier = Modifier.wrapContentWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
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

    @Composable
    fun AddSongDialog(viewModel: CommonProfileViewModel, onDismissRequest: () -> Unit) {
        var songIdListString by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            title = { Text("添加歌曲到当前歌单") },
            text = {
                Column {
                    Text("folderId=${viewModel.playListTypeId}")
                    OutlinedTextField(
                        value = songIdListString,
                        onValueChange = { songIdListString = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(text = "请输入songId,多个以英文逗号分隔") })
                }
            },
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    onClick = {
                        if (songIdListString.isBlank()){
                            return@TextButton
                        }
                        if (!songIdListString.matches(Regex("^[0-9,]+$"))) {
                            return@TextButton
                        }
                        val songIdList = songIdListString.split(",").mapNotNull { it.trim().toLongOrNull() }
                        OpenApiSDK.getOpenApi().addSongToFolder(
                            folderId = viewModel.playListTypeId.toString(),
                            songIdList = songIdList){
                            if (it.isSuccess()){
                                UiUtils.showToast("添加成功")
                                viewModel.fetchData()
                                onDismissRequest()
                            }else{
                                UiUtils.showToast("添加失败:${it.errorMsg}")
                            }
                        }
                    }
                ) {
                    Text(text = "添加歌曲到歌单", fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismissRequest() }){ Text("取消") }
            }
        )
    }

}