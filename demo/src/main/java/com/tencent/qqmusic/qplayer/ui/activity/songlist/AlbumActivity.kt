package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.qplayer.ui.activity.folder.FolderViewModel


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
            if (albumId.isNotEmpty()) {
                AlbumPage(albums = listOf(albumViewModel.album))
            }
            albumViewModel.fetchAlbumByAlbumId(albumId)
        }
    }
}
