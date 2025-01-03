package com.tencent.qqmusic.qplayer.ui.activity.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.*
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.QLog

// 
// Created by clydeazhang on 2022/1/4 11:30 上午.
// Copyright (c) 2022 Tencent. All rights reserved.
// 
class WebViewActivity : Activity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        val url = intent.getStringExtra("url") ?: "null"
        QLog.i(TAG, "load url:$url")
        webView = findViewById<WebView>(R.id.web)
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
                        setData(Uri.parse(url))
                    }
                    startActivity(intent)
                    return true
                }
                return false
            }
        }
        webView?.post {
            Log.i(TAG, "webview width : ${webView?.width}")
        }

    }

    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }

}