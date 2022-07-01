package com.tencent.qqmusic.qplayer.ui.activity

//
// Created by tylertan on 2021/12/3
// Copyright (c) 2021 Tencent. All rights reserved.
//

object MustInitConfig {

    const val APP_ID = ""
    const val APP_KEY = ""
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

}