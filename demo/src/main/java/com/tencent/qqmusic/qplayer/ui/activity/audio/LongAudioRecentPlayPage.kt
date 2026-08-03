package com.tencent.qqmusic.qplayer.ui.activity.audio

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.model.LongAudioItem
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.songlist.CommonProfileActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoilApi::class)
@Composable
fun LongAudioRecentPlayListPage(items: List<LongAudioItem>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(1.dp)) {
        items(items.size) { index ->
            val item = items.getOrNull(index) ?: return@items
            LongAudioRecentPlayItem(item)
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun LongAudioRecentPlayItem(item: LongAudioItem) {
    val activity = LocalContext.current as Activity
    val albumId = item.albumId?.takeIf { it > 0 }?.toString()
    val recentPlayTime = formatRecentPlayTime(item.lastTime)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !albumId.isNullOrEmpty()) {
                activity.startActivity(
                    Intent(activity, CommonProfileActivity::class.java)
                        .putExtra(SongListActivity.KEY_ALBUM_ID, albumId)
                )
            }
            .padding(4.dp)
    ) {
        Box(modifier = Modifier.size(60.dp)) {
            Image(
                painter = rememberImagePainter(item.cover),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = item.title.orEmpty(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.subTitle?.takeIf { it.isNotEmpty() }?.let {
                Text(text = it, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = item.singer?.joinToString("/") ?: "",
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.long_audio_recent_album,
                    item.albumTitle.orEmpty(),
                    item.albumId?.toString().orEmpty()
                ),
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (recentPlayTime.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.long_audio_recent_time,
                        recentPlayTime
                    ),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun formatRecentPlayTime(lastTime: Long?): String {
    if (lastTime == null || lastTime <= 0) {
        return ""
    }
    val timestamp = if (lastTime < 1_000_000_000_000L) lastTime * 1000 else lastTime
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
