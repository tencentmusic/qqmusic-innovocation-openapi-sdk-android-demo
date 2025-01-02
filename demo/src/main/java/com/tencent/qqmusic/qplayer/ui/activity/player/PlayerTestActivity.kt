package com.tencent.qqmusic.qplayer.ui.activity.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import com.tencent.protocal.PlayDefine.PlayMode
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiCallback
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums.Quality
import com.tencent.qqmusic.openapisdk.hologram.EdgeMvProvider
import com.tencent.qqmusic.openapisdk.model.PlaySpeedType
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import com.tencent.qqmusic.qplayer.ui.activity.mv.MVPlayerActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListActivity
import com.tencent.qqmusic.qplayer.ui.activity.songlist.SongListViewModel
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

//
// Created by clydeazhang on 2022/3/10 10:23 上午.
// Copyright (c) 2022 Tencent. All rights reserved.
//
class PlayerTestActivity : ComponentActivity() {
    companion object {
        private const val TAG = "@@@PlayerTestActivity"
        private const val RESULT_CODE_GO_PlayPage = 10086
    }


    private val sharedPreferences: SharedPreferences? by lazy {
        try {
            this.getSharedPreferences("OpenApiSDKEnv", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            QLog.e("OtherScreen", "getSharedPreferences error e = ${e.message}")
            null
        }
    }
    private val songListViewModel: SongListViewModel by viewModels()
    private val songList = mutableStateOf(emptyList<SongInfo>())
    private var reInitPlayerEnv: Button? = null

    @SuppressLint("MissingInflatedId","SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            R.layout.activity_player_test
        )
        val edt = findViewById<EditText>(R.id.edt_song)
        val btnPlay = findViewById<Button>(R.id.btn_play)
        val btnQuality = findViewById<Button>(R.id.btn_change_quality)
        val edtFolder = findViewById<EditText>(R.id.edt_folder)
        val btnFolder = findViewById<Button>(R.id.btn_folder)
        val btnUseDolby = findViewById<Button>(R.id.btn_use_dolby)


        val mvID = findViewById<EditText>(R.id.play_mv_id).apply {
            hint = "a0040pizz4a"
        }

        findViewById<Button>(R.id.mv_test_button).apply {
            setOnClickListener {
                val cacheSize = mvID.text.toString().ifEmpty { "a0040pizz4a" }
                startActivity(Intent(this@PlayerTestActivity, MVPlayerActivity::class.java).apply {
                    putExtra(MVPlayerActivity.MV_ID, cacheSize)
                })

            }
        }

        reInitPlayerEnv = findViewById(R.id.reinit_player_env)
        val btnOpenMap = findViewById<Button>(R.id.btn_open_map_test)
        edt.setText("335918510,317178463,316688109,332183304")
        initButton()

        refresh()

        edt.setTextIsSelectable(true)
        btnPlay.setOnClickListener {
            getSongList {
                if (it.isSuccess()) {
                    val ret =
                        OpenApiSDK.getPlayerApi().playSongs(it.data!!, it.data!!.indices.random())
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        //OpenApiSDK.getPlayerApi().seek(301318)
                    }, 10)
                    Log.d(TAG, "playsongs ret = $ret")
                    finish()
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        startActivity(Intent(this, PlayerActivity::class.java))
                    }, 1000)
                } else {
                    showErrToast(it)
                }
            }
        }

        findViewById<Button>(R.id.btn_set_play_list).setOnClickListener {
            getSongList {
                if (it.isSuccess()) {
                    OpenApiSDK.getPlayerApi().setPlayList(it.data!!, -1)
                    finish()
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        startActivity(Intent(this, PlayerActivity::class.java))
                    }, 1000)
                } else {
                    showErrToast(it)
                }
            }
        }

        findViewById<Button>(R.id.btn_add_to_next).setOnClickListener {
            getSongList {
                if (it.isSuccess()) {
                    OpenApiSDK.getPlayerApi().addToNext(it.data!!)
                    finish()
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        startActivity(Intent(this, PlayerActivity::class.java))
                    }, 1000)
                } else {
                    showErrToast(it)
                }
            }
        }

        findViewById<Button>(R.id.btn_add_to_next_and_play).setOnClickListener {
            getSongList {
                if (it.isSuccess()) {
                    OpenApiSDK.getPlayerApi().addToNext(it.data!!, true)
                    finish()
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        startActivity(Intent(this, PlayerActivity::class.java))
                    }, 1000)
                } else {
                    showErrToast(it)
                }
            }
        }

        findViewById<Button>(R.id.btn_play_list_update).setOnClickListener {
            val list = OpenApiSDK.getPlayerApi().getPlayList()
            Log.d(TAG, "before size:${list.size}")
            if (list.size < 2) {
                UiUtils.showToast("请保持列表数大于1个")
                return@setOnClickListener
            }
            val newList = ArrayList(list)
            newList.removeAt(newList.size - 1)
            AppScope.launchIO {
                OpenApiSDK.getPlayerApi().updatePlayingSongList(newList)
                Handler(Looper.getMainLooper()).postDelayed({
                    val newSize = OpenApiSDK.getPlayerApi().getPlayList().size
                    Log.d(TAG, "after size:${newSize}")
                    UiUtils.showToast("列表从${list.size} -> $newSize")
                }, 1000)
            }
        }
        findViewById<Button>(R.id.btn_add_song_to_playList).setOnClickListener{
            thread {
                val songIdText = edt.text.toString()
                val split = songIdText.split(",")
                // 0039eBnn3dVsNo
                val songIdList = mutableListOf<Long>()
                val songMidList = mutableListOf<String>()
                split.forEach {
                    val id = it.toLongOrNull()
                    if (id != null) {
                        songIdList.add(id)
                    } else {
                        songMidList.add(it)
                    }
                }
                OpenApiSDK.getOpenApi().fetchSongInfoBatch(
                    songIdList = if (songIdList.isEmpty()) null else songIdList,
                    midList = if (songMidList.isEmpty()) null else songMidList
                ) {
                    if (it.isSuccess()) {
                        if (it.data.isNullOrEmpty()) {
                            return@fetchSongInfoBatch
                        }
                        val ret = try {
                            OpenApiSDK.getPlayerApi().appendSongToPlaylist(it.data!!, it.data!!.indices.random())
                        } catch (e: Exception) {
                            0
                        }
                        Handler(Looper.getMainLooper()).postDelayed({
                            //OpenApiSDK.getPlayerApi().seek(301318)
                        }, 10)
                        Log.d(TAG, "appendSongToPlaylist ret = $ret")
                        finish()
                        Handler(Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(this, PlayerActivity::class.java))
                        }, 1000)
                    } else {
                        Toast.makeText(this, "查询歌曲失败: ${it.errorMsg}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }

        btnQuality.setOnClickListener {
            QualityAlert.showQualityAlert(this, false, {
                OpenApiSDK.getPlayerApi().setPreferSongQuality(it)
            }, {
                runOnUiThread {
                    refresh()
                }
            })
        }

        btnUseDolby.setOnClickListener {
            edt.setText("370513537")
        }

        findViewById<Button>(R.id.btn_use_hires).setOnClickListener {
            edt.setText("381885920,262607197")
        }

        findViewById<Button>(R.id.btn_use_vinyl).setOnClickListener {
            edt.setText("399932197,399932192,399932196")
        }

        edtFolder.setTextIsSelectable(true)
        btnFolder.setOnClickListener {
            val albumIdText = edtFolder.text.toString()
            startActivity(Intent(
                this@PlayerTestActivity, SongListActivity::class.java
            ).apply {
                putExtra(SongListActivity.KEY_FOLDER_ID, albumIdText)
            })
        }
        val activityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_CODE_GO_PlayPage) {
                    finish()
                }
            }

        btnOpenMap.setOnClickListener {
            activityResult.launch(Intent(this, MapTestActivity::class.java).apply { })
        }
        findViewById<Button>(R.id.btn_add_play_list).setOnClickListener {
            songList.value.toMutableList().clear()
            lifecycleScope.launch(Dispatchers.IO) {
                // 8878146441;7459586253
                songListViewModel.pagingFolderSongs(edtFolder.text.toString()) { songInfos, isEnd ->
                    delay(1)
                    val list = songList.value.toMutableList().apply {
                        addAll(songInfos)
                    }
                    songList.value = list
                    if (isEnd) {
                        val insertIndex = try {
                            OpenApiSDK.getPlayerApi().getPlayList().indices.random()
                        } catch (e: Exception) {
                            0
                        }
                        OpenApiSDK.getPlayerApi().appendSongToPlaylist(songList.value, insertIndex)
                    }
                }
            }
        }


        val edtMVCahce = findViewById<EditText>(R.id.edt_mv_cache).apply {
            hint = (sharedPreferences?.getInt("MV_CACHE", 500) ?: 500).toString()
        }
        findViewById<Button>(R.id.btn_mv_cache).apply {
            setOnClickListener {
                val cacheSize = edtMVCahce.text.toString().toIntOrNull() ?: 500
                sharedPreferences?.edit()?.putInt("MV_CACHE", cacheSize)?.apply()
                OpenApiSDK.getProviderByClass(EdgeMvProvider::class.java)?.setCacheSize(cacheSize)
            }
        }
        seekToPlayView()
        playControllerView()

        findViewById<Button>(R.id.ipc_test).setOnClickListener {
            val playTime = OpenApiSDK.getPlayerApi().getCurrentPlayTime()
            Log.d(TAG, "playTime=${playTime}")
            val songInfo = OpenApiSDK.getPlayerApi().getCurrentSongInfo()
            Log.d(TAG, "songName=${songInfo?.songName}")
            val duration = OpenApiSDK.getPlayerApi().getDuration()
            Log.d(TAG, "getDuration=${duration}")
            val playList = OpenApiSDK.getPlayerApi().getPlayList()
            Log.d(TAG, "getPlayList=${playList.map { it.songName }}")
//            int PLAY_MODE_ONESHOT_REPEAT = 101;// 单曲循环
//            int PLAY_MODE_LIST_REPEAT = 103;// 列表循环
//            int PLAY_MODE_LIST_SHUFFLE_REPEAT = 105;// 列表循环随机
            val setPlayMode = listOf(
                PlayMode.PLAY_MODE_ONESHOT_REPEAT,
                PlayMode.PLAY_MODE_LIST_REPEAT,
                PlayMode.PLAY_MODE_LIST_SHUFFLE_REPEAT).random()
            val ret = OpenApiSDK.getPlayerApi().setPlayMode(setPlayMode)
            Log.d(TAG, "ret=${ret},setPlayMode=${setPlayMode}")
            val playMode = OpenApiSDK.getPlayerApi().getPlayMode()
            Log.d(TAG, "playMode=${playMode}")
            assert(setPlayMode==playMode)
//            OpenApiSDK.getPlayerApi().seek()


        }

    }

    private fun <T> showErrToast(it: OpenApiResponse<T>) {
        UiUtils.showToast("接口请求失败: ${it.errorMsg}")
    }

    private fun getSongList(callback: OpenApiCallback<OpenApiResponse<List<SongInfo>>>) {
        val edt = findViewById<EditText>(R.id.edt_song) ?: return
        val songIdText = edt.text.toString()
        val split = songIdText.split(",")
        // 0039eBnn3dVsNo
        val songIdList = mutableListOf<Long>()
        val songMidList = mutableListOf<String>()
        split.forEach {
            val id = it.toLongOrNull()
            if (id != null) {
                songIdList.add(id)
            } else {
                songMidList.add(it)
            }
        }
        OpenApiSDK.getOpenApi().fetchSongInfoBatch(
            songIdList = if (songIdList.isEmpty()) null else songIdList,
            midList = if (songMidList.isEmpty()) null else songMidList,
            callback
        )
    }

    @SuppressLint("SetTextI18n")
    private fun refresh() {
        val textQuality = findViewById<TextView>(R.id.text_prefre_quality)

        val qualityStr = when (OpenApiSDK.getPlayerApi().getPreferSongQuality()) {
            Quality.HQ -> {
                "hq"
            }

            Quality.SQ -> {
                "sq"
            }

            Quality.SQ_SR -> {
                "SQ省流"
            }

            Quality.STANDARD -> {
                "standard"
            }

            Quality.LQ -> {
                "LQ"
            }

            PlayerEnums.Quality.DOLBY -> {
                "杜比"
            }

            Quality.HIRES -> {
                "HiRes"
            }

            Quality.EXCELLENT -> {
                "臻品音质2.0"
            }

            Quality.GALAXY -> {
                "臻品全景声"
            }

            Quality.MASTER_TAPE -> {
                "臻品母带"
            }

            Quality.MASTER_SR -> {
                "臻品母带省流"
            }

            else -> {
                "未知"
            }
        }
        textQuality.text = "当前默认音质：${qualityStr}"
    }


    private fun initButton() {
        reInitPlayerEnv?.setOnClickListener {
            reInitPlayerEnv()
        }
    }


    private fun reInitPlayerEnv() {
        OpenApiSDK.getPlayerApi().apply {
            //音质
            setPreferSongQuality(Quality.STANDARD)
            setCurrentPlaySongQuality(Quality.STANDARD)
            //倍速
            val playType = getCurrentSongInfo()?.let {
                if (it.isLongAudioSong()) {
                    PlaySpeedType.LONG_AUDIO
                } else {
                    PlaySpeedType.SONG
                }
            }
            playType?.let { setPlaySpeed(1.0F, playType) }
            //音效
            setSoundEffectType(null)
            // 清理播放列表
            clearPlayList()
            Toast.makeText(this@PlayerTestActivity, "恢复完成", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("CutPasteId")
    private fun seekToPlayView(){
        var actionStateIndex:Int = sharedPreferences?.getInt("actionStateIndex", 0) ?: 0
        // 来源PlayerManagerImpl().needExportPlayState
        val myState = mapOf(
            "达到预期播放时长执行 999 (params1时长单位s必填,负数为倒计时)" to 999,
            "初始状态 0" to PlayDefine.PlayState.MEDIAPLAYER_STATE_IDLE,
            "已准备 2" to PlayDefine.PlayState.MEDIAPLAYER_STATE_PREPARED,
            "正在播放 4" to PlayDefine.PlayState.MEDIAPLAYER_STATE_STARTED,
            "已暂停 5" to PlayDefine.PlayState.MEDIAPLAYER_STATE_PAUSED,
            "已停止 6" to PlayDefine.PlayState.MEDIAPLAYER_STATE_STOPPED,
            "播放正常结束，对应播放器的onCompletion  7" to PlayDefine.PlayState.MEDIAPLAYER_STATE_PLAYBACKCOMPLETED,
            "正在缓冲 101" to PlayDefine.PlayState.MEDIAPLAYER_STATE_BUFFERING,
            "缓冲失败 701" to PlayDefine.PlayState.MEDIAPLAYER_STATE_BUFFER_FAILED,
            )
        val seekTimerSpinnerOptions = myState.keys.toList()
        // 创建一个 ArrayAdapter 使用默认 spinner 布局和选项数组
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seekTimerSpinnerOptions)
        // 指定下拉菜单的布局样式 - 简单的列表视图
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val seekTimerSpinner = findViewById<Spinner?>(R.id.spinner_seek_timer)
        // 应用适配器到 spinner
        seekTimerSpinner?.let {
            it.adapter = adapter
            it.setSelection(actionStateIndex)
            // 设置选项选择事件监听器
            it.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    view?.let { actionStateIndex = position }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // 可选的回调，没有选项被选中时的动作
                }
            }
        }

        var playerFuncIndex:Int = sharedPreferences?.getInt("playerFuncIndex", 0) ?: 0
        val funcMap = mutableMapOf<String,()->Any?>()
        val params1 = findViewById<EditText>(R.id.edt_params1).text.toString()
        val needFade = PlayerObserver.sharedPreferences?.getBoolean("needFadeWhenPlay", false) ?: false
        funcMap["seekToPlay"] = {
            OpenApiSDK.getPlayerApi().seekToPlay(params1.toLongOrNull() ?: 60000L, needFade)
        }
        funcMap["seek"] = { OpenApiSDK.getPlayerApi().seek(params1.toIntOrNull() ?: 60000) }
        funcMap["playPos"] = { OpenApiSDK.getPlayerApi().playPos(params1.toIntOrNull() ?: 1) }
        funcMap["pause"] = { OpenApiSDK.getPlayerApi().pause() }

        val playerFuncSpinnerOptions = funcMap.keys.toList()
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_item, playerFuncSpinnerOptions)
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val playerFuncSpinner: Spinner? = findViewById(R.id.spinner_player_func)
        playerFuncSpinner?.let {
            it.adapter = adapter2
            it.setSelection(playerFuncIndex)
            it.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    view?.let { playerFuncIndex = position }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                }
            }
        }


        findViewById<Button>(R.id.btn_setToRun).setOnClickListener {
            sharedPreferences?.edit()?.putInt("actionStateIndex", actionStateIndex)?.apply()
            sharedPreferences?.edit()?.putInt("playerFuncIndex", playerFuncIndex)?.apply()
            funcMap[playerFuncSpinnerOptions[playerFuncIndex]]?.let { func ->
                when(actionStateIndex){
                    0 -> {
                        val onPlayTime = findViewById<EditText>(R.id.edt_params1).text.toString().toIntOrNull()
                        if (onPlayTime==null){
                            Toast.makeText(this, "params1为执行函数的播放时间,必填", Toast.LENGTH_SHORT).show()
                        }else{
                            // 按当前播放时间执行对应函数
                            PlayerObserver.doSomeThingOnPlayTime({ func() }, onPlayTime)
                        }
                    }
                    else -> {
                        PlayerObserver.doSomeThingOnEventStateChange(
                            func = { func() },
                            dstState = myState[seekTimerSpinnerOptions[actionStateIndex]]
                        )
                    }
                }
            }
        }
    }

    private fun playControllerView(){
        findViewById<Button>(R.id.fetchPlayDaily30).setOnClickListener {
            thread {
                OpenApiSDK.getOpenApi().fetchDailyRecommendSong {
                    if (it.isSuccess()){
                        AppScope.launchIO {
                            OpenApiSDK.getPlayerApi().playSongs(it.data!!)
                        }
                        Toast.makeText(this,"播放每日30首成功",Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(this,it.errorMsg,Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        findViewById<Button>(R.id.fetchPlayRecNewSongs).setOnClickListener {
            thread {
                val tagMapList = mapOf(12 to "内地", 9 to "韩国", 13 to "港台",
                    3 to "欧美", 8 to "日本", 1 to "最新")
                val tagId = tagMapList.keys.random()
                //12:内地；9:韩国；13:港台；3:欧美；8:日本 1:最新
                OpenApiSDK.getOpenApi().fetchNewSongRecommend(
                    tag = tagId,
                    callback = {
                        if (it.isSuccess()){
                            AppScope.launchIO {
                                OpenApiSDK.getPlayerApi().playSongs(it.data!!)
                            }
                            Toast.makeText(this,"播放新歌推荐成功:${tagMapList[tagId]}", Toast.LENGTH_SHORT).show()
                        }else{
                            Toast.makeText(this,it.errorMsg,Toast.LENGTH_SHORT).show()
                        }
                })
            }
        }
        findViewById<Button>(R.id.fetchPlaySimilarSongs).setOnClickListener {
            thread {
                val edtSimilar = findViewById<EditText>(R.id.edt_Similar)
                val edtSimilarText = edtSimilar.text.toString()
                // 0039eBnn3dVsNo
                var songId: Long? = null
                var songMid: String? = null
                val id = edtSimilarText.toLongOrNull()
                if (id != null) {
                    songId=id
                } else {
                    songMid=edtSimilarText
                }
                val hasRecList:List<Long> = OpenApiSDK.getPlayerApi().getPlayList().map { it.songId }
                OpenApiSDK.getOpenApi().fetchSimilarSong(
                    songId = songId,
                    mid = songMid,
                    hasRecommendList = hasRecList,
                    callback = {
                        if (it.isSuccess()){
                            AppScope.launchIO {
                                OpenApiSDK.getPlayerApi().playSongs(it.data!!)
                            }
                            Toast.makeText(this,"播放相似单曲成功",Toast.LENGTH_SHORT).show()
                        }else{
                            Toast.makeText(this,it.errorMsg,Toast.LENGTH_SHORT).show()
                        }
                })
            }
        }
        findViewById<Button>(R.id.fetchPersonalRecommendSong).setOnClickListener {
            thread {
                OpenApiSDK.getOpenApi().fetchPersonalRecommendSong {
                    if (it.isSuccess()){
                        AppScope.launchIO {
                            OpenApiSDK.getPlayerApi().playSongs(it.data!!)
                        }
                        Toast.makeText(this,"播放猜你喜欢成功",Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(this,it.errorMsg,Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        findViewById<Button>(R.id.fetchHomeRecSongs).setOnClickListener {
            thread {
                OpenApiSDK.getOpenApi().fetchHomepageRecommendationWithSongInfo( listOf(200)) { homeRce ->
                    if (homeRce.isSuccess()) {
                        val songInfos = mutableListOf<SongInfo>()
                        homeRce.data!!.shelfList.forEach {  shelf->
                            shelf.cardList.forEach {card->
                                val songInfo = card.songInfo
                                if(songInfo!=null){
                                    songInfos.add(songInfo)
                                }
                            }
                        }
                        if (songInfos.isEmpty()){
                            Toast.makeText(this, "首页推荐歌曲数为0", Toast.LENGTH_SHORT).show()
                            return@fetchHomepageRecommendationWithSongInfo
                        }
                        AppScope.launchIO {
                            OpenApiSDK.getPlayerApi().playSongs(songInfos)
                        }
                        Toast.makeText(this, "播放首页推荐单曲成功", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(this, homeRce.errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
//        findViewById<Button>(R.id.fetchHomeRecSongs).setOnClickListener {
//            thread {
//                OpenApiSDK.getOpenApi().fetchHomepageRecommendation( listOf(500)) { homeRce ->
//                    if (homeRce.isSuccess()) {
//                        val rcItemId = homeRce.data!!.rcItemList.firstOrNull()
//                        if (rcItemId==null){
//                            Toast.makeText(this, "首页推荐歌单为空", Toast.LENGTH_SHORT).show()
//                            return@fetchHomepageRecommendation
//                        }
//                        OpenApiSDK.getOpenApi().fetchSongOfFolder(rcItemId.dissId.toString(), callback = {
//                            if (it.isSuccess()){
//                                AppScope.launchIO {
//                                    OpenApiSDK.getPlayerApi().playSongs(it.data!!)
//                                }
//                                Toast.makeText(this, "播放首页推荐歌单成功", Toast.LENGTH_SHORT).show()
//                            }else{
//                                Toast.makeText(this, it.errorMsg, Toast.LENGTH_SHORT).show()
//                            }
//                        })
//                    }else{
//                        Toast.makeText(this, homeRce.errorMsg, Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }

        findViewById<Button>(R.id.stopPlay).setOnClickListener {
            AppScope.launchIO {
                OpenApiSDK.getPlayerApi().stop()
            }
        }

    }
}