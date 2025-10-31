package com.tencent.qqmusic.qplayer.ui.activity.songlist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.google.gson.GsonBuilder
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.core.player.proxy.SPBridgeProxy
import com.tencent.qqmusic.qplayer.ui.activity.download.DownloadActivity
import com.tencent.qqmusic.qplayer.ui.activity.main.CopyableText
import com.tencent.qqmusic.qplayer.ui.activity.player.QualityAlert
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SongProfileActivity: AppCompatActivity() {

    companion object {
        const val KEY_SONG = "KEY_SONG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val songInfo = intent?.parcelable<SongInfo>(KEY_SONG)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    SongProfileEntrance(songInfo)
                }
            }
        )
    }

    inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
        Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
    }
}


@Composable
fun SongProfileEntrance(songInfo: SongInfo?) {
    val songInfoState = remember {  mutableStateOf(songInfo) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp) ) {
        SongSearchEntrance(songInfoState, Modifier.padding(horizontal = 4.dp, vertical = 4.dp))

        if (songInfoState.value != null) {
            SongProfilePage(songInfoState.value!!, Modifier)
        }
    }
}

@Composable
fun SongSearchEntrance(songInfoState: MutableState<SongInfo?>, modifier: Modifier) {
    val searchTextState = remember { mutableStateOf<String>("") }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    TextField(value = searchTextState.value,
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(4.dp),
        singleLine = true,
        onValueChange = {
            searchTextState.value = it
            coroutineScope.launch(Dispatchers.IO) {
                val isNumber = it.isNotEmpty() && Regex("^[0-9]+$").matches(it)
                val songId = if (isNumber) it.toLongOrNull() else null
                val songIdList = if (songId != null) listOf(songId) else null
                val songMidList = if (songId == null) listOf(it) else null
                OpenApiSDK.getOpenApi().fetchSongInfoBatch(songIdList, songMidList) { resp->
                    if (resp.isSuccess()) {
                        songInfoState.value = resp.data?.getOrNull(0)
                    }
                }
            }
        },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "")  },
        trailingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "") },
        placeholder = { Text(text = "Search") },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            focusManager.clearFocus()
        }),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = Color.Transparent, // 设置背景为透明
            disabledIndicatorColor = Color.Transparent // 去掉禁用状态时的下划线
        )
    )
}

@Composable
fun SongProfilePage(songInfo: SongInfo, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp)
            .verticalScroll(state = rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicInfoView(songInfo, Modifier)

        SongTagView(songInfo, Modifier)

        SongSAView(songInfo, Modifier)

        SongRightView(songInfo, Modifier)

        SongJsonSourceView(songInfo, Modifier)
    }
}

@Composable
fun BasicInfoView(songInfo: SongInfo, modifier: Modifier) {
    val context = LocalContext.current
    val activity = context as AppCompatActivity
    val clipboardManager = LocalClipboardManager.current
    val downloadIcon = remember { mutableStateOf(
        if (OpenApiSDK.getDownloadApi().isSongDownloaded(songInfo)) {
        R.drawable.icon_song_info_item_more_downloaded
    } else {
        R.drawable.icon_player_download_light
    }) }
    Row {
        Image(painter = painterResource(id = downloadIcon.value),
            contentDescription = null,
            modifier = Modifier
                .size(45.dp)
                .clickable(enabled = true) {
                    QualityAlert.showQualityAlert(
                        activity = activity, isDownload = true, setBlock = {
                            OpenApiSDK
                                .getDownloadApi()
                                .downloadSong(songInfo, it)
                            UiUtils.showToast("开始下载")
                            PlayDefine.PlayError.PLAY_ERR_NONE
                        }, refresh = {
                            if (OpenApiSDK.getDownloadApi().isSongDownloaded(songInfo)) {
                                downloadIcon.value = R.drawable.icon_song_info_item_more_downloaded
                            } else {
                                downloadIcon.value = R.drawable.icon_player_download_light
                            }
                        }, songInfo = songInfo)
                }
        )
        TextButton(onClick = {
            activity.startActivity(
                Intent(
                    activity,
                    DownloadActivity::class.java
                ).apply {
                    putExtra(DownloadActivity.FROM_DOWNLOAD_SONG_PAGE, true)
                })
        }) {
            Text(text = "前往已下载歌曲")
        }
    }

    Card(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        elevation = 4.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp))  {
            Text(text = "基础信息", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.End) {
                Image(
                    painter = rememberImagePainter(songInfo.smallCoverUrl()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            CopyableText("歌曲id",songInfo.songId.toString())
            CopyableText("歌曲mid",songInfo.songMid)
            CopyableText("歌曲名",songInfo.songName)
            CopyableText("歌手",songInfo.singerName)
            songInfo.otherSingerList?.let {
                CopyableText("其他歌手", songInfo.otherSingerList?.joinToString("/") { it.name })
            }
            CopyableText("歌曲专辑",songInfo.albumName)
        }
    }
}

@Composable
fun SongSAView(songInfo: SongInfo, modifier: Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        elevation = 4.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "内容安全限制(${songInfo.action?.sa})", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            CopyableText("AI作品", if (songInfo.isAISong()) "是" else "否")
        }
    }
}

@Composable
fun SongRightView(songInfo: SongInfo, modifier: Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        elevation = 4.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "歌曲权限", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            val msg = "纯人声:${songInfo.isForbidVocalAccomPureVocal().not()},纯伴奏:${songInfo.isForbidVocalAccomPureAccom().not()}"
            CopyableText(title = "伴唱限制", content = msg)
        }
    }
}

@Composable
fun SongTagView(songInfo: SongInfo, modifier: Modifier) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sharedPreferences: SharedPreferences? = try {
        SPBridgeProxy.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    Card(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        elevation = 4.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "标签信息", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            CopyableText("心情",songInfo.extraInfo?.mood)
        }
    }
}

@Composable
fun SongJsonSourceView(songInfo: SongInfo, modifier: Modifier) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val gson = GsonBuilder().setPrettyPrinting().create()
    val json = try {
        gson.toJson(songInfo)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }

    val maxLineState = remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        elevation = 4.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically)  {
                Text(text = "歌曲JSON信息", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(onClick = {
                    maxLineState.value = !maxLineState.value
                }) {
                    val res = if (maxLineState.value) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    }
                    Icon(imageVector = res, contentDescription = null)
                }
            }

            Text(text = json, fontSize = 14.sp, fontWeight = FontWeight.Normal, maxLines = if (maxLineState.value) Int.MAX_VALUE else 3,
                modifier = Modifier.padding(4.dp)
                    .clickable(onClick = { copyToClipboard(json, context, clipboardManager) })
            )
        }
    }
}

@Composable
fun RowScope.TabCell(text: String?, weight: Float, onClick: (() -> Unit)? = null) {
    Text(text = text ?: "null",
        modifier = Modifier.weight(weight).padding(8.dp).clickable(onClick = { onClick?.invoke() }),
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    )
}

fun copyToClipboard(text: String?, context: Context, clipboardManager: ClipboardManager) {
    clipboardManager.setText(AnnotatedString(text ?: ""))
    Toast.makeText(context, "已复制${text ?: ""}到剪切板", Toast.LENGTH_SHORT).show()
}