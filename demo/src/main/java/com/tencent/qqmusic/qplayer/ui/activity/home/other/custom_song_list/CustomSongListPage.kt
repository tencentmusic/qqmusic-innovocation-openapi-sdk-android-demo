package com.tencent.qqmusic.qplayer.ui.activity.home.other.custom_song_list

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.google.accompanist.flowlayout.FlowRow
import com.tencent.qqmusic.qplayer.ui.activity.main.CopyableText
import com.tencent.qqmusic.qplayer.ui.activity.songlist.CommonProfileActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity

/**
 * Author: hevinzhou
 * Created: 2025/11/22
 * Description:
 */

@Composable
fun CustomSongListPage() {
    val viewModel: CustomSongListModel = viewModel()
    LaunchedEffect(Unit) {
        viewModel.getSongListSquare()
    }
    val activity = LocalContext.current as Activity
    val squares by remember { viewModel.squares }
    LazyColumn{
        item {
            FlowRow {
                squares.forEach { songListSquare ->
                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .clickable {
                                if (songListSquare.type==2){
                                    activity.startActivity(
                                        Intent(activity, CommonProfileActivity::class.java).apply {
                                            // 歌单
                                            putExtra(SongListActivity.KEY_FOLDER_ID, songListSquare.itemId)
                                            putExtra(SongListActivity.KEY_IS_MY_LIKE_FOLDER, false)
                                        }
                                    )
                                }else{ // 榜单
                                    activity.startActivity(
                                        Intent(activity, SongListActivity::class.java)
                                            .putExtra(SongListActivity.KEY_RANK_ID,
                                                songListSquare.itemId?.toIntOrNull())
                                    )
                                }

                            }
                    ) {
                        Box(modifier = Modifier
                            .wrapContentSize()
                            .padding(10.dp)
                        ) {
                            Image(
                                painter = rememberImagePainter(songListSquare.pic),
                                contentDescription = null,
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(150.dp)
                                    .padding(2.dp),
                            )
                            Column(modifier = Modifier.background(Color.White.copy(alpha = 0.7f))) {
                                CopyableText(title = "title", content = songListSquare.title?:"null")
                                CopyableText(title = "subtitle", content = songListSquare.subtitle?:"")
                                CopyableText(title = "itemId", content = songListSquare.itemId.toString())
                                CopyableText(title = "type", content = if(songListSquare.type==1) "榜单" else "歌单")
                                CopyableText(title = "scene", content = songListSquare.scene.toString())
                            }
                        }
                    }
                }
            }
        }
    }

}