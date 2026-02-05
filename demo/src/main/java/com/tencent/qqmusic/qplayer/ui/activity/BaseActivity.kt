package com.tencent.qqmusic.qplayer.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.tencent.qqmusic.openapisdk.playerui.view.IViewWidgetHostOwner
import com.tencent.qqmusic.openapisdk.playerui.view.IViewWidgetOwner
import com.tencent.qqmusic.openapisdk.playerui.view.ViewWidgetHostOwner
import com.tencent.qqmusic.openapisdk.playerui.view.ViewWidgetOwner

/**
 * Created by silverfu on 2024/11/26.
 */
open class BaseActivity : ComponentActivity(), IViewWidgetHostOwner by ViewWidgetHostOwner() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViewWidgetOwner(this)
    }
}