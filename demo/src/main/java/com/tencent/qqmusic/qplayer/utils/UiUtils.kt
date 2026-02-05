package com.tencent.qqmusic.qplayer.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.openapisdk.model.ProfitInfo
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.openapisdk.model.SuperQualityType
import com.tencent.qqmusic.openapisdk.model.VipType
import com.tencent.qqmusic.playerinsight.util.coverErrorCode
import com.tencent.qqmusic.qplayer.App
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.core.player.proxy.SPBridgeProxy
import com.tencent.qqmusic.qplayer.core.report.PlayErr
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerNewActivity
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Created by tannyli on 2022/10/25.
 * Copyright (c) 2022 TME. All rights reserved.
 */
object UiUtils {


    private val sharedPreferences: SharedPreferences? = try {
        App.context.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        QLog.e("DebugScreen", "getSharedPreferences error e = ${e.message}")
        null
    }

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
        if (access.iotVip) {
            sb.append("iotVip").append("•")
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
        if (access.unionVip) {
            sb.append("unionVip").append("•")
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
        val big = BigDecimal(size).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
        return "(${big}MB)"
    }

    fun showToast(msg: String, isLong: Boolean = false) {
        AppScope.launchUI {
            Toast.makeText(UtilContext.getApp(), msg, if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }

    fun showPlayErrToast(errCode:Int, currSongInfo: SongInfo?=null){
        when(errCode){
            PlayDefine.PlayError.PLAY_ERR_NONE -> return
            PlayDefine.PlayError.PLAY_ERR_CANNOT_PLAY -> {
                val songInfo = currSongInfo?: OpenApiSDK.getPlayerApi().getCurrentSongInfo()
                val msg = if(songInfo?.unplayableMsg.isNullOrEmpty()){
                    "播放错误:$errCode, ${coverErrorCode(errCode)}"
                }else{
                    "播放错误:$errCode, ${songInfo?.unplayableMsg}"
                }
                showToast(msg = msg)
            }
            else -> {
                showToast(msg = "播放错误:$errCode, ${coverErrorCode(errCode)}")
            }
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
                R.drawable.hq_icon
            }

            PlayerEnums.Quality.SQ, PlayerEnums.Quality.SQ_SR -> {
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
            PlayerEnums.Quality.DTSC -> R.drawable.action_icon_dtsc
            PlayerEnums.Quality.DTSX -> R.drawable.action_icon_dtsx
            PlayerEnums.Quality.VOYAGE -> R.drawable.voyage
            PlayerEnums.Quality.CUSTOM_QUALITY_1 -> R.drawable.icon_quality_custom1
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

    fun getTodayTimestamps(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // 获取今天的开始时间戳（00:00:00）
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDayTimestamp = calendar.timeInMillis

        // 获取今天的结束时间戳（23:59:59）
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999) // 也可以设置为 999 毫秒
        val endOfDayTimestamp = calendar.timeInMillis

        return Pair(startOfDayTimestamp, endOfDayTimestamp)
    }

    fun getTimestampsForDaysAgo(days: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        // 获取今天的结束时间戳（23:59:59.999）
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTimestamp = calendar.timeInMillis

        // 获取指定天数前的起始时间戳（00:00:00）
        calendar.add(Calendar.DAY_OF_YEAR, -days) // 回退指定天数
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTimestamp = calendar.timeInMillis

        return Pair(startTimestamp, endTimestamp)
    }

    fun getUseNewPlayPageValue(): Boolean {
        return sharedPreferences?.getBoolean("newPlayerPage", false) ?: false
    }

    fun setUseNewPlayPage(new: Boolean) {
        sharedPreferences?.edit()?.putBoolean("newPlayerPage", new)?.apply()
    }

    fun gotoPlayerPage() {
        val newPage = getUseNewPlayPageValue()
        if (newPage) {
            UtilContext.getApp().startActivity(Intent(UtilContext.getApp(), PlayerNewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            UtilContext.getApp().startActivity(Intent(UtilContext.getApp(), PlayerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun setCurSoundEffectIsAI(isAI: Boolean) {
        sharedPreferences?.edit()?.putBoolean("curSoundEffectIsAI", isAI)?.apply()
    }

    fun getCurSoundEffectIsAI() = sharedPreferences?.getBoolean("curSoundEffectIsAI", false) ?: false

    fun timestampToTime(timestamp: Long): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val instant = Instant.ofEpochSecond(timestamp)
            val formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault()) // 使用系统时区
            formatter.format(instant)
        } else {
            timestamp.toString()
        }
    }

    fun getFormatNumber(number: Number?): String {
        if (number == null) return number.toString()
        val num = number.toDouble()
        return when {
            num < 10000 -> num.toString()
            num < 100_000_000 -> format(num, 10_000, "万")
            num < 1_000_000_000_000 -> format(num, 100_000_000, "亿")
            else -> format(num, 1_000_000_000_000, "万亿")
        }
    }

    fun getVipText(vipType: VipType): String{
        return when(vipType){
            VipType.GREEN_VIP -> "豪华绿钻"
            VipType.SUPER_VIP -> "超级会员"
            VipType.NONE -> "普通用户"
        }
    }

    private fun format(num: Double, divisor: Long, suffix: String): String {
        val divided = num / divisor
        return if (divided % 1.0 == 0.0) {
            "${divided.toInt()}$suffix"
        } else {
            "%.1f$suffix".format(divided)
        }
    }
    fun Int.getQualityName(): String {
        return when (this) {
            Quality.LQ->"低品质"
            Quality.STANDARD -> "标准"
            Quality.HQ -> "高品质"
            Quality.SQ -> "SQ"
            Quality.SQ_SR -> "SQ省流版"
            Quality.HIRES -> "Hires"
            Quality.EXCELLENT -> "臻品2.0"
            Quality.GALAXY -> "臻品全景声"
            Quality.DOLBY -> "杜比全景声"
            Quality.WANOS -> "Wanos"
            Quality.VOCAL_ACCOMPANY -> "伴唱"
            Quality.MASTER_TAPE -> "臻品母带"
            Quality.MASTER_SR -> "臻品母带省流版"
            Quality.VINYL -> "黑胶"
            Quality.DTSC -> "DTSC音质"
            Quality.DTSX -> "DTSX音质"
            Quality.VOYAGE -> "臻品乐航"
            Quality.CUSTOM_QUALITY_1 -> "定制音质1"
            else -> "未知音质->Quality=${this}"
        }
    }

    fun getSuperQualityTypeName(type:Int):String {
        return when (type) {
            SuperQualityType.QUALITY_TYPE_EXCELLENT->"臻品音质权益"
            SuperQualityType.QUALITY_TYPE_DOLBY->"杜比权益"
            SuperQualityType.QUALITY_TYPE_GALAXY->"臻品全景声权益"
            SuperQualityType.QUALITY_TYPE_MASTERTAPE->"臻品母带权益"
            SuperQualityType.QUALITY_TYPE_ACCOM->"伴唱权益"
            SuperQualityType.QUALITY_TYPE_WANOS->"Wanos权益"
            SuperQualityType.QUALITY_TYPE_AI_LYRIC->"AI歌词背景"
            SuperQualityType.QUALITY_TYPE_VOYAGE->"臻品乐航"
            SuperQualityType.QUALITY_TYPE_CUSTOM_QUALITY_1->"定制音质1"
            else -> "未知音质->$type"
        }
    }

    fun getProfitTypeName(type:Int):String {
        return when (type) {
            ProfitInfo.QUALITY_TYPE_EXCELLENT->"臻品音质权益"
            ProfitInfo.QUALITY_TYPE_DOLBY->"杜比权益"
            ProfitInfo.QUALITY_TYPE_GALAXY->"臻品全景声权益"
            ProfitInfo.QUALITY_TYPE_MASTERTAPE->"臻品母带/省流权益"
            ProfitInfo.QUALITY_TYPE_ACCOM->"伴唱权益"
            ProfitInfo.MUSIC_THERAPY_TYPE->"疗愈权益"
            ProfitInfo.MUSIC_VOYAGE->"臻品乐航"
            ProfitInfo.SOUND_EFFECT_TYPE->"音效权益"
            else -> "无试听->$type"
        }
    }
}