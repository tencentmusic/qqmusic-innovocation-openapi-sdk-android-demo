package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar

@Composable
fun AlbumScreen(albums: List<Album>) {
    Scaffold(
        topBar = { TopBar() }
    ) {
        AlbumPage(albums = albums)
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun AlbumPage(albums: List<Album>) {
    val activity = LocalContext.current as Activity

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(albums.size) { index ->
            val album = albums.getOrNull(index) ?: return@items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        activity.startActivity(Intent(activity, SongListActivity::class.java)
                            .putExtra(SongListActivity.KEY_ALBUM_ID, album.id))
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
                            }
                            else {
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
    }
}