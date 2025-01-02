package com.tencent.qqmusic.qplayer.ui.activity.player

import com.tencent.qqmusic.openapisdk.playerui.PlayerStyle
import com.tencent.qqmusic.openapisdk.playerui.viewmode.IPlayerMagicColorViewModel
import com.tencent.qqmusic.openapisdk.playerui.viewmode.PlayerViewModel

/**
 * Created by silverfu on 2024/12/13.
 */
class CustomPlayerViewMode : PlayerViewModel(CustomPlayerRepository) {

    /**
     * 自定义魔法色
     */
    override fun getPlayerMagicColorViewMode(playerStyle: PlayerStyle): IPlayerMagicColorViewModel {
        return CustomPlayerAlbumMagicColorViewModel(playerInfoViewModel, playerRepository)
    }
}