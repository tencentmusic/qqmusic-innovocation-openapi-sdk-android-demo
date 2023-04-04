package com.tencent.qqmusic.qplayer.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.App

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        OpenApiSDK.init(this.applicationContext, MustInitConfig.APP_ID, MustInitConfig.APP_KEY)

        findViewById<View>(R.id.btn_open_api_demo).setOnClickListener {
            startActivity(Intent(this, OpenApiDemoActivity::class.java))
        }

        findViewById<View>(R.id.btn_login_demo).setOnClickListener {
            OpenApiSDK.getLoginApi().qqMusicLogin(this) { success, errorMsg ->
                if (success) {
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, errorMsg ?: "未知异常", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<TextView>(R.id.tv).setOnClickListener {

        }
    }
}