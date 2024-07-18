package com.tencent.qqmusic.qplayer.ui.activity.audio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.rememberImagePainter
import com.google.accompanist.pager.*
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.utils.PerformanceHelper

/**
 * 长音频品类二级更多页面
 */
class LongAudioModuleContentActivity : ComponentActivity() {

    private val shelfId by lazy {
        intent.getIntExtra("shelfId", 0)
    }

    private val shelfTitle by lazy {
        intent.getStringExtra("shelfTitle") ?: ""
    }

    companion object {

        private const val TAG = "LongAudioModuleContentActivity"

        @JvmStatic
        fun start(context: Context, shelfId: Int, shelfTitle: String) {
            PerformanceHelper.monitorClick("LongAudioCategoryPageDetail_LongAudioModuleContentActivity_${shelfTitle}")
            val starter = Intent(context, LongAudioModuleContentActivity::class.java)
                .putExtra("shelfId", shelfId)
                .putExtra("shelfTitle", shelfTitle)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(topBar = { TopBar(shelfTitle) }) {
                val vm: LongAudioViewModel = viewModel()
                Box(modifier = Modifier.fillMaxSize()) {
//                    Text(text = "加载中")
                    LongAudioScreen(vm)
                }
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun LongAudioScreen(vm: LongAudioViewModel) {
        val flow = vm.pagingCategoryPageDetail(shelfId)
        val albums = flow!!.collectAsLazyPagingItems()
        val activity = LocalContext.current as Activity

        LazyColumn(state = rememberLazyListState(),  modifier = Modifier.fillMaxSize()) {
            this.items(albums) { shelf ->
                val album = shelf?.album ?: return@items
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
                            activity.startActivity(Intent(activity, SongListActivity::class.java)
                                .putExtra(SongListActivity.KEY_ALBUM_ID, album.id))
                        }
                ) {
                    Image(
                        painter = rememberImagePainter(album.pic),
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .padding(2.dp)
                    )
                    Column {
                        Text(text = album.name)
                        Text(text = "${album.songNum?.toString() ?: 0}首")
                    }
                }
            }
        }
    }

    class LongAudioViewModel : ViewModel() {

        fun pagingCategoryPageDetail(fId: Int) = androidx.paging.Pager(
            PagingConfig(
                pageSize = 20
            )
        ) {
            CategoryPageModuleContentSource(fId)
        }.flow
    }
}