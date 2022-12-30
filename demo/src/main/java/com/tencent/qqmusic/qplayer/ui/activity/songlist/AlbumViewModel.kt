package com.tencent.qqmusic.qplayer.ui.activity.songlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Album
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlbumViewModel : ViewModel() {
    var album: Album by mutableStateOf(Album())

    fun fetchAlbumByAlbumId(albumId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchAlbumDetail(albumId, null) {
                if (it.isSuccess()) {
                    album = it.data ?: Album()
                }
            }
        }
    }
}