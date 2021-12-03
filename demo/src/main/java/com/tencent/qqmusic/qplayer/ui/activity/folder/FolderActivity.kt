package com.tencent.qqmusic.qplayer.ui.activity.folder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class FolderActivity : ComponentActivity() {

    companion object {
        const val KEY_CATEGORY_IDS = "category_ids"
    }

    private val categoryIds by lazy {
        intent.getIntegerArrayListExtra(KEY_CATEGORY_IDS) ?: emptyList()
    }

    private val folderViewModel by lazy { FolderViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FolderScreen(folderViewModel.folders)
        }

        folderViewModel.fetchFolderByCategory(categoryIds)
    }
}