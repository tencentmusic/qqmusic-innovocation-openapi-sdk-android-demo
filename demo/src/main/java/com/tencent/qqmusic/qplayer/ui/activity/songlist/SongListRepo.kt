package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.util.Log
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.openapisdk.model.SongInfo

//
// Created by tylertan on 2021/12/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class SongListRepo {

    companion object {
        private const val TAG = "SongListRepo"
    }

    fun fetchSongInfoByFolder(
        folderId: String,
        page: Int,
        count: Int,
        source:Int?=null
    ): OpenApiResponse<List<SongInfo>> {
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "[fetchSongInfoByFolder]: start time $startTime")
        val ret = OpenApiSDK.getOpenApi().blockingGet<List<SongInfo>> {
            OpenApiSDK.getOpenApi().fetchSongOfFolder(folderId, page, count = count, source = source, callback = it)
        }
        Log.i(TAG, "[fetchSongInfoByFolder]: duration ${System.currentTimeMillis() - startTime}")
        return ret
    }

    fun fetchSongInfoByFolder(
        folderId: String,
        passBack: String,
        count: Int,
        source:Int?=null
    ): OpenApiResponse<List<SongInfo>> {
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "[fetchSongInfoByFolder]: start time $startTime")
        val ret = OpenApiSDK.getOpenApi().blockingGet<List<SongInfo>> {
            OpenApiSDK.getOpenApi().fetchSongOfFolder(folderId, passBack, count = count, source = source, callback = it)
        }
        Log.i(TAG, "[fetchSongInfoByFolder]: duration ${System.currentTimeMillis() - startTime}")
        return ret
    }

    fun fetchSongByRecent(): OpenApiResponse<List<SongInfo>> {
        val ret = OpenApiSDK.getOpenApi().blockingGet<List<SongInfo>> {
            OpenApiSDK.getOpenApi().fetchRecentPlaySong(callback = it)
        }
        return ret
    }

    fun fetchMyLongAudioSong(type: Int, page: Int): OpenApiResponse<List<SongInfo>> {
        val ret = OpenApiSDK.getOpenApi().blockingGet<List<SongInfo>> {
            OpenApiSDK.getOpenApi().fetchCollectedLongAudioSongList (type, page, it)
        }
        return ret
    }

    fun fetchSongInfoByAlbum(
        albumId: String,
        page: Int,
        count: Int
    ): OpenApiResponse<List<SongInfo>> {
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "[fetchSongInfoByAlbum]: start time $startTime")
        val ret = OpenApiSDK.getOpenApi().blockingGet<List<SongInfo>> {
            OpenApiSDK.getOpenApi().fetchSongOfAlbum(albumId, null, page, count = count, callback = it)
        }
        Log.i(TAG, "[fetchSongInfoByAlbum]: duration ${System.currentTimeMillis() - startTime}")
        return ret
    }

}