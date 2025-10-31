package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.FreeStrategy
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.GsonHelper
import com.tencent.qqmusic.qplayer.ui.activity.main.TopBar
import com.tencent.qqmusic.qplayer.ui.activity.player.PlayerObserver.convertTime
import com.tencent.qqmusic.qplayer.ui.activity.songlist.CommonProfileActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongProfileActivity
import com.tencent.qqmusic.qplayer.ui.activity.ui.theme.QPlayerTheme
import com.tencent.qqmusic.qplayer.utils.UiUtils

private val plachImageID: Int = R.drawable.musicopensdk_icon_light

private const val TAG = "SongDetailPage"

@Composable
fun SongDetailPage(observer: PlayerObserver) {

    Scaffold(
        topBar = {
            TopBar(
                "播放信息",
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
    val activity = LocalContext.current as Activity
    val strategy = observer?.freeStrategy
    val isAIEffect = UiUtils.getCurSoundEffectIsAI()
    val aiEffectEvent = observer?.largeModelEffectEvent

    QPlayerTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .then(Modifier.background(color = Color(248, 248, 248)))
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    val intent = Intent(activity, SongProfileActivity::class.java)
                    intent.putExtra(SongProfileActivity.KEY_SONG, songInfo)
                    activity.startActivity(intent)
                }) { Text("歌曲详情") }
            }
            Row(modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) {
                Text(text = "歌曲名称 ： ")
                Text(text = songInfo?.songName ?: "当前未播放歌曲")
            }

            if (songInfo?.isLongAudioSong() == true) {
                Row(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(text = "长音频歌曲")
                }
            }

            Row(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(text = "VIP歌曲 ： ")
                Text(text = if (songInfo?.vip == 1) "是" else "否")
            }

            if (songInfo?.isDigitalAlbum() == true) {
                Row(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(text = "数字专辑 ： ")
                    Text(text = "%.2f元".format(songInfo.payPrice() / 100f))
                }
            }

            Box(modifier = Modifier.clickable {
                activity.startActivity(
                    Intent(activity, CommonProfileActivity::class.java)
                        .putExtra(SongListActivity.KEY_SINGER_ID, songInfo?.singerId)
                )
            }) {
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

            if (songInfo?.hasQualityGalaxy() == true) {
                Row(modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) {
                    Text(text = "全景声类型 ： ")
                    val type = when {
                        songInfo.isGalaxy51Type() -> "5.1声道"
                        songInfo.isGalaxy714Type() -> "7.1.4声道"
                        songInfo.isGalaxyEffectType() -> "立体声 算法"
                        songInfo.isGalaxyStereoType() -> "双通道"
                        else -> "未知类型"
                    }
                    Text(text = type)
                }
            }
            Column(modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) {
                Text(text = "试听位置 ： ${convertTime(songInfo?.tryBegin?.toLong()?.div(1000) ?: 0L)}~${convertTime(songInfo?.tryEnd?.toLong()?.div(1000) ?: 0L)}")
                Text(text = "副歌位置 ： ${convertTime(songInfo?.chorusBegin?.toLong()?.div(1000) ?: 0L)}~${convertTime(songInfo?.chorusEnd?.toLong()?.div(1000) ?: 0L)}")
            }

            Row(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("点位id:${if ((observer?.freeScene ?: 0) == 0) "无" else observer?.freeScene}")
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = if (strategy == FreeStrategy.FREE_STRATEGY_DAILY_COUNT) {
                    "次数限免"
                } else if (strategy == FreeStrategy.FREE_STRATEGY_DAILY_DURATION) {
                    "时间限免"
                } else
                    "未限免")
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = {
                    OpenApiSDK.getOpenApi().getAllFreeListenInfo {
                        Log.i(TAG, "doGetString getAllFreeListenInfo:${GsonHelper.toJson(it)}")
                        UiUtils.showToast(GsonHelper.toJson(it), true)
                    }
                }) {
                    Text("限免详情")
                }
            }

            Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))

            if (isAIEffect) {
                Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
                Column {
                    Text(text = "大模型音效 ")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)) {
                        Text(text = "类型 ： ${aiEffectEvent?.shortType}\n")
                        Text(text = "描述 ： ${aiEffectEvent?.desc}")
                    }
                }
            }
        }
    }

}

