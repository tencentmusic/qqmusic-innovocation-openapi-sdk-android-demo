package com.tencent.qqmusic.qplayer.ui.activity.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.net.toUri
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.QLog

// 
// Created by clydeazhang on 2022/1/4 11:30 上午.
// Copyright (c) 2022 Tencent. All rights reserved.
// 
class WebViewActivity : ComponentActivity() {

    private var webView: WebView? = null

    companion object {
        private const val TAG = "@@@WebViewActivity"

        @JvmStatic
        fun start(context: Context, url: String) {
            val starter = Intent(context, WebViewActivity::class.java)
                .putExtra("url", url)
            context.startActivity(starter)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        setContentView(R.layout.activity_web)
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        val url = intent.getStringExtra("url") ?: "null"
        QLog.i(TAG, "load url:$url")
        webView = findViewById(R.id.web)
        webView?.loadUrl(url)
        webView?.settings?.apply {
            this.javaScriptEnabled = true
        }
        webView?.webChromeClient = object : WebChromeClient() {

        }
        webView?.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                Log.d(TAG, "shouldInterceptRequest, url=${request?.url?.toString()}")
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d(TAG, "shouldOverrideUrlLoading 1, url=$url")
                if (url?.startsWith("weixin://wap/pay") == true) {  // 微信支付 deeplink
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setData(url.toUri())
                    }
                    startActivity(intent)
                    return true
                }
                if (url?.startsWith("qqmusic://qq.com/ui/closeWebview") == true) {  // qq音乐关闭webview
                    this@WebViewActivity.finish()
                    return true
                }
                return false
            }
        }
        webView?.post {
            Log.i(TAG, "webview width : ${webView?.width}")
        }

    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (webView?.canGoBack() == true) {
                webView?.goBack()
            } else {
                // 如果WebView无法返回，则执行默认的返回操作
                isEnabled = false  // 临时禁用回调以允许默认行为
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true   // 重新启用回调
            }
        }
    }

}