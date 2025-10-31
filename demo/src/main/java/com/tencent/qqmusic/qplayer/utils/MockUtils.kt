package com.tencent.qqmusic.qplayer.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.tencent.qqmusic.openapisdk.business_common.config.SongQualityManager
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.openapisdk.model.PayAccessInfo
import com.tencent.qqmusic.openapisdk.model.ProfitInfo
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.openapisdk.model.SuperQualityType
import com.tencent.qqmusic.qplayer.utils.UiUtils.getQualityName
import java.lang.reflect.Field
import java.lang.reflect.Modifier


/**
 * Created by tannyli on 2021/9/1.
 * Copyright (c) 2021 TME. All rights reserved.
 */
object MockUtils {

    private const val TAG = "MockUtils"

    val SongListOwn = "8146976003"
    val SongListIdOther = "7107159327"

    val songList = listOf(314818717L, 317968884L, 316868744L, 291130348L)
    val songId = 314818717L

    val radioId = 199

    val topListId = 26

    val albumId = 55085L
    val matchId = 0

    fun testFocus(context: Context) {
        AudioFocusChangeHelper(context).apply {
            requestFocus()
            audioFocusChangeListener = object : AudioFocusChangeHelper.AudioFocusChangeListener{
                override fun audioFocusLoss() {
                    Log.i(TAG, "audioFocusLoss")
                }

                override fun audioFocusLossTransient() {
                    Log.i(TAG, "audioFocusLossTransient")
                }

                override fun audioFocusLossTransientCanDuck() {
                    Log.i(TAG, "audioFocusLossTransientCanDuck")
                }

                override fun audioFocusGain(reason: Int) {
                    Log.i(TAG, "audioFocusGain reason:$reason")
                }

            }
        }
    }
}
val allQualityList = mutableListOf<Int>()
fun getAllQuality(): List<Int> {
    if (allQualityList.isNotEmpty()){ return allQualityList }
    val fields: Array<Field> = Quality::class.java.declaredFields
    val qualityList = mutableSetOf<Int>()
    for (field in fields) {
        if (field.type === Int::class.javaPrimitiveType && Modifier.isStatic(field.modifiers)) {
            try {
                qualityList.add(field.getInt(null))
            } catch (e: IllegalAccessException) {
                Log.i("getAllQuality", "error: $e")
            }
        }
    }
    allQualityList.clear()
    allQualityList.addAll(qualityList)
    return qualityList.toList().sorted()
}

val allSuperQuality = mutableListOf<Int>()
fun getAllSuperQualityList(): List<Int> {
    if (allSuperQuality.isNotEmpty()){ return allSuperQuality }
    val fields: Array<Field> = SuperQualityType::class.java.declaredFields
    val qualityList = mutableSetOf<Int>()
    for (field in fields) {
        if (field.type === Int::class.javaPrimitiveType && Modifier.isStatic(field.modifiers)) {
            Log.i("getAllSuperQualityList", "field: ${Modifier.toString(field.modifiers)}")
            try {
                qualityList.add(field.getInt(null))
            } catch (e: IllegalAccessException) {
                Log.i("getAllSuperQualityList", "error: $e")
            }
        }
    }
    allSuperQuality.clear()
    qualityList.remove(SuperQualityType.QUALITY_TYPE_ALL)
    allSuperQuality.addAll(qualityList.toList().sorted())
    return qualityList.toList().sorted()
}

val allProfitTypeList = mutableListOf<Int>()
fun getAllProfitList(): List<Int> {
    if (allProfitTypeList.isNotEmpty()){ return allProfitTypeList }
    val fields: Array<Field> = ProfitInfo::class.java.declaredFields
    val profitTypes = mutableSetOf<Int>()
    for (field in fields) {
        if (field.type === Int::class.javaPrimitiveType && Modifier.isStatic(field.modifiers)) {
            Log.i("getAllProfitList", "field: ${Modifier.toString(field.modifiers)}")
            try {
                profitTypes.add(field.getInt(null))
            } catch (e: IllegalAccessException) {
                Log.i("getAllProfitList", "error: $e")
            }
        }
    }
    allProfitTypeList.clear()
    allProfitTypeList.addAll(profitTypes.toList().sorted())
    return profitTypes.toList().sorted()
}

