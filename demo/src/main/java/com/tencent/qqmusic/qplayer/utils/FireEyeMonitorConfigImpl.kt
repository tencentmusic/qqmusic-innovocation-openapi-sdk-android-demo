package com.tencent.qqmusic.qplayer.utils

import android.util.Log
import com.tencent.qqmusic.innovation.common.util.GsonHelper
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.business_common.monitor.IMonitorConfigApi
import com.tencent.qqmusic.openapisdk.business_common.session.SessionManager
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import org.json.JSONObject

/**
 * Create by tinguo on 2024/5/22
 * Copyright (c) 2024 TME. All rights reserved.
 */
class FireEyeMonitorConfigImpl: IMonitorConfigApi {
    override fun channelId(): String {
        return ""
    }

    override fun memoryAndTraceConfig(): Pair<JSONObject?, JSONObject?> {
        return null to JSONObject()
    }

    override fun uid(): String {
        return  ""
    }

    override fun uin(): String {
        return  ""
    }

    override fun uniqueId(): String {
        return ""
    }

    override fun versionName(): String {
        return ""
    }

    override fun xpmConfig(): JSONObject? {
        return null
    }

    override fun reportXpmEvent(event: Map<String, String>) {
        super.reportXpmEvent(event)
    }
}