package com.tencent.qqmusic.qplayer.ui.activity.home.other.ordersong

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.openapisdk.ordersong.IOrderSongApi
import com.tencent.qqmusic.openapisdk.ordersong.OnRoomChangeListener
import com.tencent.qqmusic.openapisdk.hologram.OrderSongProvider
import com.tencent.qqmusic.openapisdk.ordersong.RoomConnectStatus
import com.tencent.qqmusic.openapisdk.ordersong.RoomEvent
import com.tencent.qqmusic.openapisdk.ordersong.entity.RoomInfo
import com.tencent.qqmusic.openapisdk.ordersong.entity.RoomSongInfo
import com.tencent.qqmusic.openapisdk.ordersong.entity.UpdateOperType
import com.tencent.qqmusic.openapisdk.ordersong.entity.UpdateRoomSongListResp
import com.tencent.qqmusic.openapisdk.ordersong.report.OrderSongManager
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.core.player.proxy.SPBridgeProxy
import com.tencent.qqmusic.qplayer.openapi.network.NetworkClient.onReturn
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlin.random.Random

class OrderSongViewModel: ViewModel() {

    companion object {
        const val TAG = "OrderSongViewModel"
    }
    var roomInfo: RoomInfo? by mutableStateOf(null)
    var requestResult: String? by mutableStateOf(null)
    var needUserConfirm by mutableStateOf(false)
    var roomSongList: List<RoomSongInfo> by mutableStateOf(listOf())
    var roomConnectedStatus: RoomConnectStatus? by mutableStateOf(null)

    val testSongList = arrayListOf<SongInfo>()

    val sharedPreferences: SharedPreferences? = try {
        SPBridgeProxy.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        QLog.e("DebugScreen", "getSharedPreferences error e = ${e.message}")
        null
    }

    val enableTDE = sharedPreferences?.getBoolean("enableTdeEnv", false) ?: false

    private val roomStatusListener = object : OnRoomChangeListener {
        override fun onEvent(event: RoomEvent) {
            QLog.i(TAG, event.name)
            when(event) {
                RoomEvent.ROOM_EVENT_OPENED -> {

                }
                RoomEvent.ROOM_EVENT_CLOSED -> {
                    roomInfo = null
                    roomSongList = listOf()
                    roomConnectedStatus = null
                }
                RoomEvent.ROOM_EVENT_ROOM_INFO_CHANGE, RoomEvent.ROOM_EVENT_ROOM_INFO_CHANGE_BY_SECURITY -> {
                    queryRoom()
                }
                RoomEvent.ROOM_EVENT_SONG_LIST_CHANGE -> {
                    getRoomSongList()
                }
            }
        }

        override fun onRoomConnectStatusChange(connectStatus: RoomConnectStatus) {
            QLog.i(TAG, "connectStatus:$connectStatus")
            roomConnectedStatus = connectStatus
        }
    }

    fun init() {
        getOrderSongApi()?.registerRoomStatusListener(roomStatusListener)
    }

    override fun onCleared() {
        getOrderSongApi()?.unRegisterRoomStatusListener(roomStatusListener)
    }

    fun queryRoom(roomId:String="") {
        roomId.ifEmpty { roomInfo?.roomId }?.let { rid->
            getOrderSongApi()?.getRoomInfo(roomId = rid) {
                requestResult = getRequestResult(it, "查询房间")
                if (it.isSuccess()) {
                    if (roomInfo == null) {
                        getRoomSongList()
                    }
                    roomInfo = it.data
                }
            }
        }
    }

    fun createRoom(roomName: String?, initSongList: String?, force: Boolean) {
        val initSongIdList = initSongList?.split(",")?.mapNotNull { it.toLongOrNull() }
        getOrderSongApi()?.createRoom(roomName, initSongIdList, force) {
            requestResult = getRequestResult(it, "创建房间")
            if (it.isSuccess()) {
                needUserConfirm = false
                roomInfo = it.data?.roomInfo
                roomSongList = it.data?.roomSongList ?: listOf()
            } else {
                if (it.subRet == IOrderSongApi.CREATE_ROOM_ERR_ROOM_EXIST_IN_CUR_DEVICE ||
                    it.subRet == IOrderSongApi.CREATE_ROOM_ERR_ROOM_EXIST_IN_ANOTHER_DEVICE) {
                    needUserConfirm = true
                } else {
                    needUserConfirm = false
                }
            }
        }
    }

    fun closeRoom() {
        getOrderSongApi()?.closeRoom {
            requestResult = getRequestResult(it, "关闭房间")
            if (it.isSuccess()) {
                roomInfo = null
                roomSongList = listOf()
            }
        }
    }

