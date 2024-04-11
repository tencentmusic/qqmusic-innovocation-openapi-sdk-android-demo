package com.tencent.qqmusic.qplayer.ui.activity.mv

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.tencent.qqmusic.qplayer.baselib.util.AppScope

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

    private fun generateQRCode(content: String?): Bitmap? {
        val qrCodeWriter = QRCodeWriter()
        try {
            val bitMatrix: BitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}


