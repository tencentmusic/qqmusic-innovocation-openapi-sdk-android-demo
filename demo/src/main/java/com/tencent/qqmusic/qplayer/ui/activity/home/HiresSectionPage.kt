package com.tencent.qqmusic.qplayer.ui.activity.home

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.tencent.qqmusic.openapisdk.model.*
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.AlbumActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity

private const val TAG = "HiresSectionPage"

@Composable
fun HiresSectionPage(viewModel: HomeViewModel) {
    val activity = LocalContext.current as Activity
    var hiresShelves: List<AreaShelf> by remember {
        mutableStateOf(emptyList<AreaShelf>())
    }
    var hiresAreaTitle: String = ""
    var hiresAreaDesc: String = ""
    var hiresAreaCover: String = ""

    viewModel.fetchHiresSection(callback = {
        if (it!=null) {
            hiresAreaTitle = it.title
            hiresAreaDesc = it.desc
            hiresAreaCover = it.cover
            hiresShelves = it.shelves
        }
    })

    LazyColumn {
        items(hiresShelves.count()) { it ->
            val shelf: AreaShelf = hiresShelves.getOrNull(it) ?: return@items
            if (shelf.shelfItems.isEmpty()) return@items;
            val shelfItems: List<AreaShelfItem> = shelf.shelfItems

            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = shelf.shelfTitle,
                    color = when (shelf.shelfType) {
                        AreaShelfType.AreaShelfType_Song-> Color.Red
                        AreaShelfType.AreaShelfType_Folder-> Color.Yellow
                        AreaShelfType.AreaShelfType_Album-> Color.Blue
                        else -> Color.Gray
                    },
                    fontSize = 18.sp
                )
            }
            FlowRow {
                repeat(shelfItems.size) {
                    val item = shelfItems.getOrNull(it) ?: return@repeat
                    var title: String = ""
                    when (shelf.shelfType) {
                        AreaShelfType.AreaShelfType_Song-> {
                            title = item.songInfo?.songName.toString()
                            val songId: Long = item.songInfo?.songId ?: 0
                            Box(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        activity.startActivity(Intent(activity, SongListActivity::class.java)
                                            .putExtra(SongListActivity.KEY_SONG, songId))
                                        viewModel.fetchShelfContent(AreaId.AreaHires,
                                            shelf.shelfId,
                                            5,
                                            item.songInfo?.songId.toString(),
                                            callback = {
                                            // Do nothing, just test
                                        })
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        AreaShelfType.AreaShelfType_Folder-> {
                            title = item.folder?.name.toString()
                            val folderId: String = item.folder?.id.toString()
                            Box(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        activity.startActivity(Intent(activity, FolderActivity::class.java)
                                            .putExtra(FolderActivity.KEY_FOLDER_ID, folderId))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        AreaShelfType.AreaShelfType_Album-> {
                            title = item.album?.name.toString()
                            val albumId: String = item.album?.id.toString()
                            Box(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        activity.startActivity(Intent(activity, AlbumActivity::class.java)
                                            .putExtra(AlbumActivity.KEY_ALBUM_ID, albumId))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}