package com.tencent.qqmusic.qplayer.ui.activity.mv

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.utils.UiUtils.generateQRCode

object MvBuyQRDialog {

    fun showQRCodeDialog(context: Context, qrCodeContent: String?, block: (() -> Unit)?) {
        AppScope.launchUI {
            // 生成二维码图片
            val qrCodeBitmap = generateQRCode(qrCodeContent)

            // 创建AlertDialog
            val builder = AlertDialog.Builder(context)
            builder.setTitle("二维码展示")
            builder.setPositiveButton("关闭") { dialog, _ ->
                dialog?.dismiss()
                block?.invoke()
            }

            // 设置AlertDialog的布局
            val qrCodeImageView = ImageView(context)
            qrCodeImageView.setImageBitmap(qrCodeBitmap)
            builder.setView(qrCodeImageView)

            // 显示AlertDialog
            val dialog = builder.create()
            dialog.show()
        }
    }


}


