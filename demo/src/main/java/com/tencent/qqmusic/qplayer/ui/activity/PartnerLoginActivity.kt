package com.tencent.qqmusic.qplayer.ui.activity


import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
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
import com.tencent.qqmusic.openapisdk.core.login.IPartnerLogin
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.baselib.util.QLog

class PartnerLoginActivity : ComponentActivity() {
    private val impl = OpenApiSDK.getPartnerApi()

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
fun PartnerLoginPage(impl: IPartnerLogin? = null, sharedPreferences: SharedPreferences? = null) {
    val execute = " 执行中  "
    val appId = remember {
        mutableStateOf(sharedPreferences?.getString("appId", "") ?: "")
    }

    val accountId = remember {
        mutableStateOf(sharedPreferences?.getString("accountId", "") ?: "")
    }

    val token = remember {
        mutableStateOf(sharedPreferences?.getString("token", "") ?: "")
    }

    val result = remember {
        mutableStateOf("")
    }

    val cbForceBind = remember{
        mutableStateOf(true)
    }

    ConstraintLayout(modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())) {
        val (appIdView, appAccountView, appTokenView, loginView, writeAccount, bindView, deleteView, queryView, queryAccountIdView, resultView) = createRefs()
        TextField(value = appId.value, onValueChange = {
            appId.value = it
        }, label = { Text(text = "AppId") }, modifier = Modifier
            .padding(10.dp)
            .constrainAs(appIdView) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(writeAccount.start)
                width = Dimension.fillToConstraints
            })

        TextField(value = accountId.value, onValueChange = {
            accountId.value = it
        }, label = { Text(text = "AccountId") }, modifier = Modifier
            .padding(10.dp)
            .constrainAs(appAccountView) {
                top.linkTo(appIdView.bottom)
                start.linkTo(appIdView.start)
                end.linkTo(appIdView.end)
                width = Dimension.fillToConstraints
            })

        TextField(value = token.value, onValueChange = {
            token.value = it
        }, label = { Text(text = "token") }, modifier = Modifier
            .padding(10.dp)
            .constrainAs(appTokenView) {
                top.linkTo(appAccountView.bottom)
                start.linkTo(appAccountView.start)
                end.linkTo(appAccountView.end)
                width = Dimension.fillToConstraints
            })

        Button(onClick = {
            val com = sharedPreferences?.edit()?.apply {
                putString("appId", appId.value)
                putString("accountId", accountId.value)
                putString("token", token.value)
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

        Row (
            modifier = Modifier
                .padding(start = 30.dp, top = 60.dp, end = 30.dp)
                .fillMaxWidth()
                .height(60.dp)
                .constrainAs(bindView) {
                    width = Dimension.fillToConstraints
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(resultView.bottom)
                },
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = cbForceBind.value,
                onCheckedChange = {cbForceBind.value = it}
            )
            Text(text = "强制绑定", modifier = Modifier.padding(5.dp))
            Button(onClick = {
                result.value = execute
                impl?.updateThirdPartyAccount(appId.value, token.value, accountId.value,
                    cbForceBind.value
                ) { response -> result.value = "ret: ${response?.ret} msg : ${response?.errorMsg} bindAccountID: ${response?.data}" }
            }, modifier = Modifier.fillMaxSize()
                ) {
                Text(text = "绑定音乐帐号")
            }
        }

        Button(onClick = {
            result.value = execute
            impl?.deleteThirdPartyAccountBindState(appId.value, token.value, accountId.value
            ) { response -> result.value = "ret: ${response?.ret} msg : ${response?.errorMsg}" }
        }, modifier = Modifier
            .padding(start = 30.dp, top = 30.dp, end = 30.dp)
            .height(60.dp)
            .constrainAs(deleteView) {
                width = Dimension.fillToConstraints
                start.linkTo(bindView.start)
                end.linkTo(bindView.end)
                top.linkTo(bindView.bottom)
            }) {
            Text(text = "解除绑定")
        }

        Button(onClick = {
            result.value = execute
            impl?.queryThirdPartyAccountBindState(appId.value, token.value, accountId.value
            ) { response -> result.value = "ret: ${response?.ret} msg : ${response?.errorMsg}  bindAccountID: ${response?.data}" }
        }, modifier = Modifier
            .padding(start = 30.dp, top = 30.dp, end = 30.dp)
            .height(60.dp)
            .constrainAs(queryView) {
                width = Dimension.fillToConstraints
                start.linkTo(deleteView.start)
                end.linkTo(deleteView.end)
                top.linkTo(deleteView.bottom)
            }) {
            Text(text = "查询绑定状态")
        }

        Button(onClick = {
            result.value = execute
            result.value = "当前登录帐号的ID 是  ${impl?.queryThirdPartyAccountID()}"
        }, modifier = Modifier
            .padding(start = 30.dp, top = 30.dp, end = 30.dp)
            .height(60.dp)
            .constrainAs(queryAccountIdView) {
                width = Dimension.fillToConstraints
                start.linkTo(queryView.start)
                end.linkTo(queryView.end)
                top.linkTo(queryView.bottom)
            }) {
            Text(text = "查询当前第三方帐号ID")
        }

        Button(onClick = {
            result.value = execute
            impl?.thirdPartyAccountLogin(appId.value, token.value, accountId.value
            ) { response -> result.value = "ret: ${response?.ret} msg : ${response?.errorMsg}" }
        }, modifier = Modifier
            .padding(start = 30.dp, top = 30.dp, end = 30.dp)
            .height(60.dp)
            .constrainAs(loginView) {
                width = Dimension.fillToConstraints
                start.linkTo(queryAccountIdView.start)
                end.linkTo(queryAccountIdView.end)
                top.linkTo(queryAccountIdView.bottom)
            }) {
            Text(text = "登录第三方帐号")
        }

    }
}


