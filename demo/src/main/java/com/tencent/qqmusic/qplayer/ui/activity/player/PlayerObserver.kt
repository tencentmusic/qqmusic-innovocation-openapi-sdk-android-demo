package com.tencent.qqmusic.qplayer.ui.activity.player

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.download.DownloadError
import com.tencent.qqmusic.openapisdk.core.download.DownloadEvent
import com.tencent.qqmusic.openapisdk.core.download.DownloadListener
import com.tencent.qqmusic.openapisdk.core.download.DownloadTask
import com.tencent.qqmusic.openapisdk.core.player.IMediaEventListener
import com.tencent.qqmusic.openapisdk.core.player.IProgressChangeListener
import com.tencent.qqmusic.openapisdk.core.player.OnVocalAccompanyStatusChangeListener
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEvent
import com.tencent.qqmusic.openapisdk.core.player.VocalAccompanyConfig
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.core.player.proxy.SPBridgeProxy
import com.tencent.qqmusic.qplayer.ui.activity.aiaccompany.AiAccompanyHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

object PlayerObserver : OnVocalAccompanyStatusChangeListener {

    private const val TAG = "PlayerObserver"
    private const val WHAT_BUFFERING_TIMEOUT = 1

    var currentSong: SongInfo? by mutableStateOf<SongInfo?>(null)
    var currentState: Int? by mutableStateOf<Int?>(null)
    var currentMode: Int by mutableStateOf(OpenApiSDK.getPlayerApi().getPlayMode())
    var mCurrentQuality: Int? by mutableStateOf<Int?>(null)
    var curSongInfoChanged: Int by mutableStateOf(0)

    var playStateText: String by mutableStateOf<String>("播放状态: Idle")
    var playPosition: Float by mutableStateOf(0f)
    var seekPosition: Float by mutableStateOf(-1f)
    var playSpeed: Float by mutableStateOf(OpenApiSDK.getPlayerApi().getPlaySpeed())
    var maxVolumeRatio: Float by mutableStateOf(OpenApiSDK.getPlayerApi().getVolumeRatio())
    var vocalAccompanyConfig: VocalAccompanyConfig by mutableStateOf(OpenApiSDK.getVocalAccompanyApi().currentVocalAccompanyConfig())

    var isSeekBarTracking by mutableStateOf(false)

    private var doSomething:Pair<Int,()->Any?>? = null
    val sharedPreferences: SharedPreferences? = try {
        SPBridgeProxy.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        Log.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
        null
    }

    init {
    }

    private fun initProgressChangeListener() {
        OpenApiSDK.getVocalAccompanyApi().addVocalAccompanyStatusChangeListener(this)
        OpenApiSDK.getPlayerApi().registerProgressChangedListener(object :IProgressChangeListener{
            override fun progressChanged(curPlayTime: Long,
                                         totalTime: Long,
                                         bufferLength: Long,
                                         totalLength: Long) {
                MLog.i("PlayerPage", "curTime$curPlayTime,(${PlayerObserver.convertTime(curPlayTime / 1000)}),${convertTime(OpenApiSDK.getPlayerApi().getDuration()!!.toLong() / 1000)}, bufferLength = $bufferLength, totalLength = $totalLength")

                doSomeThingOnEventStateChange(0, Pair(curPlayTime, totalTime))
                if (isSeekBarTracking) {
                    MLog.i("PlayerPage", "isSeekBarTracking = true")
                }
                val duration = OpenApiSDK.getPlayerApi().getDuration() ?: 0
                playPosition = if (curPlayTime > duration) {
                    duration.toFloat()
                } else {
                    if (currentSong == null) 0f else curPlayTime.toFloat()
                }
                Log.i(TAG, "$playPosition")
                if (seekPosition in 0.0..playPosition.toDouble()) {
                    seekPosition = -1f
                }
            }
        })

        OpenApiSDK.getDownloadApi().registerDownloadListener(object : DownloadListener {
            override fun onEvent(event: DownloadEvent, task: DownloadTask?) {
                task?.getSongInfo()?.let {
                    if (it.songId == currentSong?.songId) {
                        if (event == DownloadEvent.DOWNLOAD_TASK_REMOVED || event == DownloadEvent.DOWNLOAD_SUCCESS) {
                            currentSong?.filePath = it.filePath
                        }
                    }
                }
            }

            override fun onCreateTaskError(song: SongInfo, err: DownloadError) {

            }

            override fun onDownloadError(task: DownloadTask, err: DownloadError) {

            }
        })
    }

    fun convertTime(num: Long): String {
        val time = num.toInt()
        val min = time / 60
        val sec = time % 60

        val secString = if (sec <= 9) "0$sec" else sec.toString()
        val minString = if (min <= 9) "0$min" else min.toString()

        return if (min == 0) {
            "00:$secString"
        } else {
            "$minString:$secString"
        }
    }

