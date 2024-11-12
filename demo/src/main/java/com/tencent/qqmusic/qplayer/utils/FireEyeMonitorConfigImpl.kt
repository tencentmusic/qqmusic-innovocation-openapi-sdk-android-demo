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
        return Global.channelId
    }

    override fun memoryAndTraceConfig(): Pair<JSONObject?, JSONObject?> {
        return null to JSONObject().apply {
            put("anr_sample_ratio", 0.10f)
            put("fps_sample_ratio", 0.10f)
        }
    }

    override fun uid(): String {
        return ""
    }

    override fun uin(): String {
        return OpenApiSDK.getLoginApi().getUserOpenId()
    }

    override fun uniqueId(): String {
        return ""
    }

    override fun versionName(): String {
        return Global.versionName
    }

    override fun xpmConfig(): JSONObject? {
        return JSONObject().apply {
            put("xpm_open_sample", 0.10f)
            put("xpm_stack_sample", 0.10f)
            put("xpm_mode_sample_n", 2)
            put("xpm_test", false)
        }
    }

    override fun reportXpmEvent(event: Map<String, String>) {
        super.reportXpmEvent(event)
        Log.i("FireEyeXpm", "reportXpmEvent: ${GsonHelper.safeToJson(event)}")
    }
}