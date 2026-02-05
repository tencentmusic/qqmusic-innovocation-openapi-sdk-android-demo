package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.qplayer.ui.activity.LoadMoreItem
import com.tencent.qqmusic.qplayer.ui.activity.loadMoreItemUI
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BuyAlbumScreen(albums: List<Album>) {
    Scaffold(
        topBar = { TopBar() },
        modifier = Modifier.semantics{ testTagsAsResourceId=true }
    ) {
        AlbumListPage(albums = albums)
    }
}

@OptIn(ExperimentalCoilApi::class, ExperimentalFoundationApi::class)
@Composable
fun BuyAlbumPage(albums: List<Album>, loadMoreItem: LoadMoreItem? = null) {
    val activity = LocalContext.current as Activity
    val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(albums.size) { index ->
            val album = albums.getOrNull(index) ?: return@items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .combinedClickable(
                        onClick = {
                            activity.startActivity(
                                Intent(activity, SongListActivity::class.java)
                                    .putExtra(SongListActivity.KEY_ALBUM_ID, album.id)
                            )
                        },
                        onLongClick = {
                            // 复制文件夹名称到剪贴板
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText(
                                    "AlbumId",
                                    album.id
                                )
                            )
                            Toast.makeText(activity, "专辑Id已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }
                    )
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
                    Text(text = album.singers?.get(0)?.name?:"")
                    Text(text = "已购${album.songNum?: 0}张")

                }
            }
        }
        loadMoreItemUI(albums.size, loadMoreItem)
    }
}