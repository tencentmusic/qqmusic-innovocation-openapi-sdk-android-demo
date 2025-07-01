package com.tencent.qqmusic.qplayer.ui.activity.folder

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class FolderViewModel : ViewModel() {

    var folders = mutableListOf<Folder>()
    var folder: Folder by mutableStateOf(Folder())

    private val _folderState: MutableStateFlow<List<Folder>> = MutableStateFlow(emptyList())
    val folderState: StateFlow<List<Folder>> = _folderState.asStateFlow()

    private val _loadMoreState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loadMoreState: StateFlow<Boolean> = _loadMoreState.asStateFlow()

    private var page: Int = 0

    fun fetchFolderByCategory(categoryIdList: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchFolderListByCategory(categoryIdList, page = page) { resp->
                if (resp.isSuccess()) {
                    folders.addAll(resp.data ?: emptyList())
                    _folderState.update { ArrayList(folders) }
                    page = if (resp.hasMore) page + 1 else page
                    _loadMoreState.update { resp.hasMore }
                } else {

                }
            }
        }
    }

    fun fetchFolderByFolderId(folderId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchFolderDetail(folderId) {
                if (it.isSuccess()) {
                    folder = it.data ?: Folder()
                } else {

                }
            }
        }
    }

}