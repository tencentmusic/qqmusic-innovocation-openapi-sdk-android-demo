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
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
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
        val edtFolder = findViewById<EditText>(R.id.edt_folder)
        val btnFolder = findViewById<Button>(R.id.btn_folder)

        val btnUseDolby = findViewById<Button>(R.id.btn_use_dolby)
        edt.setText("335918510,317178463,316688109,332183304")

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
                ToastUtils.showShort("请保持列表数大于1个")
                return@setOnClickListener
            }
            val newList = ArrayList(list)
            newList.removeAt(newList.size - 1)
            AppScope.launchIO {
                OpenApiSDK.getPlayerApi().updatePlayingSongList(newList)
                Handler(Looper.getMainLooper()).postDelayed({
                    val newSize = OpenApiSDK.getPlayerApi().getPlayList().size
                    Log.d(TAG, "after size:${newSize}")
                    ToastUtils.showShort("列表从${list.size} -> $newSize")
                }, 1000)
            }
        }

        btnQuality.setOnClickListener {
            QualityAlert.showQualityAlert(this, {
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
            else -> {
                "未知"
            }
        }
        textQuality.text = "当前默认音质：${qualityStr}"
    }
}