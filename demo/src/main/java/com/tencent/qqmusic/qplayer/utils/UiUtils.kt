package com.tencent.qqmusic.qplayer.utils

import android.widget.Toast
import com.tencent.qqmusic.innovation.common.util.UtilContext

/**
 * Created by tannyli on 2022/10/25.
 * Copyright (c) 2022 TME. All rights reserved.
 */
object UiUtils {

    fun getFormatSize(sizeByte: Int?): String {
        if (sizeByte == null) return "(0MB)"
        return "(${sizeByte / 1024 / 1024}MB)"
    }

    fun showToast(msg: String) {
        Toast.makeText(UtilContext.getApp(), msg, Toast.LENGTH_SHORT).show()
    }

}