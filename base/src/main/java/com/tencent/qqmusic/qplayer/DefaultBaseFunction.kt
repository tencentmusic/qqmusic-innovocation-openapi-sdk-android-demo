package com.tencent.qqmusic.qplayer

import android.app.Activity
import com.tencent.qqmusic.openapisdk.core.PartnerDeviceTokenInfo

class DefaultBaseFunction : IBaseFunction {
    override fun getAccount(): Account {
        return Account("11", "11")
    }

    override fun getAppCheckMode(): Int {
        return AppCheckMode.STRICT
    }

    override fun setAppCheckMode(appCheckMode: Int) {
    }

    override fun setAppIdAndAppKey(appId: String, appKey: String) {
    }


    override fun getWxAPPID(): String {
        return ""
    }

    override fun getMatchID(): String {
        return ""
    }

    override fun getQQAPPID(): String {
        return ""
    }

    override fun gotoDebugActivity(activity: Activity) {

    }

    override fun initDebug(isDebug: Boolean) {

    }

    override fun getChannelId(): String {
        return ""
    }

    override fun getPartnerDeviceTokenInfo(): PartnerDeviceTokenInfo? {
        return null
    }
}