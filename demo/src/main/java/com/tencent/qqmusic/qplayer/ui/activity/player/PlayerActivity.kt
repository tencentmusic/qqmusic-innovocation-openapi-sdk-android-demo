package com.tencent.qqmusic.qplayer.ui.activity.player

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.tencent.qqmusic.qplayer.utils.DownloadTaskManager
import kotlinx.coroutines.launch

class PlayerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PlayerActivity"
    }


    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DownloadTaskManager.init()
        setContent {
            MainPage()
        }
    }

    @SuppressLint("CoroutineCreationDuringComposition")
    @ExperimentalPagerApi
    @Composable
    fun MainPage() {
        val activity = LocalContext.current as Activity

        val pagerState = rememberPagerState()
        val coroutineScope = rememberCoroutineScope()
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        val callback = remember {
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    coroutineScope.launch {
                        if (pagerState.currentPage != 1) {
                            pagerState.scrollToPage(1)
                        } else {
                            activity.finish()
                        }
                    }

                }
            }
        }
        DisposableEffect(key1 = Unit, effect = {
            dispatcher?.addCallback(callback)
            onDispose {
                callback.remove()
            }
        })


        HorizontalPager(count = 3, state = pagerState) {
            when (it) {
                0 -> SongDetailPage(observer = PlayerObserver)
                1 -> PlayerScreen(observer = PlayerObserver)
                2 -> PlayControlTestPage()
                else -> PlayerScreen(observer = PlayerObserver)
            }
        }
        coroutineScope.launch {
            if (pagerState.pageCount == 0) {
                pagerState.scrollToPage(0)
            } else {
                pagerState.scrollToPage(1)
            }
        }
    }


}