package com.tencent.qqmusic.qplayer.ui.activity

import android.app.Activity
import androidx.activity.ComponentActivity
import com.tencent.qqmusic.openapisdk.playerui.view.IViewWidgetOwner
import com.tencent.qqmusic.openapisdk.playerui.view.ViewWidgetOwner

/**
 * Created by silverfu on 2024/11/26.
 */
open class BaseActivity(private val widgetOwner: IViewWidgetOwner = ViewWidgetOwner()) : ComponentActivity(),
    IViewWidgetOwner by widgetOwner {

    override fun onWidgetResume() {
        widgetOwner.onWidgetResume()
    }

    override fun onWidgetPause() {
        widgetOwner.onWidgetPause()
    }
    override fun onDestroy() {
        super.onDestroy()
        widgetOwner.onWidgetDestroy()
    }

    override fun onPause() {
        super.onPause()
        widgetOwner.onWidgetPause()
    }


    override fun onResume() {
        super.onResume()
        widgetOwner.onWidgetResume()
    }

}