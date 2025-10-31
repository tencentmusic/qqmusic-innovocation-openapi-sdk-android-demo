package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.Activity
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.ai.AIListenError
import com.tencent.qqmusic.openapisdk.model.VipInfo
import com.tencent.qqmusic.openapisdk.model.VipType
import com.tencent.qqmusic.openapisdk.model.aiaccompany.AIGlobalAccompanyRole
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.login.WebViewActivity
import com.tencent.qqmusic.qplayer.utils.UiUtils
import com.tencent.qqmusic.qplayer.utils.UiUtils.getVipText

object AIAccompanyDialog {
    const val TAG = "AIAccompanyDialog"
    fun show(activity: Activity) {
        val api = OpenApiSDK.getAIGlobalListenTogetherApi()
        api.getAiRoleList {
            if (it.isSuccess() && !it.data.isNullOrEmpty()) {
                showDialog(activity, it.data!!)
            } else {
                UiUtils.showToast("获取AI伴听列表失败")
            }
        }
    }

    fun showDialog(activity: Activity, options: List<AIGlobalAccompanyRole>) {
        OpenApiSDK.getOpenApi().fetchGreenMemberInformation {
            it.data
            showDialog(activity, options, it.data)
        }
    }

    private fun showDialog(
        activity: Activity,
        options: List<AIGlobalAccompanyRole>,
        vipInfo: VipInfo?
    ) {
        val currentRole = OpenApiSDK.getAIGlobalListenTogetherApi().getCurrentAIAccompanyRole()?.roleId
        val adapter =
            object : ArrayAdapter<AIGlobalAccompanyRole>(activity, R.layout.item_option, options) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: activity.layoutInflater.inflate(
                        R.layout.item_option,
                        parent,
                        false
                    )
                    val option = options[position]
                    view.findViewById<TextView>(R.id.text_option).text = option.name

                    val iv = view.findViewById<ImageView>(R.id.image_option)
                    Glide.with(iv.context).load(option.headIcon).into(iv)
                    val showVipType = option.getShowIcon(vipInfo)
                    // 在图片右下角添加小图标
                    val checkIcon = view.findViewById<ImageView>(R.id.op_option)
                    if (currentRole == option.roleId){
                        checkIcon.visibility = View.VISIBLE
                        checkIcon.setImageResource(android.R.drawable.checkbox_on_background)
                        checkIcon.alpha = 0.8f
                    }else{
                        checkIcon.visibility = View.GONE
                    }
                    val badgeIcon = view.findViewById<ImageView>(R.id.image_icon)
                    when (showVipType) {
                        VipType.GREEN_VIP -> {
                            badgeIcon.visibility = View.VISIBLE
                            Glide.with(badgeIcon.context)
                                .load(vipInfo?.greenVipIcon)
                                .into(badgeIcon)
                        }
                        VipType.SUPER_VIP -> {
                            badgeIcon.visibility = View.VISIBLE
                            Glide.with(badgeIcon.context)
                                .load(vipInfo?.hugeVipIcon)
                                .into(badgeIcon)
                        }
                        else -> badgeIcon.visibility = View.GONE
                    }
                    val info = view.findViewById<TextView>(R.id.text_info)
                    val infoBuilder = StringBuilder()
                    option.action?.access?.playAccess?.let {
                        val rightList = mutableListOf<String>()
                        if (it.normal==true){
                            rightList.add("普通")
                        }
                        if (it.greenVip==true){
                            rightList.add("绿钻")
                        }
                        if (it.svip==true){
                            rightList.add("超会")
                        }
                        if (it.iotVip==true){
                            rightList.add("IOT")
                        }
                        infoBuilder.append("(${rightList.joinToString("|")})")
                    }
                    info.text = infoBuilder.toString()

                    val info2 = view.findViewById<TextView>(R.id.text_info2)
                    val infoBuilder2 = StringBuilder()
                    val remainSecond = option.getRemainSecond()
                    if (option.isTryingListen()) {
                        infoBuilder2.append("正在试用,剩余${remainSecond}秒")
                    }else if (option.canOpenTryListen()) {
                        infoBuilder2.append("可试用${remainSecond}秒")
                    }else if (option.canUse(vipInfo).not()){
                        infoBuilder2.append("需要${getVipText(showVipType)}")
                    }
                    info2.text = infoBuilder2.toString()

                    val info3 = view.findViewById<TextView>(R.id.text_info3)
                    val infoBuilder3 = StringBuilder()
                    infoBuilder3.append(option.roleDesc)
                    info3.text = infoBuilder3.toString()
                    return view
                }
            }

        AlertDialog.Builder(activity).apply {
            setTitle("AI伴听解读")
            setSingleChoiceItems(adapter, 0) { dialog, which ->
                selectRole(options[which], activity, dialog)
            }
            setPositiveButton("打开伴听") { dialog, _ ->
                UiUtils.showToast(
                    OpenApiSDK.getAIGlobalListenTogetherApi().openAIListenTogether().msg
                )
                dialog.dismiss()
            }
            setNegativeButton("关闭伴听") { dialog, _ ->
                OpenApiSDK.getAIGlobalListenTogetherApi().closeAIListenTogether()
                dialog.dismiss()
            }
        }.show()
    }

    private fun selectRole(
        option: AIGlobalAccompanyRole,
        activity: Activity,
        alertDialog: DialogInterface
    ) {
        if (Global.getLoginModuleApi().hasLogin().not()){
            UiUtils.showToast("需要登录")
        }
        val ret =
            OpenApiSDK.getAIGlobalListenTogetherApi().selectAIAccompanyRole(option)
        when (ret) {
            AIListenError.NEED_ACCESS -> {
                UiUtils.showToast("切换失败:${ret.msg}")
                if (option.canOpenTryListen()) {
                    AlertDialog.Builder(activity).apply {
                        setMessage("是否开启试用？")
                        setPositiveButton("开启", { dialog, _ ->
                            OpenApiSDK.getAIGlobalListenTogetherApi().openGlobalAiAccompanyTry(option.roleId) {
                                if (it.isSuccess()) {
                                    UiUtils.showToast("已开启试用，请重新选择")
                                } else {
                                    UiUtils.showToast("开启试用失败:${it.errorMsg}")
                                    option.action?.buyVipUrl?.let { url ->
                                        WebViewActivity.start(
                                            activity,
                                            url
                                        )
                                    }
                                }
                                dialog.dismiss()
                            }
                        })
                        setNegativeButton("取消", { dialog, _ ->
                            dialog.dismiss()
                        })
                    }.show()
                } else {
                    option.action?.buyVipUrl?.let { url ->
                        WebViewActivity.start(
                            activity,
                            url
                        )
                    }
                }
            }

            AIListenError.SUCCESS -> {
                UiUtils.showToast("已切换到${option.name}")
                alertDialog.dismiss()
            }

            else -> {
                UiUtils.showToast("切换失败:${ret.msg}")
            }
        }
    }
}