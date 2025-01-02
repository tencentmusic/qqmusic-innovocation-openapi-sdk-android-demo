package com.tencent.qqmusic.qplayer.ui.activity

import androidx.fragment.app.Fragment
import com.tencent.qqmusic.openapisdk.playerui.view.IViewWidgetOwner
import com.tencent.qqmusic.openapisdk.playerui.view.ViewWidgetOwner

/**
 * Created by silverfu on 2024/11/26.
 */
class BaseFragment(private val widgetOwner: IViewWidgetOwner = ViewWidgetOwner()) : Fragment(), IViewWidgetOwner by widgetOwner {

    override fun isResume(): Boolean {
        return isResumed
    }
}