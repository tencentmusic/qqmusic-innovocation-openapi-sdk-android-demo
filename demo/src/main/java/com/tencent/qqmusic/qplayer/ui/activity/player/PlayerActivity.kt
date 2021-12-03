package com.tencent.qqmusic.qplayer.ui.activity.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class PlayerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PlayerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlayerScreen(PlayerObserver)
        }
    }
}