package com.tencent.qqmusic.qplayer.ui.activity.search

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class SearchPageActivity : ComponentActivity() {


    companion object {
        const val lyricIntentTag = "lyric"
        const val searchType = "searchtype"
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