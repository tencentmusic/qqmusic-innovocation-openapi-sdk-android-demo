package com.tencent.qqmusic.qplayer

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Debug
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.innovation.common.util.DeviceUtils
import com.tencent.qqmusic.innovation.common.util.ProcessUtils
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.business_common.event.event.LogEvent
import com.tencent.qqmusic.openapisdk.core.InitConfig
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.network.NetworkTimeoutConfig
import com.tencent.qqmusic.qplayer.baselib.util.GsonHelper
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.utils.FireEyeMonitorConfigImpl
import com.tencent.qqmusic.qplayer.utils.PrivacyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Created by tannyli on 2021/8/31.
 * Copyright (c) 2021 TME. All rights reserved.
 */
class App : Application() {


    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        context  = this
    }

    override fun onCreate() {
        super.onCreate()

        if (!ProcessUtils.isMainProcess()) {
//            Debug.waitForDebugger()
            // 非主进程 不用初始化sdk
            return
        }

        if (PrivacyManager.isGrant()) {
            init(this)
        } else {
            PrivacyManager.delayPrivacyEnd {
                init(this)
            }
        }
    }


    private fun openStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }

    companion object {
        private const val TAG = "App"
        lateinit var context: Context

        fun init(context: Context) {
            Log.i(TAG, "init Application")
            OpenApiSDK.registerBusinessEventHandler {
                when (it.code) {
                    LogEvent.LogFileCanNotWrite -> {
                        GlobalScope.launch(Dispatchers.Main) {
                            delay(2000)
                            Toast.makeText(context, "日志路径不可读写，使用默认路径", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            val sharedPreferences: SharedPreferences? = try {
                context.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
            } catch (e: Exception) {
                QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
                null
            }

            val isUseForegroundService = sharedPreferences?.getBoolean("isUseForegroundService", true) ?: true
            val logFileDir = sharedPreferences?.getString("logFileDir", "")
            val savedTimeoutConfig = sharedPreferences?.getString("NetworkTimeoutConfig", "")
            val enableAccountPartner = sharedPreferences?.getBoolean("accountModePartner", false) ?: false
            val lowMemoryMode = sharedPreferences?.getBoolean("lowMemoryMode", false) ?: false
            val timeoutConfig = GsonHelper.safeFromJson(savedTimeoutConfig, NetworkTimeoutConfig::class.java) ?: NetworkTimeoutConfig.DEFAULT()
            val isMutiChannel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2
            MLog.i(TAG, "isMutiChannel:$isMutiChannel ,${Build.VERSION.SDK_INT}")
            val initConfig = InitConfig(
                context.applicationContext,
                MustInitConfig.APP_ID,
                MustInitConfig.APP_KEY,
                DeviceUtils.getAndroidID(),
            ).apply {
                this.isUseForegroundService = isUseForegroundService
                this.crashConfig = InitConfig.CrashConfig(enableNativeCrashReport = true, enableAnrReport = true)
                this.deviceConfigInfo.apply {
                    hardwareInfo = ""
                    this.lowMemoryMode = lowMemoryMode
                }
                this.enableBluetoothListener = false
                this.useMediaPlayerWhenPlayDolby = sharedPreferences?.getBoolean("useMediaPlayerWhenPlayDolby", false) ?: false
                this.logFileDir = logFileDir
                this.networkTimeoutConfig = timeoutConfig
                this.isMutiChannel = isMutiChannel
                if (enableAccountPartner) {
                    this.accountMode = InitConfig.AccountMode.PARTNER_INDEPENDENT
                }
            }
            Global.setMonitorConfigApi(FireEyeMonitorConfigImpl())
            val start = System.currentTimeMillis()
            OpenApiSDK.init(initConfig)
            OpenApiSDK.setAppForeground(true)
            GlobalScope.launch(Dispatchers.Default) {
                try {
                    val enableLog = sharedPreferences?.getBoolean("enableLog", true) ?: true
                    OpenApiSDK.getLogApi().setLogEnable(enableLog)
                } catch (ignore: Exception) {
                }
            }
            Log.i(TAG, "init cost:${System.currentTimeMillis() - start}")
        }
    }
}