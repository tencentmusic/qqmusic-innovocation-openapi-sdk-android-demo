package com.tencent.qqmusic.qplayer.ui.activity.area

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.cachedIn

/**
 * Created by tannyli on 2023/10/24.
 * Copyright (c) 2023 TME. All rights reserved.
 */
class AreaListViewModel: ViewModel() {

    fun areaListPageDetail(areaId: Int, shelfId: Int) = androidx.paging.Pager(
        PagingConfig(
            pageSize = 20
        )
    ) {
        AreaListContentSource(areaId, shelfId)
    }.flow.cachedIn(viewModelScope)


}