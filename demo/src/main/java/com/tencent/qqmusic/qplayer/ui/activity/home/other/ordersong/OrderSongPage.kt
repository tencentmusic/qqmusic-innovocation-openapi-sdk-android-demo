package com.tencent.qqmusic.qplayer.ui.activity.home.other.ordersong

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.openapisdk.model.PlayParam
import com.tencent.qqmusic.openapisdk.ordersong.entity.RoomMemberInfo
import com.tencent.qqmusic.openapisdk.ordersong.entity.RoomSongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.core.player.playlist.MusicPlayList
import com.tencent.qqmusic.qplayer.ui.activity.main.CopyableText
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import com.tencent.qqmusic.qplayer.ui.activity.songlist.PlayListParams
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongProfileActivity
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI


@RequiresApi(Build.VERSION_CODES.N)
@SuppressLint("UnrememberedMutableState")
@Composable
fun OrderSongPage() {

    val viewModel: OrderSongViewModel = viewModel()
    viewModel.init()
    viewModel.updateTestSongs()
//    viewModel.queryRoom()

    var roomNameInput by remember { mutableStateOf("") }
    var roomIdInput by remember { mutableStateOf(viewModel.roomInfo?.roomId?:"") }
    var initSongIdList by remember {
        mutableStateOf(
            OpenApiSDK.getPlayerApi().getPlayList().map {
                it.songId
            }.joinToString(",").ifEmpty {
                "680285,273914224,667022,467428380"
            })
    }
    var showInputDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        item {
            Text("接口请求状态：\n${viewModel.requestResult}")
            Divider()
            Text("房间连接状态：${viewModel.roomConnectedStatus}")
            RoomInfoPage(viewModel)
            Divider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = roomNameInput,
                    onValueChange = { roomNameInput = it },
                    label = { Text("房间名") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = initSongIdList,
                        onValueChange = { initSongIdList = it.removePrefix(",").removeSuffix(",") },
                        label = { Text("创建房间初始歌曲ID列表") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                    IconButton(
                        onClick = {
                            val addSongList = viewModel.updateTestSongs().map { it.songId }
                            if (addSongList.isNotEmpty()) {
                                initSongIdList += addSongList.joinToString(
                                    separator = ",",
                                    prefix = ","
                                )
                                viewModel.testSongList.clear()
                            }
                            initSongIdList = initSongIdList.removePrefix(",").removeSuffix(",")
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "随机添加歌曲ID")
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = roomIdInput,
                    onValueChange = { roomIdInput = it },
                    label = { Text("房间id") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Button(
                    onClick = {
                        viewModel.queryRoom(roomIdInput)
                    }
                ) {
                    Text("查询房间")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        viewModel.createRoom(roomNameInput, initSongIdList, false)
                    }
                ) {
                    Text("创建房间")
                }

                Button(
                    onClick = {
                        viewModel.closeRoom()
                    }
                ) {
                    Text("关闭房间")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        viewModel.joinRoom()
                    }
                ) {
                    Text("加入房间")
                }

                Button(
                    onClick = {
                        viewModel.joinRoom(true)
                    }
                ) {
                    Text("强制加入房间")
                }

            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        viewModel.getRoomSongList()
                    }
                ) {
                    Text("获取房间已点歌曲")
                }
                Button(onClick = {
                    showInputDialog = true
                }) {
                    Text("添加歌曲至歌单")
                }
            }
        }



        item {
            Text("已点歌曲数：${viewModel.roomSongList.size}首")
            Divider()
        }

        items(viewModel.roomSongList) {
            RoomSongInfo(it, viewModel)
        }
        if (showInputDialog) {
        }
    }

    if (showInputDialog) {
        val om = mapOf(
            "随机添加歌曲" to OptionType.SELECTOR,
            "随机批量添加歌曲" to OptionType.SELECTOR,
            "添加指定歌曲ids" to OptionType.EDITOR
        )
        ShowCheckDialog(
            options = om,
            onOptionsSelected = { resList ->
                val selected = resList[0]
                val songIds = resList[1]
                when (selected) {
                    "随机添加歌曲" -> viewModel.addRandomSong()
                    "随机批量添加歌曲" -> viewModel.addRandomSong(true)
                    "添加指定歌曲ids" -> {
                        viewModel.addSongList(songIds.split(",").mapNotNull {
                            it.toLongOrNull()
                        }) {
                        }
                    }
                }
                showInputDialog = false
            },
            onDismissRequest = { showInputDialog = false }
        )
    }

    if (viewModel.needUserConfirm) {
        RoomActionDialog(
            onDismissRequest = {
                viewModel.needUserConfirm = false
            },
            onRebuildRoom = {
                viewModel.createRoom(roomNameInput, initSongIdList, true)
            },
            onJoinRoom = {
                viewModel.joinRoom()
            }
        )
    }
}

@Composable
fun RoomActionDialog(
    onDismissRequest: () -> Unit,
    onRebuildRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "房间操作选择",
            )
        },
        text = {
            Text("请选择您想要执行的操作：强制重建房间或加入现有房间")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRebuildRoom()
                    onDismissRequest()
                }
            ) {
                Text("强制重建房间")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onJoinRoom()
                    onDismissRequest()
                }
            ) {
                Text("加入房间")
            }
        },
        modifier = modifier
    )
}

