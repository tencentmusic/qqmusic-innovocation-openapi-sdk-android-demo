package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.LoadMoreItem
import com.tencent.qqmusic.qplayer.ui.activity.loadMoreItemUI
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.utils.UiUtils.getFormatNumber

@Composable
fun AlbumScreen(albums: List<Album>) {
    Scaffold(
        topBar = { TopBar("专辑列表") }
    ) {
        AlbumListPage(albums = albums)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalCoilApi::class)
@Composable
fun albumItemUI(album: Album) {
    val activity = LocalContext.current as Activity
    val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .combinedClickable(
                onClick = {
                    activity.startActivity(
                        Intent(activity, CommonProfileActivity::class.java)
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
        val picUrl = album.pic.takeUnless { it.isNullOrEmpty() } ?: album.albumMediumUrl
        Box(
            modifier = Modifier
                .size(60.dp)
        ) {
            Image(
                painter = rememberImagePainter(picUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = "播放量:${getFormatNumber(album.listenNum)}",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 8.sp
            )
        }

        Column {
            Text(text = album.name.let { if(it.length>15) it.substring(0..15) + "..." else it},
                fontSize = 14.sp, fontWeight = FontWeight.Bold)
            album.subName?.let {
                Text(text = if(it.length>15) it.substring(0..15) + "..." else it, fontSize = 12.sp)
            }
            Text(text = "${album.singerName}", fontSize = 12.sp, color = Color.Gray)
        }
        Image(
            painter = painterResource(
                if (album.favState == 1)
                    R.drawable.icon_collect
                else
                    R.drawable.icon_uncollect
            ),
            contentDescription = null,
            modifier = Modifier
                .width(40.dp)
                .height(40.dp)
                .padding(10.dp)
                .clickable {
                    if (album.favState == 1) {
                        OpenApiSDK.getOpenApi().collectAlbum(false, listOf(album.id)) {
                            if (it.isSuccess()) {
                                ToastUtils.showShort("取消收藏成功 需要刷新页面")
                            } else {
                                ToastUtils.showShort("取消收藏失败：${it.errorMsg}")
                            }
                        }
                    } else {
                        OpenApiSDK.getOpenApi().collectAlbum(true, listOf(album.id)) {
                            if (it.isSuccess()) {
                                ToastUtils.showShort("收藏成功 需要刷新页面")
                            } else {
                                ToastUtils.showShort("收藏失败：${it.errorMsg}")
                            }
                        }
                    }
                }
        )
    }
}

@Composable
fun AlbumListPage(albums: List<Album>, loadMoreItem: LoadMoreItem? = null) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(1.dp)) {
        items(albums.size) { index ->
            val album = albums.getOrNull(index) ?: return@items
            albumItemUI(album)
        }
        loadMoreItemUI(albums.size, loadMoreItem = loadMoreItem)
    }
}