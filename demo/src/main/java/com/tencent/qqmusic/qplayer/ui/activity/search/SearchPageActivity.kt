package com.tencent.qqmusic.qplayer.ui.activity.search

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.SingerWiki
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchPageActivity : ComponentActivity() {


    companion object {
        const val lyricIntentTag = "lyric"
        const val searchType = "searchtype"
        const val singerIntentTag = "singer"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = intent.getStringExtra(searchType)
        setContent {
            when (type) {
                lyricIntentTag -> {
                    val data = intent.getStringExtra(lyricIntentTag) ?: ""
                    LyricPage(data = data)
                }

                singerIntentTag -> {
                    val data = intent.getIntExtra(singerIntentTag, 0)
                    SingerPage(id = data)
                }
            }
        }
    }

    @Composable
    private fun SingerPage(id: Int) {
        val singer = remember {
            mutableStateOf<SingerWiki?>(null)
        }

        LaunchedEffect(key1 = id, block = {
            launch(Dispatchers.IO) {
                OpenApiSDK.getOpenApi().fetchSingerWiki(id) {
                    singer.value = if (it.isSuccess()) {
                        it.data
                    } else {
                        SingerWiki().apply {
                            desc = it.errorMsg
                        }
                    }
                }
            }
        })

        LazyColumn {

            item {
                Text(text = "基础信息", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            items(singer.value?.basicInfo?.entries?.toList() ?: emptyList()) {
                Text(text = "${it.key}  :  ${it.value}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }

            item {
                Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
            }

            singer.value?.groupListInfo?.map {
                items(it.entries.toList()) { entry ->
                    Text(text = "${entry.key}  :  ${entry.value}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                item {
                    Divider(thickness = 1.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp, start = 10.dp, end = 20.dp))
                }
            }
            item {
                Text(text = "歌手简介", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Text(text = singer.value?.desc ?: "", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
            item {
                Divider(thickness = 3.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
            }

            items(singer.value?.otherInfo?.entries?.toList() ?: emptyList()) {
                Column {
                    Text(text = "${it.key}  :  ${it.value}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Divider(thickness = 1.dp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp, start = 10.dp, end = 20.dp))
                }
            }

        }
    }

    @Composable
    private fun LyricPage(data: String) {
        val list = data.split("\n")
        LazyColumn {
            items(list) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    Text(text = it, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
    }

}