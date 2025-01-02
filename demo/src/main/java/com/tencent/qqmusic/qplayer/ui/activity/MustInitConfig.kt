package com.tencent.qqmusic.qplayer.ui.activity

import android.text.TextUtils
import com.tencent.qqmusic.qplayer.BaseFunctionManager
import com.tencent.qqmusic.qplayer.BuildConfig

//
// Created by tylertan on 2021/12/3
// Copyright (c) 2021 Tencent. All rights reserved.
//

object MustInitConfig {
    val proxy = BaseFunctionManager.proxy

    val APP_ID: String
        get() = if (!TextUtils.isEmpty(BuildConfig.DEMO_APPID)) {
            BuildConfig.DEMO_APPID
        } else proxy.getAccount().appid


    val APP_KEY
        get() = if (!TextUtils.isEmpty(BuildConfig.DEMO_APPKEY)) {
            BuildConfig.DEMO_APPKEY
        } else proxy.getAccount().appKey


    fun getAppCheckMode(): Boolean {
        return proxy.getAppCheckMode()
    }

    fun setAppCheckMode(strick: Boolean) {
        proxy.setAppCheckMode(strick)
    }

}