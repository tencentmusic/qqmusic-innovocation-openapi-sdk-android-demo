package com.tencent.qqmusic.qplayer.ui.activity.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.App
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.QLog

class DemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.init(this)
        setContent {
            MainScreen()
        }
    }

//    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
//        super.onRestoreInstanceState(savedInstanceState)
//        AppScope.launchIO {
//            val songList = SongListRepo().fetchSongInfoByFolder("8513652479", 0, 50).data
//                ?: emptyList()
//            Log.i("lichaojian", "lichaojian onRestoreInstanceState")
//            OpenApiSDK.getPlayerApi().updatePlayingSongList(songList)
//        }
//    }
}

@Composable
fun loginExpiredDialog(showDialog: Boolean, setShowDialog: (Boolean) -> Unit) {
    if (showDialog) {
        Dialog(onDismissRequest = {

        }) {
            Column(
                modifier = Modifier.background(color = Color.Yellow).width(300.dp).height(300.dp),
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
fun Navigation(navController: NavHostController) {
    NavHost(navController, startDestination = NavigationItem.Home.route) {
        composable(NavigationItem.Home.route) {
            HomeScreen()
        }
        composable(NavigationItem.Books.route) {
            SearchScreen()
        }
        composable(NavigationItem.Profile.route) {
            MineScreen()
        }
        composable(NavigationItem.Other.route) {
            OtherScreen()
        }
    }
}

@Composable
fun TopBar(title: String = stringResource(R.string.app_name)) {
    TopAppBar(
        title = { Text(text = title, fontSize = 18.sp) },
        contentColor = Color.White
    )
}

@Composable
fun TopBarPreview() {
    TopBar()
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Books,
        NavigationItem.Profile,
        NavigationItem.Other
    )
    BottomNavigation(
        contentColor = Color.White
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            BottomNavigationItem(
                icon = { Icon(painterResource(id = item.icon), contentDescription = item.title) },
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
                }
            )
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
    loginExpiredDialog(showDialog = showDialog, setShowDialog = setShowDialog)
    OpenApiSDK.getLoginApi().setLoginStateErrorCallback {
        QLog.i("DemoActivity", "setLoginStateErrorCallback msg = $it")
        setShowDialog(true)
    }
    Scaffold(
//        topBar = { TopBar() },
        bottomBar = { BottomNavigationBar(navController) }
    ) {
        Box(modifier = Modifier.padding(it)) {
            Navigation(navController = navController)
        }
    }
}