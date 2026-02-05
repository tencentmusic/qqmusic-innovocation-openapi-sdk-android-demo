package com.tencent.qqmusic.qplayer.ui.activity.musichall

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.qqmusic.qplayer.ui.activity.BaseComposeActivity
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.home.rankPage
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.player.FloatingPlayerPage

/**
 * Create by tinguo on 2025/4/22
 * CopyWrite (c) 2025 TME. All rights reserved.
 */
class MusicHallActivity: BaseComposeActivity() {

    companion object {
        const val TAG = "MusicHallActivity"
        const val KEY_TYPE = "type"

        const val TYPE_SINGER       = 1 // 歌手
        const val TYPE_ALBUM        = 2 // 专辑
        const val TYPE_SONGLIST     = 3 // 歌单
        const val TYPE_TOPLIST       = 4 // 榜单
    }

    private var type: Int = 0

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TYPE, type)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        type = if (savedInstanceState != null) {
            savedInstanceState.getInt(KEY_TYPE, 0)
        } else {
            intent.getIntExtra(KEY_TYPE, 0)
        }

        if (type == 0) {
            Toast.makeText(this, "非法跳转类型($type)", Toast.LENGTH_SHORT).show()
            return
        }

        setContent {
            MusicHallScreen(type)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun MusicHallScreen(type: Int) {
        val title = when (type) {
            TYPE_SINGER -> "歌手"
            TYPE_ALBUM -> "专辑"
            TYPE_SONGLIST -> "歌单广场"
            TYPE_TOPLIST -> "排行榜"
            else -> ""
        }
        Surface {
            Scaffold(
                topBar = {
                    TopBar(title)
                },
                modifier = Modifier.semantics{ testTagsAsResourceId=true },
                bottomBar = {
                    FloatingPlayerPage()
                }
            ) {
                when (type) {
                    TYPE_SINGER -> {
                        SingerListPage()
                    }
                    TYPE_TOPLIST -> {
                        val homeViewModel: HomeViewModel = viewModel()
                        rankPage(homeViewModel)
                    }
                    else -> {
                        Text("类型($type)实现中...")
                    }
                }
            }
        }
    }

}