package com.tencent.qqmusic.qplayer.ui.activity.area

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.model.AreaShelfItem
import com.tencent.qqmusic.openapisdk.model.AreaShelfType
import com.tencent.qqmusic.qplayer.ui.activity.home.PodcastItem
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import kotlinx.coroutines.flow.Flow

/**
 * Created by tannyli on 2023/10/24.
 * Copyright (c) 2023 TME. All rights reserved.
 */
class AreaListActivity: ComponentActivity() {

    private val areaId by lazy {
        intent.getIntExtra("areaId", 0)
    }

    private val areaShelfType by lazy {
        intent.getIntExtra("areaShelfType", 0)
    }

    private val shelfId by lazy {
        intent.getIntExtra("shelfId", 0)
    }

    private val title by lazy {
        intent.getStringExtra("title") ?: ""
    }

   companion object {

       private const val TAG = "AreaListActivity"
       @JvmStatic
       fun start(context: Context, areaId: Int, areaShelfType: Int, shelfId: Int, title: String) {
           val starter = Intent(context, AreaListActivity::class.java)
               .putExtra("areaId", areaId)
               .putExtra("areaShelfType", areaShelfType)
               .putExtra("shelfId", shelfId)
               .putExtra("title", title)
           context.startActivity(starter)
       }
   }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Scaffold(topBar = { TopBar(title)}) {
                val vm: AreaListViewModel = viewModel()
                val flow = vm.areaListPageDetail(areaId, shelfId)
                Box(modifier = Modifier.fillMaxSize()) {
                    AreaListScreen(vm = vm, flow)
                }
            }
        }

    }

    @Composable
    fun AreaListScreen(vm: AreaListViewModel, flow: Flow<PagingData<AreaShelfItem>>? = null) {
        val albums = flow!!.collectAsLazyPagingItems()
        val activity = LocalContext.current as Activity

        Log.i(TAG, "AreaListScreen:areaId:$areaId, shelfId:$shelfId")

        LazyColumn(state = rememberLazyListState(),  modifier = Modifier.fillMaxSize()) {
            this.items(albums) { shelf ->
                if (areaShelfType == AreaShelfType.AreaShelfType_Album) {
                    val album = shelf?.album ?: return@items
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                activity.startActivity(
                                    Intent(activity, SongListActivity::class.java)
                                        .putExtra(SongListActivity.KEY_ALBUM_ID, album.id)
                                )
                            }
                    ) {
                        Image(
                            painter = rememberImagePainter(album.pic),
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .padding(2.dp)
                        )
                        Column {
                            Text(text = album.name)
                            Text(text = "${album.songNum?.toString() ?: 0}首")
                        }
                    }
                } else if (areaShelfType == AreaShelfType.AreaShelfType_Folder) {
                    val folder = shelf?.folder ?: return@items
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                activity.startActivity(
                                    Intent(activity, SongListActivity::class.java)
                                        .putExtra(SongListActivity.KEY_FOLDER_ID, folder.id)
                                )
                            }
                    ) {
                        Image(
                            painter = rememberImagePainter(folder.picUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .padding(2.dp)
                        )
                        Column {
                            Text(text = folder.name)
                            Text(text = "${folder.songNum?.toString() ?: 0}首")
                        }
                    }
                } else if (areaShelfType == AreaShelfType.AreaShelfType_Song) {
                    val song = shelf?.songInfo ?: return@items
                    val title = song.songName.toString()
                    val songId: Long = song.songId ?: 0
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(16.dp)
                            .clickable {
                                activity.startActivity(
                                    Intent(activity, SongListActivity::class.java)
                                        .putExtra(SongListActivity.KEY_SONG, songId)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column {
                            if (song.isLongAudioSong()) {
                                PodcastItem(song = song)
                            } else {
                                Text(text = title, fontSize = 16.sp)
                            }

                            Text(text = "Vip ：${if (song.vip == 1) "VIP" else "普通"}")
                        }
                    }
                }

            }
        }
    }
}