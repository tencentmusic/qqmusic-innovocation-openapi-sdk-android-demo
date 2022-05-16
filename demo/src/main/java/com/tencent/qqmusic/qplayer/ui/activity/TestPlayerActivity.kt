package com.tencent.qqmusic.qplayer.ui.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.R

// 
// Created by clydeazhang on 2022/1/14 3:38 下午.
// Copyright (c) 2022 Tencent. All rights reserved.
// 
class TestPlayerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_player)

        findViewById<View>(R.id.pre).setOnClickListener {
            OpenApiSDK.getPlayerApi().prev()
        }
        findViewById<View>(R.id.play).setOnClickListener {
            OpenApiSDK.getPlayerApi().play()
        }
        findViewById<View>(R.id.pause).setOnClickListener {
            OpenApiSDK.getPlayerApi().pause()
        }
        findViewById<View>(R.id.next).setOnClickListener {
            OpenApiSDK.getPlayerApi().next()
        }
        findViewById<View>(R.id.set_list).setOnClickListener {
            OpenApiSDK.getOpenApi().fetchDailyRecommendSong {
                if (it.isSuccess()) {
                    OpenApiSDK.getPlayerApi().setPlayList(it.data!!)
                }
            }
        }
        findViewById<View>(R.id.playsongs).setOnClickListener {
            OpenApiSDK.getOpenApi().fetchDailyRecommendSong {
                if (it.isSuccess()) {
                    OpenApiSDK.getPlayerApi().playSongs(it.data!!)
                }
            }
        }
        findViewById<View>(R.id.btn_test).setOnClickListener {
            OpenApiSDK.getPlayerApi().pause()
        }
    }

}