@Composable
fun RoomInfoPage(viewModel: OrderSongViewModel) {
    val clipboard = LocalClipboardManager.current
    Column {
        val roomInfo = viewModel.roomInfo
        CopyableText("房间ID", "${roomInfo?.roomId}")
        CopyableText("房间名","${roomInfo?.name}")
        CopyableText("创建者","${roomInfo?.creator?.name}")
        roomInfo?.members?.forEach {
            RoomMember(it, "房间成员")
        }
        CopyableText("创建时间", UiUtils.timestampToTime(roomInfo?.createTime ?: 0))
        if (roomInfo != null) {
            Row(
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth()
            ) {
                Image(
                    painter = rememberImagePainter(roomInfo.roomPic ?: ""),
                    "",
                )
                roomInfo.roomUrl?.let { originalUrl ->
                    val url = if (viewModel.enableTDE) {
                        try {
                            URI(originalUrl).let { uri ->
                                URI(
                                    "https",
                                    "fastest.y.qq.com",
                                    uri.path,
                                    "_tde_id=49993&_tde_token=608f82ce-88b2-45d9-b213-173ed6077a28&${uri.query}",
                                    uri.fragment
                                ).toString()
                            }
                        } catch (e: Exception) {
                            originalUrl // 如果URL解析失败，返回原值
                        }
                    } else {
                        originalUrl
                    }
                    val bitmap = UiUtils.generateQRCode(url)?.asImageBitmap()
                    if (bitmap != null) {
                        var showUrlDialog by remember { mutableStateOf(false) } // 新增状态控制
                        Box(modifier = Modifier.clickable { showUrlDialog = true }) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "房间二维码",
                                modifier = Modifier.size(120.dp)
                            )
                        }

                        // 点击后显示URL的对话框
                        if (showUrlDialog) {
                            AlertDialog(
                                onDismissRequest = { showUrlDialog = false },
                                title = { Text("房间URL") },
                                text = {
                                    Column {
                                        Text(url)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = {
                                            clipboard.setText(AnnotatedString(url))
                                        }){
                                            Text("复制")
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showUrlDialog = false }) {
                                        Text("关闭")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoomSongInfo(roomSong: RoomSongInfo, viewModel: OrderSongViewModel) {
    Column {
        RoomMember(roomSong.memberInfo, "点歌人")
        roomSong?.song?.let {
            OrderSongInfo(PlayListParams(viewModel.getPlaySongList(), it), roomSong, viewModel)
        }
        Divider()
    }
}

@Composable
fun RoomMember(memberInfo: RoomMemberInfo?, text: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        CopyableText(text,"${memberInfo?.name}")
        Image(painter = rememberImagePainter(memberInfo?.pic ?: ""), "")
    }
}

@Composable
fun OrderSongInfo(params: PlayListParams, roomSong: RoomSongInfo, viewModel: OrderSongViewModel) {
    val activity = LocalContext.current as Activity
    val currentSong = PlayerObserver.currentSong
    val coroutineScope = rememberCoroutineScope()

    val collectState = remember {
        mutableStateOf(params.startSong?.hot == 1)
    }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .height(70.dp)
            .clickable {
                if (params.displayOnly.not()) {
                    val result = if (params.playCachedOnly) {
                        val startPos = params.startSong?.let { params.songList.indexOf(it) } ?: -1
                        OpenApiSDK.getPlayerApi().playCachedSongs(params.songList, startPos)
                    } else {
                        val startPos = params.startSong?.let { params.songList.indexOf(it) } ?: -1
                        val playMode = OpenApiSDK.getPlayerApi().getPlayMode()
                        OpenApiSDK
                            .getPlayerApi()
                            .playSongs(
                                PlayParam(
                                    params.playListType,
                                    params.playListTypeId,
                                    params.songList,
                                    startPos,
                                    playMode,
                                    startPlay = true
                                )
                            )
                    }
                    if (result == 0) {
                        PerformanceHelper.monitorClick("SongItemUI_PlayerActivity")
                        UiUtils.gotoPlayerPage()
                        coroutineScope.launch(Dispatchers.IO) {
                            val resType = when (params.playListType) {
                                MusicPlayList.PLAY_LIST_ALBUM_TYPE -> 3
                                MusicPlayList.PLAY_LIST_FOLDER_TYPE -> 4
                                else -> return@launch
                            }
                            OpenApiSDK.getOpenApi()
                                .reportRecentPlay("${params.playListTypeId}", resType) { resp ->
                                    Log.i("itemUI", "reportRecentPlay resp: $resp")
                                }
                        }
                    } else {
                        coroutineScope.launch(Dispatchers.Main) {
                            UiUtils.showPlayErrToast(result,params.startSong)
                        }
                    }
                }
            }
    ) {
        val (cover, songInfo, next, playingIcon, collect, mv) = createRefs()

        Image(
            painter = rememberImagePainter(params.startSong?.smallCoverUrl()),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .padding(2.dp)
                .constrainAs(cover) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(songInfo.start)
                }
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .constrainAs(songInfo) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(cover.end)
                }, horizontalAlignment = Alignment.Start
        ) {
            val txtColor = if (params.startSong?.canPlay() == true) {
                Color.Black
            } else {
                Color.Gray
            }
            Text(text = params.startSong?.songName ?: "", color = txtColor)
            Text(
                text = params.startSong?.singerName ?: "未知",
                color = txtColor
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (params.startSong?.vip == 1) {
                    Image(
                        painter = painterResource(R.drawable.pay_icon_in_cell_old),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .width(18.dp)
                            .height(10.dp)
                    )
                }
                if (params.startSong?.longAudioVip == 1) {
                    Image(
                        painter = painterResource(R.drawable.ic_long_audio_vip_new),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .width(18.dp)
                            .height(10.dp)
                    )
                }
                if (params.startSong?.hasQualityHQ() == true) {
                    Image(
                        painter = painterResource(R.drawable.hq_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .width(18.dp)
                            .height(10.dp)
                    )
                }
                if (params.startSong?.isFreeLimit() == true) {
                    Image(
                        painter = painterResource(R.drawable.free_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .width(18.dp)
                            .height(10.dp)
                    )

                } else if (params.startSong != null && OpenApiSDK.getPlayerApi()
                        .getSongHasQuality(params.startSong, Quality.WANOS)
                ) {
                    Image(
                        painter = painterResource(R.drawable.acion_icon_quality_wanos),
                        contentDescription = null,
                        modifier = Modifier
                            .width(18.dp)
                            .height(10.dp)
                    )
                } else if (params.startSong != null && OpenApiSDK.getPlayerApi()
                        .getSongHasQuality(params.startSong, Quality.VINYL)
                ) {
                    Image(
                        painter = painterResource(R.drawable.action_icon_quality_vinyl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(18.dp)
                            .height(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = params.startSong?.extraInfo?.mood ?: "",
                    fontSize = 8.sp,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(2.dp)
                        .wrapContentWidth()
                        .height(10.dp)
                )
            }
        }

        if (currentSong?.songId == params.startSong?.songId) {
            Image(
                painter = painterResource(R.drawable.list_icon_playing),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .width(30.dp)
                    .height(30.dp)
                    .constrainAs(playingIcon) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(next.start)
                    }
            )
        }

        Image(
            painter = painterResource(
                if (collectState.value)
                    R.drawable.icon_collect
                else
                    R.drawable.icon_uncollect
            ),
            contentDescription = null,
            modifier = Modifier
                .constrainAs(collect) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    if (currentSong?.songId == params.startSong?.songId) {
                        end.linkTo(playingIcon.start)
                    } else if (params.displayOnly.not()) {
                        end.linkTo(next.start)
                    } else {
                        end.linkTo(parent.end)
                    }
                }
                .width(40.dp)
                .height(40.dp)
                .padding(10.dp)
        )

        if (params.displayOnly.not()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .constrainAs(next) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    }) {
                TextButton(
                    modifier = Modifier.height(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        viewModel.moveToTop(roomSong)
                    }) {
                    Text(text = "置顶", fontSize = 10.sp)
                }

                TextButton(
                    modifier = Modifier.height(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        params.startSong ?: return@TextButton
                        viewModel.deleteSong(roomSong)
                    }
                ) {
                    Text(text = "删除", fontSize = 10.sp)
                }

                TextButton(
                    modifier = Modifier.height(20.dp),
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        val intent = Intent(activity, SongProfileActivity::class.java)
                        intent.putExtra(SongProfileActivity.KEY_SONG, params.startSong)
                        activity.startActivity(intent)
                    }
                ) {
                    Text(text = "详情", fontSize = 10.sp)
                }
            }
        }
    }
}

enum class OptionType {
    SELECTOR,
    EDITOR
}

@Composable
fun ShowCheckDialog(
    options: Map<String, OptionType>,
    onOptionsSelected: (List<String>) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedOption by remember { mutableStateOf("") }
    var perInputStr by remember { mutableStateOf("") }
    AlertDialog(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 关闭按钮
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = "Close",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismissRequest() }
                )
            }
        },
        onDismissRequest = onDismissRequest,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                options.forEach { (option, optionType) ->
                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .clickable { selectedOption = option }
                    ) {
                        RadioButton(
                            modifier = Modifier.wrapContentSize(),
                            selected = selectedOption == option,
                            onClick = { selectedOption = option }
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.body1.merge(),
                            modifier = Modifier
                                .wrapContentSize()
                                .align(Alignment.CenterVertically),
                        )
                    }
                    Divider()
                }
                if (options.get(selectedOption) == OptionType.EDITOR) {
                    TextField(
                        value = perInputStr,
                        onValueChange = { perInputStr = it },
                        label = { Text("请输入") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )

                }
            }

        },
        confirmButton = {
            TextButton(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp),
                onClick = {
                    if (selectedOption.isNotEmpty()) {
                        onOptionsSelected(listOf(selectedOption, perInputStr))
                    }
                }
            ) {
                Text("确认")
            }
        }
    )
}