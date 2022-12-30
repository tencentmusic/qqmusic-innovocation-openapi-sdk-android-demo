package com.tencent.qqmusic.qplayer.utils

/**
 * Created by tannyli on 2022/10/25.
 * Copyright (c) 2022 TME. All rights reserved.
 */
object UiUtils {

    fun getFormatSize(sizeByte: Int?): String {
        if (sizeByte == null) return "(0MB)"
        return "(${sizeByte / 1024 / 1024}MB)"
    }


}