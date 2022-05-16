package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.model.SearchType
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.core.utils.pref.QQPlayerPreferencesNew
import com.tencent.qqmusicsdk.protocol.PlayDefine
import kotlin.concurrent.thread


//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

private const val TAG = "PlayerPage"

@Composable
fun PlayerScreen(observer: PlayerObserver) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "QPlayer", fontSize = 18.sp) },
                contentColor = Color.White,
                actions = {
                }
            )
        }
    ) {
        PlayerPage(observer)
    }
}

fun Int.qualityToStr(): String {
    return when (this) {
        PlayerEnums.Quality.SQ -> {
            "SQ"
        }
        PlayerEnums.Quality.HQ -> {
            "HQ"
        }
        PlayerEnums.Quality.STANDARD -> {
            "STANDARD"
        }
        PlayerEnums.Quality.LQ -> {
            "LQ"
        }
        else -> {
            "unknown"
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlayerPage(observer: PlayerObserver) {
    val activity = LocalContext.current as Activity

    val currSong = observer.currentSong
    val currState = observer.currentState
    val currMode = observer.currentMode
    val qualityNew = observer.mCurrentQuality
    val playStateText = observer.playStateText
    val quality = remember { mutableStateOf(OpenApiSDK.getPlayerApi().getCurrentPlaySongQuality()) }

    val modeOrder =
        mutableListOf(PlayerEnums.Mode.LIST, PlayerEnums.Mode.ONE, PlayerEnums.Mode.SHUFFLE)
    val qualityOrder =
        mutableListOf(
            PlayerEnums.Quality.LQ,
            PlayerEnums.Quality.STANDARD,
            PlayerEnums.Quality.HQ,
            PlayerEnums.Quality.SQ
        )

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 封面图
        Image(
            painter = rememberImagePainter(currSong?.bigCoverUrl()),
            contentDescription = null,
            modifier = Modifier.size(300.dp)
        )

        // 歌曲信息
        Text(
            text = currSong?.songName ?: "未知",
            fontSize = 20.sp
        )
        Text(
            text = currSong?.singerName ?: "未知",
            fontSize = 20.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val icMode: Int = when (currMode) {
                PlayerEnums.Mode.LIST -> {
                    R.drawable.ic_play_mode_normal
                }
                PlayerEnums.Mode.ONE -> {
                    R.drawable.ic_play_mode_single
                }
                PlayerEnums.Mode.SHUFFLE -> {
                    R.drawable.ic_play_mode_random
                }
                else -> {
                    R.drawable.ic_play_mode_normal
                }
            }

            // 播放模式
            Image(
                painter = painterResource(id = icMode),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clickable {
                        val currIndex = modeOrder.indexOf(currMode)
                        OpenApiSDK
                            .getPlayerApi()
                            .setPlayMode(
                                modeOrder.getOrNull(currIndex + 1)
                                    ?: PlayerEnums.Mode.LIST
                            )
                    }
            )

            val icQuality: Int = when (quality.value) {
                PlayerEnums.Quality.HQ -> {
                    R.drawable.ic_hq
                }
                PlayerEnums.Quality.SQ -> {
                    R.drawable.ic_sq
                }
                PlayerEnums.Quality.STANDARD -> {
                    R.drawable.ic_standard_quality
                }
                else -> {
                    R.drawable.ic_lq
                }
            }

            // 播放品质
            Image(
                painter = painterResource(id = icQuality),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clickable {
                        thread {
                            val currIndex = qualityOrder.indexOf(
                                OpenApiSDK
                                    .getPlayerApi()
                                    .getCurrentPlaySongQuality()
                            )
                            val nextQuality = qualityOrder.getOrNull(currIndex + 1)
                                ?: PlayerEnums.Quality.LQ
                            val ret =
                                OpenApiSDK
                                    .getPlayerApi()
                                    .setCurrentPlaySongQuality(nextQuality)
                            when (ret) {
                                PlayDefine.PlayError.PLAY_ERR_NONE -> {
                                    Log.d(
                                        TAG,
                                        "切换歌曲品质成功"
                                    )
                                    quality.value = nextQuality
                                }
                                4 -> {
                                    activity.runOnUiThread {
                                        Toast
                                            .makeText(activity, "试听歌曲无法切换音质", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                                205 -> {
                                    activity.runOnUiThread {
                                        Toast
                                            .makeText(activity, "没有对应音质", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                                else -> {
                                    activity.runOnUiThread {
                                        Toast
                                            .makeText(activity, "音质切换失败: $ret", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                    Log.e(TAG, "切换歌曲品质失败, ret=$ret")
                                }
                            }
                        }
                    }
            )

            // 播放列表
            Image(
                painter = painterResource(id = R.drawable.ic_playlist),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clickable {
                        activity.startActivity(Intent(activity, PlayListActivity::class.java))
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
            Button(onClick = {
                activity.startActivity(Intent(activity, PlayerTestActivity::class.java).apply {

                })
            }, content = {
                Text("进入播放测试页")
            })
            Button(onClick = {
                thread {
                    OpenApiSDK.getPlayerApi().seek(193680)
                }
            }, content = {
                Text("seek")
            })
        }

        // 播放控制
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_previous),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        thread {
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
                            val ret = if (isPlaying) {
                                OpenApiSDK
                                    .getPlayerApi()
                                    .pause()
                            } else {
                                OpenApiSDK
                                    .getPlayerApi()
                                    .play()
                            }
                            Log.d(TAG, "play or pause, ret=$ret")
                            if (ret != 0) {
                                observer.playStateText = "暂停或开始失败(ret=$ret)"
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
        }

        Text(text = playStateText, modifier = Modifier.padding(top = 10.dp))

    }

}