package com.tencent.qqmusic.qplayer.ui.activity.player

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.ErrorCode
import com.tencent.qqmusic.openapisdk.core.player.Event
import com.tencent.qqmusic.openapisdk.core.player.IMediaEventListener
import com.tencent.qqmusic.openapisdk.core.player.Key
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.core.utils.pref.QQPlayerPreferences

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

object PlayerObserver {

    private const val TAG = "PlayerObserver"

    var currentSong: SongInfo? by mutableStateOf<SongInfo?>(null)
    var currentState: Int? by mutableStateOf<Int?>(null)
    var currentMode: Int? by mutableStateOf<Int?>(null)

    private val event = object : IMediaEventListener {
        override fun onEvent(event: String, arg: Bundle) {
            Log.i(TAG, "onEvent: event $event")

            when (event) {
                Event.API_EVENT_PLAY_SONG_CHANGED -> {
                    if (arg.containsKey(Key.API_EVENT_KEY_PLAY_SONG)) {
                        val curr = arg.getParcelable(Key.API_EVENT_KEY_PLAY_SONG) as? SongInfo
                        val currSongQuality = OpenApiSDK.getPlayerApi().getSongQuality()
                        val songQualityPref = QQPlayerPreferences.getInstance().wifiQuality
                        Log.d(TAG, "onEvent: currSong $curr, song quality $currSongQuality, quality pref $songQualityPref")
                        currentSong = curr
                    }
                }
                Event.API_EVENT_SONG_PLAY_ERROR -> {
                    val code = arg.getInt(Key.API_RETURN_KEY_CODE)
                    val subCode = arg.getInt(Key.API_RETURN_KEY_SUBCODE)

                    if (ErrorCode.acceptable(code)) {
                        // 正常情况
                        return
                    }

                    Toast.makeText(
                        UtilContext.getApp(),
                        "播放遇到错误，code:$code, subCode:$subCode msg:${ErrorCode.errorMessage(code)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                Event.API_EVENT_PLAY_STATE_CHANGED -> {
                    val state = arg.getInt(Key.API_EVENT_KEY_PLAY_STATE)
                    Log.i(TAG, "onEvent: current state $state")
                    currentState = state
                }
                Event.API_EVENT_PLAY_MODE_CHANGED -> {
                    val mode = arg.getInt(Key.API_EVENT_KEY_PLAY_MODE)
                    Log.i(TAG, "onEvent: current mode $mode")
                    currentMode = mode
                }
                else -> {
                }
            }
        }
    }

    fun registerSongEvent() {
        OpenApiSDK.getPlayerApi().registerEventListener(event)
    }

    fun unregisterSongEvent() {
        OpenApiSDK.getPlayerApi().unregisterEventListener(event)
    }

}