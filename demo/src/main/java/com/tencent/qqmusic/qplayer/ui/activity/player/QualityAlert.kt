package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ListAdapter
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusic.qplayer.utils.hasWanos
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

    var qualityOrderString =
        arrayOf(
            "LQ",
            "STANDARD",
            "HQ",
            "SQ",
            "DOLBY",
            "HIRES",
            "EXCELLENT",
            "GALAXY",
        )

    fun showQualityAlert(activity: Activity, isDownload: Boolean, setBlock: (Int)->Int, refresh: (Int)->Unit) {
        val curSong = OpenApiSDK.getPlayerApi().getCurrentSongInfo()
        if (curSong?.hasWanos() == true) {
            qualityOrderString = arrayOf("WANOS")
        }
        val stringArray = qualityOrderString.map {
            val quality = qualityOrder.getOrNull(qualityOrderString.indexOf(it)) ?: qualityOrder[0]
            val accessStr = UiUtils.getFormatAccessLabel(curSong, quality, isDownload)
            val tryPlayQualityLabel = if (OpenApiSDK.getPlayerApi().canTryOpenQuality(curSong, quality)) {
                "-可试听"
            } else ""
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
                    it + UiUtils.getFormatSize(curSong?.getSizeDolby()?.toLong()) + accessStr + tryPlayQualityLabel
                }
                "HIRES" -> {
                    it + UiUtils.getFormatSize(curSong?.getSizeHiRes()?.toLong()) + accessStr
                }
                "EXCELLENT" -> {
                    if (isDownload) {
                        "臻品音质2.0 - 不支持下载"
                    } else {
                        "臻品音质2.0-$accessStr$tryPlayQualityLabel"
                    }
                }
                "GALAXY" -> {
                    if (curSong?.isGalaxyEffectType() == true) {
                        "臻品全景声-$accessStr$tryPlayQualityLabel"
                    } else {
                        "臻品全景声" + UiUtils.getFormatSize(curSong?.getSizeGalaxy()?.toLong()) + accessStr + tryPlayQualityLabel
                    }
                }
                "WANOS" -> {
                    if (isDownload) {
                        "WANOS - 不支持下载"
                    } else {
                        "WANOS $accessStr"
                    }
                }
                else -> {
                    it
                }
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setAdapter(CustomArrayAdapter(activity, stringArray, curSong)) { _, which ->
                thread {
                    val nextQuality = qualityOrder.getOrNull(which)
                        ?: PlayerEnums.Quality.LQ
                    val ret = setBlock(nextQuality)
                    val msg = when (ret) {
                        PlayDefine.PlayError.PLAY_ERR_NONE -> {
                            refresh(nextQuality)
                            "切换歌曲品质成功"
                        }
                        PlayDefine.PlayError.PLAY_ERR_DEVICE_NO_SUPPORT -> if (nextQuality == Quality.DOLBY) "设备不支持杜比" else "设备不支持臻品2.0"
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

    class CustomArrayAdapter(context: Context, private val items: List<String>, val songInfo: SongInfo?) : ArrayAdapter<String>(context, android.R.layout.select_dialog_item, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val textView = view.findViewById<TextView>(android.R.id.text1)
            if (songInfo == null || items.size != qualityOrder.size) {
                textView.setTextColor(context.resources.getColor(R.color.text_black))
            } else {
                val quality = qualityOrder[position]
                val hasQuality = OpenApiSDK.getPlayerApi().getSongHasQuality(songInfo, quality)
                if (!hasQuality) {
                    textView.setTextColor(context.resources.getColor(R.color.text_gray)) // Set your color here
                } else {
                    textView.setTextColor(context.resources.getColor(R.color.text_black)) // Set your color here
                }
            }
            return view
        }
    }
}