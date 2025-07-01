package com.tencent.qqmusic.qplayer.ui.activity.home

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.musichall.MusicHallActivity

/**
 * Create by tinguo on 2025/4/22
 * CopyWrite (c) 2025 TME. All rights reserved.
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicHallEntrancePage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val gotoMusicHall: (type: Int) -> Unit = { type->
        val intent = Intent(context, MusicHallActivity::class.java)
        intent.putExtra(MusicHallActivity.KEY_TYPE, type)
        context.startActivity(intent)
    }

    val state = rememberLazyGridState()
    LazyVerticalGrid(columns = GridCells.Fixed(4) ,
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        state = state,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            MusicHallEntranceItem(cover = R.drawable.musichall_singer, title = "歌手", modifier = Modifier.combinedClickable(
                onClick = {
                    gotoMusicHall(MusicHallActivity.TYPE_SINGER)
                }
            ))
        }
        item {
            MusicHallEntranceItem(cover = R.drawable.musichall_singer, title = "排行榜", modifier = Modifier.combinedClickable(
                onClick = {
                    gotoMusicHall(MusicHallActivity.TYPE_TOPLIST)
                }
            ))
        }
        item {
            MusicHallEntranceItem(cover = R.drawable.musichall_singer, title = "歌单", modifier = Modifier.combinedClickable(
                onClick = {
                    gotoMusicHall(MusicHallActivity.TYPE_SONGLIST)
                }
            ))
        }
        item {
            MusicHallEntranceItem(cover = R.drawable.musichall_singer, title = "专辑", modifier = Modifier.combinedClickable(
                onClick = {
                    gotoMusicHall(MusicHallActivity.TYPE_ALBUM)
                }
            ))
        }
    }
}

@Composable
fun MusicHallEntranceItem(cover: Int, title: String, modifier: Modifier = Modifier) {

    Box(modifier = modifier.width(64.dp).wrapContentHeight()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(cover), contentDescription = title, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, style = MaterialTheme.typography.body1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }

}

