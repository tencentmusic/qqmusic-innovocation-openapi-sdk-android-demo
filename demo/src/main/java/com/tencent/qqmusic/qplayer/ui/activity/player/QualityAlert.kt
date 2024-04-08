package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
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
            PlayerEnums.Quality.EXCELLENT,
            PlayerEnums.Quality.GALAXY
        )

    val qualityOrderString =
        arrayOf(
            "LQ",
            "STANDARD",
            "HQ",
            "SQ",
            "DOLBY",
            "HIRES",
            "EXCELLENT",
            "GALAXY"
        )

    fun showQualityAlert(activity: Activity, isDownload: Boolean, setBlock: (Int)->Int, refresh: (Int)->Unit) {
        val curSong = OpenApiSDK.getPlayerApi().getCurrentSongInfo()
        val stringArray = qualityOrderString.map {
            val quality = qualityOrder.getOrNull(qualityOrderString.indexOf(it)) ?: qualityOrder[0]
            val accessStr = UiUtils.getFormatAccessLabel(curSong, quality, isDownload)
            when (it) {
                "LQ" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeLQ()?.toLong()) + accessStr
                }
                "STANDARD" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeStandard()?.toLong()) + accessStr
                }
                "HQ" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeHQ()?.toLong()) + accessStr
                }
                "SQ" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeSQ()?.toLong()) + accessStr
                }
                "DOLBY" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeDolby()?.toLong()) + accessStr
                }
                "HIRES" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeHiRes()?.toLong()) + accessStr
                }
                "EXCELLENT" -> {
                    if (isDownload) {
                        "臻品音质2.0 - 不支持下载"
                    } else {
                        "臻品音质2.0$accessStr"
                    }
                }
                "GALAXY" -> {
                    if (curSong?.isGalaxyEffectType() == true) {
                        "臻品全景声" + accessStr
                    } else {
                        "臻品全景声" + UiUtils.getFormatSize(curSong?.getSizeGalaxy()?.toLong()) + accessStr
                    }
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
                        PlayDefine.PlayError.PLAY_ERR_NEED_SUPER_VIP -> "需要超级会员"
                        PlayDefine.PlayError.PLAY_ERR_NEED_PAY_ALBUM -> "需要专辑付费"
                        PlayDefine.PlayError.PLAY_ERR_NEED_PAY_TRACK -> "需要单曲付费"
                        PlayDefine.PlayError.PLAY_ERR_NEED_VIP_LONG_AUDIO -> "需要听书会员"
                        else -> "切换歌曲品质失败, ret=$ret"
                    }
                    activity.runOnUiThread {
                        if (!isDownload) {
                            Toast
                                .makeText(activity, msg, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }.show()
    }
}