    fun toOneDigits(num: Float): Float {
        return "%.1f".format(num).toFloatOrNull() ?: num
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
                        if (currentSong?.songId != curr?.songId) {
                            playPosition = -1F
                        }
                        currentSong = curr
                        AiAccompanyHelper.handleSongChangeAndPlayTransitionIntro(curr)
                        AiAccompanyHelper.handleSongChangeAndPlayVoice(curr)
                        val currentPlaySongQuality =
                            OpenApiSDK.getPlayerApi().getCurrentPlaySongQuality()
                        Log.d(TAG, "play song changed: $curr, filePath: ${curr?.filePath}")
                        Log.d(TAG, "play currentPlaySongQuality: $currentPlaySongQuality")

                        if (currentPlaySongQuality != null) {
                            mCurrentQuality = currentPlaySongQuality
                        }
                        seekPosition = -1F
                        playSpeed = OpenApiSDK.getPlayerApi().getPlaySpeed()
                    }
                    if(AiAccompanyHelper.isListenTogetherOpen){
                        if (OpenApiSDK.getPlayerApi().getPlayList().size-OpenApiSDK.getPlayerApi().getCurPlayPos()<=3){
                            AiAccompanyHelper.fetchRec2AppendSongList()
                        }
                    }
                }
                PlayerEvent.Event.API_EVENT_SONG_PLAY_ERROR -> {
                    GlobalScope.launch(Dispatchers.Main) {
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
                }

                PlayerEvent.Event.API_EVENT_PLAY_LIST_CHANGED -> {
                    val size = arg.getInt(PlayerEvent.Key.API_EVENT_KEY_PLAY_LIST_SIZE, 0)
                    if (size == 0) {
                        currentSong = null
                    } else {
                        val list = OpenApiSDK.getPlayerApi().getPlayList()
                        list.contains(currentSong)
                    }
                    val currentPlaySongQuality = OpenApiSDK.getPlayerApi().getCurrentPlaySongQuality()
                    Log.d(TAG, "init currentPlaySongQuality: $currentPlaySongQuality")
                    if (currentPlaySongQuality != mCurrentQuality) {
                        mCurrentQuality = currentPlaySongQuality
                    }
                }
                PlayerEvent.Event.API_EVENT_PLAY_STATE_CHANGED -> {
                    val state = arg.getInt(PlayerEvent.Key.API_EVENT_KEY_PLAY_STATE)
                    Log.i(TAG, "onEvent: current state $state")
                    currentState = state
                    handler.removeMessages(WHAT_BUFFERING_TIMEOUT)
                    doSomeThingOnEventStateChange(state)
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
                            val currentSongInfo = OpenApiSDK.getPlayerApi().getCurrentSongInfo()
                            if (currentSongInfo?.canPlayTry() == true && !currentSongInfo.canPlayWhole()) {
                                Toast.makeText(
                                    UtilContext.getApp(),
                                    "完整播放受限，将播放试听片段",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {

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
                            OpenApiSDK.getPlayerApi().getCurrentPlaySongQuality()?.let {
                                mCurrentQuality = it
                                Log.d(TAG, "play mCurrentQuality: $mCurrentQuality")
                            }

                            val isPlayingDownloadSong = OpenApiSDK.getPlayerApi().isPlayingDownloadLocalFile()
                            if (isPlayingDownloadSong) {
                                Toast.makeText(
                                    UtilContext.getApp(),
                                    "正在为您播放下载歌曲，本次播放不消耗流量",
                                    Toast.LENGTH_SHORT
                                ).show()
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

                PlayerEvent.Event.API_EVENT_PLAY_SPEED_CHANGED -> {
                    playSpeed = arg.getFloat(PlayerEvent.Key.API_EVENT_KEY_PLAY_SPEED, 1.0f)
                }
                PlayerEvent.Event.API_EVENT_PLAY_SERVICE_STATE_CHANGED -> {
                    val connect = arg.getBoolean(PlayerEvent.Key.API_EVENT_KEY_PLAY_SERVICE_CONNECTED)
                    Log.i(TAG, "API_EVENT_PLAY_SERVICE_STATE_CHANGED connect=$connect")
                    if (connect) {
                        initProgressChangeListener()
                    }
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

    override fun onVocalAccompanyStatusChange(vocalScale: Int, enable: Boolean) {
        mCurrentQuality = OpenApiSDK.getPlayerApi().getCurrentPlaySongQuality()
    }

    fun doSomeThingOnEventStateChange(func: ()-> Any?, dstState:Int?=null){
        dstState ?: return
        doSomething = Pair(dstState, func)
    }

    fun doSomeThingOnPlayTime(func: ()-> Any?, onPlayTimeSec:Int){
        doSomething = Pair(onPlayTimeSec, func)
    }

    private fun doSomeThingOnEventStateChange(curState:Int, playTime:Pair<Long,Long>?=null){
        doSomething?.let { (dstState,func)->
            if (playTime!=null){
                val curPlayTime = playTime.first
                val totalTime = playTime.second
                var runTime = dstState*1000L
                if (dstState<0){
                    runTime += (totalTime/1000*1000)
                }
                if (curPlayTime >= runTime){
                    Log.d(TAG, "doSomeThingOnPlayTime:hit playTime=${curPlayTime}")
                    func()
                    doSomething = null
                }
            }else if(dstState ==curState){
                Log.d(TAG, "doSomeThingOnEventStateChange:hit playState=$curState")
                func()
                doSomething = null
            }
        }
    }
    private val tryPauseFirst = sharedPreferences?.getBoolean("tryPauseFirst", false)
    private val needFade = sharedPreferences?.getBoolean("needFadeWhenPlay", false) ?: false
    fun tryPauseFirst(){
        if(tryPauseFirst==true){
            val result = OpenApiSDK.getPlayerApi().pause(needFade)
            Log.d(TAG, "[tryPauseFirst] pause needFade=$needFade ret=$result")
        }
    }
}