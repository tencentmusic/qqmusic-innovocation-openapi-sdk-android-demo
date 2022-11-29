package com.tencent.qqmusic.qplayer.ui.activity.folder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//
// Created by tylertan on 2021/11/2
// Copyright (c) 2021 Tencent. All rights reserved.
//

class FolderViewModel : ViewModel() {

    var folders: List<Folder> by mutableStateOf(emptyList())
    var folder: Folder by mutableStateOf(Folder())

    fun fetchFolderByCategory(categoryIdList: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            OpenApiSDK.getOpenApi().fetchFolderListByCategory(categoryIdList) {
                if (it.isSuccess()) {
                    folders = it.data ?: emptyList()
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