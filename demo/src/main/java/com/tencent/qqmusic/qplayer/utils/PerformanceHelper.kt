package com.tencent.qqmusic.qplayer.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.tencent.qqmusic.openapisdk.hologram.HologramManager
import com.tencent.qqmusic.openapisdk.hologram.service.IFireEyeXpmService


object PerformanceHelper {

    @Composable
    fun MonitorListScroll(scrollState: LazyListState, location: String) {
        if (scrollState.isScrollInProgress){
            DisposableEffect(key1 = Unit) {
                HologramManager.getService(IFireEyeXpmService::class.java)?.monitorXpmEvent(
                    IFireEyeXpmService.XpmEvent.LIST_SCROLL, location, 1
                )
                onDispose {
                    HologramManager.getService(IFireEyeXpmService::class.java)?.monitorXpmEvent(
                        IFireEyeXpmService.XpmEvent.LIST_SCROLL, location, 0
                    )
                }
            }
        }
    }

    fun monitorPageScroll(location: String) {
        HologramManager.getService(IFireEyeXpmService::class.java)?.monitorXpmEvent(
            IFireEyeXpmService.XpmEvent.PAGE_SCROLL,
            location
        )
    }

    fun monitorClick(location: String) {
        HologramManager.getService(IFireEyeXpmService::class.java)
            ?.monitorXpmEvent(
                IFireEyeXpmService.XpmEvent.CLICK, location
            )
    }

    fun monitorMvPlay(location: String) {
        HologramManager.getService(IFireEyeXpmService::class.java)?.monitorXpmEvent(
            IFireEyeXpmService.XpmEvent.MV_PLAY, location
        )
    }


}