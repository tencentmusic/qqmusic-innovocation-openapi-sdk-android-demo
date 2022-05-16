package com.tencent.qqmusic.qplayer.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.SongCacheCallback
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R

// 
// Created by clydeazhang on 2021/12/20 2:49 下午.
// Copyright (c) 2021 Tencent. All rights reserved.
//
@SuppressLint("SetTextI18n")
class SongCacheDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SongCacheDemoActivity"
    }

    private var songInfo: SongInfo? = null

    private val exist by lazy {
        findViewById<TextView>(R.id.exist)
    }
    private val display by lazy {
        findViewById<TextView>(R.id.display)
    }
    private val edit by lazy {
        findViewById<EditText>(R.id.edit)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_cache)
        findViewById<View>(R.id.start).setOnClickListener {
            getCurrentSongId()?.let {
                querySongInfo(it) { songInfo ->
                    display.text = "开始缓存, 请稍等..."
                    OpenApiSDK.getSongCacheApi().startCache(songInfo, object : SongCacheCallback {
                        override fun onSuccess(songInfo: SongInfo) {
                            Toast.makeText(this@SongCacheDemoActivity, "缓存成功", Toast.LENGTH_SHORT)
                                .show()
                            display.text = "缓存成功"
                            refreshExist()
                        }

                        override fun onFailed(songInfo: SongInfo, errorMsg: String) {
                            Toast.makeText(this@SongCacheDemoActivity, "缓存失败", Toast.LENGTH_SHORT)
                                .show()
                            refreshExist()
                            display.text = "缓存失败: $errorMsg"
                        }
                    })
                }
            }
        }
        findViewById<View>(R.id.stop).setOnClickListener {
            getCurrentSongId()?.let {
                querySongInfo(it) { songInfo ->
                    display.text = "停止缓存"
                    val stopCache = OpenApiSDK.getSongCacheApi().stopCache(songInfo)
                    if (stopCache) {
                        display.text = "停止缓存成功"
                        refreshExist()
                    } else {
                        display.text = "停止缓存失败"
                    }
                }
            }
        }
        findViewById<View>(R.id.clear).setOnClickListener {
            getCurrentSongId()?.let {
                querySongInfo(it) { songInfo ->
                    display.text = "清除缓存"
                    val stopCache = OpenApiSDK.getSongCacheApi().clearCache(songInfo)
                    if (stopCache) {
                        display.text = "清除缓存成功"
                        refreshExist()
                    } else {
                        display.text = "清除缓存失败"
                    }
                }
            }
        }
        findViewById<View>(R.id.clearAll).setOnClickListener {
            OpenApiSDK.getSongCacheApi().clearAllCaches()
        }
        findViewById<View>(R.id.check).setOnClickListener {
            refreshExist()
        }

        findViewById<View>(R.id.play).setOnClickListener {
            getCurrentSongId()?.let {
                querySongInfo(it) { songInfo ->
                    OpenApiSDK.getPlayerApi().playSongs(listOf(songInfo), 0)
                }
            }
        }

        findViewById<View>(R.id.size).setOnClickListener {
            val cacheSize = OpenApiSDK.getSongCacheApi().getCacheSize()
            Log.d(TAG, "cacheSize=$cacheSize byte")
            Toast.makeText(this, "size: ${cacheSize}byte", Toast.LENGTH_SHORT).show()
        }

        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                refreshExist()
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })
        // 免费歌曲：316868744
        // vip歌曲：97773 晴天
        // 无版权歌曲：314818717
        edit.setText("316868744")

        refreshExist()
    }


    @SuppressLint("SetTextI18n")
    private fun refreshExist() {
        val songId = getCurrentSongId()
        if (songId != null) {
            querySongInfo(songId) {
                exist.text = "歌曲id${songId}缓存是否存在：${
                    OpenApiSDK.getSongCacheApi().checkHasCached(it)
                }"
            }
        } else {
            exist.text = "请输入歌曲id"
        }
    }

    private fun getCurrentSongId(): Long? {
        val id = edit.text.toString().toLongOrNull()
        return if (id != null) {
            id
        } else {
            display.text = "输入的songId格式不正确"
            null
        }

    }

    private fun querySongInfo(songId: Long, callback: (SongInfo) -> Unit) {
        if (songInfo != null && songId == songInfo!!.songId ?: -1) {
            callback.invoke(songInfo!!)
            return
        }
        OpenApiSDK.getOpenApi().fetchSongInfoBatch(songIdList = listOf(songId)) {
            if (it.isSuccess()) {
                val first = it.data!!.first()
                songInfo = first
                callback.invoke(first)
            } else {
                display.text = "查询歌曲信息失败：${it.errorMsg}"
            }
        }
    }
}