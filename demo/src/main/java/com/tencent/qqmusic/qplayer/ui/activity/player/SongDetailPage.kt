package com.tencent.qqmusic.qplayer.ui.activity.player

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.annotation.ExperimentalCoilApi
import com.tencent.qqmusic.qplayer.R
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.innovation.common.util.UtilContext
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayCallback
import com.tencent.qqmusic.qplayer.ui.activity.ui.theme.QPlayerTheme

private val plachImageID: Int = R.drawable.musicopensdk_icon_light

@Composable
fun SongDetailPage(observer: PlayerObserver) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "播放信息", fontSize = 18.sp) },
                contentColor = Color.White,
                actions = {
                }
            )
        }
    ) {
        DetailPage(observer = observer)
    }
}

@OptIn(ExperimentalCoilApi::class)
@Preview
@Composable
fun DetailPage(observer: PlayerObserver? = null) {
    val songInfo = observer?.currentSong
    var vipInfo by remember { mutableStateOf(Global.getLoginModuleApi().vipInfo) }
    var canTryExcellentQuality by remember { mutableStateOf(false) }
    var canTryGalaxyQuality by remember { mutableStateOf(false) }

    QPlayerTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                .fillMaxSize()
//                .then(Modifier.verticalScroll(rememberScrollState())) //加了这个方法会导致图片加载不出来，原因未知
                .then(Modifier.background(color = Color(248, 248, 248)))
        ) {
            Row(modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) {
                Text(text = "歌曲名称 ： ")
                Text(text = songInfo?.songName ?: "当前未播放歌曲")
            }


            Row(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(text = "VIP歌曲 ： ")
                Text(text = if (songInfo?.vip == 1) "是" else "否")
            }

            if (songInfo?.isDigitalAlbum() == true) {
                Row(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(text = "数字专辑 ： ")
                    Text(text = "%.2f元".format((songInfo.payPrice ?: 0) / 100f))
                }
            }

            Row(modifier = Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "歌手 ： ")
                Text(text = songInfo?.singerName ?: "无")
                Image(
                    painter = rememberImagePainter(songInfo?.singerPic150x150 ?: plachImageID,
                        builder = {
                            crossfade(false)
                            placeholder(plachImageID)
                        }),
                    contentDescription = "",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(60.dp)
                        .clip(CircleShape)
                )

            }


            Row(modifier = Modifier.padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "专辑 ： ")
                Text(text = if (songInfo?.albumName.isNullOrEmpty()) "无" else songInfo?.albumName ?: "无")
                Image(
                    painter = rememberImagePainter(songInfo?.bigCoverUrl() ?: plachImageID,
                        builder = {
                            crossfade(false)
                            placeholder(plachImageID)
                        }),
                    contentDescription = "",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(60.dp)
                        .clip(CircleShape)
                )
            }

            Row(modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) {
                Text(text = "歌曲流派 ： ")
                Text(text = songInfo?.genre ?: "")
            }

            //技术类信息
            Box(
                Modifier
                    .background(color = Color(211, 211, 211), shape = RoundedCornerShape(3.dp))
                    .then(Modifier.padding(5.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row {
                        Text(text = "歌曲ID ： ")
                        SelectionContainer {
                            Text(text = songInfo?.songId?.toString() ?: "无")
                        }
                    }

                    Row(modifier = Modifier.padding(start = 5.dp)) {
                        Text(text = "BPM ： ")
                        Text(text = songInfo?.bpm?.toString() ?: songInfo?.let { "无" } ?: kotlin.run { "无" })
                    }
                }
            }

            Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
            Text(
                text = "能否试听臻品音质：$canTryExcellentQuality, 是否试听过：${vipInfo?.excellentQualityTried}," +
                        "试听剩余时间：${vipInfo?.excellentQualityTryTimeLeft}"
            )

            Button(
                onClick = {
                    Global.getOpenApi().canTryPlayExcellentQuality {
                        canTryExcellentQuality = it.data ?: true
                        vipInfo = Global.getLoginModuleApi().vipInfo
                    }

                }
            ) {
                Text(text = "查看臻品音质试听状态")
            }
            Button(
                onClick = {
                    OpenApiSDK.getPlayerApi().tryToOpenExcellentQuality(object : PlayCallback {
                        override fun onSuccess() {
                            Toast.makeText(UtilContext.getApp(), "试听成功！", Toast.LENGTH_SHORT).show()
                        }

                        override fun onFailure(errCode: Int, msg: String?) {
                            Toast.makeText(UtilContext.getApp(), "试听失败！$errCode, msg: $msg", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            ) {
                Text(text = "试听臻品音质")
            }

            Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))

            Text(
                text = "能否试听臻品全景声：$canTryGalaxyQuality, 是否试听过：${vipInfo?.galaxyQualityTried}," +
                        "试听剩余时间：${vipInfo?.galaxyQualityTryTimeLeft}"
            )

            Button(
                onClick = {
                    Global.getOpenApi().canTryPlayGalaxyQuality {
                        canTryGalaxyQuality = it.data ?: true
                        vipInfo = Global.getLoginModuleApi().vipInfo
                    }

                }
            ) {
                Text(text = "查看臻品全景声试听状态")
            }
            Button(
                onClick = {
                    OpenApiSDK.getPlayerApi().tryToOpenGalaxyQuality(object : PlayCallback {
                        override fun onSuccess() {
                            Toast.makeText(UtilContext.getApp(), "试听成功！", Toast.LENGTH_SHORT).show()
                        }

                        override fun onFailure(errCode: Int, msg: String?) {
                            Toast.makeText(UtilContext.getApp(), "试听失败！$errCode, msg: $msg", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            ) {
                Text(text = "试听臻品全景声")
            }

            Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))

        }
    }

}

