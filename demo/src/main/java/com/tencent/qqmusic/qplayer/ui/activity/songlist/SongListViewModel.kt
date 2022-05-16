package com.tencent.qqmusic.qplayer.ui.activity.songlist

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayListPagingSource

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class SongListViewModel : ViewModel() {

    fun pagingFolderSongs(folderId: String) = Pager(PagingConfig(pageSize = 50)) {
        FolderSongPagingSource(folderId)
    }.flow

    fun pagingPlayList() = Pager(PagingConfig(pageSize = 50)) {
        PlayListPagingSource()
    }.flow

}