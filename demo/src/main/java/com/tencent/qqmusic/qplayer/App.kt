package com.tencent.qqmusic.qplayer

import android.app.Application
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver

/**
 * Created by tannyli on 2021/8/31.
 * Copyright (c) 2021 TME. All rights reserved.
 */
class App : Application() {

    companion object{
        private const val TAG = "App"
    }

    override fun onCreate() {
        super.onCreate()

        MustInitConfig.check()

        OpenApiSDK.init(
            this.applicationContext,
            MustInitConfig.APP_ID,
            MustInitConfig.APP_KEY
        )
        PlayerObserver.registerSongEvent()
    }

}