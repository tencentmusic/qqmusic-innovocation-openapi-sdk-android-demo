package com.tencent.qqmusic.qplayer.ui.activity.player.lyric

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import com.tencent.qqmusic.openapisdk.playerui.LyricStyleManager
import com.tencent.qqmusic.openapisdk.playerui.viewmode.PlayerViewModel
import com.tencent.qqmusic.openapisdk.playerui.viewmode.ViewportSize
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.BaseActivity
import com.tencent.qqmusic.qplayer.ui.activity.player.CustomPlayerViewMode
import com.tencent.qqmusic.qplayer.ui.activity.player.widget.KTVLyricStyleWidget

/**
 * Created by tannyli on 2025/11/1.
 * Copyright (c) 2025 TME. All rights reserved.
 */
class PlayerImmersionLyricActivity: BaseActivity() {

    val viewModel by lazy { ViewModelProvider(this)[PlayerViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_immersion_lyric)

        if (!LyricStyleManager.isLyricKTVStyle()) {
            // 非KTV歌词样式，直接退出
            finish()
            return
        }

        bindWidget(
            KTVLyricStyleWidget(
                viewModel,
                findViewById<ViewGroup>(R.id.lyric_fragment_container)
            )
        )

    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

}