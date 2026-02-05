package com.tencent.qqmusic.qplayer.ui.activity.player

//import com.tencent.qqmusic.qplayer.ui.activity.lyric.LyricActivity
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.aiaccompany.AiAccompanyHelper
import com.tencent.qqmusic.qplayer.ui.activity.lyric.LyricNewActivity
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVPlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver.tryPauseFirst
import com.tencent.qqmusic.qplayer.ui.activity.songlist.CommonProfileActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.ui.activity.ui.QQMusicSlider
import com.tencent.qqmusic.qplayer.ui.activity.ui.Segment
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlin.concurrent.thread


//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

private const val TAG = "PlayerPage"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlayerScreen(observer: PlayerObserver) {
    Scaffold(topBar = { TopBar(title = "播放页") },
        modifier = Modifier.semantics{ testTagsAsResourceId=true }) {
        PlayerPage(observer)
    }
}

@Composable
fun PlayerPage(observer: PlayerObserver) {
    val activity = LocalContext.current as Activity
    val currSong = observer.currentSong
    var curSongInfoChanged = observer.curSongInfoChanged
    val currState = observer.currentState
    val currMode = observer.currentMode
    val playStateText = observer.playStateText
    var quality = observer.mCurrentQuality
    var aiText = observer.mCurrentAiText
    val collectState = remember {
        mutableStateOf(currSong?.hot == 1)
    }
    val sentence = observer.curSentence

    val modeOrder =
        mutableListOf(PlayerEnums.Mode.LIST, PlayerEnums.Mode.ONE, PlayerEnums.Mode.SHUFFLE, PlayerEnums.Mode.ONE_NOT_REPEAT)
    val lyricView = lyric(observer= observer) {
        activity.startActivity(Intent(activity, LyricNewActivity::class.java))
    }

    val sharedPreferences: SharedPreferences? = observer.sharedPreferences

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(state = rememberScrollState())
        ,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 封面图
        Image(
            painter = rememberImagePainter(currSong?.getAlbumPicUrlBySize("1500")),
            contentDescription = null,
            modifier = Modifier
                .size(300.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        // 歌曲信息
        Text(
            text = currSong?.songName ?: "未知",
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = buildString {
                append(currSong?.singerName ?: "未知")
                currSong?.otherSingerList?.let {
                    if (it.isNotEmpty()){
                        append("/${it.joinToString("/"){ it.title?:it.name }}")
                    }
                }
            },
            fontSize = 14.sp,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable {
                    currSong?.singerId?.let {
                        activity.startActivity(
                            Intent(activity, CommonProfileActivity::class.java)
                                .putExtra(SongListActivity.KEY_SINGER_ID, it)
                        )
                    }
                }
        )
        aiText?.let {
            Text(
                text = it,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Text(
            text = sentence.ifBlank { "无歌词" },
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        AndroidView(
            factory = {
                lyricView
            }, modifier = Modifier
                .padding(top = 40.dp)
                .size(width = 400.dp, height = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            // 播放品质
            Image(
                painter = painterResource(id = UiUtils.getQualityIcon(quality)),
                contentDescription = null,
                colorFilter = if (quality == Quality.MASTER_SR || quality == Quality.SQ_SR) ColorFilter.tint(
                    Color.Blue,
                    BlendMode.SrcAtop
                ) else null,
                modifier = Modifier
                    .size(50.dp)
                    .clickable {
                        tryPauseFirst()
                        QualityAlert.showQualityAlert(
                            activity = activity,
                            isDownload = false,
                            setBlock = {
                                OpenApiSDK
                                    .getPlayerApi()
                                    .setCurrentPlaySongQuality(it,observer.autoPlaySwitchQuality)
                            },
                            refresh = {
                                quality = it
                            })
                    }
            )

            val downloadIcon = if (currSong != null && OpenApiSDK.getDownloadApi().isSongDownloaded(currSong)) {
                R.drawable.icon_song_info_item_more_downloaded
            } else {
                R.drawable.icon_player_download_light
            }
            Image(painter = painterResource(id = downloadIcon),
                contentDescription = null,
                modifier = Modifier
                    .size(45.dp)
                    .clickable(enabled = currSong != null) {
                        if (currSong != null) {
                            Log.i(TAG, "currSong ${currSong.extraInfo?.trace}")
                            QualityAlert.showQualityAlert(
                                activity = activity, isDownload = true, setBlock = {
                                    OpenApiSDK
                                        .getDownloadApi()
                                        .downloadSong(currSong, it)
                                    PlayDefine.PlayError.PLAY_ERR_NONE
                                }, refresh = {})
                        }
                    }
            )

            Image(painter = painterResource(if (collectState.value)
                R.drawable.icon_collect
            else
                R.drawable.icon_uncollect),
                contentDescription = null,
                modifier = Modifier
                    .size(45.dp)
                    .clickable(enabled = currSong != null) {
                        if (currSong != null) {
                            Log.i(TAG, "currSong ${currSong.extraInfo?.trace}")
                            OpenApiSDK
                                .getOpenApi()
                                .fetchPersonalFolder {
                                    if (it.isSuccess()) {
                                        val folderId =
                                            it.data?.firstOrNull { folder -> folder.name == "我喜欢" }?.id
                                                ?: return@fetchPersonalFolder
                                        if (collectState.value) {
                                            OpenApiSDK
                                                .getOpenApi()
                                                .deleteSongFromFolder(
                                                    folderId,
                                                    midList = listOf(currSong.songMid!!)
                                                ) { resp ->
                                                    if (resp.isSuccess()) {
                                                        collectState.value = !collectState.value
                                                    } else {
                                                        ToastUtils.showShort("code=${resp.ret},msg=${resp.errorMsg}")
                                                        Log.e(
                                                            TAG,
                                                            "del resp: $resp"
                                                        )
                                                    }
                                                }
                                        } else {
                                            OpenApiSDK
                                                .getOpenApi()
                                                .addSongToFolder(
                                                    folderId,
                                                    songList = listOf(currSong)
                                                ) { resp ->
                                                    if (resp.isSuccess()) {
                                                        collectState.value = !collectState.value
                                                    } else {
                                                        ToastUtils.showShort("code=${resp.ret},msg=${resp.errorMsg}")
                                                        Log.e(TAG, "add resp: $resp")
                                                    }
                                                }
                                        }
                                    }
                                }
                        }
                    }
            )

            // 音效
            Image(
                painter = painterResource(id = R.drawable.ic_sound_effect),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clickable {
//                        val path = activity.cacheDir.absolutePath
//                        EKeyDecryptor.decryptFile("$path/D0M1001vpnQI0PTUUg.mmp4","$path/D0M1001vpnQI0PTUUg.mp4", "NzJyMk1HVWNUpnJZ5PGNpRWxwutZKLtrvB6r5y4EreIJ7FJaMQjwO5tsnBDMALmO/ZlM5cEKWc1a/zO81WRRqMqJJjrSmYuNHXRR6ZWRFxJ0N1IKsXrHgm7zX5Hba93Pg6M+fTOClR+tV6Sc2zJ/NcVwTj0E0/G+OSdM41n40piEOxSjqHnU10YuSYUhaGem5ofR+DSBw227vlZ4UO6un10H2rpyrKJV2VRnRbVbYOcpYamcZFiQxKM7omLYemTnptgubsMHxqDHJYLhF5in2beeqXSTYrzrkfNW9/6QR4n0sRHCn+vqzJP30tnINzivJkfJUFLGZX2k2uG4gtZIQNZRPsqFFnJu8EC/Pp5lgGPqY3UsxmvITGZtAruD6DWq+sTbAVRGszvZTMWZQO/K8fjsUyOQ8fTlCxqDzjU8yIb+x60veY1l7Gm25yjBHrUOpM26hgfvrduRsDbUKl+VXpX85CMhcP8UuHCp08y2xD0x/9LCqJNbxTjQ3DMQ1umZPgniSUqqrxaSo9RYwJD8gs16QfWXw35GCxn2u58087hDV4HwUPGsbSw2LDuYpGHE3YILuCJmVpNjicungigNWXggYaQHjQUP7Q/sspp/d7LiIeUpPnGjg035ZpJcKqT29M8W5n7TLEFhcuhj+K8xKYrcsuV38oU9uoOvI+DJKh04M1OL9yfEW+nbDGPJMRs6")
                        activity.startActivity(Intent(activity, SoundEffectActivity::class.java))
                    }
            )
        }

        val isPlaying = currState == PlayDefine.PlayState.MEDIAPLAYER_STATE_STARTED

        // 播放控制
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color(observer.magicColor?.second?:0)), onClick = {
                activity.startActivity(Intent(activity, PlayerTestActivity::class.java).apply {

                })
            }, content = {
                Text("进入播放测试页")
            })
            Button(modifier = Modifier.padding(start = 10.dp), onClick = {
                if (currSong?.mvVid?.isNotEmpty() == true) {
                    activity.startActivity(Intent(activity, MVPlayerActivity::class.java).apply {
                        putExtra(MVPlayerActivity.MV_ID, currSong?.mvVid)
                    })
                } else {
                    AppScope.launchUI {
                        Toast.makeText(activity, "该歌曲没有MV可以播放", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text(text = "播放MV")
            }
            Button(modifier = Modifier.padding(start = 10.dp), onClick = {
                UiUtils.setUseNewPlayPage(true)
                UiUtils.gotoPlayerPage()
            }) {
                Text(text = "新版播放页")
            }
        }
        // 播放控制
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currSong == null) {
                    observer.convertTime(0)
                } else {
                    val time = observer.playPosition / 1000
                    observer.convertTime(time.toLong())
                },
                fontFamily = FontFamily.Monospace
            )
            val onlyTryPlay = currSong?.canPlayTry() == true && !currSong.canPlayWhole()
            val tryBegin = if (onlyTryPlay) currSong?.tryBegin?.toFloat() ?: 0F else currSong?.chorusBegin?.toFloat() ?: 0F
            val tryEnd = if (onlyTryPlay) currSong?.tryEnd?.toFloat() ?: 0F else currSong?.chorusEnd?.toFloat() ?: 0F
            val segments = mutableListOf<Segment>().apply {
                add(Segment(name = "tryBegin", start = tryBegin))
                if (tryEnd > 0) {
                    add(Segment(name = "tryEnd", start = tryEnd, color = Color.Gray))
                }
            }

            var position = if (observer.seekPosition >= 0) observer.seekPosition else observer.playPosition
            val duration = OpenApiSDK.getPlayerApi().getDuration()?.toFloat() ?: 100f
            Log.i(TAG, "$position,$duration")
            if (position > duration) {
                position = 0F
            }

            QQMusicSlider(
                value = position,
                range = 0f..(OpenApiSDK.getPlayerApi().getDuration()?.toFloat() ?: 100f),
                segments = segments,
                onValueChange = { newValue ->
                    observer.isSeekBarTracking = true
                    observer.seekPosition = newValue
                },
                progressBegin = if (onlyTryPlay) tryBegin else 0F,
                onValueChangeFinished = {
                    observer.isSeekBarTracking = false
                    thread {
                        var seekPosition = observer.seekPosition.toInt()
                        if (onlyTryPlay && (seekPosition < (currSong?.tryBegin ?: 0) || seekPosition > (currSong?.tryEnd ?: 0))) {
                            seekPosition = currSong?.tryBegin ?: 0
                            AppScope.launchUI {
                                Toast.makeText(activity, "完整播放受限，将播放试听片段", Toast.LENGTH_SHORT).show()
                            }
                        }
                        tryPauseFirst()
                        val res = OpenApiSDK.getPlayerApi().seekToPlay(seekPosition.toLong(),
                            PlayerObserver.needFade)
//                        val res = OpenApiSDK.getPlayerApi().seek(seekPosition, true)
                        if (res.toInt() == seekPosition) {
                            QLog.i(TAG, "PlayerPage seek success res = $res, seekPosition = $seekPosition")
                        } else {
                            QLog.i(TAG, "PlayerPage seek fail res = $res, seekPosition = $seekPosition")
                        }
                        observer.seekPosition = seekPosition.toFloat()
                    }
                },
                modifier = Modifier
                    .weight(1f, true)
                    .padding(horizontal = 10.dp)
            )

            Text(
                text = PlayerObserver.convertTime((OpenApiSDK.getPlayerApi().getDuration() ?: 0L) / 1000L),
                fontFamily = FontFamily.Monospace
            )
        }
        // 播放控制

        // 播放控制
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            val icMode: Int = when (currMode) {
                PlayerEnums.Mode.LIST -> {
                    R.drawable.ic_play_mode_normal
                }

                PlayerEnums.Mode.ONE -> {
                    R.drawable.ic_play_mode_single
                }

                PlayerEnums.Mode.ONE_NOT_REPEAT -> {
                    R.drawable.icon_play_bar_play_mode_single_light
                }
                PlayerEnums.Mode.SHUFFLE -> {
                    R.drawable.ic_play_mode_random
                }

                else -> {
                    R.drawable.ic_play_mode_normal
                }
            }
            Image(
                painter = painterResource(id = icMode),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(30.dp)
                    .clickable {
                        tryPauseFirst()
                        val currIndex = modeOrder.indexOf(currMode)
                        OpenApiSDK
                            .getPlayerApi()
                            .setPlayMode(
                                if (OpenApiSDK.getPlayerApi().isRadio()) {
                                    if (currIndex == 0) PlayerEnums.Mode.ONE_NOT_REPEAT else PlayerEnums.Mode.LIST
                                } else {
                                    modeOrder.getOrNull(currIndex + 1)
                                        ?: PlayerEnums.Mode.LIST
                                }
                            )
                    }
            )

            Image(
                painter = painterResource(id = R.drawable.ic_previous),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        thread {
                            tryPauseFirst()
                            val ret = OpenApiSDK
                                .getPlayerApi()
                                .prev()
                            Log.d(TAG, "prev, ret=$ret")
                            if (ret != 0) {
                                observer.playStateText = "上一曲失败(ret=$ret)"
                            }
                        }
                    }
            )


            Image(
                painter = painterResource(id = if (isPlaying) R.drawable.ic_state_playing else R.drawable.ic_state_paused),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        thread {
                            tryPauseFirst()
                            val ret = if (isPlaying) {
                                OpenApiSDK
                                    .getPlayerApi()
                                    .pause(observer.needFade)
                            } else {
                                OpenApiSDK
                                    .getPlayerApi()
                                    .play(observer.needFade)
                            }
                            Log.d(TAG, "play or pause, ret=$ret")
                            if (ret != 0) {
                                tryPauseFirst()
                                val song = OpenApiSDK
                                    .getPlayerApi()
                                    .getCurrentSongInfo()
                                observer.playStateText = if (song?.canPlay() != true) {
                                    song?.unplayableMsg ?: ""
                                } else {
                                    "暂停或开始失败(ret=$ret)"
                                }
                            } else {
                                val action = if (isPlaying) {
                                    AiAccompanyHelper.TYPE_PAUSE
                                } else {
                                    AiAccompanyHelper.TYPE_RESUME
                                }
                                AiAccompanyHelper.handlePlayActionAndPlayVoice(action)
                            }
                        }
                    }
            )
            Image(
                painter = painterResource(id = R.drawable.ic_next), contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        thread {
                            tryPauseFirst()
                            val ret = OpenApiSDK
                                .getPlayerApi()
                                .next()
                            Log.d(TAG, "next, ret=$ret")
                            if (ret != 0) {
                                observer.playStateText = "下一曲失败(ret=$ret)"
                            }
                        }
                    }
            )


            Image(
                painter = painterResource(id = R.drawable.ic_playlist),
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .clickable {
                        val intent = Intent(activity, PlayListActivity::class.java)
                        activity.startActivity(intent)
                    }
            )

            Image(
                painter = painterResource(id = R.drawable.ic_ai_accompany),
                contentDescription = null,
                colorFilter = if (OpenApiSDK.getAIGlobalListenTogetherApi()
                        .isAIListenTogetherOpened()
                ) ColorFilter.tint(Color.Green, BlendMode.SrcAtop) else null,
                modifier = Modifier
                    .size(30.dp)
                    .clickable {
                        AIAccompanyDialog.show(activity = activity)
                    }
            )
        }

        Text(text = playStateText, modifier = Modifier.padding(top = 10.dp))

        Text(text = "A:${PlayerObserver.convertTime(PlayerObserver.actualDuration/1000L)}," +
                "T:${PlayerObserver.convertTime(PlayerObserver.playDuration/1000L)}")
    }
}

@Preview
@Composable
fun PreviewPlayerScreen() {
    Scaffold {
        val observer = PlayerObserver
        PlayerPage(observer)
    }
}