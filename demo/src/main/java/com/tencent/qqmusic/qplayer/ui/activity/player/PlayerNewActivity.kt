package com.tencent.qqmusic.qplayer.ui.activity.player

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import com.tencent.qqmusic.openapisdk.playerui.viewmode.PlayerViewModel
import com.tencent.qqmusic.openapisdk.playerui.viewmode.ViewportSize
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.BaseActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.widget.PlayerNewViewWidget
import com.tencent.qqmusic.qplayer.utils.UiUtils


/**
 * Created by silverfu on 2024/11/26.
 */
class PlayerNewActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        bindPlayerViewModel(CustomPlayerViewMode())
        bindWidget(PlayerNewViewWidget(getViewModel(), findViewById<ViewGroup>(R.id.main_view)))

        findViewById<ImageView>(R.id.bnt_player_style).setOnClickListener {
            startActivity(Intent(this, PlayerStyleActivity::class.java))
        }
        findViewById<ImageView>(R.id.bnt_old_player).setOnClickListener {
            UiUtils.setUseNewPlayPage(false)
            startActivity(Intent(this, PlayerActivity::class.java))
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    private fun viewPortSizeChange() {
        val wm = this.getSystemService(WINDOW_SERVICE) as WindowManager
        val width = wm.defaultDisplay.width
        val height = wm.defaultDisplay.height
        viewPortSizeChange(ViewportSize(width, height))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewPortSizeChange()
    }
}