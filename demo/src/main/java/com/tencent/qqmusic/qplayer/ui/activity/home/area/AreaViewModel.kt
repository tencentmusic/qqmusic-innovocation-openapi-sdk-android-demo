package com.tencent.qqmusic.qplayer.ui.activity.home.area

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.AreaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AreaNewAlbumModel(val areaId: Int) {
    var newAlbumList: List<Album> by mutableStateOf(emptyList())
    var newAlbumHasMoreState = mutableStateOf(false)
    var type = mutableStateOf<Int?>(null)
    var newAlbumNextPage = 0

    suspend fun fetchNewAlbumList(type: Int?) {
        if (this.type.value != type) {
            this.type.value = type
            newAlbumNextPage = 0
            newAlbumList = emptyList()
            newAlbumHasMoreState.value = false
        }
        OpenApiSDK.getOpenApi().fetchNewAlbum(areaId, type=type, page = newAlbumNextPage) { resp->
            if (resp.isSuccess()) {
                val newList = ArrayList<Album>()
                newList.addAll(newAlbumList)
                (resp.data ?: emptyList()).let { newList.addAll(it) }
                newAlbumList = newList
                newAlbumHasMoreState.value = resp.hasMore
                newAlbumNextPage++
            }
        }
    }
}

class AreaViewModel: ViewModel() {

    var areaId: List<AreaInfo> by mutableStateOf(emptyList())

    private val newAlbumAreaModels = mutableMapOf<Int, AreaNewAlbumModel>()

    fun fetchNewAlbumAreaList() {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchNewAlbumsByArea { resp->
                if (resp.isSuccess()) {
                    areaId = resp.data ?: emptyList()
                }
            }
        }
    }

    private fun areaNewAlbumModel(area: Int): AreaNewAlbumModel {
        if (newAlbumAreaModels[area] == null) {
            newAlbumAreaModels[area] = AreaNewAlbumModel(area)
        }
        return newAlbumAreaModels[area]!!
    }

    fun fetchNewAlbum(area: Int, type: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            areaNewAlbumModel(area).fetchNewAlbumList(type)
        }
    }

    fun fetchMoreNewAlbum(area: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val model = areaNewAlbumModel(area)
            model.fetchNewAlbumList(model.type.value)
        }
    }

    fun albumList(area: Int): List<Album> = areaNewAlbumModel(area).newAlbumList

    fun hasMore(area: Int): State<Boolean> = areaNewAlbumModel(area).newAlbumHasMoreState

    fun albumType(area: Int): State<Int?> = areaNewAlbumModel(area).type
}