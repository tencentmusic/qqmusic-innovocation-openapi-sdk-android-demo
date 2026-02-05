package com.tencent.qqmusic.qplayer.ui.activity.login


import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.login.LoginApi
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.main.CopyableText
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.utils.UiUtils

class TokenLoginActivity : ComponentActivity() {

    private val impl = OpenApiSDK.getLoginApi()

    private val sharedPreferences: SharedPreferences? by lazy {
        try {
            getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "getSharedPreferences error e = ${e.message}")
            null
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Scaffold(topBar = { TopBar("token登录") },
            modifier = Modifier.semantics{ testTagsAsResourceId=true }) {
            PartnerLoginPage(impl, sharedPreferences)
        } }
        val deviceToken = OpenApiSDK.getLoginApi().getDeviceLoginToken()
        Log.d(TAG, "deviceToken = $deviceToken")
    }

    companion object {
        private const val TAG = "TokenLogin"
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

    val deviceToken = remember {
        mutableStateOf(sharedPreferences?.getString("partner_deviceToken", "")?:"")
    }

    val result = remember {
        mutableStateOf("")
    }

    val cbForceBind = remember{
        mutableStateOf(true)
    }

    ConstraintLayout(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(state = rememberScrollState())) {
        val (appIdView, appAccountView, appTokenView,loginView, writeAccount, bizView, deleteView, queryView, loginView2, currDeviceToken, deviceTokenView, dualView, resultView) = createRefs()
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
                putString("partner_deviceToken", deviceToken.value)
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
            impl?.deviceTokenLogin(loginToken.value
            ) { suc, msg ->
                UiUtils.showToast("登录结果：$suc $msg")
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
            Text(text = "登录设备帐号")
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
            .constrainAs(loginView2) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(loginView.bottom)
            }) {
            Text(text = "登录第三方帐号")
        }

        Box(modifier = Modifier
            .padding(10.dp)
            .constrainAs(currDeviceToken) {
                top.linkTo(loginView2.bottom)
                start.linkTo(loginView2.start)
                end.linkTo(loginView2.end)
                width = Dimension.fillToConstraints
            }){
            Row {
                val text = remember { mutableStateOf(impl?.getDeviceLoginToken()?:"无") }
                CopyableText("副屏token",text.value)
                IconButton(onClick = {
                    text.value = impl?.getDeviceLoginToken()?:"无"
                }) { Icon(Icons.Default.Refresh, contentDescription = "刷新") }
            }
        }

        TextField(value = deviceToken.value, onValueChange = {
            deviceToken.value = it
        }, label = { Text(text = "deviceToken") }, modifier = Modifier
            .padding(10.dp)
            .constrainAs(deviceTokenView) {
                top.linkTo(currDeviceToken.bottom)
                start.linkTo(currDeviceToken.start)
                end.linkTo(currDeviceToken.end)
                width = Dimension.fillToConstraints
            })

        Button(onClick = {
            result.value = execute
            impl?.deviceTokenLogin(deviceToken.value){ b,m->
                result.value = "登录结果:$b,$m"
                UiUtils.showToast(result.value)
            }
        }, modifier = Modifier
            .padding(start = 30.dp, top = 30.dp, end = 30.dp)
            .height(60.dp)
            .constrainAs(dualView) {
                width = Dimension.fillToConstraints
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(deviceTokenView.bottom)
            }
        ) {
            Text(text = "副屏登录")
        }



    }
}



