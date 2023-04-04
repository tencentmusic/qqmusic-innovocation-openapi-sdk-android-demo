package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlin.concurrent.thread

object QualityAlert {
    private const val TAG = "QualityAlert"
    val qualityOrder =
        mutableListOf(
            PlayerEnums.Quality.LQ,
            PlayerEnums.Quality.STANDARD,
            PlayerEnums.Quality.HQ,
            PlayerEnums.Quality.SQ,
            PlayerEnums.Quality.DOLBY,
            PlayerEnums.Quality.HIRES,
            PlayerEnums.Quality.EXCELLENT
        )

    val qualityOrderString =
        arrayOf(
            "LQ",
            "STANDARD",
            "HQ",
            "SQ",
            "DOLBY",
            "HIRES",
            "EXCELLENT"
        )

    fun showQualityAlert(activity: Activity, setBlock: (Int)->Int, refresh: (Int)->Unit) {
        val curSong = Global.getPlayerModuleApi().getCurrentSongInfo()
        val stringArray = qualityOrderString.map {
            when (it) {
                "LQ" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeLQ())
                }
                "STANDARD" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeStandard())
                }
                "HQ" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeHQ())
                }
                "SQ" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeSQ())
                }
                "DOLBY" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeDolby())
                }
                "HIRES" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeHiRes())
                }
                "EXCELLENT" -> {
                    "臻品音质2.0"
                }
                else -> {
                    it
                }
            }
        }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setItems(stringArray) { _, which ->
                thread {
                    val nextQuality = qualityOrder.getOrNull(which)
                        ?: PlayerEnums.Quality.LQ
                    val ret = setBlock(nextQuality)
                    val msg = when (ret) {
                        PlayDefine.PlayError.PLAY_ERR_NONE -> {
                            refresh(nextQuality)
                            "切换歌曲品质成功"
                        }
                        PlayDefine.PlayError.PLAY_ERR_DEVICE_NO_SUPPORT -> "设备不支持杜比"
                        PlayDefine.PlayError.PLAY_ERR_NO_QUALITY -> "没有对应音质"
                        PlayDefine.PlayError.PLAY_ERR_PLAYER_ERROR -> "播放器异常"
                        PlayDefine.PlayError.PLAY_ERR_NEED_VIP -> "需要VIP"
                        PlayDefine.PlayError.PLAY_ERR_CANNOT_PLAY -> "歌曲不能播放"
                        PlayDefine.PlayError.PLAY_ERR_NONETWORK -> "无网络"
                        PlayDefine.PlayError.PLAY_ERR_UNSUPPORT -> "试听歌曲无法切换音质"
                        else -> "切换歌曲品质失败, ret=$ret"
                    }
                    activity.runOnUiThread {
                        Toast
                            .makeText(activity, msg, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }.show()
    }
}