    fun joinRoom(force:Boolean=false) {
        getOrderSongApi()?.joinRoom(force) {
            requestResult = getRequestResult(it, "加入房间")
            if (it.isSuccess()) {
                roomInfo = it.data?.roomInfo
                roomSongList = it.data?.roomSongList ?: listOf()
            }
        }
    }

    fun getRoomSongList() {
        getOrderSongApi()?.getRoomPlayList {
            requestResult = getRequestResult(it, "获取房间歌曲列表")
            if (it.isSuccess()) {
                onRoomSongChange(it.data?.songList)
            }
        }
    }

    private fun onRoomSongChange(songs: List<RoomSongInfo>?) {
        val needPlay = roomSongList.isEmpty()
        roomSongList = songs ?: listOf()
        if (needPlay) {
            roomSongList?.map {
                it.song
            }?.filterNotNull()?.let { songList ->
                OpenApiSDK.getPlayerApi().playSongs(songList, 0)
            }
        } else {
            roomSongList?.map {
                it.song
            }?.filterNotNull()?.let { songList ->
                OpenApiSDK.getPlayerApi().updatePlayList(songList)
            }
        }
    }

    fun getPlaySongList(): List<SongInfo> {
        return roomSongList.map {
            it.song
        }.filterNotNull()
    }

    fun updateTestSongs():List<SongInfo> {
        if (testSongList.isEmpty()){
            OpenApiSDK.getOpenApi().fetchPersonalRecommendSong {
                getRequestResult(it, "添加歌曲id")
                if (it.isSuccess()) {
                    testSongList.clear()
                    it.data?.let { songs ->
                        testSongList.addAll(songs)
                    }
                }
            }
        }
        return testSongList
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun addRandomSong(isBatch:Boolean=false, isReTry: Boolean=false) {
        if (testSongList.isEmpty() && isReTry.not()) {
            updateTestSongs().apply { addRandomSong(isBatch=isBatch, isReTry=true) }
        }else{
            val songIdsToAdd = if (isBatch) testSongList.map { it.songId } else listOf(testSongList.random().songId)
            addSongList(songIdsToAdd){ resp ->
                if (resp.isSuccess()){
                    testSongList.removeIf { it.songId in songIdsToAdd }
                }
            }
        }
    }

    fun addSongList(songs: List<Long>, callback: (OpenApiResponse<UpdateRoomSongListResp>) -> Unit){
        getOrderSongApi()?.addSongToPlayList(songListToAdd=songs) { resp ->
            requestResult = getRequestResult(resp, "添加歌曲-${songs}")
            if (resp.isSuccess()) {
                onRoomSongChange(resp.data?.songListAfterUpdate)
            }
            callback.onReturn(resp)
        }
    }

    fun deleteSong(song: RoomSongInfo) {
        song.song?.songId?.let {
            getOrderSongApi()?.updateSongsInRoom(listOf(it), UpdateOperType.DELETE_SONGS) {
                requestResult = getRequestResult(it, "删除歌曲-${song.song?.songName}")
                if (it.isSuccess()) {
                    onRoomSongChange(it.data?.songListAfterUpdate)
                }
            }
        }
    }

    fun moveToTop(song: RoomSongInfo) {
        val songAfterMoveUp = ArrayList(roomSongList).apply {
            remove(song)
            add(0, song)
        }.toList().filterNotNull().map {
            it.song?.songId ?: -1
        }
        getOrderSongApi()?.updateSongsInRoom(songAfterMoveUp, UpdateOperType.SORT_LIST) {
            requestResult = getRequestResult(it, "排序歌曲-${song.song?.songName}")
            if (it.isSuccess()) {
                onRoomSongChange(it.data?.songListAfterUpdate)
            }
        }
    }

    fun test() {
        OpenApiSDK.getPlayerApi().playSongs(testSongList)
    }

    fun testUpdate() {
        val song = testSongList.removeAt(Random.nextInt(testSongList.size - 1))
        Log.d("keyao", "delete song: ${song.songName}")
        OpenApiSDK.getPlayerApi().updatePlayList(testSongList)
    }

    fun testDeleteCur() {
        val curSong = OpenApiSDK.getPlayerApi().getCurrentSongInfo()
        testSongList.remove(curSong)
        Log.d("keyao", "delete song: ${curSong?.songName}")
        OpenApiSDK.getPlayerApi().updatePlayList(testSongList)
    }

    private fun getOrderSongApi(): IOrderSongApi? {
        return OpenApiSDK.getProviderByClass(OrderSongProvider::class.java)?.getOrderSongApi()
    }

    private fun <T> getRequestResult(resp: OpenApiResponse<T>, from: String): String {
        return if (resp.isSuccess()) {
            "$from 请求成功"
        } else {
            resp.errorMsg?.let { UiUtils.showToast(it) }
            "$from 请求失败，ret=${resp.ret}, subRet=${resp.subRet}, errMsg=${resp.errorMsg}"
        }
    }
}