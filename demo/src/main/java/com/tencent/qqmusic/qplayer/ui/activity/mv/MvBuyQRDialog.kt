package com.tencent.qqmusic.qplayer.ui.activity.mv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.ui.activity.login.WebViewActivity
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusic.qplayer.utils.UiUtils.generateQRCode
import kotlinx.coroutines.delay

object MvBuyQRDialog {

    fun showQRCodeDialog(context: Context, qrCodeContent: String?, block: (() -> Unit)?) {
        AppScope.launchUI {
            // 生成二维码图片
            val qrCodeBitmap = generateQRCode(qrCodeContent)
            // 创建AlertDialog
            val builder = AlertDialog.Builder(context)
            builder.setTitle("二维码展示")
            builder.setOnDismissListener {
                block?.invoke()
            }
            // 设置AlertDialog的布局
            val qrCodeImageView = ImageView(context)
            qrCodeImageView.setImageBitmap(qrCodeBitmap)
            qrCodeImageView.setOnClickListener {
                showTextDialog(context,"链接",qrCodeContent?:"")
            }
            builder.setView(qrCodeImageView)
            builder.setMessage("p.s. 点击二维码查看链接")
            qrCodeContent?.let {
                builder.setNeutralButton("复制链接") { _, _ ->
                    /* 复制操作 */
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("链接", it)
                    clipboard.setPrimaryClip(clip)
                    UiUtils.showToast("链接已复制")
                }
                builder.setPositiveButton("打开网页") { _, _ ->
                    /* 打开网页 */
                    WebViewActivity.start(context, it)
                }
            }
            // 显示AlertDialog
            val dialog = builder.create()
            dialog.show()
        }
    }

    fun showTextDialog(context: Context, title: String? = null, message: String, autoCloseMs: Long?=null) {
        AppScope.launchUI {
            val dialogBuilder = AlertDialog.Builder(context).apply {
                setTitle(title)
                setMessage(message)
            }

            val dialog = dialogBuilder.create().apply {
                    // 启用关闭功能
                    setCancelable(true)
                    show()
                }
            autoCloseMs?.let {
                if (it>0){
                    delay(it)
                    // 关闭dialog
                    dialog.dismiss()
                }
            }
        }
    }

}


