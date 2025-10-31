package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Scaffold
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar


class AlbumActivity : ComponentActivity() {
    companion object {
        const val KEY_ALBUM_ID = "album_id"
    }

    private val albumId by lazy {
        intent.getStringExtra(KEY_ALBUM_ID) ?: ""
    }

    private val albumViewModel by lazy { AlbumViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(topBar = { TopBar("专辑列表")}) {
                if (albumId.isNotEmpty()) {
                    AlbumListPage(albums = listOf(albumViewModel.album))
                }
                albumViewModel.fetchAlbumByAlbumId(albumId)
            }
        }
    }
}
