package com.tencent.qqmusic.qplayer

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tencent.qqmusic.innovation.common.util.DeviceUtils
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig

/**
 * Created by tannyli on 2021/8/31.
 * Copyright (c) 2021 TME. All rights reserved.
 */
class App : Application() {


    override fun onCreate() {
        super.onCreate()
        init(this.applicationContext)
    }

    companion object {
        private const val TAG = "App"


        fun init(context: Context) {
            Log.i(TAG, "init Application")
            OpenApiSDK.init(
                context.applicationContext,
                MustInitConfig.APP_ID,
                MustInitConfig.APP_KEY,
                DeviceUtils.getAndroidID(),
                false
            )
        }
    }
}