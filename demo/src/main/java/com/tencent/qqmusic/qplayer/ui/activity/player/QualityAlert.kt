package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.tencent.qqmusic.openapisdk.business_common.utils.Utils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlin.concurrent.thread
import kotlin.jvm.internal.Intrinsics.Kotlin

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
            PlayerEnums.Quality.GALAXY,
            PlayerEnums.Quality.MASTER_TAPE,
            PlayerEnums.Quality.MASTER_SR,
        )

    var qualityOrderString = arrayOf("")

    private fun initQualityOrderString() {
        qualityOrderString = arrayOf(
                "LQ",
                "STANDARD",
                "HQ",
                "SQ",
                "DOLBY",
                "HIRES",
                "EXCELLENT",
                "GALAXY",
                "MASTER_TAPE",
                "MASTER_SR",
            )
    }

    fun showQualityAlert(activity: Activity, isDownload: Boolean, setBlock: (Int)->Int, refresh: (Int)->Unit) {
        initQualityOrderString()
        val curSong = OpenApiSDK.getPlayerApi().getCurrentSongInfo()
        var realQuality: Int? = null
        curSong?.apply {
            if (OpenApiSDK.getPlayerApi().getSongHasQuality(curSong, Quality.WANOS)) {
                qualityOrderString = arrayOf("WANOS")
                realQuality = Quality.WANOS
            } else if (OpenApiSDK.getPlayerApi().getSongHasQuality(curSong, Quality.VINYL)) {
                qualityOrderString = arrayOf("黑胶")
                realQuality = Quality.VINYL
            }
        }

        val stringArray = qualityOrderString.map {
            val quality = qualityOrder.getOrNull(qualityOrderString.indexOf(it)) ?: kotlin.run {
                realQuality ?: qualityOrder[2]
            }

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
                "MASTER_TAPE" -> {
                    "臻品母带 "+ UiUtils.getFormatSize(curSong?.getSongQuality(Quality.MASTER_TAPE)?.size?.toLong()) + accessStr + tryPlayQualityLabel
                }
                "MASTER_SR" -> {
                    "臻品母带省流版"+ UiUtils.getFormatSize(curSong?.getSongQuality(Quality.MASTER_SR)?.size?.toLong()) + accessStr + tryPlayQualityLabel
                }
                else -> {
                    it
                }
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setAdapter(CustomArrayAdapter(activity, stringArray, curSong)) { _, which ->
                thread {
                    val nextQuality = if (realQuality != null) {
                        realQuality
                    } else {
                        qualityOrder.getOrNull(which)
                    } ?: PlayerEnums.Quality.LQ

                    val ret = setBlock(nextQuality)
                    val msg = when (ret) {
                        PlayDefine.PlayError.PLAY_ERR_NONE -> {
                            refresh(nextQuality)
                            "切换歌曲品质成功"
                        }
                        PlayDefine.PlayError.PLAY_ERR_DEVICE_NO_SUPPORT -> "设备不支持${Utils.qualityToString(nextQuality)} 音质"
                        PlayDefine.PlayError.PLAY_ERR_NO_QUALITY -> "没有对应音质"
                        PlayDefine.PlayError.PLAY_ERR_PLAYER_ERROR -> "播放器异常"
                        PlayDefine.PlayError.PLAY_ERR_NEED_VIP -> "需要VIP"
                        PlayDefine.PlayError.PLAY_ERR_CANNOT_PLAY -> "歌曲不能播放"
                        PlayDefine.PlayError.PLAY_ERR_NONETWORK -> "无网络"
                        PlayDefine.PlayError.PLAY_ERR_UNSUPPORT,
                        PlayDefine.PlayError.PLAY_ERR_CAN_NOT_SET_CURRENT_QUALITY -> "不支持切换此音质"
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