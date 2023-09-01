package com.tencent.qqmusic.qplayer.utils

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.model.SongInfo
import java.math.BigDecimal

/**
 * Created by tannyli on 2022/10/25.
 * Copyright (c) 2022 TME. All rights reserved.
 */
object UiUtils {

    fun getFormatAccessLabel(info: SongInfo?, quality: Int): String {
        info ?: return ""
        val default = ""
        val access = Global.getPlayerModuleApi().getAccessByQuality(info, quality) ?: return default
        val sb = StringBuilder()
        if (access.vip) {
            sb.append("vip").append("•")
        }
        if (!access.vip && access.hugeVip) {
            sb.append("SuperVip").append("•")
        }
        if (access.vipLongAudio) {
            sb.append("vipAudio").append("•")
        }
        if (access.payTrack) {
            sb.append("payTrack").append("•")
        }
        if (access.payAlbum) {
            sb.append("payAlbum").append("•")
        }
        if (sb.isNotEmpty()) {
            sb.deleteAt(sb.length - 1)
            return "[${sb.toString()}]"
        }
        return default
    }

    fun getFormatSize(sizeByte: Long?): String {
        if (sizeByte == null) return "(0MB)"
        val size = sizeByte.toDouble() / 1024 / 1024
        val big = BigDecimal(size).setScale(2,BigDecimal.ROUND_HALF_UP).toDouble()
        return "(${big}MB)"
    }

    fun showToast(msg: String) {
        Toast.makeText(UtilContext.getApp(), msg, Toast.LENGTH_SHORT).show()
    }

    fun isStrInt(str: String): Boolean {
        return str.toIntOrNull() != null
    }

    fun getDisplayWidth(context: Context): Int {
        val configuration = context.resources.configuration
        val screenWidth = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            context.resources.displayMetrics.heightPixels
        } else {
            context.resources.displayMetrics.widthPixels
        }
        return screenWidth
    }

    fun getDisplayHeight(context: Context): Int {
        val configuration = context.resources.configuration
        val screenWidth = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            context.resources.displayMetrics.widthPixels
        } else {
            context.resources.displayMetrics.heightPixels
        }
        return screenWidth
    }

    fun dp2pxf(dipValue: Float): Float {
        return dipValue * UtilContext.getApp().resources.displayMetrics.density + 0.5f
    }

    fun px2dp(pxValue: Float): Int {
        return (pxValue / UtilContext.getApp().resources.displayMetrics.density + 0.5f).toInt()
    }

}