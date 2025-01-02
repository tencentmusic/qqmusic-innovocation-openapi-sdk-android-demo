package com.tencent.qqmusicrecognition.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.AuthType
import com.tencent.qqmusic.openapisdk.hologram.WXLoginProvider


//
// Created by tylertan on 2020-02-24.
// Copyright (c) 2020 Tencent. All rights reserved.
//

class WXEntryActivity : Activity(), IWXAPIEventHandler {

    companion object {
        const val TAG = "WXEntryActivity"
    }

    private val wechat by lazy {
        OpenApiSDK.getProviderByClass(WXLoginProvider::class.java)?.wxInternal
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //QLog.i(
        //    TAG, "[onCreate] uri=${intent?.data}, " +
        //            "extras=${intent?.extras?.keySet()?.toString()}"
        //)
        //intent?.extras?.run {
        //    for (key in keySet()) {
        //        val value = get(key)
        //        QLog.i(TAG, "$key:$value")
        //    }
        //}

        try {
            val intent = intent
            wechat?.handleIntent(intent, this)
        } catch (e: Exception) {
            QLog.e(TAG, "[onCreate] ", e)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        QLog.i(TAG, "onNewIntent")
        setIntent(intent)
        wechat?.handleIntent(intent, this)
    }

    override fun onReq(request: BaseReq?) {
        QLog.i(TAG, "[onReq] ")
    }

    /**
     * 注意：分享到微信后点击返回探歌，走的也是这里
     */
    override fun onResp(resp: BaseResp?) {
        val type = resp?.type
        QLog.i(TAG, "[onResp] response type $type")
        val authCode: String?
        if (type == ConstantsAPI.COMMAND_LAUNCH_WX_MINIPROGRAM) {
            val launchMiniProResp: WXLaunchMiniProgram.Resp = resp as WXLaunchMiniProgram.Resp
            //对应小程序组件 <button open-type="launchApp"> 中的 app-parameter 属性
            authCode = launchMiniProResp.extMsg
            OpenApiSDK.getLoginApi().onGetAuthCode(AuthType.WX, authCode)

            val launcherAct = packageManager.getLaunchIntentForPackage(packageName)
            if (launcherAct != null) {
                launcherAct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launcherAct)
            }
        }
        finish()
    }

}