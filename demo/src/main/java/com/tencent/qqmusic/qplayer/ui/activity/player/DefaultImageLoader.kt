package com.tencent.qqmusic.qplayer.ui.activity.player

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.playerui.PlayerStyleManager
import com.tencent.qqmusic.openapisdk.playerui.loader.IImageLoader
import com.tencent.qqmusic.openapisdk.playerui.loader.ImageLoaderListener
import com.tencent.qqmusic.qplayer.playerui.R

class DefaultImageLoader : IImageLoader.Stub() {

    override fun loadImage(url: String, listener: ImageLoaderListener?) {
        Glide.with(UtilContext.getApp()).asBitmap().load(url).into(object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(p0: Bitmap, p1: Transition<in Bitmap>?) {
                val bitmapDrawable = BitmapDrawable(p0)
                listener?.onLoaderComplete(url, null, p0)
                MLog.i("PlayerNewActivity", "loadAlbum onResourceReady $bitmapDrawable")
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                listener?.onLoaderFailed(url, null, ContextCompat.getDrawable(UtilContext.getApp(), R.drawable.qqmusic_default_album))
            }
        })
    }

    override fun loadImage(imageView: ImageView, url: String, listener: ImageLoaderListener?) {
        Glide.with(imageView.context).load(url).into(imageView)
    }
}