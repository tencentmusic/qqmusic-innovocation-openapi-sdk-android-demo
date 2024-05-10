package com.tencent.qqmusic.qplayer.utils

import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.model.SongInfo

/**
 * Created by silverfu on 2024/4/25.
 */


fun SongInfo.hasWanos(): Boolean {
    return (getSongQuality(PlayerEnums.Quality.WANOS)?.size ?: 0) > 0
}