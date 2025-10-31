package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Album
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.model.SingerDetail
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.core.player.playlist.MusicPlayList
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.Type

const val ORDER_UNDEFINE: Int = Int.MAX_VALUE
const val ORDER_POSITIVE: Int = 1
const val ORDER_REVERSE: Int = 2
const val ORDER_DEFAULT: Int = 0



sealed class ProfileDataModel<T> {
    protected var _data: MutableState<T?> = mutableStateOf<T?>(null)
    val data: State<T?> = _data

    abstract suspend fun fetchData()
}

sealed class ProfileDataListModel<T>(): ProfileDataModel<List<T>>() {
    protected var _hasMore = mutableStateOf(false)
    val hasMore: State<Boolean> = _hasMore
    var nextPage = 0
    protected var _total = mutableIntStateOf(0)
    val total: State<Int> = _total

    internal var _order = mutableIntStateOf(ORDER_UNDEFINE)
    val order: State<Int> = _order

    override suspend fun fetchData() {
        _hasMore.value = false
        nextPage = 0
        _data.value = emptyList()
        fetchMoreData()
    }

    abstract suspend fun fetchMoreData()
}

class FolderProfileModel(private val folderId: Long): ProfileDataModel<Folder>() {
    override suspend fun fetchData() {
        OpenApiSDK.getOpenApi().fetchFolderDetail(folderId = "$folderId") { resp->
            if (resp.isSuccess()) {
                _data.value = resp.data
            }
        }
    }
}

class FolderSongListModel(private val folderId: Long, private val isMyLikeFolder: Boolean): ProfileDataListModel<SongInfo>() {
    private var pagePassBack = ""

    override suspend fun fetchData() {
        pagePassBack = ""
        super.fetchData()
    }

    override suspend fun fetchMoreData() {
        if (isMyLikeFolder) {
            // 我喜欢最多请求1000首歌
            OpenApiSDK.getOpenApi().fetchSongOfMyLikeFolder(passBack = pagePassBack, count = 1000) { resp ->
                if (resp.isSuccess()) {
                    val newList = ArrayList<SongInfo>()
                    newList.addAll(data.value ?: emptyList())
                    newList.addAll(resp.data ?: emptyList())
                    _data.value = newList
                    _hasMore.value = resp.hasMore
                    _total.value = resp.totalCount ?: 0
                    nextPage++
                    pagePassBack = resp.passBack ?: ""
                }
            }
        } else {
            OpenApiSDK.getOpenApi().fetchSongOfFolder(folderId.toString(), passBack = pagePassBack, count = 300, null) { resp ->
                if (resp.isSuccess()) {
                    val newList = ArrayList<SongInfo>()
                    newList.addAll(data.value ?: emptyList())
                    newList.addAll(resp.data ?: emptyList())
                    _data.value = newList
                    _hasMore.value = resp.hasMore
                    _total.value = resp.totalCount ?: 0
                    nextPage++
                    pagePassBack = resp.passBack ?: ""
                }
            }
        }
    }
}

class SingerProfileModel(private val singerId: Long): ProfileDataModel<SingerDetail>() {
    override suspend fun fetchData() {
        OpenApiSDK.getOpenApi().fetchSingerDetail(singerId = singerId.toInt()) { resp->
            if (resp.isSuccess()) {
                _data.value = resp.data
            }
        }
    }
}

class SingerSongListModel(private val singerId: Long): ProfileDataListModel<SongInfo>() {
    override suspend fun fetchMoreData() {
        OpenApiSDK.getOpenApi().fetchSongOfSinger(singerId = singerId.toInt(), page = nextPage) { resp->
            if (resp.isSuccess()) {
                val newList = ArrayList<SongInfo>()
                newList.addAll(data.value ?: emptyList())
                newList.addAll(resp.data ?: emptyList())
                _data.value = newList
                _hasMore.value = resp.hasMore
                _total.value = resp.totalCount ?: 0
                nextPage++
            }
        }
    }
}

class SingerAlbumListModel(private val singerId: Long): ProfileDataListModel<Album>() {
    override suspend fun fetchMoreData() {
        OpenApiSDK.getOpenApi().fetchAlbumOfSinger(singerId = singerId.toInt(), page = nextPage) { resp->
            if (resp.isSuccess()) {
                val newList = ArrayList<Album>()
                newList.addAll(data.value ?: emptyList())
                newList.addAll(resp.data ?: emptyList())
                _data.value = newList
                _hasMore.value = resp.hasMore
                _total.value = resp.totalCount ?: 0
                nextPage++
            }
        }
    }

}

class AlbumProfileModel(val albumId: Long): ProfileDataModel<Album>() {
    override suspend fun fetchData() {
        OpenApiSDK.getOpenApi().fetchAlbumDetail(albumId = albumId.toString()) { resp->
            if (resp.isSuccess()) {
                _data.value = resp.data
            }
        }
    }
}

class AlbumSongListModel(val albumId: Long): ProfileDataListModel<SongInfo>() {
    override suspend fun fetchMoreData() {
        val order = if (_order.intValue != ORDER_UNDEFINE) _order.intValue else ORDER_DEFAULT
        OpenApiSDK.getOpenApi().fetchSongOfAlbum(albumId = albumId.toString(), page = nextPage, count = 50, orderBy = order) { resp->
            if (resp.isSuccess()) {
                val newList = ArrayList<SongInfo>()
                newList.addAll(data.value ?: emptyList())
                newList.addAll(resp.data?.songList ?: emptyList())
                _data.value = newList
                _hasMore.value = resp.hasMore
                _total.intValue = resp.totalCount ?: 0
                _order.intValue = resp.data?.orderBy.takeUnless { it == ORDER_DEFAULT } ?: ORDER_UNDEFINE
                nextPage++
            }
        }
    }
}

