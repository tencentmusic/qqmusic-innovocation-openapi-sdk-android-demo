package com.tencent.qqmusic.qplayer.ui.activity.main

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.business_common.event.BaseBusinessEvent
import com.tencent.qqmusic.openapisdk.business_common.event.BusinessEventHandler
import com.tencent.qqmusic.openapisdk.business_common.event.event.LoginEvent
import com.tencent.qqmusic.openapisdk.business_common.event.event.TransactionEvent
import com.tencent.qqmusic.openapisdk.business_common.event.event.TransactionPushData
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.ISDKSpecialNeedInterface
import com.tencent.qqmusic.openapisdk.core.player.PlayerModuleFunctionConfigParam
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.home.VIPSuccessDialog
import com.tencent.qqmusic.qplayer.ui.activity.player.FloatingPlayerPage
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver
import com.tencent.qqmusicplayerprocess.service.NotificationParams

class DemoActivity : ComponentActivity() {
    private val TAG = "DemoActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPlay()
        setContent {
            MainScreen()
        }
    }


    private fun initPlay() {
        val sharedPreferences: SharedPreferences? = try {
            getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
            null
        }
        OpenApiSDK.getPlayerApi().setEnableCallStateListener(false)
        OpenApiSDK.getPlayerApi().setSDKSpecialNeedInterface(object : ISDKSpecialNeedInterface {

            val title: String = try {
                UtilContext.getApp().applicationInfo.loadLabel(UtilContext.getApp().packageManager).toString() + " 正在运行"
            } catch (e: Exception) {
                MLog.e(TAG, "Application e :", e)
                "音乐程序正在运行"
            }

            override fun getNotification(playSong: SongInfo?): Notification? {
                var notification: Notification? = null
                var builder: Notification.Builder? = null
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channelId = "10000"
                            val channelName = "新聊天消息"
                            val importance = NotificationManager.IMPORTANCE_HIGH
                            val notificationManager = getSystemService(Application.NOTIFICATION_SERVICE) as NotificationManager
                            val channel =
                                NotificationChannel(channelId, channelName, importance)
                            notificationManager.createNotificationChannel(channel)
                        }

                        builder = Notification.Builder(UtilContext.getApp(), "10000")
                        builder.setContentTitle(title)
                            .setContentText(playSong?.songName ?: "")
                            .setSmallIcon(R.drawable.icon_notification)
                            .setOngoing(false)
                            .setCategory(Notification.CATEGORY_SERVICE)
                        notification = builder.build()
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN -> {
                        builder = Notification.Builder(UtilContext.getApp())
                        builder.setContentTitle(NotificationParams.SNotificationTitle)
                        builder.setSmallIcon(R.drawable.icon_notification)
                        builder.setOngoing(false)
                        notification = builder.build()
                    }
                    else -> {
                        notification = Notification()
                    }
                }
                return notification
            }

            val param = PlayerModuleFunctionConfigParam().apply {
                enableRestorePlaylistFunctionality = sharedPreferences?.getBoolean("restore_play_list", true) ?: true
                autoPlayErrNum = sharedPreferences?.getInt("restore_play_list_err_num", 0) ?: 0
                isAutoPlayNext = sharedPreferences?.getBoolean("error_auto_next", true) ?: true
                playWhenRequestFocusFailed = sharedPreferences?.getBoolean("playWhenRequestFocusFailed", true) ?: true

            }

            override fun getPlayerModuleFunctionConfigParam(): PlayerModuleFunctionConfigParam {
                return param
            }


        })


        OpenApiSDK.getPlayerApi().setEnableBluetoothListener(false)

        PlayerObserver.registerSongEvent()
    }

}

@Composable
fun loginExpiredDialog(showDialog: Boolean, setShowDialog: (Boolean) -> Unit) {
    if (showDialog) {
        Dialog(onDismissRequest = {

        }) {
            Column(
                modifier = Modifier
                    .background(color = Color.Yellow)
                    .width(300.dp)
                    .height(300.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "登录过期啦")
                Button(onClick = {
                    setShowDialog(false)
                }, modifier = Modifier.padding(16.dp)) {
                    Text("知道了")
                }
            }
        }
    }
}

@Composable
fun Navigation(navController: NavHostController, categoryViewModel: HomeViewModel) {
    NavHost(navController, startDestination = NavigationItem.Home.route) {
        composable(NavigationItem.Home.route) {
            HomeScreen(categoryViewModel)
        }
        composable(NavigationItem.Books.route) {
            SearchScreen()
        }
        composable(NavigationItem.Profile.route) {
            MineScreen()
        }
        composable(NavigationItem.Setting.route) {
            AppSetting()
        }
    }
}

@Composable
fun TopBar(title: String = stringResource(R.string.app_name)) {
    TopAppBar(
        title = { Text(text = title, fontSize = 18.sp) }, contentColor = Color.White
    )
}

@Composable
fun TopBarPreview() {
    TopBar()
}

@Composable
fun BottomNavigationBar(navController: NavController) {

    val items = listOf(
        NavigationItem.Home, NavigationItem.Books, NavigationItem.Profile, NavigationItem.Setting
    )
    BottomNavigation(
        contentColor = Color.White
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            BottomNavigationItem(icon = { Icon(painterResource(id = item.icon), contentDescription = item.title) },
                label = { Text(text = item.title, fontSize = 10.sp) },
                selectedContentColor = Color.White,
                unselectedContentColor = Color.White.copy(0.4f),
                alwaysShowLabel = true,
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                })
        }
    }
}

@Composable
fun MainScreen(categoryViewModel: HomeViewModel = viewModel()) {
    categoryViewModel.fetchUserLoginStatus()
    val navController = rememberNavController()
    var showVipDialog = remember {
        mutableStateOf(false)
    }
    var vipData: MutableState<TransactionPushData?> = remember {
        mutableStateOf(null)
    }


    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val listener = object : BusinessEventHandler {
            override fun handle(event: BaseBusinessEvent) {
                when (event.code) {
                    LoginEvent.UserAccountLoginExpired -> {
                        setShowDialog(true)
                    }
                    TransactionEvent.TransactionEventCode -> {
                        showVipDialog.value = true
                        vipData.value = event.data as TransactionPushData
                    }
                }
            }
        }

        OpenApiSDK.registerBusinessEventHandler(listener)
        onDispose {
            OpenApiSDK.unregisterBusinessEventHandler(listener)
        }
    }
    loginExpiredDialog(showDialog = showDialog, setShowDialog = setShowDialog)
    if (showVipDialog.value) {
        VIPSuccessDialog(
            vipData.value,
            categoryViewModel
        ) {
            showVipDialog.value = false
        }
    }
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (host, playerPage, bottomNavigationBar) = createRefs()
        Box(modifier = Modifier.constrainAs(host) {
            top.linkTo(parent.top)
            bottom.linkTo(playerPage.top)
            height = Dimension.fillToConstraints
        }) {
            Navigation(navController = navController, categoryViewModel)
        }
        Box(modifier = Modifier.constrainAs(playerPage) {
            bottom.linkTo(bottomNavigationBar.top)
        }) {
            FloatingPlayerPage()
        }
        Box(modifier = Modifier.constrainAs(bottomNavigationBar) {
            bottom.linkTo(parent.bottom)
        }) {
            BottomNavigationBar(navController)
        }
    }

}