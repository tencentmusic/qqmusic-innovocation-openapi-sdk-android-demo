package com.tencent.qqmusic.qplayer.ui.activity.main

import android.os.Bundle
import androidx.activity.ComponentActivity

class PlayProcessReportTestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.let {
            handleReportInfo(it.getBundleExtra("extra"))
        }
    }

    private fun handleReportInfo(bundle: Bundle?) {
        bundle?.let {
            when (bundle.getString("key")) {
                "crash" -> {
                    Thread(Runnable {
                        Thread.sleep(1000)
                        throw Exception("crash in play process")
                    }).start()
                }
                else -> {}
            }
        }
    }
}