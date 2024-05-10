package com.tencent.qqmusic.qplayer.ui.activity.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.hologram.EdgeMvProvider
import com.tencent.qqmusic.openapisdk.model.PlaySpeedType
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVPlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlin.concurrent.thread

// 
// Created by clydeazhang on 2022/3/10 10:23 上午.
// Copyright (c) 2022 Tencent. All rights reserved.
// 
class PlayerTestActivity : ComponentActivity() {
    companion object {
        private const val TAG = "@@@PlayerTestActivity"
    }


    private val sharedPreferences: SharedPreferences? by lazy {
        try {
            this.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
            null
        }
    }


    private var reInitPlayerEnv: Button? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            R.layout.activity_player_test
        )

        val edt = findViewById<EditText>(R.id.edt_song)
        val btnPlay = findViewById<Button>(R.id.btn_play)
        val btnQuality = findViewById<Button>(R.id.btn_change_quality)
        val edtFolder = findViewById<EditText>(R.id.edt_folder)
        val btnFolder = findViewById<Button>(R.id.btn_folder)

        val btnUseDolby = findViewById<Button>(R.id.btn_use_dolby)


        val mvID = findViewById<EditText>(R.id.play_mv_id).apply {
            hint = "1861944"
        }

        findViewById<Button>(R.id.mv_test_button).apply {
            setOnClickListener {
                val cacheSize = mvID.text.toString().ifEmpty { "1861944" }
                startActivity(Intent(this@PlayerTestActivity, MVPlayerActivity::class.java).apply {
                    putExtra(MVPlayerActivity.MV_ID, cacheSize)
                })

            }
        }

        reInitPlayerEnv = findViewById(R.id.reinit_player_env)
        edt.setText("335918510,317178463,316688109,332183304")
        initButton()

        refresh()

        edt.setTextIsSelectable(true)
        btnPlay.setOnClickListener {
            thread {
                val songIdText = edt.text.toString()
                val split = songIdText.split(",")
                // 0039eBnn3dVsNo
                val songIdList = mutableListOf<Long>()
                val songMidList = mutableListOf<String>()
                split.forEach {
                    val id = it.toLongOrNull()
                    if (id != null) {
                        songIdList.add(id)
                    } else {
                        songMidList.add(it)
                    }
                }
                OpenApiSDK.getOpenApi().fetchSongInfoBatch(
                    songIdList = if (songIdList.isEmpty()) null else songIdList,
                    midList = if (songMidList.isEmpty()) null else songMidList
                ) {
                    if (it.isSuccess()) {
                        val ret = OpenApiSDK.getPlayerApi().playSongs(it.data!!, it.data!!.indices.random())
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            //OpenApiSDK.getPlayerApi().seek(301318)
                        }, 10)
                        Log.d(TAG, "playsongs ret = $ret")
                        finish()
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            startActivity(Intent(this, PlayerActivity::class.java))
                        }, 1000)
                    } else {
                        Toast.makeText(this, "查询歌曲失败: ${it.errorMsg}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        findViewById<Button>(R.id.btn_play_list_update).setOnClickListener {
            val list = OpenApiSDK.getPlayerApi().getPlayList()
            Log.d(TAG, "before size:${list.size}")
            if (list.size < 2) {
                UiUtils.showToast("请保持列表数大于1个")
                return@setOnClickListener
            }
            val newList = ArrayList(list)
            newList.removeAt(newList.size - 1)
            AppScope.launchIO {
                OpenApiSDK.getPlayerApi().updatePlayingSongList(newList)
                Handler(Looper.getMainLooper()).postDelayed({
                    val newSize = OpenApiSDK.getPlayerApi().getPlayList().size
                    Log.d(TAG, "after size:${newSize}")
                    UiUtils.showToast("列表从${list.size} -> $newSize")
                }, 1000)
            }
        }

        btnQuality.setOnClickListener {
            QualityAlert.showQualityAlert(this, false, {
                OpenApiSDK.getPlayerApi().setPreferSongQuality(it)
            }, {
                runOnUiThread {
                    refresh()
                }
            })
        }

        btnUseDolby.setOnClickListener {
//            edt.setText("359434619,359434618,359434626,359434622,359434624")
            edt.setText("370513537")
        }

        findViewById<Button>(R.id.btn_use_hires).setOnClickListener {
//            edt.setText("262607197")
            edt.setText("381885920,262607197")
        }

        edtFolder.setTextIsSelectable(true)
        btnFolder.setOnClickListener {
            val albumIdText = edtFolder.text.toString()
            startActivity(
                Intent(
                    this@PlayerTestActivity,
                    SongListActivity::class.java
                ).apply {
                    putExtra(SongListActivity.KEY_FOLDER_ID, albumIdText)
                }
            )
        }


        val edtMVCahce = findViewById<EditText>(R.id.edt_mv_cache).apply {
            hint = (sharedPreferences?.getInt("MV_CACHE", 500) ?: 500).toString()
        }
        val btnMVCache = findViewById<Button>(R.id.btn_mv_cache).apply {
            setOnClickListener {
                val cacheSize = edtMVCahce.text.toString().toIntOrNull() ?: 500
                sharedPreferences?.edit()?.putInt("MV_CACHE", cacheSize)?.apply()
                OpenApiSDK.getProviderByClass(EdgeMvProvider::class.java)?.setCacheSize(cacheSize)
            }
        }
        seekToPlayView()
        playControllerView()
    }

    private fun refresh() {
        val textQuality = findViewById<TextView>(R.id.text_prefre_quality)

        val qualityStr = when (OpenApiSDK.getPlayerApi().getPreferSongQuality()) {
            PlayerEnums.Quality.HQ -> {
                "hq"
            }

            PlayerEnums.Quality.SQ -> {
                "sq"
            }

            PlayerEnums.Quality.STANDARD -> {
                "standard"
            }

            PlayerEnums.Quality.LQ -> {
                "LQ"
            }

            PlayerEnums.Quality.DOLBY -> {
                "杜比"
            }

            PlayerEnums.Quality.HIRES -> {
                "HiRes"
            }

            PlayerEnums.Quality.EXCELLENT -> {
                "臻品音质2.0"
            }

            PlayerEnums.Quality.GALAXY -> {
                "臻品全景声"
            }

            else -> {
                "未知"
            }
        }
        textQuality.text = "当前默认音质：${qualityStr}"
    }


    private fun initButton() {
        reInitPlayerEnv?.setOnClickListener {
            reInitPlayerEnv()
        }
    }


    private fun reInitPlayerEnv() {
        OpenApiSDK.getPlayerApi().apply {
            //音质
            setCurrentPlaySongQuality(PlayerEnums.Quality.STANDARD)
            //倍速
            val playType = getCurrentSongInfo()?.let {
                if (it.isLongAudioSong()) {
                    PlaySpeedType.LONG_AUDIO
                } else {
                    PlaySpeedType.SONG
                }
            }
            playType?.let {
                setPlaySpeed(1.0F, playType)
            }

            //音效
            setSoundEffectType(null)
            Toast.makeText(this@PlayerTestActivity, "恢复完成", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("CutPasteId")
    private fun seekToPlayView(){
        var actionStateIndex:Int = sharedPreferences?.getInt("actionStateIndex", 0) ?: 0
        val seekTimerSpinner: Spinner = findViewById(R.id.spinner_seek_timer)
        // 来源PlayerManagerImpl().needExportPlayState
        val myState = mapOf(
            "初始状态 0" to PlayDefine.PlayState.MEDIAPLAYER_STATE_IDLE,
            "已准备 2" to PlayDefine.PlayState.MEDIAPLAYER_STATE_PREPARED,
            "正在播放 4" to PlayDefine.PlayState.MEDIAPLAYER_STATE_STARTED,
            "已暂停 5" to PlayDefine.PlayState.MEDIAPLAYER_STATE_PAUSED,
            "已停止 6" to PlayDefine.PlayState.MEDIAPLAYER_STATE_STOPPED,
            "播放正常结束，对应播放器的onCompletion  7" to PlayDefine.PlayState.MEDIAPLAYER_STATE_PLAYBACKCOMPLETED,
            "正在缓冲 101" to PlayDefine.PlayState.MEDIAPLAYER_STATE_BUFFERING,
            "缓冲失败 701" to PlayDefine.PlayState.MEDIAPLAYER_STATE_BUFFER_FAILED
            )
        val seekTimerSpinnerOptions = myState.keys.toList()

        // 创建一个 ArrayAdapter 使用默认 spinner 布局和选项数组
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seekTimerSpinnerOptions)

        // 指定下拉菜单的布局样式 - 简单的列表视图
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // 应用适配器到 spinner
        seekTimerSpinner.adapter = adapter

        seekTimerSpinner.setSelection(actionStateIndex)

        // 设置选项选择事件监听器
        seekTimerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                // 选中后的动作
                actionStateIndex = position
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 可选的回调，没有选项被选中时的动作
            }
        }
        findViewById<EditText>(R.id.edt_params1).apply {
            hint = (sharedPreferences?.getString("params1", "") ?: "").toString()
        }

        findViewById<EditText>(R.id.edt_params2).apply {
            hint = (sharedPreferences?.getString("params2", "") ?: "").toString()
        }

        var playerFuncIndex:Int = sharedPreferences?.getInt("playerFuncIndex", 0) ?: 0
        val playerFuncSpinner: Spinner = findViewById(R.id.spinner_player_func)
        val funcMap = mutableMapOf<String,()->Any?>()
        val params1 = findViewById<EditText>(R.id.edt_params1).text.toString()
        funcMap["seekToPlay"] = {
            OpenApiSDK.getPlayerApi().seekToPlay(params1.toLongOrNull() ?: 60000L)
        }
        funcMap["seek"] = { OpenApiSDK.getPlayerApi().seek(params1.toIntOrNull() ?: 60000) }
        funcMap["playPos"] = { OpenApiSDK.getPlayerApi().playPos(params1.toIntOrNull() ?: 1) }
        val playerFuncSpinnerOptions = funcMap.keys.toList()
        // 创建一个 ArrayAdapter 使用默认 spinner 布局和选项数组
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_item, playerFuncSpinnerOptions)

        // 指定下拉菜单的布局样式 - 简单的列表视图
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // 应用适配器到 spinner
        playerFuncSpinner.adapter = adapter2

        playerFuncSpinner.setSelection(playerFuncIndex)

        // 设置选项选择事件监听器
        playerFuncSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                // 选中后的动作
                playerFuncIndex = position
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 可选的回调，没有选项被选中时的动作
            }
        }

        findViewById<Button>(R.id.btn_setToRun).setOnClickListener {
            sharedPreferences?.edit()?.putString("params1", findViewById<EditText>(R.id.edt_params1).text.toString())?.apply()
            sharedPreferences?.edit()?.putString("params2", findViewById<EditText>(R.id.edt_params2).text.toString())?.apply()

            sharedPreferences?.edit()?.putInt("actionStateIndex", actionStateIndex)?.apply()
            sharedPreferences?.edit()?.putInt("playerFuncIndex", playerFuncIndex)?.apply()
            funcMap[playerFuncSpinnerOptions[playerFuncIndex]]?.let { func ->
                PlayerObserver.doSomeThingOnEventStateChange(
                    func = { func() },
                    dstState = myState[seekTimerSpinnerOptions[actionStateIndex]]
                )
            }

        }
    }

    private fun playControllerView(){
        findViewById<Button>(R.id.stopPlay).setOnClickListener {
            AppScope.launchIO {
                OpenApiSDK.getPlayerApi().stop()
            }
        }

    }
}