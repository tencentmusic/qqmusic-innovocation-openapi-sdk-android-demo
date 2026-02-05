package com.tencent.qqmusic.qplayer.ui.activity.home.other.custom_song_list

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.SongListItem
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Author: hevinzhou
 * Created: 2025/11/22
 * Description:合作方运营列表：https://ocs.tmeoa.com/project/109?tab=2&group=2146
 */
class CustomSongListModel: ViewModel() {

    companion object {
        const val TAG = "ConfLibraryModel"
    }

    var squares: MutableState<List<SongListItem>> = mutableStateOf(emptyList())

    fun getSongListSquare() {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchCustomSongListSquare {
                val songListSquare = if (it.isSuccess()) {
                    it.data ?: emptyList()
                } else {
                    QLog.e(TAG, "getSongListSquare failed:$it")
                    emptyList()
                }
                squares.value = songListSquare
            }
        }
    }
}