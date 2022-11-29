package com.tencent.qqmusic.qplayer.ui.activity.folder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.Folder

/**
 * 歌单列表页面
 * 支持传入category_ids，获取分类歌单
 * 支持传入folder_id，获取单个歌单
 */
class FolderActivity : ComponentActivity() {

    companion object {
        const val KEY_CATEGORY_IDS = "category_ids"
        const val KEY_FOLDER_ID = "folder_id"
        const val KEY_FOLDERS = "folders"
    }

    private val categoryIds by lazy {
        intent.getIntegerArrayListExtra(KEY_CATEGORY_IDS) ?: emptyList()
    }

    private val folderId by lazy {
        intent.getStringExtra(KEY_FOLDER_ID) ?: ""
    }

    private val folderIds by lazy {
        intent.getStringArrayListExtra(KEY_FOLDERS) ?: emptyList()
    }

    private val folderViewModel by lazy { FolderViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (categoryIds.isNotEmpty()) {
                FolderScreen(folderViewModel.folders)
            }
            else if (folderId.isNotEmpty()) {
                FolderScreen(listOf(folderViewModel.folder))
            }
            else if (folderIds.isNotEmpty()) {
                // 需要批量获取歌单详情接口
            }
        }

        folderViewModel.fetchFolderByCategory(categoryIds)
        folderViewModel.fetchFolderByFolderId(folderId)
    }
}