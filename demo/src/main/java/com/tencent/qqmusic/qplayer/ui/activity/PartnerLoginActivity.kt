package com.tencent.qqmusic.qplayer.ui.activity

import android.app.Activity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.google.gson.GsonBuilder
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.JobDispatcher

class PartnerLoginActivity : Activity() {

    companion object {
        private const val TAG = "PartnerLoginActivity"
    }

    private lateinit var partner_app_id_edt: EditText
    private lateinit var partner_account_id_edt: EditText
    private lateinit var partner_access_token_edt: EditText
    private val messageTextView: TextView by lazy {
        findViewById<TextView>(R.id.return_message)
    }

    private var partner_app_id_str: String? = null
    private var partner_id_str: String? = null
    private var partner_name_str: String? = null
    private var partner_account_id_str: String? = null
    private var partner_access_token_str: String? = null

    private val prettyGson by lazy {
        GsonBuilder().setPrettyPrinting().create()
    }

    private fun getParamStr() {
        partner_app_id_str = partner_app_id_edt.text?.toString()
        partner_account_id_str = partner_account_id_edt.text?.toString()
        partner_access_token_str = partner_access_token_edt.text?.toString()
        if (partner_app_id_str.isNullOrEmpty()) {
            partner_app_id_str = null
            partner_name_str = partner_app_id_str
        }
        if (partner_account_id_str.isNullOrEmpty()) {
            partner_account_id_str = null
            partner_id_str = partner_account_id_str
        }
        if (partner_access_token_str.isNullOrEmpty()) {
            partner_access_token_str = null
        }
    }

    private fun displayResult(response: OpenApiResponse<*>?, message: String?) {
        val builder = StringBuilder()
        if (response != null) {
            builder.append("OpenApiResponse: ").append("\n")
            builder.append("ret=").append(response.ret).append("\n")
            builder.append("subRet=").append(response.subRet).append("\n")
            builder.append("errorMsg=").append(response.errorMsg).append("\n")
            builder.append("page=").append(response.page).append("\n")
            builder.append("totalCount=").append(response.totalCount).append("\n")
            builder.append("hasMore=").append(response.hasMore).append("\n")
            builder.append("data=").append(prettyGson.toJson(response.data)).append("\n")
        }
        if (!message.isNullOrEmpty()) {
            builder.append("\n").append(message).append("\n")
        }
        val text = builder.toString()
        val spannable = SpannableStringBuilder(text)
        messageTextView.text = spannable
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner_login)

        partner_app_id_edt = findViewById(R.id.edt_partner_app_id)
        partner_account_id_edt = findViewById(R.id.edt_partner_account_id)
        partner_access_token_edt = findViewById(R.id.edt_partner_access_token)

        findViewById<View>(R.id.btn_setupPartnerEnv).setOnClickListener {
            JobDispatcher.doOnBackground {
                getParamStr()
                val bindStatus = OpenApiSDK.getOpenApi().setupPartnerEnv(
                    partner_app_id_str ?: "",
                    partner_access_token_str ?: "",
                    partner_account_id_str ?: "",
                )
                val message = if (bindStatus == 1) {
                    "成功设置第三方信息，该第三方信息已经绑定"
                } else {
                    "成功设置第三方信息，该第三方信息没有绑定或出错，code：$bindStatus"
                }
                JobDispatcher.doOnMain {
                    displayResult(null, message)
                }
            }
        }

        findViewById<View>(R.id.btn_clearPartnerEnv).setOnClickListener {
            JobDispatcher.doOnBackground {
                getParamStr()
                OpenApiSDK.getOpenApi().removePartnerEnv()
                JobDispatcher.doOnMain {
                    displayResult(null, "已清空第三方信息")
                }
            }
        }

        findViewById<View>(R.id.btn_bind).setOnClickListener {
            JobDispatcher.doOnBackground {
                OpenApiSDK.getOpenApi().bindPartnerAccount(
                    Global.getLoginModuleApi().partnerIdInfo?.partnerAccountId ?: "",
                    callback = {
                        JobDispatcher.doOnMain {
                            displayResult(it, null)
                        }
                    }
                )
            }
        }

        findViewById<View>(R.id.btn_unbind).setOnClickListener {
            JobDispatcher.doOnBackground {
                OpenApiSDK.getOpenApi().unbindPartnerAccount(callback = {
                    JobDispatcher.doOnMain {
                        displayResult(it, null)
                    }
                })
            }
        }
    }
}