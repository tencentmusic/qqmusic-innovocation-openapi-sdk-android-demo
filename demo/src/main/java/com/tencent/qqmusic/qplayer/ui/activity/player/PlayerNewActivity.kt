package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import com.tencent.qqmusic.openapisdk.playerui.viewmode.PlayerViewModel
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.model.PlayerStyleData
import com.tencent.qqmusic.openapisdk.playerui.WallpaperStyleManager
import com.tencent.qqmusic.openapisdk.playerui.view.wallpaper.QQWallPaperService
import com.tencent.qqmusic.openapisdk.playerui.viewmode.ViewportSize
import com.tencent.qqmusic.qplayer.App
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.BaseActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.lyric.PlayerImmersionLyricStyleActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.widget.PlayerNewViewWidget
import com.tencent.qqmusic.qplayer.utils.UiUtils


/**
 * Created by silverfu on 2024/11/26.
 */
class PlayerNewActivity : BaseActivity() {

    val viewModel by lazy { ViewModelProvider(this)[PlayerViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        //获取全局的ViewModel
        bindWidget(PlayerNewViewWidget(viewModel, findViewById<ViewGroup>(R.id.main_view)))

        findViewById<ImageView>(R.id.bnt_player_style).setOnClickListener {
            startActivity(Intent(this, PlayerStyleActivity::class.java))
        }

        findViewById<Button>(R.id.bnt_immersion_lyric).setOnClickListener {
            startActivity(Intent(this, PlayerImmersionLyricStyleActivity::class.java))
        }

        findViewById<ImageView>(R.id.bnt_old_player).setOnClickListener {
            UiUtils.setUseNewPlayPage(false)
            startActivity(Intent(this, PlayerActivity::class.java))
        }
        findViewById<ImageButton?>(R.id.btn_back)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<Button>(R.id.bnt_set_wallpaper)?.setOnClickListener {
            WallpaperStyleManager.setStyle(PlayerStyleData())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun viewPortSizeChange() {
        val wm = this.getSystemService(WINDOW_SERVICE) as WindowManager
        val width = wm.defaultDisplay.width
        val height = wm.defaultDisplay.height
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewPortSizeChange()
    }
}