class CommonProfileViewModel: ViewModel() {

    var playListType: Int by mutableIntStateOf(0)
    var playListTypeId: Long by mutableLongStateOf(0L)

    private val _selectedSongs = mutableStateListOf<SongInfo>()
    val selectedSongs: List<SongInfo> get() = _selectedSongs

    private val profileModels = mutableMapOf<Type, ProfileDataModel<*>>()

    private fun registerProfileModel(type: Type, model: ProfileDataModel<*>) {
        profileModels[type] = model
    }

    private fun <T> type(clazz: Class<T>): Type {
        return TypeToken.get(clazz).type
    }

    private fun <T> listType(clazz: Class<T>): Type {
        return TypeToken.getParameterized(List::class.java, clazz).type
    }

    fun initWithArguments(bundle: Bundle) {
        val folderId = bundle.getString(SongListActivity.KEY_FOLDER_ID, "")?.toLongOrNull()
        val singerId = bundle.getInt(SongListActivity.KEY_SINGER_ID, -1).takeUnless { it == -1 }?.toLong()
        val albumId = bundle.getString(SongListActivity.KEY_ALBUM_ID, "")?.toLongOrNull()
        val rankId = bundle.getInt(SongListActivity.KEY_RANK_ID, -1).takeUnless { it == -1 }?.toLong()
        val isMyLikeFolder = bundle.getBoolean(SongListActivity.KEY_IS_MY_LIKE_FOLDER, false)

        when {
            folderId != null -> {
                playListType = MusicPlayList.PLAY_LIST_FOLDER_TYPE
                playListTypeId = folderId
                registerProfileModel(type(Folder::class.java), FolderProfileModel(folderId))
                registerProfileModel(listType(SongInfo::class.java), FolderSongListModel(folderId, isMyLikeFolder))
            }
            singerId != null -> {
                playListType = MusicPlayList.PLAY_LIST_LOCAL_SINGER_TYPE
                playListTypeId = singerId
                registerProfileModel(type(SingerDetail::class.java), SingerProfileModel(singerId))
                registerProfileModel(listType(SongInfo::class.java), SingerSongListModel(singerId))
                registerProfileModel(listType(Album::class.java), SingerAlbumListModel(singerId))
            }
            albumId != null -> {
                playListType = MusicPlayList.PLAY_LIST_ALBUM_TYPE
                playListTypeId = albumId
                registerProfileModel(type(Album::class.java), AlbumProfileModel(albumId))
                registerProfileModel(listType(SongInfo::class.java), AlbumSongListModel(albumId))
            }
        }
    }

    fun fetchData() {
        viewModelScope.launch(Dispatchers.IO) {
            profileModels.forEach { (_, model)->
                model.fetchData()
            }
        }
    }

    fun <T> fetchMoreData(typeToken: Class<T>) {
        viewModelScope.launch(Dispatchers.IO) {
            val model = profileModels[listType<T>(typeToken)] as? ProfileDataListModel<*>
            model?.fetchMoreData()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> profileData(typeToken: Class<T>): State<T?>? {
        val model = profileModels[type<T>(typeToken)] as? ProfileDataModel<T>
        return model?.data as? State<T?>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> profileDataList(typeToken: Class<T>): State<List<T>?>? {
        val model = profileModels[listType<T>(typeToken)]
        return model?.data as? State<List<T>?>
    }

    fun <T> hasMore(typeToken: Class<T>): State<Boolean>? {
        val model = profileModels[listType<T>(typeToken)] as? ProfileDataListModel<*>
        return model?.hasMore
    }

    fun <T> total(typeToken: Class<T>): State<Int>? {
        val model = profileModels[listType<T>(typeToken)] as? ProfileDataListModel<*>
        return model?.total
    }

    fun <T> order(typeToken: Class<T>): State<Int>? {
        val model = profileModels[listType(typeToken)] as? ProfileDataListModel<*>
        return model?.order
    }

    fun <T> order(typeToken: Class<T>, order: Int) {
        val model = profileModels[listType(typeToken)] as? ProfileDataListModel<*>
        model?._order?.intValue = order
        fetchData()
    }

    fun toggleSongSelection(song: SongInfo) {
        if (_selectedSongs.contains(song)) {
            _selectedSongs.remove(song)
        } else {
            _selectedSongs.add(song)
        }
    }

    fun selectAllSongs(songs: List<SongInfo>) {
        _selectedSongs.clear()
        _selectedSongs.addAll(songs)
    }

    fun clearSelection() {
        _selectedSongs.clear()
    }

    fun deleteSelectedSongs() {
        val folderId = playListTypeId.toString()
        val delSongList = selectedSongs
        OpenApiSDK.getOpenApi().deleteSongFromFolder(
            folderId = folderId,
            songIdList = delSongList.map { it.songId },
            midList = null,
            songTypes = delSongList.map { it.songType.toString() }){
            if (it.isSuccess()){
                UiUtils.showToast("删除成功")
            }else{
                UiUtils.showToast(it.errorMsg?:"删除失败")
            }
            fetchData()
        }
    }

}