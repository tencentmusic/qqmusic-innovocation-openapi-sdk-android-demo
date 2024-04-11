package com.tencent.qqmusic.qplayer.ui.activity.mv

import android.app.Activity
import com.tencent.qqmusic.edgemv.data.MediaQuality
import com.tencent.qqmusic.edgemv.data.MediaResDetail
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


    fun showQualityAlert(activity: Activity, info: MediaResDetail?, supportQualityList: List<MediaQuality>, block: ((MediaQuality?) -> Unit)?) {
        val qualityList = qualityMap.filter { supportQualityList.contains(it.key) }

        val list = qualityList.map {
            it.value + UiUtils.getFormatSize(info?.getQualitySize(it.key)?.toLong())
        }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setItems(list) { _, which ->
                MediaScope.launchIO {
                    block?.invoke(qualityList.keys.toList()[which])
                }
            }.setTitle("选择需要的清晰度").show()
    }

}