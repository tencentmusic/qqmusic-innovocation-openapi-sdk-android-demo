package com.tencent.qqmusic.qplayer

import android.app.Activity

class DefaultBaseFunction : IBaseFunction {
    override fun getAccount(): Account {
        return Account("11", "11")
    }

    override fun getAppCheckMode(): Boolean {
        return false
    }

    override fun setAppCheckMode(strick: Boolean) {

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

    override fun initDebug() {

    }
}