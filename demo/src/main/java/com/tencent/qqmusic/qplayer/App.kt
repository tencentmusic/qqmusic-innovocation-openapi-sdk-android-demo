package com.tencent.qqmusic.qplayer

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.annotation.Keep
import androidx.lifecycle.Observer
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.business_common.event.event.LogEvent
import com.tencent.qqmusic.openapisdk.business_common.utils.ProcessUtil
import com.tencent.qqmusic.openapisdk.core.DeviceType
import com.tencent.qqmusic.openapisdk.core.IAPPCallback
import com.tencent.qqmusic.openapisdk.core.InitConfig
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.network.INetworkCheckInterface
import com.tencent.qqmusic.openapisdk.core.network.InitNetworkConfig
import com.tencent.qqmusic.openapisdk.core.network.NetworkTimeoutConfig
import com.tencent.qqmusic.openapisdk.hologram.PlayerUIProvider
import com.tencent.qqmusic.openapisdk.playerui.PlayerStyleManager
import com.tencent.qqmusic.playerinsight.CustomInsightConfig
import com.tencent.qqmusic.qplayer.baselib.util.GsonHelper
import com.tencent.qqmusic.qplayer.baselib.util.Md5Utils
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.player.DefaultImageLoader
import com.tencent.qqmusic.qplayer.utils.PrivacyManager
import com.tencent.qqmusic.qplayer.utils.SettingsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by tannyli on 2021/8/31.
 * Copyright (c) 2021 TME. All rights reserved.
 */
@Keep
class App : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        context = this
    }

    override fun onCreate() {
        super.onCreate()

        if (!ProcessUtil.inMainProcess(this)) {
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

        @JvmStatic
        fun init(context: Context) {
            Log.i(TAG, "init Application")

            //按需初始化playerKit 播放器模块
            PlayerStyleManager.observeForever(Observer {
                if (it.from == PlayerStyleManager.SET_STYLE_PERMISSION_DENIED) {
                    ToastUtils.showLong("播放器样式权益已失效")
                } else if (it.from == PlayerStyleManager.SET_STYLE_DEFAULT_NULL) {
                    ToastUtils.showLong("没有设置播放器样式")
                }
                MLog.i("PlayerStyleManager", "updatePlayerStyle $it， ")
            })
            PlayerStyleManager.setImageLoader(DefaultImageLoader())
            if (!BuildConfig.isLiteSDK) {
                OpenApiSDK.addProvider(PlayerUIProvider())
            }
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

            BaseFunctionManager.proxy.initDebug(BuildConfig.IS_DEBUG)

            val isUseForegroundService = sharedPreferences?.getBoolean("isUseForegroundService", true) ?: true
            val logFileDir = sharedPreferences?.getString("logFileDir", "")
            val savedTimeoutConfig = sharedPreferences?.getString("NetworkTimeoutConfig", "")
            val enableAccountPartner = sharedPreferences?.getBoolean("accountModePartner", false) ?: false
            val lowMemoryMode = sharedPreferences?.getBoolean("lowMemoryMode", false) ?: false
            val timeoutConfig = GsonHelper.safeFromJson(savedTimeoutConfig, NetworkTimeoutConfig::class.java) ?: NetworkTimeoutConfig.DEFAULT()
            val isMutiChannel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2
            MLog.i(TAG, "isMutiChannel:$isMutiChannel ,${Build.VERSION.SDK_INT}")
            val demoHardwareInfo = sharedPreferences?.getString("demoHardwareInfo", "L9") ?: "L9"
            val demoBrand = sharedPreferences?.getString("demoBrand", "Xiaomi") ?: "Xiaomi"
            val demoOpiDeviceId = sharedPreferences?.getString("demoOpiDeviceId", "123456789") ?: "123456789"
            val showAudioEffectToast = sharedPreferences?.getBoolean("demoAudioEffectToast", true) ?: true
            val printPlayStuckTrack = sharedPreferences?.getBoolean("demoPrintPlayStuckTrack",false)?:false
            val initConfig = InitConfig(
                context.applicationContext,
                MustInitConfig.APP_ID,
                MustInitConfig.APP_KEY,
                demoOpiDeviceId, //合作方请自行获取唯一设备ID并传入
            ).apply {
                this.appForeground = true
                this.isUseForegroundService = isUseForegroundService
                this.crashConfig = InitConfig.CrashConfig(enableNativeCrashReport = true, enableAnrReport = true)
                this.deviceConfigInfo.apply {
                    this.brand = demoBrand
                    this.hardwareInfo = demoHardwareInfo
                    this.lowMemoryMode = lowMemoryMode
                    // 设置设备类型
                    this.deviceType = when (Md5Utils.getMD5String(MustInitConfig.APP_ID)) {
                        "a3ef4dd61511b86a1e288ed3df6223fb" -> DeviceType.PHONE
                        else -> DeviceType.CAR
                    }
                }
                this.insightConfig = CustomInsightConfig(true, showAudioEffectToast, printPlayStuckTrack)
                this.enableBluetoothListener = false
                this.useMediaPlayerWhenPlayDolby = sharedPreferences?.getBoolean("useMediaPlayerWhenPlayDolby", false) ?: false
                this.logFileDir = logFileDir
                this.networkTimeoutConfig = timeoutConfig
                this.isMutiChannel = isMutiChannel
                if (enableAccountPartner) {
                    this.accountMode = InitConfig.AccountMode.PARTNER_INDEPENDENT
                }
                if (sharedPreferences?.getBoolean("useCustomNetworkCheck", false) == true) {
                    val networkStatus = sharedPreferences.getBoolean("networkAvailable", true)
                    SettingsUtil.isNetworkAvailable = networkStatus
                    this.networkChecker = object : INetworkCheckInterface {
                        override fun isNetworkAvailable(): Boolean {
                            return SettingsUtil.isNetworkAvailable
                        }
                    }
                }
                this.appCallback = object : IAPPCallback {
                    override fun getProfileTag(): String {
//                        return JSONObject().apply {
//                            put("name", "sss")
//                        }.toString()
                        return "{\"tags\":[\"chinoiserie\",\"china_chic\",\"folk\",\"electronica\"],\"cityLevel\":\"first_tier_city\"}"
                    }
                }

                needWnsPushService = sharedPreferences?.getBoolean("enableWns", true) ?: true
                initNetworkConfig = InitNetworkConfig(enableQuic = true)
            }
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