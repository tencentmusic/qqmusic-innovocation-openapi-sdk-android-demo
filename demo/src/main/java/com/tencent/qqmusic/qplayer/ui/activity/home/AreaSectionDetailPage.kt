package com.tencent.qqmusic.qplayer.ui.activity.home

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import com.google.accompanist.flowlayout.FlowRow
import com.tencent.qqmusic.openapisdk.model.Area
import com.tencent.qqmusic.openapisdk.model.AreaId
import com.tencent.qqmusic.openapisdk.model.AreaShelf
import com.tencent.qqmusic.openapisdk.model.AreaShelfItem
import com.tencent.qqmusic.openapisdk.model.AreaShelfType
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.area.AreaListActivity
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderListActivity
import com.tencent.qqmusic.qplayer.ui.activity.login.WebViewActivity
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.utils.UiUtils

private const val TAG = "AreaSectionDetailPage"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AreaSectionDetailPageWithTitle(title:String, areaId: Int, viewModel: HomeViewModel){
    Scaffold(topBar = { TopBar(title)},
        modifier = Modifier.semantics{ testTagsAsResourceId=true }) {
        AreaSectionDetailPage(areaId, viewModel)
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun AreaSectionDetailPage(areaId: Int, viewModel: HomeViewModel) {
    val activity = LocalContext.current as Activity
    var areaShelves: List<AreaShelf>? by remember {
        mutableStateOf(emptyList<AreaShelf>())
    }
    var areaAreaTitle = ""
    var areaAreaDesc = ""
    var areaAreaCover = ""
    val callback: (Area?, String?) -> Unit = { area, msg->
        if (area != null) {
            viewModel.showRefreshButton = false
            areaAreaTitle = area.title
            areaAreaDesc = area.desc
            areaAreaCover = area.cover
            areaShelves = area.shelves
        }else{
            msg?.let{
                UiUtils.showToast(it)
                viewModel.showRefreshButton = true
            }
        }
    }

    updateAreaData(areaId,viewModel,callback)

    if (viewModel.showWanosDialog && areaId == AreaId.Wanos) {
        AlertDialog(
            onDismissRequest = {
                viewModel.showWanosDialog = false
            },
            title = {
                Text(text = "恭喜你！")
            },
            text = {
                Text(
                    "棒！获取WANOS试听权益！"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.openFreeLimitedTimeAuthWanos() {
                            viewModel.showWanosDialog = !it.isSuccess()
                            Toast.makeText(activity, "WANOS试听权益领取成功！", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("我要领取")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.showWanosDialog = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    RefreshButton(viewModel) {
        updateAreaData(areaId,viewModel,callback)
    }

    LazyColumn {
        items(areaShelves?.count() ?: 0) { it ->
            val shelf: AreaShelf = areaShelves?.getOrNull(it) ?: return@items
            if (shelf.shelfItems.isEmpty()) return@items;
            val shelfItems: List<AreaShelfItem> = shelf.shelfItems
            if(it==0){ // 封面
                Box(modifier = Modifier.height(100.dp)){
                    Image(
                        painter = rememberImagePainter(
                            data = areaAreaCover,
                            builder = {
                                transformations(RoundedCornersTransformation())
                            }
                        ),
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clickable {
                                areaAreaCover.let {
                                    WebViewActivity.start(activity, it)
                                }
                            }
                    )
                    Text(
                        text = areaAreaTitle,
                        modifier = Modifier
                            .wrapContentSize()
                            .background(color = Color.White.copy(alpha = 0.7f))
                            .align(Alignment.TopStart),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold)
                    Text(
                        text = areaAreaDesc,
                        modifier = Modifier
                            .wrapContentSize()
                            .background(color = Color.White.copy(alpha = 0.5f))
                            .align(Alignment.BottomStart))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text( // 运营标题
                    text = shelf.shelfTitle,
                    color = when (shelf.shelfType) {
                        AreaShelfType.AreaShelfType_Song -> Color.Red
                        AreaShelfType.AreaShelfType_Folder -> Color.Yellow
                        AreaShelfType.AreaShelfType_Album -> Color.Blue
                        else -> Color.Gray
                    },
                    fontSize = 18.sp
                )

                Text(
                    text = when (shelf.shelfType) {
                        AreaShelfType.AreaShelfType_Song -> "更多歌曲"
                        AreaShelfType.AreaShelfType_Folder -> "更多歌单"
                        AreaShelfType.AreaShelfType_Album -> "更多专辑"
                        else -> ""
                    },
                    color = Color.Gray,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .wrapContentWidth()
                        .clickable {
                            AreaListActivity.start(
                                activity,
                                areaId,
                                shelf.shelfType,
                                shelf.shelfId,
                                shelf.shelfTitle
                            )
                        }
                )

            }
            FlowRow {
                repeat(minOf(6, shelfItems.size)) {
                    val item = shelfItems.getOrNull(it) ?: return@repeat
                    var title: String = ""
                    when (shelf.shelfType) {
                        AreaShelfType.AreaShelfType_Song -> {
                            title = item.songInfo?.songName.toString()
                            val songId: Long = item.songInfo?.songId ?: 0
                            Box(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        activity.startActivity(
                                            Intent(
                                                activity,
                                                SongListActivity::class.java
                                            ).putExtra(SongListActivity.KEY_SONG, songId)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column {
                                    if (item.songInfo?.isLongAudioSong() == true) {
                                        PodcastItem(song = item.songInfo)
                                    } else {
                                        Text(text = title, fontSize = 16.sp, color =if (item.songInfo?.canPlay()==true) Color.Black else Color.Gray)
                                    }

                                    Text(text = "Vip ：${if (item.songInfo?.vip == 1) "VIP" else "普通"}", color =if (item.songInfo?.canPlay()==true) Color.Black else Color.Gray)
                                }
                            }
                        }

                        AreaShelfType.AreaShelfType_Folder -> {
                            title = item.folder?.name.toString()
                            val folderId: String = item.folder?.id.toString()
                            Box(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        activity.startActivity(
                                            Intent(
                                                activity,
                                                FolderListActivity::class.java
                                            ).putExtra(FolderListActivity.KEY_FOLDER_ID, folderId)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column {
                                    Text(
                                        text = title,
                                        fontSize = 16.sp
                                    )
                                    Text(text = "歌曲数量 ：${item.folder?.songNum.toString()}")
                                }
                            }
                        }

                        AreaShelfType.AreaShelfType_Album -> {
                            title = item.album?.name.toString()
                            val albumId: String = item.album?.id.toString()
                            Box(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        activity.startActivity(
                                            Intent(
                                                activity,
                                                AlbumActivity::class.java
                                            ).putExtra(AlbumActivity.KEY_ALBUM_ID, albumId)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column {
                                    Text(
                                        text = title,
                                        fontSize = 16.sp
                                    )
                                    Text(text = "歌曲数量 ：${item.album?.songNum.toString()}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodcastItem(song: SongInfo?) {
    if (song == null) {
        return
    }
    ConstraintLayout(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
        val (cover, songInfo, collect) = createRefs()
        Image(
            painter = rememberImagePainter(song.smallCoverUrl()),
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
                    end.linkTo(parent.end)
                }, horizontalAlignment = Alignment.Start
        ) {
            val txtColor = if (song.canPlay()) {
                Color.Black
            } else {
                Color.Gray
            }
            Text(text = song.songName, color = txtColor, modifier = Modifier.fillMaxWidth())
            Text(
                text = (song.singerName ?: "未知") + ", ${song.updateTime}" + ", ${song.listenCount}",
                color = txtColor
            )
            Row {
                if (song.vip == 1) {
                    Image(
                        painter = painterResource(R.drawable.pay_icon_in_cell_old),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .width(18.dp)
                            .height(10.dp)
                    )
                }
                if (song.longAudioVip == 1) {
                    Image(
                        painter = painterResource(R.drawable.ic_long_audio_vip_new),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .width(18.dp)
                            .height(10.dp)
                    )
                }
            }
        }

    }
}

private fun updateAreaData(areaId:@AreaId Int, viewModel: HomeViewModel, callback: (Area?, String?) -> Unit) {
    when (areaId) {
        AreaId.AreaDolby -> {
            viewModel.fetchDolbySection(callback)
        }

        AreaId.AreaHires -> {
            viewModel.fetchHiresSection(callback)
        }

        AreaId.AreaGalaxy -> {
            viewModel.fetchGalaxySection(callback)
        }

        AreaId.Vinly -> {
            viewModel.fetchVinylSection(callback)
        }

        AreaId.Master -> {
            viewModel.fetchMasterSection(callback)
        }

        else -> {
            viewModel.fetchSection(areaId, callback)
        }
    }
}