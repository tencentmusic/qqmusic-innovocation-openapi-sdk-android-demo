package com.tencent.qqmusic.qplayer.ui.activity.player

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.openapisdk.playerui.fxeffect.utils.BitmapAlgorithms
import com.tencent.qqmusic.openapisdk.playerui.fxeffect.utils.MagicColorUtil
import com.tencent.qqmusic.openapisdk.playerui.magiccolor.ImageViewUtils
import com.tencent.qqmusic.openapisdk.playerui.repository.IPlayerRepository
import com.tencent.qqmusic.openapisdk.playerui.utils.Utils
import com.tencent.qqmusic.openapisdk.playerui.viewmode.IPlayerInfoViewModel
import com.tencent.qqmusic.openapisdk.playerui.viewmode.IPlayerMagicColorViewModel
import com.tencent.qqmusic.openapisdk.playerui.viewmode.MagicColor
import com.tencent.qqmusic.openapisdk.playerui.viewmode.MusicBaseViewModel

/**
 * Created by silverfu on 2024/12/13.
 */
class CustomPlayerAlbumMagicColorViewModel(
    private val playerInfoViewMode: IPlayerInfoViewModel,
    private val playerRepository: IPlayerRepository
) : MusicBaseViewModel(), IPlayerMagicColorViewModel {

    companion object {
        const val TAG = "PlayerAlbumMagicColorViewModel"
    }

    override val defaultBackgroundColor: Int = Color.WHITE
    override val defaultForegroundColor: Int = Color.WHITE
    override val defaultHighlightColor: Int = Color.WHITE
    override val defaultProgressColor: Int = Color.WHITE
    private val defaultMagicColor = MagicColor(
        mapOf(
            MagicColor.KEY_BACKGROUND_COLOR to defaultBackgroundColor,
            MagicColor.KEY_HIGHLIGHT_COLOR to defaultHighlightColor,
            MagicColor.KEY_FOREGROUND_COLOR to defaultForegroundColor,
            MagicColor.KEY_PROGRESSBAR_COLOR to defaultProgressColor
        )
    )
    private val _magicColorLiveData = MutableLiveData(defaultMagicColor)

    override val magicColorLiveData: LiveData<MagicColor> get() = _magicColorLiveData

    private val playAlbumObserver = object : Observer<Result<Drawable?>> {
        override fun onChanged(t: Result<Drawable?>) {
            val albumDrawable = t.getOrNull()
            if (albumDrawable is BitmapDrawable) {
                val bitmap = ImageViewUtils.getBitmapFromDrawable(albumDrawable)
                val resizedBitmap = BitmapAlgorithms.resizedBitmap(bitmap, 10, 10)
                var result = MagicColorUtil.getLightMagicColorPair(resizedBitmap)
                result = MagicColorUtil.getAdjustDefaultPlayerLightMagicColorPair(result)
                val colorConfig = playerInfoViewMode.getPlayerStyle().styleConfig?.vinyl?.color
                val backgroundColor = result.first
                val backgroundColor2 = result.second

                val highlightColor = Utils.parseColor(colorConfig?.textHighLightColor, backgroundColor)
                val foregroundColor = Utils.parseColor(colorConfig?.textColor, defaultForegroundColor)
                val magicColor = Utils.parseColor(colorConfig?.magicColor, defaultForegroundColor)
                _magicColorLiveData.value = MagicColor(
                    mapOf(
                        MagicColor.KEY_BACKGROUND_COLOR to backgroundColor,
                        MagicColor.KEY_BACKGROUND_COLOR2 to backgroundColor2,
                        MagicColor.KEY_HIGHLIGHT_COLOR to highlightColor,
                        MagicColor.KEY_FOREGROUND_COLOR to foregroundColor,
                        MagicColor.KEY_PROGRESSBAR_COLOR to highlightColor,
                        MagicColor.KEY_FILL_BACKGROUND to magicColor,
                    )
                )
            } else {
                _magicColorLiveData.value = defaultMagicColor
            }
            MLog.i(TAG, "new magicColor ${_magicColorLiveData.value}")
        }
    }

    init {
        playerRepository.observableAlbumDrawable.observeForever(playAlbumObserver)
    }

    override fun clear() {
        super.clear()
        playerRepository.observableAlbumDrawable.removeObserver(playAlbumObserver)
    }
}