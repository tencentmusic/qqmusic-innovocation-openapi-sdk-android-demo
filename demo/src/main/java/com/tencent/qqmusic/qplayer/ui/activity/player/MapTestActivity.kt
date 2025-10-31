package com.tencent.qqmusic.qplayer.ui.activity.player

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import coil.load
import com.tencent.qqmusic.innovation.common.util.DeviceUtils
import com.tencent.qqmusic.innovation.common.util.GsonUtils
import com.tencent.qqmusic.openapisdk.core.InitConfig
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.player.PlayDefine.RadioType
import com.tencent.qqmusic.openapisdk.core.player.PlayerEnums
import com.tencent.qqmusic.openapisdk.core.report.PageFromBean
import com.tencent.qqmusic.openapisdk.core.report.ReportDataBean
import com.tencent.qqmusic.openapisdk.model.Folder
import com.tencent.qqmusic.openapisdk.model.SearchType
import com.tencent.qqmusic.openapisdk.model.SongInfo
import com.tencent.qqmusic.openapisdk.model.SongListItem
import com.tencent.qqmusic.openapisdk.model.SongListItemType
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.ui.activity.MustInitConfig
import com.tencent.qqmusic.qplayer.ui.activity.home.HomeViewModel
import com.tencent.qqmusic.qplayer.ui.activity.person.MineViewModel
import com.tencent.qqmusic.qplayer.utils.UiUtils
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.concurrent.thread

class MapTestActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MapTest"
        private val MineVM by lazy { MineViewModel() }
        private val HomeVM by lazy { HomeViewModel() }
        private const val RESULT_CODE_GO_PlayPage = 10086
        private var runnable: Runnable? = null
        private val handler = Handler(Looper.getMainLooper())

    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_test)
        // 登录
        loginView()
        // 无需登录态
        withoutLoginView()
        // 需要登录态
        needLoginView()
        radioView()
        // 其他设置
        otherSettingView()

        OpenApiSDK.getReportApi().reportExposureEvent(ReportDataBean(123).apply {
            addExtra("int1", 1)
            addExtra("str2", "2")
        })
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    // 登录
    @SuppressLint("SetTextI18n")
    private fun loginView() {
        val textLoginStatus = findViewById<TextView>(R.id.text_login_status)
        val btnMapTestLogin = findViewById<Button>(R.id.btn_map_test_login)
        val headIcon = findViewById<ImageView>(R.id.headIcon_ImageView)
        val vipIcon = findViewById<ImageView>(R.id.vipIcon_ImageView)
        val loginState = MutableLiveData<Boolean>()

        loginState.observe(this) {
            if (it) {
                btnMapTestLogin.text = "退出登录"
                textLoginStatus.text =
                    "登录方式：QQ音乐登录\n用户名:${HomeVM.userInfo.value?.nickName}\n"
                if (MineVM.userVipInfo.value?.isSuperVip() == true) {
                    PlayerTestObj.currentVipIcon = MineVM.userVipInfo.value?.hugeVipIcon.toString()
                } else if (MineVM.userVipInfo.value?.isVip() == true) {
                    PlayerTestObj.currentVipIcon = MineVM.userVipInfo.value?.greenVipIcon.toString()
                }
                Log.i(
                    TAG,
                    "vip:${MineVM.userVipInfo.value?.isVip()},super_vip:${MineVM.userVipInfo.value?.isSuperVip()}"
                )
                vipIcon.load(PlayerTestObj.currentVipIcon)
                headIcon.load(HomeVM.userInfo.value?.avatarUrl)
            } else {
                btnMapTestLogin.text = "登录"
                textLoginStatus.text = "未登录"
                vipIcon.setImageDrawable(null)
                headIcon.setImageDrawable(null)
            }
        }

        HomeVM.loginState.observe(this) {
            runnable?.let { handler.removeCallbacks(it) }
            runnable = Runnable {
                MineVM.updateData()
                try {
                    this.lifecycleScope.launch {
                        withTimeout(30 * 1000) {
                            while (MineVM.userVipInfo.value == null) {
                                delay(200)
                            }
                            loginState.postValue(HomeVM.loginState.value?.first)
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    UiUtils.showToast("登录态更新超时")
                }
            }
            // 500ms防抖
            handler.postDelayed(runnable!!, 500)
        }

        btnMapTestLogin.setOnClickListener {
            thread {
                if (btnMapTestLogin.text == "登录") {
                    OpenApiSDK.getLoginApi().qqMusicLogin(this) { b, msg ->
                        if (b) {
                            HomeViewModel.clearRequestState()
                            MineVM.updateData()
                            UiUtils.showToast("登录成功")
                        } else {
                            UiUtils.showToast("登录失败: $msg")
                        }
                    }
                } else {
                    loginOut()
                }
            }
        }

    }

    // 无需登录态：
    private fun withoutLoginView() {
        val btnMapTransPlayFirstTime = findViewById<Button>(R.id.btn_trans_play_first_time)
        val btnMapTransPlay = findViewById<Button>(R.id.btn_trans_play)
        val btnMapTestSongInfo = findViewById<Button>(R.id.btn_map_test_songInfo)
        val btnMapTestRank = findViewById<Button>(R.id.btn_map_rank)
        val btnMapTestSongList = findViewById<Button>(R.id.btn_map_self_list)
        val btnMapTestSelfRadio = findViewById<Button>(R.id.btn_map_self_radio)
        val btnMapTestMerchant = findViewById<Button>(R.id.btn_map_merchant_list)
        val btnMapTestMember = findViewById<Button>(R.id.btn_map_member_list)

        // 随机歌单+随机播放(pos=0)+songInfo序列化+根据type设置列表循环和随机
        btnMapTransPlayFirstTime.setOnClickListener {
            thread {
                // 退出登录
                loginOut()
                // 只用默认值去请求
                OpenApiSDK.getOpenApi().fetchCustomSceneSongList(count = 100) { resp ->
                    if (resp.isSuccess()) {
                        val songInfoList = resp.data!!.second
                        val songListJson = mutableListOf<String>()
                        songInfoList.forEach { songInfo ->
                            songListJson.add(
                                OpenApiSDK.getOpenApi().transSongInfoToJson(songInfo)
                            )
                        }
                        val parseSongInfoList: List<SongInfo> = songListJson.map { songInfoJson ->
                            OpenApiSDK.getOpenApi().parseJsonToSongInfo(songInfoJson)!!
                        }.filter { song -> song.canPlay() }
                        handler.postDelayed({
                            // 根据type设置列表循环和随机
                            var ret = OpenApiSDK.getPlayerApi()
                                .setPlayMode(if (resp.data!!.first.type == 1) PlayerEnums.Mode.SHUFFLE else PlayerEnums.Mode.LIST)
                            UiUtils.showToast("setPlayMode->${ret}")
                            ret = OpenApiSDK.getPlayerApi()
                                .setPreferSongQuality(PlayerEnums.Quality.STANDARD) // 设置标准
                            UiUtils.showToast("setPreferSongQuality->${ret}")
                            ret = OpenApiSDK.getPlayerApi().playSongs(parseSongInfoList, pos = 0)
                            UiUtils.showToast("playSongs->${ret}")
                        }, 200)
                        returnPlayPage()
                    } else {
                        handler.post {
                            UiUtils.showToast("请求失败: ${resp.errorMsg}")
                        }
                    }
                }
            }
        }

        // 循环切歌单+songInfo序列化+HQ+根据type设置列表循环和随机
        btnMapTransPlay.setOnClickListener {
            thread {
                if (PlayerTestObj.customSongListSquare == null) return@thread
                updateCustomSongListSquare()
                val songListItem =
                    PlayerTestObj.customSongListSquare!![PlayerTestObj.currentItemIndex]
                songListItem.itemId?.let { itemId ->
                    PlayerTestObj.currentItemIndex =
                        if (PlayerTestObj.currentItemIndex >= PlayerTestObj.customSongListSquare!!.size) 0 else PlayerTestObj.currentItemIndex + 1
                    songListItem.type?.let { itemType ->
                        OpenApiSDK.getOpenApi().fetchCustomSceneSongList(
                            itemId, itemType, page = 0, count = 50
                        ) { songInfoListRsp ->
                            if (songInfoListRsp.isSuccess()) {
                                val transSongs = OpenApiSDK.getOpenApi()
                                    .transSongListToJson(songInfoListRsp.data!!.second)
                                val songs =
                                    OpenApiSDK.getOpenApi().parseJsonToSongList(transSongs)
                                handler.postDelayed({
                                    OpenApiSDK.getPlayerApi()
                                        .setPlayMode(if (itemType == 1) PlayerEnums.Mode.SHUFFLE else PlayerEnums.Mode.LIST)
                                    OpenApiSDK.getPlayerApi().setPreferSongQuality(
                                        PlayerEnums.Quality.HQ
                                    )
                                    val ret = songs?.let { it4 ->
                                        OpenApiSDK.getPlayerApi()
                                            .playSongs(it4.filter { song -> song.canPlay() })
                                    }
                                    Log.d(TAG, "playsongs ret = $ret")
                                }, 200)
                                returnPlayPage()
                                handler.post {
                                    UiUtils.showToast("播放第${PlayerTestObj.currentItemIndex}个歌单")
                                }
                            } else {
                                handler.post {
                                    UiUtils.showToast("播放失败: ${songInfoListRsp.errorMsg}")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 播放榜单、歌单
        btnMapTestSongInfo.setOnClickListener {
            thread {
                updateCustomSongListSquare()
                val songListItemList =
                    (PlayerTestObj.customSongListSquare?.filter { it.type in 1..2 }?.toMutableList() ?: mutableListOf())
                        .apply { shuffle() }
                songListItemList.forEach {
                    it.type?.let { it1 ->
                        it.itemId?.let { it2 ->
                            OpenApiSDK.getOpenApi().fetchCustomSceneSongList(
                                it2,
                                it1, page = 0, count = 100
                            ) { it3 ->
                                if (it3.isSuccess()) {
                                    val vipSongs = mutableListOf<SongInfo>()
                                    assert(it3.data!!.second.size == 100)
                                    it3.data!!.second.forEach { it4 ->
                                        // 确认所有歌曲可播放
                                        assert(it4.canPlayWhole()) { "${it4.songId}不可播放" }
                                        assert(it4.hasLinkHQ() || it4.hasLinkLQ() || it4.hasLinkStandard()) { "${it4.songId}没有播放链接" }
                                        assert(it4.unplayableCode == 0) { "${it4.songId}不可播放" }
                                        if (it4.vip == 1 && it1 == 2) {
                                            vipSongs.add(it4)
                                        }
                                    }
                                    handler.postDelayed({
                                        UiUtils.showToast(
                                            if (it.type == 2) "${it.title} 测试通过,vip歌曲数=${vipSongs.size}" else "${it.title}测试通过")
                                        OpenApiSDK.getPlayerApi()
                                            .playSongs(vipSongs.filter { song -> song.canPlay() })
                                    }, 200)

                                } else {
                                    handler.post {
                                        UiUtils.showToast(
                                            "itemId:${it.title},测试失败:${it3.errorMsg}")
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }

        btnMapTestRank.setOnClickListener {
            playSongsByItemType(SongListItemType.SONG_LIST_ITEM_TYPE_RANK)
        }

        btnMapTestSongList.setOnClickListener {
            playSongsByItemType(SongListItemType.SONG_LIST_ITEM_TYPE_FOLDER)
        }

        btnMapTestSelfRadio.setOnClickListener {
            playSongsByItemType(SongListItemType.SONG_LIST_ITEM_TYPE_RADIO)
        }

        btnMapTestMerchant.setOnClickListener {
            playSongsByItemType(SongListItemType.SONG_LIST_ITEM_TYPE_FOLDER_BUSINESS)
        }

        btnMapTestMember.setOnClickListener {
            playSongsByItemType(SongListItemType.SONG_LIST_ITEM_TYPE_FOLDER_VIP)
        }
    }

    // 需要登录态：
    @SuppressLint("SetTextI18n")
    private fun needLoginView() {
        val btnMapTestMyFav = findViewById<Button>(R.id.btn_map_test_my_fav)
        val btnMapTestRecent = findViewById<Button>(R.id.btn_map_test_recently)
        val btnMapTestDaily30 = findViewById<Button>(R.id.btn_map_test_daily30)
        val btnMapFav = findViewById<Button>(R.id.btn_map_fav)

        btnMapTestMyFav.setOnClickListener {
            thread {
                OpenApiSDK.getOpenApi().fetchPersonalFolder { it0 ->
                    if (it0.isSuccess()) {
                        val myFav = it0.data!!.first() // 我喜欢歌单
                        myFav.let { it1 ->
                            OpenApiSDK.getOpenApi().fetchSongOfFolder(it1.id, 0, 100) { it2 ->
                                if (it2.isSuccess()) {
                                    it2.data!!.forEach { songInfo ->
                                        songInfo.canPlay()
                                    }
                                    handler.postDelayed({
                                        var ret = OpenApiSDK.getPlayerApi()
                                            .setPlayMode(PlayerEnums.Mode.LIST) // 设置列表循环
                                        UiUtils.showToast(
                                            "setPlayMode->${ret}")
                                        ret = OpenApiSDK.getPlayerApi()
                                            .setPreferSongQuality(PlayerEnums.Quality.HQ) // 设置HQ
                                        UiUtils.showToast(
                                            "setPreferSongQuality->${ret}")
                                        ret = OpenApiSDK.getPlayerApi()
                                            .playSongs(it2.data!!.filter { song -> song.canPlay() })
                                        UiUtils.showToast(
                                            "playSongs->${ret}")
                                        Log.d(TAG, "playsongs ret = $ret")
                                    }, 200)
                                    returnPlayPage()
                                } else {
                                    UiUtils.showToast(
                                        "测试失败: ${it2.errorMsg}")
                                }
                            }
                        }
                    } else {
                        UiUtils.showToast("测试失败: ${it0.errorMsg}")
                    }
                }
            }
        }

        btnMapTestRecent.setOnClickListener {
            thread {
                OpenApiSDK.getOpenApi()
                    .fetchRecentPlaySong(PlayerTestObj.lastUpdateTimeStamp) { recentPlaySongResp ->
                        if (recentPlaySongResp.isSuccess()) {
                            PlayerTestObj.lastUpdateTimeStamp = System.currentTimeMillis()
                            val songInfoList = OpenApiSDK.getOpenApi().parseJsonToSongList(
                                OpenApiSDK.getOpenApi().transSongListToJson(recentPlaySongResp.data)
                            )
                            if (songInfoList != null) {
                                handler.postDelayed({
                                    OpenApiSDK.getPlayerApi()
                                        .setPlayMode(PlayerEnums.Mode.LIST) // 设置列表循环
                                    OpenApiSDK.getPlayerApi()
                                        .setPreferSongQuality(PlayerEnums.Quality.HQ) // 设置HQ
                                    OpenApiSDK.getPlayerApi()
                                        .playSongs(songInfoList.filter { it.canPlay() })
                                }, 200)
                                returnPlayPage()
                            } else {
                                UiUtils.showToast(
                                    "测试失败:播放列表为空!${recentPlaySongResp.data?.size}")
                            }
                        } else {
                            UiUtils.showToast(
                                "测试失败: ${recentPlaySongResp.ret},${recentPlaySongResp.errorMsg}")
                        }
                    }
            }
        }

        btnMapTestDaily30.setOnClickListener {
            thread {
                OpenApiSDK.getOpenApi().fetchDailyRecommendSong {
                    if (it.isSuccess()) {
                        handler.postDelayed({
                            OpenApiSDK.getPlayerApi().setPreferSongQuality(PlayerEnums.Quality.HQ)
                            Thread.sleep(500)
                            OpenApiSDK.getPlayerApi().setPlayMode(PlayerEnums.Mode.LIST)
                            Thread.sleep(500)
                            OpenApiSDK.getPlayerApi()
                                .playSongs(it.data!!.filter { song -> song.canPlay() })
                            Thread.sleep(500)
                        }, 200)
                        returnPlayPage()
                    } else {
                        UiUtils.showToast("报错:${it.errorMsg}")
                    }
                }
            }
        }

        btnMapFav.setOnClickListener {
            thread {
                // 获取我喜欢歌单id
                val selfFavFileId = PlayerTestObj.selfFavFileId ?: run {
                    val resp = OpenApiSDK.getOpenApi().blockingGet<List<Folder>> {
                        this.fetchPersonalFolder(it)
                    }
                    if (resp.isSuccess() && resp.data != null) {
                        resp.data!!.firstOrNull()?.id.also {
                            PlayerTestObj.selfFavFileId = it
                        }
                    } else {
                        runOnUiThread {
                            UiUtils.showToast(
                                "无法获取我喜欢歌单:${resp.errorMsg}")
                        }
                        return@thread
                    }
                }
                if (selfFavFileId != null) {
                    runOnUiThread {
                        // 获取歌曲
                        val input = EditText(this).apply { setText("472512596") }
                        AlertDialog.Builder(this)
                            .setTitle("输入歌曲id:")
                            .setView(input)
                            .setPositiveButton("收藏") { _, _ ->
                                val songIdList =
                                    input.text.toString().split(',')
                                        .mapNotNull { it.toLongOrNull() }
                                thread {
                                    operateSongFolder(selfFavFileId, songIdList, true)
                                }
                            }
                            .setNegativeButton("取消收藏") { _, _ ->
                                val songIdList = input.text.toString().split(',')
                                    .mapNotNull { it.toLongOrNull() }
                                thread {
                                    operateSongFolder(selfFavFileId, songIdList, false)
                                }
                            }
                            .setNeutralButton("关闭", null)
                            .show()
                    }
                }
            }
        }

    }

    private fun radioView() {
        findViewById<Button>(R.id.btn_map_set_radio_playlist).setOnClickListener {
            OpenApiSDK.getPlayerApi().playRadio(autoPlay = false, callback = {
                UiUtils.showToast( "设置首批歌曲size:${it?.second?.size}")
                UiUtils.showToast(
                    "第一首歌曲:《${it?.second?.first()?.songName}》")
            })
        }

        findViewById<Button>(R.id.btn_map_set_radio_playlist_play).setOnClickListener {
            OpenApiSDK.getPlayerApi().playRadio(autoPlay = true, callback = {
                val reportSong = it?.second?.first()
                val clickReportDataBean = ReportDataBean(
                    1234,
                    reportSong?.songMid ?: "", reportSong?.songId ?: 0
                )
                clickReportDataBean.addExtra("ext", "{\"key1\":\"value1\",\"key2\":\"value2\"}")
                clickReportDataBean.addExtra("int1", 1)
                clickReportDataBean.addExtra("int2", 2)
                clickReportDataBean.addExtra("int3", 3)
                clickReportDataBean.addExtra("int4", 4)
                clickReportDataBean.addExtra("int5", 5)
                clickReportDataBean.addExtra("str1", reportSong?.songName ?: "str1")
                clickReportDataBean.addExtra("str3", "str3")
                clickReportDataBean.addExtra("str4", "str4")
                clickReportDataBean.addExtra("str5", "str5")
                clickReportDataBean.abt = reportSong?.extraInfo?.abt
                clickReportDataBean.trace = reportSong?.extraInfo?.trace
                OpenApiSDK.getReportApi().reportClickEvent(clickReportDataBean)
                val exposureReportDataBean = ReportDataBean(
                    123,
                    reportSong?.songMid ?: "", reportSong?.songId ?: 0
                )
                exposureReportDataBean.addExtra("ext", "{\"key1\":\"value1\",\"key2\":\"value2\"}")
                exposureReportDataBean.addExtra("int1", 1)
                exposureReportDataBean.addExtra("int2", 2)
                exposureReportDataBean.addExtra("int3", 3)
                exposureReportDataBean.addExtra("int4", 4)
                exposureReportDataBean.addExtra("int5", 5)
                exposureReportDataBean.addExtra("str1", reportSong?.songName ?: "str1")
                exposureReportDataBean.addExtra("str3", "str3")
                exposureReportDataBean.addExtra("str4", "str4")
                exposureReportDataBean.addExtra("str5", "str5")
                exposureReportDataBean.abt = reportSong?.extraInfo?.abt
                exposureReportDataBean.trace = reportSong?.extraInfo?.trace
                OpenApiSDK.getReportApi().reportExposureEvent(exposureReportDataBean)
                UiUtils.showToast( "首批歌曲size:${it?.second?.size}")
                UiUtils.showToast(
                    "第一首歌曲:《${it?.second?.first()?.songName}》")
            })
        }

        findViewById<Button>(R.id.btn_map_set_radio_playlist_songs).setOnClickListener {
            OpenApiSDK
                .getOpenApi()
                .fetchSongInfoBatch(
                    songIdList = listOf(TEST_SONG_ID, TEST_SONG_ID_2),
                    callback = {
                        OpenApiSDK.getPlayerApi().playRadio(it.data, autoPlay = false)
                        UiUtils.showToast(
                            "第一首歌曲:《${it.data?.first()?.songName}》")
                    }
                )
        }


        findViewById<Button>(R.id.btn_map_set_radio_playlist_songs_play).setOnClickListener {
            OpenApiSDK
                .getOpenApi()
                .fetchSongInfoBatch(
                    songIdList = listOf(282984491, 281936398, 107192076, 350626593, 292495504),
                    callback = {
                        OpenApiSDK.getPlayerApi().playRadio(
                            it.data,
                            itemId = RadioType.RADIO_TYPE_RECOMMEND_DJ_FORCE_ITEM_ID,
                            autoPlay = true
                        )
                        UiUtils.showToast(
                            "第一首歌曲:《${it.data?.first()?.songName}》")
                    }
                )

        }

        findViewById<Button>(R.id.btn_map_set_radio_playlist_songs_play_free).setOnClickListener {
            OpenApiSDK.getPlayerApi().playRadio(itemId = RadioType.RADIO_TYPE_RECOMMEND_DJ_FREE_ITEM_ID, autoPlay = true)

        }

        findViewById<Button>(R.id.btn_map_stop_radio).setOnClickListener {
            OpenApiSDK.getPlayerApi().stopRadio()
        }

        findViewById<Button>(R.id.btn_map_search_song).setOnClickListener {
            OpenApiSDK.getOpenApi().search("周杰伦歌曲", SearchType.SONG, source = SongListItemType.SONG_LIST_ITEM_TYPE_VOICE_SEARCH) {
                OpenApiSDK.getPlayerApi().playRadio(it.data?.songList, autoPlay = false)
                UiUtils.showToast(
                    "第一首歌曲:《${it.data?.songList?.first()?.songName}》")
            }
        }

        findViewById<Button>(R.id.btn_map_search_folder).setOnClickListener {
//            OpenApiSDK.getPlayerApi().playRadio(itemId = RadioType.RADIO_TYPE_RECOMMEND_DJ_FREE_ITEM_ID, autoPlay = true)

        }
    }

    // 其他设置：
    private fun otherSettingView() {
        findViewById<Button>(R.id.btn_map_get_operations).setOnClickListener {
            OpenApiSDK.getOpenApi().getOperationsInfo {
                UiUtils.showToast("运营信息：${GsonUtils.toJson(it.data)}")
            }
        }


        findViewById<Button>(R.id.btn_refresh_songInfo).setOnClickListener {
            OpenApiSDK.getPlayerApi().getPlayList().let { songs ->
                OpenApiSDK.getOpenApi().refreshSongInfoBatch(songInfoList = songs) {
                    if (it.isSuccess()) {
                        val newSongInfoList = it.data!!
                        for (i in 0..songs.size.dec()) {
                            if (songs[i].extraInfo?.songToken.isNullOrEmpty() != newSongInfoList[i].extraInfo?.songToken.isNullOrEmpty()) {
                                // 刷新后token消失或新增了
                                UiUtils.showToast(
                                    "歌曲:${newSongInfoList[i].songName},更新失败")
                            }
                        }
                        OpenApiSDK.getPlayerApi().playSongs(newSongInfoList)
                    } else {
                        UiUtils.showToast("更新失败:${it.errorMsg}")
                    }
                }
            }
        }

        findViewById<Button>(R.id.btn_songInfo_transfer).setOnClickListener {
            val playList = OpenApiSDK.getPlayerApi().getPlayList()
            if (playList.isEmpty()){
                UiUtils.showToast("当前播放列表为空，无法推荐相似单曲")
                return@setOnClickListener
            }
            val testSongInfo = playList.random()
            val transSongInfo = OpenApiSDK.getOpenApi().transSongInfoToJson(testSongInfo)
            val parseSongInfo = OpenApiSDK.getOpenApi().parseJsonToSongInfo(transSongInfo)
            if (parseSongInfo==null){
                UiUtils.showToast("歌曲转换失败:${testSongInfo.songId}")
                return@setOnClickListener
            }
            OpenApiSDK.getPlayerApi().playSongs(listOf(parseSongInfo))
        }


        findViewById<Button>(R.id.btn_map_test_rebuild).setOnClickListener {
            OpenApiSDK.getPlayerApi().stopRadio()
            OpenApiSDK.destroy()
//            runOnUiThread {
//                UiUtils.showToast(this, "等待5s后重新初始化", Toast.LENGTH_SHORT).show()
//                Thread.sleep(5 * 1000)
//            }
            val initConfig = InitConfig(
                this.applicationContext,
                MustInitConfig.APP_ID,
                MustInitConfig.APP_KEY,
                DeviceUtils.getAndroidID()
            ).apply {
                this.appForeground = true
                this.crashConfig =
                    InitConfig.CrashConfig(enableNativeCrashReport = true, enableAnrReport = true)
            }
            OpenApiSDK.init(initConfig)
        }
        findViewById<Button>(R.id.btn_map_set_callback_null).setOnClickListener {
            OpenApiSDK.getPlayerApi().setPlayLyricCallback(null)
            OpenApiSDK.getPlayerApi().setPlayProgressCallback(null)
            OpenApiSDK.getPlayerApi().setPlayRadioCallback(null)
        }

        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            OpenApiSDK.getPlayerApi().stop()
        }
    }

    private fun loginOut() {
        PlayerTestObj.currentVipIcon =
            "https://y.qq.com/mediastyle/lv-icon/v10/vip/2x/d-svip1.png?max_age=2592000"
        MineVM.logout()
    }

    /**
     * 歌单操作-添加/删除歌曲
     * @param folderId 歌单ID
     * @param songIdList 歌曲id列表
     * @param isAdding true:新增，false:删除
     */
    private fun operateSongFolder(folderId: String, songIdList: List<Long>?, isAdding: Boolean) {
        val resp = if (isAdding) {
            OpenApiSDK.getOpenApi().blockingGet<Boolean> {
                this.addSongToFolder(folderId = folderId, songIdList = songIdList, callback = it)
            }
        } else {
            OpenApiSDK.getOpenApi().blockingGet {
                this.deleteSongFromFolder(
                    folderId = folderId,
                    songIdList = songIdList,
                    callback = it
                )
            }
        }
        runOnUiThread {
            if (resp.isSuccess() && resp.data != null) {
                val actionText = if (isAdding) "成功收藏" else "取消收藏成功"
                UiUtils.showToast( actionText)
            } else {
                val actionText = if (isAdding) "收藏失败" else "取消收藏失败"
                UiUtils.showToast( "$actionText:${resp.errorMsg}")
            }
        }
    }

    // 回到播放页
    private fun returnPlayPage() {
        val intent = Intent()
        setResult(RESULT_CODE_GO_PlayPage, intent)
        finish()
    }

    // 更新地图自定义歌单列表
    private fun updateCustomSongListSquare(force:Boolean=false) {
        if (PlayerTestObj.customSongListSquare.isNullOrEmpty() || force) {
            val resp = OpenApiSDK.getOpenApi().blockingGet<List<SongListItem>> {
                OpenApiSDK.getOpenApi().fetchCustomSongListSquare(it)
            }
            if (resp.isSuccess()) {
                PlayerTestObj.customSongListSquare = resp.data
            } else {
                runOnUiThread {
                    UiUtils.showToast("接口异常:${resp.errorMsg}")
                }
            }
        }
    }

    /**
     * 播放指定的ItemType
     * @param:songListItemType: [SongListItemType]
     */
    private fun playSongsByItemType(songListItemType: Int) {
        thread {
            updateCustomSongListSquare()
            val songItemList = PlayerTestObj.customSongListSquare?.filter {
                it.type == songListItemType
            } ?: return@thread
            PlayerTestObj.currentItemIndex =
                if (PlayerTestObj.currentItemIndex >= songItemList.size) 0 else PlayerTestObj.currentItemIndex
            if (songItemList.isEmpty()||PlayerTestObj.currentItemIndex > songItemList.lastIndex) {
                runOnUiThread{
                    UiUtils.showToast("没有找到该类型的歌单")
                }
                updateCustomSongListSquare(force = true)
                return@thread
            }
            val songItem = songItemList[PlayerTestObj.currentItemIndex]
            PlayerTestObj.currentItemIndex += 1
            OpenApiSDK.getOpenApi().fetchCustomSceneSongList(
                songItem.itemId.toString(),
                songItem.type!!,
                "", 0, 20
            ) {
                if (it.isSuccess()) {
                    val songItemInfo = it.data?.first
                    if (songItemInfo?.type != songItem.type){
                        UiUtils.showToast("歌单类型不匹配,可能走到了兜底歌单:type=${songItemInfo?.type}")
                    }
                    val songInfoList = it.data?.second
                    songInfoList?.let { songList ->
                        handler.postDelayed({
                            OpenApiSDK.getPlayerApi()
                                .setPlayMode(if (songListItemType == SongListItemType.SONG_LIST_ITEM_TYPE_RANK) PlayerEnums.Mode.SHUFFLE else PlayerEnums.Mode.LIST) // 设置列表循环
                            OpenApiSDK.getPlayerApi()
                                .setPreferSongQuality(PlayerEnums.Quality.HQ) // 设置HQ
                            OpenApiSDK.getReportApi()
                                .setPlaySongsFrom(PageFromBean(songItemInfo?.type!!, songItemInfo.itemId))
                            OpenApiSDK.getPlayerApi()
                                .playSongs(songList)
                        }, 200)
                        UiUtils.showToast("播放歌单:${songItem.title},type:${it.data!!.first.type}")
                        returnPlayPage()
                        return@fetchCustomSceneSongList
                    }
                }
                updateCustomSongListSquare(force = true)
                runOnUiThread {
                    UiUtils.showToast("报错:${it.errorMsg},type=${songItem.type}")
                }
            }
        }
    }
}


object PlayerTestObj {
    var currentItemIndex: Int = 0
    var lastUpdateTimeStamp: Long = 0
    var currentVipIcon: String? =
        "https://y.qq.com/mediastyle/lv-icon/v10/vip/2x/d-svip1.png?max_age=2592000"
    var selfFavFileId: String? = null
    var customSongListSquare: List<SongListItem>? = null
}