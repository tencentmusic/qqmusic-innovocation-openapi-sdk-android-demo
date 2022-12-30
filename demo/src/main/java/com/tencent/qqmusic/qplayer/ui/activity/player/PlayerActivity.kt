package com.tencent.qqmusic.qplayer.ui.activity.player

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class PlayerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PlayerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PlayerObserver.currentSong == null) {
            Toast.makeText(this, "暂无正在播放的歌曲", Toast.LENGTH_SHORT).show()
        }

        setContent {
            PlayerScreen(PlayerObserver)
        }
    }
}