package com.tencent.qqmusic.qplayer.ui.activity.login


import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.LoginApi
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.utils.UiUtils

class TokenLoginActivity : ComponentActivity() {
    private val impl = OpenApiSDK.getLoginApi()

    private val sharedPreferences: SharedPreferences? by lazy {
        try {
            getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            QLog.e("PartnerLoginActivity", "getSharedPreferences error e = ${e.message}")
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PartnerLoginPage(impl, sharedPreferences) }
    }
}

@Preview
@Composable
fun PartnerLoginPage(impl: LoginApi? = null, sharedPreferences: SharedPreferences? = null) {
    val execute = " 执行中  "
    val partnerCode = remember {
        mutableStateOf(sharedPreferences?.getString("partnerCode", "") ?: "")
    }

    val partnerBiz = remember {
        mutableStateOf(sharedPreferences?.getString("partnerBiz", "") ?: "")
    }

    val openId = remember {
        mutableStateOf(sharedPreferences?.getString("openId", "") ?: "")
    }

    val loginToken = remember {
        mutableStateOf(sharedPreferences?.getString("loginToken", "") ?: "")
    }

    val result = remember {
        mutableStateOf("")
    }

    val cbForceBind = remember{
        mutableStateOf(true)
    }

    ConstraintLayout(modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())) {
        val (appIdView, appAccountView, appTokenView, loginView, writeAccount, bizView, deleteView, queryView, queryAccountIdView, resultView) = createRefs()
        TextField(value = partnerCode.value, onValueChange = {
            partnerCode.value = it
        }, label = { Text(text = "partnerCode") }, modifier = Modifier
            .padding(10.dp)
            .constrainAs(appIdView) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(writeAccount.start)
                width = Dimension.fillToConstraints
            })

        TextField(value = partnerBiz.value, onValueChange = {
            partnerBiz.value = it
        }, label = { Text(text = "partnerBiz") }, modifier = Modifier
            .padding(10.dp)
            .constrainAs(bizView) {
                top.linkTo(appIdView.bottom)
                start.linkTo(appIdView.start)
                end.linkTo(writeAccount.start)
                width = Dimension.fillToConstraints
            })

        TextField(value = openId.value, onValueChange = {
            openId.value = it
        }, label = { Text(text = "openId") }, modifier = Modifier
            .padding(10.dp)
            .constrainAs(appAccountView) {
                top.linkTo(bizView.bottom)
                start.linkTo(appIdView.start)
                end.linkTo(appIdView.end)
                width = Dimension.fillToConstraints
            })

        TextField(value = loginToken.value, onValueChange = {
            loginToken.value = it
        }, label = { Text(text = "loginToken") }, modifier = Modifier
            .padding(10.dp)
            .constrainAs(appTokenView) {
                top.linkTo(appAccountView.bottom)
                start.linkTo(appAccountView.start)
                end.linkTo(appAccountView.end)
                width = Dimension.fillToConstraints
            })

        Button(onClick = {
            val com = sharedPreferences?.edit()?.apply {
                putString("partnerCode", partnerCode.value)
                putString("openId", openId.value)
                putString("loginToken", loginToken.value)
                putString("partnerBiz", partnerBiz.value)
            }?.commit()
            result.value = if (sharedPreferences != null && com == true) "写入成功" else "写入失败"
        }, modifier = Modifier
            .fillMaxHeight()
            .padding(10.dp)
            .constrainAs(writeAccount) {
                height = Dimension.fillToConstraints
                width = Dimension.preferredWrapContent
                end.linkTo(parent.end)
                top.linkTo(parent.top)
                bottom.linkTo(appTokenView.bottom)

            }) {
            Text(text = "写入帐号信息")
        }
        Box(
            modifier = Modifier
                .height(60.dp)
                .width(200.dp)
                .constrainAs(resultView) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(writeAccount.bottom)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = result.value)
        }

        Button(onClick = {
            result.value = execute
            impl?.qqMusicTokenLogin(partnerCode.value, partnerBiz.value, openId.value, loginToken.value
            ) { suc, msg ->
                UiUtils.showToast("登录结果：$suc $msg")
//                if (suc) {
//                    mineViewModel.updateData()
//                }
            }
        }, modifier = Modifier
            .padding(start = 30.dp, top = 30.dp, end = 30.dp)
            .height(60.dp)
            .constrainAs(loginView) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(resultView.bottom)
            }) {
            Text(text = "登录第三方帐号")
        }

    }
}


