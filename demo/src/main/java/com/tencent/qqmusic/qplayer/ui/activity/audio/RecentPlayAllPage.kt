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
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.model.LongAudioItem
import com.tencent.qqmusic.openapisdk.model.RecentPlayAllItem
import com.tencent.qqmusic.openapisdk.model.RecentPlayType
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.songlist.CommonProfileActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoilApi::class)
@Composable
fun RecentPlayAllPage(items: List<RecentPlayAllItem>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(1.dp)) {
        items(items.size) { index ->
            val item = items.getOrNull(index) ?: return@items
            RecentPlayAllRow(item)
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun RecentPlayAllRow(item: RecentPlayAllItem) {
    val activity = LocalContext.current as Activity
    val album = item.detail?.album
    val folder = item.detail?.playlist
    val longAudio = item.detail?.longAudio
    val clickIntent = when (item.type) {
        RecentPlayType.SONG -> Intent(activity, SongListActivity::class.java)
            .putExtra(SongListActivity.KEY_RECENT_PLAY_SONG, true)
        RecentPlayType.ALBUM -> album?.toAlbumIntent(activity)
        RecentPlayType.FOLDER -> folder?.toFolderIntent(activity)
        RecentPlayType.LONG_AUDIO_BOOK -> longAudio?.toLongAudioIntent(activity)
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = clickIntent != null) {
                clickIntent?.let { activity.startActivity(it) }
            }
            .padding(4.dp)
    ) {
        Box(modifier = Modifier.size(60.dp)) {
            Image(
                painter = rememberImagePainter(item.coverUrl()),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = item.title(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subTitle(),
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.lastTime > 0) {
                Text(
                    text = stringResource(
                        R.string.long_audio_recent_time,
                        formatRecentPlayTime(item.lastTime)
                    ),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun RecentPlayAllItem.title(): String {
    return when (type) {
        RecentPlayType.ALBUM -> detail?.album?.name.orEmpty()
        RecentPlayType.FOLDER -> detail?.playlist?.name.orEmpty()
        RecentPlayType.LONG_AUDIO_BOOK -> detail?.longAudio?.title.orEmpty()
        else -> detail?.comm?.title ?: stringResource(R.string.recent_play_all_song_summary)
    }
}

@Composable
private fun RecentPlayAllItem.subTitle(): String {
    return when (type) {
        RecentPlayType.ALBUM -> {
            val album = detail?.album
            stringResource(
                R.string.recent_play_all_album_desc,
                album?.singerName.orEmpty(),
                album?.songNum ?: 0
            )
        }
        RecentPlayType.FOLDER -> {
            val folder = detail?.playlist
            stringResource(
                R.string.recent_play_all_folder_desc,
                folder?.creator?.name.orEmpty(),
                folder?.songNum ?: 0
            )
        }
        RecentPlayType.LONG_AUDIO_BOOK -> {
            val longAudio = detail?.longAudio
            stringResource(
                R.string.recent_play_all_long_audio_desc,
                longAudio?.albumTitle.orEmpty(),
                longAudio?.singer?.joinToString("/").orEmpty()
            )
        }
        else -> stringResource(
            R.string.recent_play_all_song_desc,
            detail?.comm?.count ?: 0
        )
    }
}

private fun RecentPlayAllItem.coverUrl(): String? {
    return when (type) {
        RecentPlayType.ALBUM -> detail?.album?.getAlbumPic()
        RecentPlayType.FOLDER -> detail?.playlist?.picUrl
        RecentPlayType.LONG_AUDIO_BOOK -> detail?.longAudio?.cover
        else -> detail?.comm?.pic
    }
}

private fun Album.toAlbumIntent(activity: Activity): Intent? {
    val albumId = id.takeIf { it.isNotEmpty() && it != "0" } ?: return null
    return Intent(activity, CommonProfileActivity::class.java)
        .putExtra(SongListActivity.KEY_ALBUM_ID, albumId)
}

private fun Folder.toFolderIntent(activity: Activity): Intent? {
    val folderId = id.takeIf { it.isNotEmpty() && it != "0" } ?: return null
    return Intent(activity, CommonProfileActivity::class.java)
        .putExtra(SongListActivity.KEY_FOLDER_ID, folderId)
}

private fun LongAudioItem.toLongAudioIntent(activity: Activity): Intent? {
    val longAudioAlbumId = albumId?.takeIf { it > 0 }?.toString() ?: return null
    return Intent(activity, CommonProfileActivity::class.java)
        .putExtra(SongListActivity.KEY_ALBUM_ID, longAudioAlbumId)
}

private fun formatRecentPlayTime(lastTime: Long): String {
    val timestamp = if (lastTime < 1_000_000_000_000L) lastTime * 1000 else lastTime
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
