package com.tencent.qqmusic.qplayer.utils

import android.app.Activity
import android.app.AlertDialog
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import com.tencent.qqmusic.qplayer.PrivacyHelper
import com.tencent.qqmusiccommon.SimpleMMKV
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by silverfu on 2024/2/20.
 */
object PrivacyManager {

    private const val KEY_LAST_PRIVACY_GRANT_TIME = "KEY_LAST_PRIVACY_GRANT_TIME"
    private val runnableList = CopyOnWriteArrayList<(() -> Unit)>()

    fun init(activity: Activity, callback: ((ok: Boolean) -> Unit)) {
        if (isGrant()) {
            callback.invoke(true)
        } else {
            val dialog = AlertDialog.Builder(activity).setTitle("隐私协议")
                .setMessage(Html.fromHtml(PrivacyHelper.getPrivacyText(activity)))
                .setNegativeButton("cancel") { dialog, which ->
                    dialog?.dismiss()
                    activity.finish()
                }.setPositiveButton("ok") { dialog, which ->
                    updateGrantTime()
                    doPrivacyEnd()
                    dialog?.dismiss()
                    callback.invoke(true)
                }.create()

            dialog.setOnShowListener {
                val textView: TextView? = dialog.findViewById(android.R.id.message)
                textView?.maxLines = Int.MAX_VALUE
                textView?.movementMethod = LinkMovementMethod.getInstance()
            }
            dialog.setCancelable(false)
            dialog.show()
        }
    }

    private fun doPrivacyEnd() {
        runnableList.forEach { it.invoke() }
        runnableList.clear()
    }

    fun delayPrivacyEnd(run: (() -> Unit)) {
        runnableList.add(run)
    }

     fun isGrant(): Boolean {
        val sp = SimpleMMKV.getCommonMMKV()
        val lastPrivacyGrantTime = sp.getLong(KEY_LAST_PRIVACY_GRANT_TIME, 0)
        return lastPrivacyGrantTime > PrivacyHelper.getPrivacyLastModifyTime()
    }

    fun updateGrantTime(){
        val sp = SimpleMMKV.getCommonMMKV()
        sp.putLong(KEY_LAST_PRIVACY_GRANT_TIME, System.currentTimeMillis() / 1000)
    }

}