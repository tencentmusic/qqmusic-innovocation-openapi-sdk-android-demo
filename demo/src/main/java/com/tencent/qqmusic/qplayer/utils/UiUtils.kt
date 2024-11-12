package com.tencent.qqmusic.qplayer.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import java.math.BigDecimal

/**
 * Created by tannyli on 2022/10/25.
 * Copyright (c) 2022 TME. All rights reserved.
 */
object UiUtils {

    fun getFormatAccessLabel(info: SongInfo?, quality: Int, isDownload: Boolean = false): String {
        info ?: return ""
        val default = ""
        val access = if (isDownload) {
            OpenApiSDK.getDownloadApi().getDownloadAccessByQuality(info, quality)
        } else {
            OpenApiSDK.getPlayerApi().getAccessByQuality(info, quality)
        }
        if (access == null) {
            return default
        }

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
        AppScope.launchUI {
            Toast.makeText(UtilContext.getApp(), msg, Toast.LENGTH_SHORT).show()
        }
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

    fun getQualityIcon(quality: Int?): Int {
        // 播放模式
        val icQuality: Int = when (quality) {
            PlayerEnums.Quality.HQ -> {
                R.drawable.action_icon_quality_hq
            }
            PlayerEnums.Quality.SQ -> {
                R.drawable.action_icon_quality_sq
            }
            PlayerEnums.Quality.STANDARD -> {
                R.drawable.action_icon_quality_standard
            }
            PlayerEnums.Quality.DOLBY -> {
                R.drawable.action_icon_dolby_quality
            }
            PlayerEnums.Quality.HIRES -> {
                R.drawable.action_icon_quality_hires
            }
            PlayerEnums.Quality.EXCELLENT -> {
                R.drawable.action_icon_excellent_quality
            }
            PlayerEnums.Quality.GALAXY -> {
                R.drawable.action_icon_galaxy_quality
            }
            PlayerEnums.Quality.VOCAL_ACCOMPANY -> {
                R.drawable.action_icon_quality_va
            }
            PlayerEnums.Quality.WANOS -> {
                R.drawable.acion_icon_quality_wanos
            }
            PlayerEnums.Quality.VINYL -> {
                R.drawable.action_icon_quality_vinyl
            }
            PlayerEnums.Quality.MASTER_TAPE, PlayerEnums.Quality.MASTER_SR -> {
                R.drawable.master_tape_icon
            }
            else -> {
                R.drawable.ic_lq
            }
        }
        return icQuality
    }


     fun generateQRCode(content: String?): Bitmap? {
         if (content.isNullOrEmpty()) {
             return null
         }
        val qrCodeWriter = QRCodeWriter()
        try {
            val bitMatrix: BitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}