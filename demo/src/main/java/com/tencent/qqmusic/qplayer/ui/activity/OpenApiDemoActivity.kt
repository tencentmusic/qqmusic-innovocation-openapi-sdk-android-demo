package com.tencent.qqmusic.qplayer.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.openapisdk.model.SearchType
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApi
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiCallback
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.openapisdk.model.SearchResult
import com.tencent.qqmusic.openapisdk.model.VipInfo
import com.tencent.qqmusic.qplayer.baselib.util.QLog
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@SuppressLint("SetTextI18n")
class OpenApiDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OpenApiDemoActivity"
        private val testMultiBlockingGetBlock: OpenApi.(OpenApiCallback<OpenApiResponse<VipInfo>>) -> Unit =
            {
                OpenApiSDK.getOpenApi().fetchGreenMemberInformation(callback = it)
            }
    }

    private val methodNameToBlock = mutableMapOf<String, (MethodNameWidthParam) -> Unit>()
    private val methodNameWithParamList = mutableListOf<MethodNameWidthParam>()

    private val openApi by lazy { OpenApiSDK.getOpenApi() }
    private val displayTv: TextView by lazy {
        findViewById<TextView>(R.id.display)
    }
    private lateinit var spinner: Spinner
    private lateinit var param1: EditText
    private lateinit var param2: EditText
    private lateinit var param3: EditText
    private lateinit var param4: EditText
    private lateinit var param5: EditText

    private var paramStr1: String? = null
    private var paramStr2: String? = null
    private var paramStr3: String? = null
    private var paramStr4: String? = null
    private var paramStr5: String? = null

    // ????????????: 8219435055, ?????????????????????
    //
    private var createFolderId = "8202113137"
    private var createFolderSuccess: Boolean? = null
    private var orderId = ""

    private val errorOpiNameAndMsg = mutableListOf<String>()
    private val displayResponseCallback: OpenApiCallback<OpenApiResponse<*>> = {
        displayOpenApiResponse(it)
    }
    private var displayMode = true
    private var waitCallbackLatch = CountDownLatch(0)

    private inner class MethodNameWidthParam(
        val name: String,
        val edtHint: List<String>,
        val paramDefault: List<String?>
    )

    private inner class CallbackWithName(val param: MethodNameWidthParam) :
        OpenApiCallback<OpenApiResponse<*>> {
        override fun invoke(data: OpenApiResponse<*>) {
            val methodName = param.name
            if (displayMode) {
                displayResponseCallback.invoke(data)
            } else {
                if (!data.isSuccess()) {
                    errorOpiNameAndMsg.add("${methodName}: ${data.errorMsg}")
                }
                waitCallbackLatch.countDown()
            }
        }

    }

    private fun dismissParamEdt() {
        param1.visibility = View.GONE
        param2.visibility = View.GONE
        param3.visibility = View.GONE
        param4.visibility = View.GONE
        param5.visibility = View.GONE
    }

    private fun fillDefaultParamIfNull(param: MethodNameWidthParam) {
        if (displayMode) {
            val defaultParam = param.paramDefault
            if (paramStr1 == null && defaultParam.isNotEmpty()) {
                paramStr1 = defaultParam[0]
            }
            if (paramStr2 == null && defaultParam.size > 1) {
                paramStr2 = defaultParam[1]
            }
            if (paramStr3 == null && defaultParam.size > 2) {
                paramStr3 = defaultParam[2]
            }
            if (paramStr4 == null && defaultParam.size > 3) {
                paramStr4 = defaultParam[3]
            }
            if (paramStr5 == null && defaultParam.size > 4) {
                paramStr5 = defaultParam[4]
            }
        } else {
            paramStr1 = null
            paramStr2 = null
            paramStr3 = null
            paramStr4 = null
            paramStr5 = null
            val defaultParam = param.paramDefault
            if (defaultParam.isNotEmpty()) {
                paramStr1 = defaultParam[0]
            }
            if (defaultParam.size > 1) {
                paramStr2 = defaultParam[1]
            }
            if (defaultParam.size > 2) {
                paramStr3 = defaultParam[2]
            }
            if (defaultParam.size > 3) {
                paramStr4 = defaultParam[3]
            }
            if (defaultParam.size > 4) {
                paramStr5 = defaultParam[4]
            }
        }
    }

    private fun getParamStr() {
        paramStr1 = param1.text?.toString()
        paramStr2 = param2.text?.toString()
        paramStr3 = param3.text?.toString()
        paramStr4 = param4.text?.toString()
        paramStr5 = param5.text?.toString()
        if (paramStr1.isNullOrEmpty()) {
            paramStr1 = null
        }
        if (paramStr2.isNullOrEmpty()) {
            paramStr2 = null
        }
        if (paramStr3.isNullOrEmpty()) {
            paramStr3 = null
        }
        if (paramStr4.isNullOrEmpty()) {
            paramStr4 = null
        }
        if (paramStr5.isNullOrEmpty()) {
            paramStr5 = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_api_demo)
        spinner = findViewById<Spinner>(R.id.spinner)
        param1 = findViewById(R.id.edt_param_1)
        param2 = findViewById(R.id.edt_param_2)
        param3 = findViewById(R.id.edt_param_3)
        param4 = findViewById(R.id.edt_param_4)
        param5 = findViewById(R.id.edt_param_5)
        initMethodNameList()
        val arrayAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            android.R.id.text1,
            methodNameWithParamList.map { it.name }
        )
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onMethodNameSelected(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
        setupMethodNameToBlock()

        findViewById<Button>(R.id.btn_send).setOnClickListener {
            displayTv.text = "???????????????, ??????????????????..."
            displayMode = true
            getParamStr()

            val position = spinner.selectedItemPosition
            val methodNameWidthParam = methodNameWithParamList[position]
            val name = methodNameWidthParam.name

            val block = methodNameToBlock[name]
            try {
                block!!.invoke(methodNameWidthParam)
            } catch (e: Throwable) {
                Log.i(TAG, "block invoke failed", e)
            }
        }

        findViewById<View>(R.id.btn_test2).setOnClickListener {
            repeat(1000) {
                thread {
                    val resp = OpenApiSDK.getOpenApi().blockingGet<VipInfo>(
                        testMultiBlockingGetBlock)
                    if (!resp.isSuccess()) {
                        QLog.e(TAG, "??????????????????, resp=${resp}")
                    } else {
                        QLog.i(TAG, "??????????????????")
                    }
                }
            }
        }

        findViewById<View>(R.id.btn_test).setOnClickListener {
            errorOpiNameAndMsg.clear()
            displayTv.text = "????????????, ?????????..."
            thread {
                displayMode = false
                createFolderSuccess = null
                val keys = methodNameToBlock.keys
                waitCallbackLatch = CountDownLatch(keys.size)
                for (name in keys) {
                    var methodWithParam: MethodNameWidthParam? = null
                    for (param in methodNameWithParamList) {
                        if (param.name == name) {
                            methodWithParam = param
                        }
                    }
                    val block = methodNameToBlock[name]
                    if (name == "deleteFolder") {
                        while (createFolderSuccess == null) {
                            // ????????????????????????????????????
                            Thread.sleep(500)
                        }
                        if (createFolderSuccess == false) {
                            // ?????????????????????, ???????????????
                            waitCallbackLatch.countDown()
                            continue
                        }
                    }
                    try {
                        block?.invoke(methodWithParam!!)
                    } catch (e: Exception) {
                        Log.e(TAG, "invoke method, name=$name", e)
                    }
                }
                val ret = waitCallbackLatch.await(30, TimeUnit.SECONDS)
                val text = if (ret) {
                    if (errorOpiNameAndMsg.isNotEmpty()) {
                        buildString {
                            append("???????????????: \n")
                            for (name in errorOpiNameAndMsg) {
                                append(name).append("\n")
                            }
                        }
                    } else {
                        "??????????????????"
                    }
                } else {
                    "???????????????????????????????????????"
                }
                runOnUiThread {
                    displayTv.text = text
                }
                displayMode = true
            }
        }

        val tvLoginInfo = findViewById<TextView>(R.id.tv_login_info)
        tvLoginInfo.text = "????????????: ${OpenApiSDK.getLoginApi().hasLogin()}"

        val edtWatch = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                getParamStr()
            }

        }
        param1.addTextChangedListener(edtWatch)
        param2.addTextChangedListener(edtWatch)
        param3.addTextChangedListener(edtWatch)
        param4.addTextChangedListener(edtWatch)
        param5.addTextChangedListener(edtWatch)
    }

    private fun onMethodNameSelected(position: Int) {
        val methodParam = methodNameWithParamList[position]

        dismissParamEdt()
        val count = methodParam.edtHint.size
        if (count >= 1) {
            param1.visibility = View.VISIBLE
        }
        if (count >= 2) {
            param2.visibility = View.VISIBLE
        }
        if (count >= 3) {
            param3.visibility = View.VISIBLE
        }
        if (count >= 4) {
            param4.visibility = View.VISIBLE
        }
        if (count >= 5) {
            param5.visibility = View.VISIBLE
        }
        methodParam.edtHint.forEachIndexed { index, hint ->
            val i = index + 1
            if (i == 1) {
                param1.hint = hint
            }
            if (i == 2) {
                param2.hint = hint
            }
            if (i == 3) {
                param3.hint = hint
            }
            if (i == 4) {
                param4.hint = hint
            }
            if (i == 5) {
                param5.hint = hint
            }
        }
    }

    private val prettyGson by lazy {
        GsonBuilder().setPrettyPrinting().create()
    }

    private fun displayOpenApiResponse(response: OpenApiResponse<*>) {
        val builder = StringBuilder()
        builder.append("OpenApiResponse: ").append("\n")
        builder.append("ret=").append(response.ret).append("\n")
        builder.append("subRet=").append(response.subRet).append("\n")
        builder.append("errorMsg=").append(response.errorMsg).append("\n")
        builder.append("page=").append(response.page).append("\n")
        builder.append("totalCount=").append(response.totalCount).append("\n")
        builder.append("hasMore=").append(response.hasMore).append("\n")
        builder.append("data=").append(prettyGson.toJson(response.data)).append("\n")
        val text = builder.toString()
        val spannable = SpannableStringBuilder(text)
        displayTv.text = spannable
    }

    private fun String.edtParamToList(): List<String> {
        return split(",")
    }

    private fun setupMethodNameToBlock() {

//        val openApi = OpenApiSDK.getOpenApi()
//        openApi.fetchFolderDetail(folderId = "123456") { response->
//            if (response.isSuccess()) {
//                val folderDetail = response.data
//                Log.e(TAG, "??????????????????: $folderDetail")
//            } else {
//                Log.e(TAG, "????????????????????????: ${response.errorMsg}")
//            }
//        }

        methodNameToBlock["fetchGreenMemberInformation"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchGreenMemberInformation(commonCallback)
//            thread {
//                val ret = Global.getOpenApi().blockingGet<SearchResult> {
//                    search("?????????", SearchType.ALBUM, callback = it)
//                }
//                if (ret.isSuccess()) {
//                    MLog.i(TAG, "??????????????????: ${ret.data}")
//                } else {
//                    MLog.i(TAG, "????????????")
//                }
            //commonCallback.invoke(ret)
//            }
        }
        methodNameToBlock["fetchUserInfo"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchUserInfo(commonCallback)
        }
        methodNameToBlock["createGreenOrder"] = {
            val callback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.createGreenOrder(paramStr1!!, paramStr2!!, paramStr3!!.toInt()) {
                if (it.isSuccess()) {
                    orderId = it.data!!
                }
                callback.invoke(it)
            }
        }
        methodNameToBlock["queryGreenOrder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.queryGreenOrder(
                paramStr1!!,
                paramStr2!!,
                commonCallback
            )
        }
        methodNameToBlock["searchAlbum"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.search(
                paramStr1!!, SearchType.ALBUM, paramStr2?.toInt() ?: 0,
                paramStr3?.toInt() ?: 20, callback = commonCallback
            )
        }
        methodNameToBlock["searchSong"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.search(
                paramStr1!!, SearchType.SONG, paramStr2?.toInt() ?: 0,
                paramStr3?.toInt() ?: 20, callback = commonCallback
            )
        }
        methodNameToBlock["searchFolder"] = {
            val callback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.search(paramStr1!!, SearchType.FOLDER, paramStr2?.toInt() ?: 0,
                paramStr3?.toInt() ?: 20, callback = { data ->
                    data.data?.folderList?.forEach { folder ->
                        val creator = folder.creator
                    }
                    callback.invoke(data)
                })
        }
        methodNameToBlock["searchRadio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.search(
                paramStr1!!, SearchType.RADIO, paramStr2?.toInt() ?: 0,
                paramStr3?.toInt() ?: 20, callback = commonCallback
            )
        }
        methodNameToBlock["searchLyric"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.search(
                paramStr1!!, SearchType.LYRIC, paramStr2?.toInt() ?: 0,
                paramStr3?.toInt() ?: 10, callback = commonCallback
            )
        }
        methodNameToBlock["searchSmart"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.searchSmart(paramStr1!!, commonCallback)
        }
        methodNameToBlock["createFolder"] = {
            val callback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.createFolder(paramStr1!!) {
                if (it.isSuccess()) {
                    createFolderId = it.data!!
                }
                createFolderSuccess = it.isSuccess()
                callback.invoke(it)
            }
        }
        methodNameToBlock["deleteFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.deleteFolder(paramStr1!!, commonCallback)
        }
        methodNameToBlock["addSongToFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.addSongToFolder(
                paramStr1!!,
                paramStr2?.edtParamToList()?.map { v -> v.toLong() },
                paramStr3?.edtParamToList(),
                paramStr4?.edtParamToList(), commonCallback
            )
        }
        methodNameToBlock["fetchSongOfFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 3805603854: ?????????????????????21??????????????????
            openApi.fetchSongOfFolder(
                paramStr1!!,
                paramStr2?.toInt() ?: 0,
                paramStr3?.toInt() ?: 20,
                callback = commonCallback
            )
        }
        methodNameToBlock["fetchPersonalFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchPersonalFolder(commonCallback)
        }
        methodNameToBlock["fetchCollectedFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchCollectedFolder(commonCallback)
        }
        methodNameToBlock["collectFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 3805603854: ?????????????????????21??????????????????
            openApi.collectFolder(paramStr1!!, commonCallback)
        }
        methodNameToBlock["unCollectFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 3805603854: ?????????????????????21??????????????????
            openApi.unCollectFolder(paramStr1!!, commonCallback)
        }
        methodNameToBlock["deleteSongFromFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.deleteSongFromFolder(
                paramStr1!!,
                paramStr2?.edtParamToList()?.map { v -> v.toLong() },
                paramStr3?.edtParamToList(),
                paramStr4?.edtParamToList(),
                commonCallback
            )
        }
        methodNameToBlock["fetchCategoryOfFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchCategoryOfFolder(commonCallback)
        }
        methodNameToBlock["fetchFolderListByCategory"] = {
            val callback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 3317: ????????????
            openApi.fetchFolderListByCategory(
                paramStr1!!.edtParamToList().map { v -> v.toInt() },
                paramStr2?.toInt() ?: 0,
                paramStr3?.toInt() ?: 20,
                callback = {
                    it.data?.forEach { folder ->
                        val creator = folder.creator
                    }
                    callback.invoke(it)
                })
        }
        methodNameToBlock["fetchFolderDetail"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 3805603854: ?????????????????????21??????????????????
            openApi.fetchFolderDetail(paramStr1!!, commonCallback)
        }
        methodNameToBlock["fetchSongInfoBatch"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchSongInfoBatch(
                paramStr1?.edtParamToList()?.map { v -> v.toLong() },
                paramStr2?.edtParamToList(), commonCallback
            )
        }
        methodNameToBlock["fetchNewSongRecommend"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchNewSongRecommend(paramStr1!!.toInt(), commonCallback)
        }
        methodNameToBlock["fetchLyric"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchLyric(paramStr1?.toLong(), paramStr2, commonCallback)
        }
        methodNameToBlock["fetchCategoryOfPublicRadio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchCategoryOfPublicRadio(commonCallback)
        }
        methodNameToBlock["fetchPublicRadioListByCategory"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 22: ??????
            openApi.fetchPublicRadioListByCategory(
                paramStr1!!.edtParamToList().map { v -> v.toInt() }, commonCallback
            )
        }
        methodNameToBlock["fetchSongOfPublicRadio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 568: ??????
            openApi.fetchSongOfPublicRadio(
                paramStr1!!.toInt(),
                paramStr2?.toInt() ?: 10,
                callback = commonCallback
            )
        }
        methodNameToBlock["fetchJustListenRadio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchJustListenRadio(commonCallback)
        }
        methodNameToBlock["fetchSongOfJustListenRadio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 601: ????????????-??????
            openApi.fetchSongOfJustListenRadio(paramStr1!!.toInt(), commonCallback)
        }
        methodNameToBlock["fetchCategoryOfRank"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchCategoryOfRank(commonCallback)
        }
        methodNameToBlock["fetchRankDetailByCategory"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // ?????????->?????????
            openApi.fetchRankDetailByCategory(
                paramStr1!!.edtParamToList().map { v -> v.toInt() },
                commonCallback
            )
        }
        methodNameToBlock["fetchSongOfRank"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // ?????????
            openApi.fetchSongOfRank(
                paramStr1!!.toInt(),
                paramStr2?.toInt() ?: 0,
                paramStr2?.toInt() ?: 20, callback = commonCallback
            )
        }
        methodNameToBlock["fetchAlbumDetail"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 4805991: ?????????
            openApi.fetchAlbumDetail(paramStr1, paramStr2, commonCallback)
        }
        methodNameToBlock["fetchSongOfAlbum"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 4805991: ?????????
            openApi.fetchSongOfAlbum(
                paramStr1, paramStr2, paramStr3?.toInt() ?: 0,
                paramStr4?.toInt() ?: 20, callback = commonCallback
            )
        }
        methodNameToBlock["fetchSongOfSinger"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 19535: ?????????
            openApi.fetchSongOfSinger(
                paramStr1!!.toInt(),
                paramStr2?.toInt() ?: 0,
                paramStr3?.toInt() ?: 20,
                paramStr4?.toInt() ?: 0,
                callback = commonCallback
            )
        }
        methodNameToBlock["fetchHotSingerList"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchHotSingerList(
                paramStr1?.toInt() ?: -100,
                paramStr2?.toInt() ?: -100,
                paramStr3?.toInt() ?: -100,
                callback = commonCallback
            )
        }
        methodNameToBlock["fetchAlbumOfSinger"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 19535: ?????????
            openApi.fetchAlbumOfSinger(
                paramStr1!!.toInt(),
                paramStr2?.toInt() ?: 0,
                paramStr3?.toInt() ?: 20,
                paramStr4?.toInt() ?: 0,
                callback = commonCallback
            )
        }
        methodNameToBlock["fetchDailyRecommendSong"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchDailyRecommendSong(commonCallback)
        }
        methodNameToBlock["fetchPersonalRecommendSong"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchPersonalRecommendSong(commonCallback)
        }
        methodNameToBlock["fetchSimilarSong"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchSimilarSong(
                paramStr1?.toLong(),
                paramStr2,
                paramStr3?.edtParamToList()?.map { v -> v.toLong() },
                callback = commonCallback
            )
        }
        methodNameToBlock["fetchCategoryOfRecommendLongAudio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchCategoryOfRecommendLongAudio(commonCallback)
        }
        methodNameToBlock["fetchAlbumListOfRecommendLongAudioByCategory"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // "????????????".hashCode = 898115530
            // "?????????": 28753163
            openApi.fetchAlbumListOfRecommendLongAudioByCategory(
                paramStr1!!.edtParamToList().map { v -> v.toInt() },
                commonCallback
            )
        }
        methodNameToBlock["fetchGuessLikeLongAudio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchGuessLikeLongAudio(commonCallback)
        }
        methodNameToBlock["fetchCategoryOfRankLongAudio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchCategoryOfRankLongAudio(commonCallback)
        }
        methodNameToBlock["fetchAlbumListOfRankLongAudioByCategory"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 4013: ????????? 937: ????????????
            openApi.fetchAlbumListOfRankLongAudioByCategory(
                paramStr1!!.edtParamToList().map { v -> v.toInt() },
                commonCallback
            )
        }
        methodNameToBlock["fetchCategoryOfLongAudio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchCategoryOfLongAudio(commonCallback)
        }
        methodNameToBlock["fetchCategoryFilterOfLongAudio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 1001: ????????? 1502: ??????
            openApi.fetchCategoryFilterOfLongAudio(
                paramStr1!!.edtParamToList().map { v -> v.toInt() },
                commonCallback
            )
        }
        methodNameToBlock["fetchAlbumListOfLongAudioByCategory"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 1001: ????????? 1502: ??????
            // 2: ????????????
            openApi.fetchAlbumListOfLongAudioByCategory(
                paramStr1!!.edtParamToList().map { v -> v.toInt() },
                paramStr2!!.edtParamToList().map { v -> v.toInt() },
                paramStr3?.toInt() ?: 0,
                paramStr4?.toInt() ?: 20,
                paramStr5?.toInt() ?: 0,
                callback = commonCallback
            )
        }
        methodNameToBlock["fetchRecentUpdateLongAudio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchRecentUpdateLongAudio(commonCallback)
        }
        methodNameToBlock["fetchLikeListLongAudio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchLikeListLongAudio(commonCallback)
        }
        methodNameToBlock["fetchRecentPlayLongAudio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchRecentPlayLongAudio(commonCallback)
        }

        methodNameToBlock["fetchRecentPlaySong"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchRecentPlaySong(paramStr1!!.toLong(), callback = commonCallback)
        }
        methodNameToBlock["fetchRecentPlayAlbum"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchRecentPlayAlbum(paramStr1!!.toLong(), callback = commonCallback)
        }
        methodNameToBlock["fetchRecentPlayFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchRecentPlayFolder(paramStr1!!.toLong(), callback = commonCallback)
        }
        //methodNameToBlock["reportRecentPlay"] = {
        //    val commonCallback = CallbackWithName(it)
        //    fillDefaultParamIfNull(it)
        //    openApi.reportRecentPlay(paramStr1!!.toLong(), paramStr2!!.toInt(), commonCallback)
        //}
        methodNameToBlock["musicSkill"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)

            val slotsMap = mutableMapOf<String, String>()
            val kvStrList = paramStr2?.split(",") ?: emptyList()
            for (kvStr in kvStrList) {
                val kv = kvStr.split(":")
                slotsMap[kv.first()] = kv[1]
            }

            openApi.musicSkill(
                paramStr1!!,
                slotsMap,
                paramStr3 ?: "",
                paramStr4?.toLong(),
                paramStr5?.toInt() ?: 20,
                callback = commonCallback
            )
        }
        methodNameToBlock["reportRecentPlay"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.reportRecentPlay(paramStr1!!.toLong(), 2, callback = commonCallback)
        }
    }

    private fun initMethodNameList() {
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchGreenMemberInformation", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchUserInfo", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "createGreenOrder",
                listOf("matchId", "????????????", "????????????????????????"),
                listOf(MustInitConfig.MATCH_ID, "", "2")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "queryGreenOrder",
                listOf("matchId", "??????id"),
                listOf(MustInitConfig.MATCH_ID, "")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchAlbum", listOf("?????????", "?????????????????????", "?????????????????????????????????"), listOf("?????????", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchSong", listOf("?????????", "?????????????????????", "?????????????????????????????????"), listOf("?????????", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchFolder", listOf("?????????", "?????????????????????", "?????????????????????????????????"), listOf("?????????", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchRadio", listOf("?????????", "?????????????????????", "?????????????????????????????????"), listOf("?????????", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchLyric", listOf("?????????", "?????????????????????", "?????????????????????????????????"), listOf("?????????", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchSmart", listOf("?????????"), listOf("?????????")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "createFolder", listOf("????????????"), listOf("OpenApi????????????")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "deleteFolder", listOf("??????id"), listOf("8202113137")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfFolder",
                listOf("??????id", "?????????????????????", "?????????????????????????????????"),
                listOf("3805603854", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchPersonalFolder", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCollectedFolder", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "collectFolder", listOf("??????id"), listOf("3805603854")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "unCollectFolder", listOf("??????id"), listOf("3805603854")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "addSongToFolder",
                listOf("??????id", "??????id??????", "??????mid??????", "????????????"),
                listOf("8219435055", "314818717,317968884,316868744,291130348", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "deleteSongFromFolder",
                listOf("??????id", "??????id??????", "??????mid??????", "????????????"),
                listOf("8219435055", "314818717", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryOfFolder", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchFolderListByCategory",
                listOf("??????id??????", "?????????????????????", "?????????????????????????????????"),
                listOf("0,3317", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchFolderDetail", listOf("??????id"), listOf("3805603854")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongInfoBatch",
                listOf("??????id??????", "??????mid??????"),
                listOf("314818717,317968884,316868744,291130348", null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchNewSongRecommend", listOf("??????"), listOf("12")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchLyric", listOf("??????id", "??????mid"), listOf("314818717")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryOfPublicRadio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchPublicRadioListByCategory", listOf("??????id??????"), listOf("22")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfPublicRadio", listOf("??????id", "?????????????????????????????????"), listOf("568", null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchJustListenRadio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfJustListenRadio", listOf("??????id"), listOf("601")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryOfRank", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchRankDetailByCategory", listOf("??????id??????"), listOf("0,62")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfRank",
                listOf("??????id", "?????????????????????", "?????????????????????????????????"),
                listOf("62", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAlbumDetail", listOf("??????id", "??????mid"), listOf("4805991", null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfAlbum",
                listOf("??????id", "??????mid", "?????????????????????", "?????????????????????????????????"),
                listOf("4805991", null, null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfSinger",
                listOf("??????id", "?????????????????????", "?????????????????????????????????", "???????????????0?????????; 1????????????"),
                listOf("19535", null, null, "0")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchHotSingerList", listOf("??????", "??????", "??????"), listOf("-100", "-100", "-100")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAlbumOfSinger",
                listOf("??????id", "?????????????????????", "?????????????????????????????????", "???????????????0?????????; 1????????????"),
                listOf("19535", null, null, "0")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchDailyRecommendSong", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchPersonalRecommendSong", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSimilarSong",
                listOf("??????id", "??????mid", "???????????????id??????"),
                listOf("314818717", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryOfRecommendLongAudio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAlbumListOfRecommendLongAudioByCategory",
                listOf("??????id??????"),
                listOf(if (OpenApiSDK.getLoginApi().hasLogin()) "898115530" else "28753163")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchGuessLikeLongAudio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryOfRankLongAudio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAlbumListOfRankLongAudioByCategory", listOf("??????id??????"), listOf("4013,937")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryOfLongAudio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryFilterOfLongAudio", listOf("??????id??????"), listOf("1001,1502")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAlbumListOfLongAudioByCategory",
                listOf("??????id??????", "??????id??????", "?????????????????????", "?????????????????????????????????", "???????????????0?????????; 1????????????"),
                listOf("1001,1502", "2,0", null, null, "0")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchRecentUpdateLongAudio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchLikeListLongAudio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchRecentPlayLongAudio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchRecentPlaySong", listOf("??????????????????"), listOf("0")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchRecentPlayAlbum", listOf("??????????????????"), listOf("0")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchRecentPlayFolder", listOf("??????????????????"), listOf("0")
            )
        )
        //methodNameWithParamList.add(
        //    MethodNameWidthParam(
        //        "reportRecentPlay", listOf("??????id", "????????????"), listOf("314818717", "2")
        //    )
        //)
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "musicSkill",
                listOf("??????", "?????????(kv?????????????????????????????????????????????)", "????????????", "??????????????????id(?????????)", "???????????????????????????"),
                listOf("????????????", "?????????:?????????,????????????:??????", "?????????????????????", null, null)
            )
        )
    }

}