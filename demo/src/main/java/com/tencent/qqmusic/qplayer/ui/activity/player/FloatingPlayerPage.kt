package com.tencent.qqmusic.qplayer.ui.activity.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.TextUtils
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import coil.compose.rememberImagePainter
import com.lyricengine.base.Lyric
import com.lyricengine.widget.LyricScrollView
import com.lyricengine.widget.LyricUIState
import com.lyricengine.widget.LyricViewParams
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.core.internal.lyric.LyricLoadInterface
import com.tencent.qqmusic.qplayer.core.player.proxy.SPBridgeProxy
import com.tencent.qqmusictvsdk.internal.lyric.LyricManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun FloatingPlayerPage(observer: PlayerObserver = PlayerObserver) {
    val activity = LocalContext.current as Activity
    val isPlaying = observer.currentState == PlayDefine.PlayState.MEDIAPLAYER_STATE_STARTED
    val currentSong = observer.currentSong
    val lyricView = lyric(fontSize = 40) {
        activity.startActivity(Intent(activity, PlayerActivity::class.java))
    }

    val sharedPreferences: SharedPreferences? = try {
        SPBridgeProxy.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
        null
    }

    ConstraintLayout(modifier = Modifier
        .fillMaxWidth()
        .height(70.dp)
        .background(Color(0xFFF0FFF0))
        .clickable {
            activity.startActivity(Intent(activity, PlayerActivity::class.java))
        }) {

        val (image1, title, lyric, vipIcon, play, song_list) = createRefs()

        Image(
            painter = rememberImagePainter(currentSong?.bigCoverUrl(),
                builder = {
                    crossfade(false)
                }),
            contentDescription = "",
            modifier = Modifier
                .padding(start = 10.dp)
                .size(60.dp)
                .constrainAs(image1) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .clip(RoundedCornerShape(4.dp))
        )

        Text(
            textAlign = TextAlign.Center,
            text = currentSong?.songName ?: "", modifier = Modifier.constrainAs(title) {
                start.linkTo(image1.end, 10.dp)
                bottom.linkTo(lyric.top, 5.dp)
                top.linkTo(parent.top, 10.dp)
                end.linkTo(play.start, if (currentSong?.vip == 1) 18.dp else 0.dp)
            },
            maxLines = 1,
            softWrap = false
        )


        if (currentSong?.vip == 1) {
            Image(
                painter = painterResource(R.drawable.pay_icon_in_cell_old),
                contentDescription = null,
                modifier = Modifier
                    .width(18.dp)
                    .height(10.dp)
                    .constrainAs(vipIcon) {
                        start.linkTo(title.end, 10.dp)
                        bottom.linkTo(title.bottom)
                        top.linkTo(title.top)
                    }
            )
        }
        if (currentSong?.longAudioVip == 1) {
            Image(
                painter = painterResource(R.drawable.ic_long_audio_vip_new),
                contentDescription = null,
                modifier = Modifier
                    .width(28.dp)
                    .height(10.dp)
                    .constrainAs(vipIcon) {
                        start.linkTo(title.end, 10.dp)
                        bottom.linkTo(title.bottom)
                        top.linkTo(title.top)
                    }
            )
        }

        Box(modifier = Modifier.constrainAs(lyric) {
            start.linkTo(image1.end, 10.dp)
            bottom.linkTo(parent.bottom, 10.dp)
            top.linkTo(title.bottom)
            end.linkTo(play.start)
            width = Dimension.fillToConstraints
        }) {
            AndroidView(
                factory = {
                    lyricView
                }, modifier = Modifier
                    .size(width = 400.dp, height = 20.dp)
            )

        }

        val needFade = sharedPreferences?.getBoolean("needFadeWhenPlay", false) ?: false

        Image(
            painter = painterResource(id = if (isPlaying) R.drawable.ic_state_playing else R.drawable.ic_state_paused),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clickable {
                    AppScope.launchIO {
                        OpenApiSDK
                            .getPlayerApi()
                            .apply {
                                if (isPlaying) pause(needFade) else play()
                                val song = getCurrentSongInfo()
                                if (song?.canPlay() != true) {
                                    withContext(Dispatchers.Main) {
                                        Toast
                                            .makeText(activity, song?.unplayableMsg ?: "", Toast.LENGTH_SHORT)
                                            .show()
                                    }

                                }
                            }
                    }
                }
                .constrainAs(play) {
                    bottom.linkTo(parent.bottom)
                    top.linkTo(parent.top)
                    end.linkTo(song_list.start, 5.dp)
                }
        )

        Image(
            painter = painterResource(id = R.drawable.ic_playlist),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .constrainAs(song_list) {
                    end.linkTo(parent.end, margin = 10.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .clickable {
                    val intent = Intent(activity, PlayListActivity::class.java)
                    activity.startActivity(intent)
                }
        )

    }


}

@Composable
fun lyric(
    fontSize: Int = 50,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    click: (() -> Unit)? = null
): LyricScrollView {
    val activity = LocalContext.current as Activity
    val lyricView: LyricScrollView by remember {
        mutableStateOf(
            LyricScrollView(activity, null).apply {
                setSingeMode(LyricViewParams.SINGLE_STATE_FIRST)
                setSingleLine(true)
                setFontSize(fontSize.dp.value.toInt())
                setColor(android.graphics.Color.parseColor("#30888888"))
                setColorH(android.graphics.Color.parseColor("#888888"))
                setOnClickListener {
                    click?.let { it.invoke() }
                }
                setSpeed(OpenApiSDK.getPlayerApi().getCurrentPlayTime() ?: 0L, OpenApiSDK.getPlayerApi().getPlaySpeed())
            }
        )
    }


    val lifeListener = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                lyricView.startTimer()
                LyricManager.instance.startLoadLyric(0)
            }
            Lifecycle.Event.ON_PAUSE -> {
                lyricView.stopTimer()
                LyricManager.instance.stopLoadLyric(0)
            }
        }
    }


    val listener: LyricLoadInterface by remember {
        mutableStateOf(object : LyricLoadInterface {
            override fun onLoadSuc(lyric: Lyric?, transLyric: Lyric?, romaLyric: Lyric?, state: Int) {
                lyricView.setLyric(lyric, transLyric, romaLyric, state)
            }

            override fun onLoadOther(searcText: String?, state: Int) {
                if (state == LyricUIState.STATE_LOAD_NONE) {
                    lyricView.setSingeMode(LyricViewParams.SINGLE_STATE_FIRST)
                }
                if (!TextUtils.isEmpty(searcText)) {
                    lyricView.setNoLyricTips(searcText)
                }
                lyricView.setState(state)
            }

            override fun onLyricSeek(position: Long, playSpeed: Float) {
                lyricView.seek(position)
                lyricView.setSpeed(position, playSpeed)
            }

            override fun onLyricStart(isStart: Boolean) {
                lyricView.apply {
                    if (isStart) startTimer() else stopTimer()
                }
            }

            override fun onLyricClear() {
                lyricView.clearLyric()
            }

        })
    }

    DisposableEffect(Unit) {
        LyricManager.instance.addLoadPlayLyricListener(listener)
        lifecycleOwner.lifecycle.addObserver(lifeListener)
        onDispose {
            LyricManager.instance.removeLoadPlayLyricListener(listener)
            lifecycleOwner.lifecycle.removeObserver(lifeListener)
        }
    }
    return lyricView
}


