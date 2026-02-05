package com.tencent.qqmusic.qplayer.ui.activity.mv

import android.app.Activity
import com.tencent.qqmusic.edgemv.data.MediaQuality
import com.tencent.qqmusic.edgemv.data.MediaResDetail
import com.tencent.qqmusic.edgemv.data.UserIdentity
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusic.utils.MediaScope

object MediaQualityDialog {
    val qualityMap = mutableMapOf<MediaQuality, String>().apply {
        put(MediaQuality.LQ, "标清 360P")
        put(MediaQuality.HQ, "高清 480P")
        put(MediaQuality.SQ, "超清 720P")
        put(MediaQuality.BR_1080, "蓝光 1080P")
        put(MediaQuality.HIGH_1080, "1080_高刷")
        put(MediaQuality.NORMAL_4K, "4K 超清")
        put(MediaQuality.EXCELLENT, "臻品视听")
        put(MediaQuality.DOLBY_4K, "杜比视界")
    }


    fun showQualityAlert(
        activity: Activity,
        info: MediaResDetail?,
        supportQualityList: List<MediaQuality>,
        block: ((MediaQuality?) -> Unit)?
    ) {
        val qualityList = qualityMap.filter { supportQualityList.contains(it.key) }

        val list = qualityList.map {
            val quality = it.key
            val tip = getUserIdentifier(quality, info)
            it.value + UiUtils.getFormatSize(info?.getQualitySize(quality)?.toLong()) + if (tip.isNullOrBlank()) "" else "[$tip]"
        }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setItems(list) { _, which ->
                AppScope.launchIO {
                    block?.invoke(qualityList.keys.toList()[which])
                }
            }.setTitle("选择需要的清晰度").show()
    }


    private fun getUserIdentifier(quality: MediaQuality, info: MediaResDetail?): String? {
        val identity = info?.getQualityIdentity(quality)
        return when (identity) {
            UserIdentity.NORMAL-> return "普通"
            UserIdentity.VIP -> return "豪华绿钻"
            UserIdentity.IOT_VIP -> return "IOT会员"
            UserIdentity.SUPER_VIP -> return "超会"
            UserIdentity.PAY_FOR_MEDIA -> return "购买"
            else -> null

        }
    }

}