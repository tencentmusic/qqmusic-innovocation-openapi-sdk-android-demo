package com.tencent.qqmusic.qplayer.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.songlist.PlayListParams
import com.tencent.qqmusic.qplayer.ui.activity.songlist.itemUI
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 
// Created by clydeazhang on 2021/12/20 2:49 下午.
// Copyright (c) 2021 Tencent. All rights reserved.
//
@SuppressLint("SetTextI18n")
class SongCacheDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SongCacheDemoActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CachePage()
        }
    }

    @Preview
    @Composable
    private fun CachePage() {
        var usage by remember { mutableStateOf(TextFieldValue("200")) }
        var cachedSongList by remember { mutableStateOf(listOf<SongInfo>()) }
        val coroutineScope = rememberCoroutineScope()
        var cachedSize by remember { mutableStateOf(0L) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    cachedSize = getCacheSize()
                }) {
                    Text(text = "获取已缓存大小")
                }

                Text(text = "缓存大小：$cachedSize Mb")
            }

            Row {
                TextField(
                    value = usage,
                    label = {
                        Text(text = "输入缓存上限-Mb")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        usage = it
                    },
                    modifier = Modifier.wrapContentSize()
                )

                Button(onClick = {
                    if (UiUtils.isStrInt(usage.text)) {
                        val ret = OpenApiSDK.getSongCacheApi().setCacheMaxSize(usage.text.toInt())
                        showSetCacheSizeRet(ret)
                    }
                }) {
                    Text(text = "设置")
                }
            }

            Button(onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    OpenApiSDK.getSongCacheApi().clearAllCaches()
                    withContext(Dispatchers.Main) {
                        cachedSize = getCacheSize()
                    }
                }
            }) {
                Text(text = "清除缓存")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            cachedSongList = OpenApiSDK.getSongCacheApi().getCachedSongList()
                        }
                    }
                }) {
                    Text(text = "获取已缓存歌曲")
                }
                
                Text(text = "歌曲数量：${cachedSongList.size}首", modifier = Modifier.padding(start = 10.dp))
            }

            if (cachedSongList.isEmpty()) {
                Text(text = "暂无已缓存歌曲")
            } else {
                LazyColumn {
                    items(cachedSongList) {
                        itemUI(PlayListParams(cachedSongList, it, playCachedOnly = true))
                    }
                }
            }
        }
    }

    private fun showSetCacheSizeRet(ret: Int) {
        if (ret == 0) {
            getCacheSize()
        } else {
            val errMap = mapOf(-1 to "sdk异常", -2 to "参数不合法", -200 to "播放进程未启动或已死亡")
            Toast.makeText(this,"设置失败:${errMap[ret]}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCacheSize(): Long {
        val cacheSize = OpenApiSDK.getSongCacheApi().getCacheSize()
        Log.d(TAG, "cache : $cacheSize cacheSize=${cacheSize / 1024 /1024} MB")
        Toast.makeText(this, "size: ${UiUtils.getFormatSize(cacheSize)}", Toast.LENGTH_SHORT).show()
        return cacheSize / 1024 / 1024
    }
}