fun SongInfo.hasUrl(@Quality quality:Int): Boolean{
    return when (quality) {
        Quality.LQ -> this.hasLinkLQ()
        Quality.STANDARD -> this.hasLinkStandard()
        Quality.HQ -> this.hasLinkHQ()
        Quality.SQ -> this.hasLinkSQ()
        Quality.DOLBY -> this.hasLinkDolby()
        Quality.HIRES -> this.hasLinkHiRes()
        Quality.VOCAL_ACCOMPANY -> this.hasLinkVocalAccompany()
        Quality.EXCELLENT -> false
        Quality.GALAXY -> if (this.isGalaxyEffectType()) false else this.hasLinkGalaxy()
        Quality.WANOS,
        Quality.MASTER_TAPE,
        Quality.MASTER_SR,
        Quality.SQ_SR,
        Quality.DTSC,
        Quality.DTSX,
        Quality.VINYL -> {
            (this.getSongQuality(quality)?.url ?: "").isNotEmpty()
        }
        else -> (this.getSongQuality(quality)?.url ?: "").isNotEmpty()
    }
}

fun PayAccessInfo.getNeedVip(): String {
    return if (this.vip) {
        "绿钻"
    } else if (this.iotVip) {
        "iot会员"
    } else if (this.hugeVip){
        "超会"
    } else if (this.payAlbum) {
        "数专"
    } else if (this.payTrack) {
        "单曲付费"
    } else if (this.vipLongAudio) {
        "长音频会员"
    } else {
        "普通"
    }
}

@SuppressLint("DefaultLocale")
data class SongQualityInfo(val songInfo: SongInfo, @Quality  val quality: Int){
    val title = quality.getQualityName()
    val sizeMB=SongQualityManager.getSongQualitySize(songInfo,quality) / 1024f / 1024f // 音质大小MB
    val playRightSwitch= SongQualityManager.getSongCanPlayQuality(songInfo, quality) // switch权限
    val playRightUrl=SongQualityManager.getSongCanPlayQuality(songInfo, quality, true)  // url权限
    val playNeedVip = OpenApiSDK.getPlayerApi().getAccessByQuality(songInfo, quality)?.getNeedVip()
    val playHasUrl = songInfo.hasUrl(quality) // 后台实际下发链接
    val downloadRightSwitch = when(quality) {
        Quality.STANDARD -> songInfo.canDownloadNormal()
        Quality.HQ -> songInfo.canDownloadHQ()
        Quality.SQ -> songInfo.canDownloadSQ()
        Quality.DOLBY -> songInfo.canDownloadDolby()
        Quality.HIRES -> songInfo.canDownloadHiRes()
        Quality.GALAXY -> songInfo.canDownloadGalaxy()
        Quality.VINYL -> songInfo.canDownloadVinyl()
        else -> songInfo.canDownloadFile(quality)
    }
    val downloadNeedVip = SongQualityManager.getAccessByQuality(songInfo.action?.downloadAccess, quality)?.getNeedVip()
    val cacheNeedVip = SongQualityManager.getAccessByQuality(songInfo.action?.cacheAccess, quality)?.getNeedVip()

    val hasQuality = SongQualityManager.getSongHasQuality(songInfo,quality)

    override fun toString(): String {
        val text = mutableListOf<String>()
        if (hasQuality){
            text.add("${String.format("%.2f", sizeMB)}MB")
            text.add("权限判断:$playRightSwitch")
            text.add("链接判断:$playRightUrl")
            text.add("链接:${if(playHasUrl) "有" else "无"}")
        }else{
            text.add("无")
        }
        return text.joinToString(" |")
    }

    fun SongInfo.canDownloadFile(quality: Int): Boolean {
        val canDownload = when (quality) {
            Quality.STANDARD -> {
                canDownloadNormal()
            }

            Quality.HQ -> {
                canDownloadHQ()
            }

            Quality.SQ -> {
                canDownloadSQ()
            }

            Quality.DOLBY -> {
                canDownloadDolby()
            }

            Quality.HIRES -> {
                canDownloadHiRes()
            }

            Quality.GALAXY -> {
                canDownloadGalaxy()
            }

            Quality.MASTER_TAPE -> {
                canDownloadMasterTape()
            }
            Quality.VINYL -> {
                canDownloadVinyl()
            }
            Quality.DTSC,
            Quality.DTSX -> {
                canDownloadDts()
            }

            else -> {
                false
            }
        }
        return canDownload
    }
}