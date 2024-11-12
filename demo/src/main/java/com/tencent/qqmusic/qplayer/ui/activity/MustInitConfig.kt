package com.tencent.qqmusic.qplayer.ui.activity

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.qplayer.BuildConfig
import com.tencent.qqmusic.qplayer.baselib.util.QLog

//
// Created by tylertan on 2021/12/3
// Copyright (c) 2021 Tencent. All rights reserved.
//

object MustInitConfig {
    private val sharedPreferences: SharedPreferences? =
        try {
            UtilContext.getApp().getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
            null
        }


    fun openStrictMode(): Boolean {
        return getAppCheckMode()
    }

    val APP_ID: String = ""
    val APP_KEY: String = ""
    const val QQ_APP_ID = ""
    const val WX_APP_ID = ""
    const val MATCH_ID = ""

    fun check() {
        val condition =
            APP_ID.isEmpty() || APP_KEY.isEmpty() || QQ_APP_ID.isEmpty() || WX_APP_ID.isEmpty() || MATCH_ID.isEmpty()
        assert(!condition) {
            "请先设置对应ID/Key值！"
        }
    }


    fun getAppCheckMode(): Boolean {
        return sharedPreferences?.getBoolean("app_id_mode", true) ?: true
    }

    fun setAppCheckMode(strick: Boolean) {
        sharedPreferences?.edit()?.putBoolean("app_id_mode", strick)?.apply()
    }

}