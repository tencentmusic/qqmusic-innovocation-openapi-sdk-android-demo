package com.tencent.qqmusic.qplayer.ui.activity.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.IMediaEventListener
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEvent
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.core.player.MusicPlayerHelper

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

object PlayerObserver {

    private const val TAG = "PlayerObserver"
    private const val WHAT_BUFFERING_TIMEOUT = 1

    var currentSong: SongInfo? by mutableStateOf<SongInfo?>(null)
    var currentState: Int? by mutableStateOf<Int?>(null)
    var currentMode: Int by mutableStateOf(OpenApiSDK.getPlayerApi().getPlayMode())
    var mCurrentQuality: Int? by mutableStateOf<Int?>(null)

    var playStateText: String by mutableStateOf<String>("播放状态: Idle")
    var playPosition: Float by mutableStateOf(0f)
    var seekPosition: Float by mutableStateOf(-1f)


    init {
        MusicPlayerHelper.getInstance().registerProgressChangedInterface { curTime: Long, totalTime: Long ->
            playPosition = if (currentSong == null) 0f else curTime.toFloat()
            if (seekPosition in 0.0..playPosition.toDouble()) {
                seekPosition = -1f
            }
        }
    }


    fun convertTime(num: Long): String {
        val time = num.toInt()
        val min = time / 60
        val sec = time % 60

        val secString = if (sec <= 9) "0$sec" else sec.toString()
        val minString = if (sec <= 9) "0$min" else min.toString()

        return if (min == 0) {
            secString
        } else {
            "$minString:$secString"
        }
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                WHAT_BUFFERING_TIMEOUT -> {
                    if (currentState == PlayDefine.PlayState.MEDIAPLAYER_STATE_BUFFERING) {
                        setPlayState("缓冲超时")
                    }
                }
                else -> {
                }
            }
        }
    }

    private fun resetPlayProgress() {
        seekPosition = -1f
        playPosition = 0f
    }

    private val event = object : IMediaEventListener {
        override fun onEvent(event: String, arg: Bundle) {

            when (event) {
                PlayerEvent.Event.API_EVENT_PLAY_SONG_CHANGED -> {
                    if (arg.containsKey(PlayerEvent.Key.API_EVENT_KEY_PLAY_SONG)) {
                        val curr = arg.getParcelable(PlayerEvent.Key.API_EVENT_KEY_PLAY_SONG) as? SongInfo
                        currentSong = curr
                        val currentPlaySongQuality =
                            OpenApiSDK.getPlayerApi().getCurrentPlaySongQuality()
                        Log.d(TAG, "play song changed: $curr")
                        Log.d(TAG, "play currentPlaySongQuality: $currentPlaySongQuality")

                        if (currentPlaySongQuality != null) {
                            mCurrentQuality = currentPlaySongQuality
                        }
                        seekPosition = -1F
                    }
                }
                PlayerEvent.Event.API_EVENT_SONG_PLAY_ERROR -> {
                    val errorCode = arg.getInt(PlayerEvent.Key.API_EVENT_KEY_ERROR_CODE)
                    Log.i(TAG, "onEvent: current error $errorCode")
                    if (currentSong?.canPlay() != true) {
                        Toast.makeText(
                            UtilContext.getApp(),
                            currentSong?.unplayableMsg ?: "",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            UtilContext.getApp(),
                            "播放遇到错误，code:$errorCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    setPlayState("播放错误(code=${errorCode})")
                    resetPlayProgress()
                }

                PlayerEvent.Event.API_EVENT_PLAY_LIST_CHANGED -> {
                    val size = arg.getInt(PlayerEvent.Key.API_EVENT_KEY_PLAY_LIST_SIZE, 0)
                    if (size == 0) {
                        currentSong = null
                    } else {
                        val list = OpenApiSDK.getPlayerApi().getPlayList()
                        list.contains(currentSong)
                    }
                }
                PlayerEvent.Event.API_EVENT_PLAY_STATE_CHANGED -> {
                    val state = arg.getInt(PlayerEvent.Key.API_EVENT_KEY_PLAY_STATE)
                    Log.i(TAG, "onEvent: current state $state")
                    currentState = state
                    handler.removeMessages(WHAT_BUFFERING_TIMEOUT)
                    when (state) {
                        PlayDefine.PlayState.MEDIAPLAYER_STATE_BUFFERING -> {
                            handler.sendEmptyMessageDelayed(WHAT_BUFFERING_TIMEOUT, 20000)
                            setPlayState("缓冲中")
                        }
                        PlayDefine.PlayState.MEDIAPLAYER_STATE_IDLE -> {
                            setPlayState("Idle")
                        }
                        PlayDefine.PlayState.MEDIAPLAYER_STATE_PAUSED -> {
                            setPlayState("已暂停")
                        }
                        PlayDefine.PlayState.MEDIAPLAYER_STATE_STARTED -> {
                            setPlayState("播放中")
                            if (currentSong?.canPlayTry() == true && currentSong?.canPlayWhole() != true) {
                                Toast.makeText(
                                    UtilContext.getApp(),
                                    "完整播放受限，将播放试听片段",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        PlayDefine.PlayState.MEDIAPLAYER_STATE_STOPPED -> {
                            setPlayState("停止")
                        }

                        PlayDefine.PlayState.MEDIAPLAYER_STATE_PLAYBACKCOMPLETED -> {
                            setPlayState("当前歌曲播放已经完成")
                        }

                        PlayDefine.PlayState.MEDIAPLAYER_STATE_PREPARED -> {
                            setPlayState("已准备")
                            if (mCurrentQuality == -1) {
                                mCurrentQuality = OpenApiSDK.getPlayerApi().getCurrentPlaySongQuality()
                                Log.d(TAG, "play mCurrentQuality: $mCurrentQuality")

                            }
                        }
                    }
                }
                PlayerEvent.Event.API_EVENT_PLAY_MODE_CHANGED -> {
                    val mode = arg.getInt(PlayerEvent.Key.API_EVENT_KEY_PLAY_MODE)
                    Log.i(TAG, "onEvent: current mode $mode")
                    currentMode = mode
                }
                PlayerEvent.Event.API_EVENT_SEEK_CHANGED -> {
                    val pos = arg.getLong(PlayerEvent.Key.API_EVENT_KEY_SEEK)
                    Log.i(TAG, "seek pos=$pos")
                }
                else -> {
                }
            }
        }
    }

    private fun setPlayState(test: String) {
        playStateText = "播放状态: $test"
    }

    fun registerSongEvent() {
        OpenApiSDK.getPlayerApi().registerEventListener(event)
    }

    fun unregisterSongEvent() {
        OpenApiSDK.getPlayerApi().unregisterEventListener(event)
    }

}