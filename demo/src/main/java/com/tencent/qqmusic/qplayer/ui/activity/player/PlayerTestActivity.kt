package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.qplayer.R
import kotlin.concurrent.thread

// 
// Created by clydeazhang on 2022/3/10 10:23 上午.
// Copyright (c) 2022 Tencent. All rights reserved.
// 
class PlayerTestActivity : Activity() {
    companion object {
        private const val TAG = "@@@PlayerTestActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            R.layout.activity_player_test
        )

        val edt = findViewById<EditText>(R.id.edt_song)
        val btnPlay = findViewById<Button>(R.id.btn_play)
        val btnQuality = findViewById<Button>(R.id.btn_change_quality)

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
                        val ret = OpenApiSDK.getPlayerApi().playSongs(it.data!!)
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

        btnQuality.setOnClickListener {
            val next = when (OpenApiSDK.getPlayerApi().getPreferSongQuality()) {
                PlayerEnums.Quality.HQ -> {
                    PlayerEnums.Quality.STANDARD
                }
                PlayerEnums.Quality.STANDARD -> {
                    PlayerEnums.Quality.LQ
                }
                PlayerEnums.Quality.LQ -> {
                    PlayerEnums.Quality.SQ
                }
                else -> {
                    PlayerEnums.Quality.HQ
                }
            }
            val ret = OpenApiSDK.getPlayerApi().setPreferSongQuality(next)
            if (ret != 0) {
                Toast.makeText(this, "切换失败：$ret", Toast.LENGTH_SHORT).show()
            }

            refresh()
        }
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
            else -> {
                "未知"
            }
        }
        textQuality.text = "当前默认音质：${qualityStr}"
    }
}