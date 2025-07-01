package com.tencent.qqmusic.qplayer.ui.activity.main

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.rememberImagePainter
import com.google.accompanist.flowlayout.FlowRow
import com.tencent.qqmusic.openapisdk.model.*
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.area.AreaListActivity
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderListActivity
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity

private const val TAG = "DolbySectionPage"

@Composable
fun AreaSectionDetailPage(areaId: Int, viewModel: HomeViewModel) {
    val activity = LocalContext.current as Activity
    var areaShelves: List<AreaShelf>? by remember {
        mutableStateOf(emptyList<AreaShelf>())
    }
    var areaAreaTitle: String = ""
    var areaAreaDesc: String = ""
    var areaAreaCover: String = ""
    val callback: (Area?) -> Unit = {
        if (it != null) {
            areaAreaTitle = it.title
            areaAreaDesc = it.desc
            areaAreaCover = it.cover
            areaShelves = it.shelves
        }
    }

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

    if (viewModel.showDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.showDialog = false
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
                        viewModel.openFreeLimitedTimeAuth() {
                            viewModel.showDialog = !it.isSuccess()
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
                        viewModel.showDialog = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    LazyColumn {
        items(areaShelves?.count() ?: 0) { it ->
            val shelf: AreaShelf = areaShelves?.getOrNull(it) ?: return@items
            if (shelf.shelfItems.isEmpty()) return@items;
            val shelfItems: List<AreaShelfItem> = shelf.shelfItems

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
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
                                        Text(text = title, fontSize = 16.sp)
                                    }

                                    Text(text = "Vip ：${if (item.songInfo?.vip == 1) "VIP" else "普通"}")
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