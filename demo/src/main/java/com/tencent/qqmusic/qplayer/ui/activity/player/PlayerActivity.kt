package com.tencent.qqmusic.qplayer.ui.activity.player

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

class PlayerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PlayerActivity"
    }


    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PlayerObserver.currentSong == null) {
            Toast.makeText(this, "暂无正在播放的歌曲", Toast.LENGTH_SHORT).show()
        }
        setContent {
            MainPage()
        }
    }

    @SuppressLint("CoroutineCreationDuringComposition")
    @ExperimentalPagerApi
    @Composable
    fun MainPage() {
        val pagerState = rememberPagerState()
        val coroutineScope = rememberCoroutineScope()

        HorizontalPager(count = 2, state = pagerState) {
            when (it) {
                0 -> SongDetailPage(observer = PlayerObserver)
                else -> PlayerScreen(observer = PlayerObserver)
            }
        }
        coroutineScope.launch {
            pagerState.scrollToPage(1)
        }
    }


}