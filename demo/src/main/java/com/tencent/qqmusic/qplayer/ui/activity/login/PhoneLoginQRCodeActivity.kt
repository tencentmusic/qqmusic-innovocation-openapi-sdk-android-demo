package com.tencent.qqmusic.qplayer.ui.activity.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.AuthType
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


// 
// Created by clydeazhang on 2022/1/6 2:30 下午.
// Copyright (c) 2022 Tencent. All rights reserved.
// 
class PhoneLoginQRCodeActivity : Activity() {

    private var isFinished = false

    companion object {
        private const val TAG = "PhoneLoginQRCodeActivity"
        private const val KEY_PHONE_TOKEN = "KEY_PHONE_TOKEN"
        private const val KEY_AUTH_CODE = "KEY_AUTH_CODE"
        private const val KEY_QR_CODE = "KEY_QR_CODE"
        private const val POLL_INTERVAL = 1000L
        private const val ERROR_EXPIRE = -10
        private const val ERROR_NO_SCAN = -11
        private const val ERROR_SCAN_SUC = -19  // 扫码成功，待确认
        private const val ERROR_CANCEL = -14
        private const val ERROR_BIND_ACCOUNT = -22

        @JvmStatic
        fun start(context: Context, phoneToken: String, authCode: String, qrCode: String) {
            val starter = Intent(context, PhoneLoginQRCodeActivity::class.java)
                .putExtra(KEY_PHONE_TOKEN, phoneToken)
                .putExtra(KEY_AUTH_CODE, authCode)
                .putExtra(KEY_QR_CODE, qrCode)
            context.startActivity(starter)
        }
    }

    private val ivQrCode by lazy {
        findViewById<ImageView>(R.id.iv_qrcode)
    }
    private val tvTips by lazy {
        findViewById<TextView>(R.id.tv_tips)
    }
    private val btnRefresh by lazy {
        findViewById<TextView>(R.id.btn_refresh)
    }

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val phoToken by lazy {
        intent.getStringExtra(KEY_PHONE_TOKEN) ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_openapisdk_qrcode)

        btnRefresh.setOnClickListener {
            startGetQrCode()
        }

        GlobalScope.launch{
            val bitmap = UiUtils.generateQRCode(intent.getStringExtra(KEY_QR_CODE))
            runOnUiThread {
                if (bitmap != null) {
                    showQrCode(bitmap)
                    if (!isDestroyed) {
                        handler.removeCallbacksAndMessages(null)
                        handler.postDelayed(Runnable {
                            requestQrCodeAuthResult(intent.getStringExtra(KEY_AUTH_CODE) ?: "")
                        }, POLL_INTERVAL)
                    }
                } else {
                    showTips("二维码生成失败", true)
                }
            }
        }
    }

    private fun startGetQrCode() {
        showTips("二维码加载中...", false)
        Global.getOpenApi().getLoginQrCode() {
            if (it.isSuccess()) {
                GlobalScope.launch {
                    val bitmap = UiUtils.generateQRCode(it.data!!.qrCode)
                    runOnUiThread {
                        if (bitmap != null) {
                            showQrCode(bitmap)
                            if (!isDestroyed) {
                                handler.postDelayed(Runnable {
                                    requestQrCodeAuthResult(it.data!!.authCode)
                                }, POLL_INTERVAL)
                            }
                        } else {
                            showTips("二维码生成失败", true)
                        }
                    }
                }
            } else {
                showTips(it.errorMsg ?: "二维码加载失败", true)
            }
        }
    }

    @SuppressLint("LongLogTag")
    private fun requestQrCodeAuthResult(pollAuthCode: String) {
        Global.getOpenApi().pollQrCodeBindResult(phoToken, pollAuthCode) {
            if (isFinished) {
                return@pollQrCodeBindResult
            }
            if (it.isSuccess()) {
                // 登录成功
                Log.i(TAG, "扫码绑定成功, authCode=${it.data}")
                OpenApiSDK.getLoginApi().onGetEncryptData(AuthType.PHONE, it.data ?: "")
                finish()
            } else {
                if (it.ret == ERROR_EXPIRE) {
                    showTips("二维码已过期", true)
                } else if (it.ret == ERROR_CANCEL) {
                    // 取消以后需要刷新二维码
                    startGetQrCode()
                } else if (it.ret == ERROR_NO_SCAN || it.ret == ERROR_SCAN_SUC) {
                    if (it.ret == ERROR_SCAN_SUC) {
                        Toast.makeText(this, "扫码成功，待确认", Toast.LENGTH_SHORT).show()
                    }
                    if (!isDestroyed) {
                        handler.postDelayed(Runnable {
                            requestQrCodeAuthResult(pollAuthCode)
                        }, POLL_INTERVAL)
                    }
                } else if (it.ret == ERROR_BIND_ACCOUNT) {
                    showTips("绑定账户失败", true)
                } else {
                    showTips("轮询结果失败：${it.errorMsg}", true)
                }
            }
        }
    }

    private fun showTips(tips: String, needBtn: Boolean) {
        ivQrCode.visibility = View.GONE
        tvTips.visibility = View.VISIBLE
        if (needBtn) {
            btnRefresh.visibility = View.VISIBLE
        } else {
            btnRefresh.visibility = View.GONE
        }
        tvTips.text = tips
    }

    private fun showQrCode(bitmap: Bitmap) {
        ivQrCode.visibility = View.VISIBLE
        tvTips.visibility = View.GONE
        btnRefresh.visibility = View.GONE
        ivQrCode.setImageBitmap(bitmap)
    }

    override fun finish() {
        isFinished = true
        super.finish()
        handler.removeCallbacksAndMessages(null)
    }
}