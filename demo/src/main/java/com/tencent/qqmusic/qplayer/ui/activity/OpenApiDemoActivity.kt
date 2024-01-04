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
import androidx.core.widget.addTextChangedListener
import com.google.gson.GsonBuilder
import com.tencent.qqmusic.openapisdk.business_common.Global
import com.tencent.qqmusic.openapisdk.business_common.login.OpenIdInfo
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApi
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiCallback
import com.tencent.qqmusic.openapisdk.core.openapi.OpenApiResponse
import com.tencent.qqmusic.openapisdk.model.SearchType
import com.tencent.qqmusic.openapisdk.model.VipInfo
import com.tencent.qqmusic.qplayer.R
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@SuppressLint("SetTextI18n")
class OpenApiDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OpenApiDemoActivity"
        private val testMultiBlockingGetBlock: OpenApi.(OpenApiCallback<OpenApiResponse<VipInfo>>) -> Unit = {
            OpenApiSDK.getOpenApi().fetchGreenMemberInformation(callback = it)
        }
        private var hasRunConcurrent = false
    }

    private val methodNameToBlock = mutableMapOf<String, (MethodNameWidthParam) -> Unit>()
    private val methodNameWithParamList = mutableListOf<MethodNameWidthParam>()

    private val openApi by lazy { OpenApiSDK.getOpenApi() }
    private val displayTv: TextView by lazy {
        findViewById<TextView>(R.id.display)
    }
    private lateinit var spinner: Spinner
    private var inputEditText: EditText? = null
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

    // 用来在一键测试中，避免异步调用顺序混乱引起的逻辑错误
    private var createFolderId: String? = null
    private var createFolderSuccess: Boolean? = null
    private var createdFolderDeleted: Boolean = false
    private var folderSongAdded: Boolean = false
    private var orderId = ""
    private var tempList = listOf<String>()

    private val errorOpiNameAndMsg = mutableListOf<String>()
    private val displayResponseCallback: OpenApiCallback<OpenApiResponse<*>> = {
        displayOpenApiResponse(it)
    }
    private var displayMode = true
    private var waitCallbackLatch = CountDownLatch(0)

    private inner class MethodNameWidthParam(
        val name: String,
        val edtHint: List<String>,
        var paramDefault: List<String?>,
    )

    private var arrayAdapter: ArrayAdapter<String>? = null

    private inner class CallbackWithName(val param: MethodNameWidthParam) : OpenApiCallback<OpenApiResponse<*>> {
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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_api_demo)
        val showDebugButton = intent.getBooleanExtra("isDebug", false)
        spinner = findViewById<Spinner>(R.id.spinner)

        inputEditText = findViewById<EditText>(R.id.method_input_key).apply {
            addTextChangedListener {
                updateMethodKey(it?.toString())
            }
        }

        param1 = findViewById(R.id.edt_param_1)
        param2 = findViewById(R.id.edt_param_2)
        param3 = findViewById(R.id.edt_param_3)
        param4 = findViewById(R.id.edt_param_4)
        param5 = findViewById(R.id.edt_param_5)
        initMethodNameList()
        arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1, methodNameWithParamList.map { it.name })
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                val item = arrayAdapter?.getItem(position) ?: ""
                onMethodNameSelected(item)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
        setupMethodNameToBlock()

        findViewById<Button>(R.id.btn_send).setOnClickListener {
            displayTv.text = "请求已发送, 正在等待响应..."
            displayMode = true
            getParamStr()

            val item = arrayAdapter?.getItem(spinner.selectedItemPosition)
            val methodNameWidthParam = methodNameWithParamList.first { it.name == item }
            val name = methodNameWidthParam.name

            val block = methodNameToBlock[name]
            try {
                block!!.invoke(methodNameWidthParam)
            } catch (e: Throwable) {
                Log.i(TAG, "block invoke failed", e)
            }
        }

        findViewById<View>(R.id.btn_test2).setOnClickListener {
            val openIdInfo = Global.getLoginModuleApi().openIdInfo
            openIdInfo?.apply {
                Global.getLoginModuleApi().openIdInfo = OpenIdInfo(
                    expireTime = expireTime,
                    openId = openId,
                    accessToken = "auh6b4002f6eb9585f13d562b6d2229e052c37daae2e2cb350582832c9ae958c410",
                    refreshToken = refreshToken,
                    type = type
                )
            }

        }
        val debugBottom = findViewById<View>(R.id.btn_test)
        debugBottom.visibility = if (showDebugButton) View.VISIBLE else View.GONE
        debugBottom.setOnClickListener {
            errorOpiNameAndMsg.clear()
            displayTv.text = "正在请求, 请稍等..."
            thread {

                // 先创建一个歌单，用作后续的deleteFolder、addSongToFolder、deleteSongFromFolder的folderId默认值
                Global.getOpenApi().blockingGet<String> {
                    Global.getOpenApi().createFolder("一键测试的前置创建歌单") { callback ->
                        if (callback.isSuccess()) {
                            createFolderId = callback.data
                            createFolderSuccess = true
                        } else {
                            createFolderSuccess = false
                        }
                        methodNameWithParamList.find {
                            it.name == "deleteFolder"
                        }?.paramDefault = listOf(createFolderId)

                        methodNameWithParamList.find {
                            it.name == "addSongToFolder"
                        }?.paramDefault = listOf(createFolderId, "314818717,317968884,316868744,291130348", null, null)

                        methodNameWithParamList.find {
                            it.name == "deleteSongFromFolder"
                        }?.paramDefault = listOf(createFolderId, "314818717", null, null)
                        it.invoke(callback)
                    }
                }

                displayMode = false
                createdFolderDeleted = false
                createdFolderDeleted = false
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
                        if (createFolderSuccess == false) {
                            // 创建接口失败了, 删除不走了
                            waitCallbackLatch.countDown()
                            continue
                        }
                    } else if (name == "addSongToFolder") {
                        // 对歌单添加歌曲要等创建成功，并且没有被删除
                        if (createFolderSuccess == false || createdFolderDeleted) {
                            waitCallbackLatch.countDown()
                            continue
                        }
                    } else if (name == "deleteSongFromFolder") {
                        // 对歌单添加歌曲要等创建成功，并且没有被删除，并且歌曲已经添加
                        if (createFolderSuccess == false || createdFolderDeleted || !folderSongAdded) {
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
                            append("失败的接口: \n")
                            for (name in errorOpiNameAndMsg) {
                                append(name).append("\n")
                            }
                        }
                    } else {
                        "所有接口已通"
                    }
                } else {
                    "接口调用超时，联系开发处理"
                }
                runOnUiThread {
                    displayTv.text = text
                }
                displayMode = true
            }
        }

        val tvLoginInfo = findViewById<TextView>(R.id.tv_login_info)
        tvLoginInfo.text = "是否登录: ${OpenApiSDK.getLoginApi().hasLogin()}"

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

    private fun updateMethodKey(key: String?) {
        val newList = methodNameWithParamList.filter { it.name.contains(key ?: "", ignoreCase = true) }
        arrayAdapter?.clear()
        arrayAdapter?.addAll(newList.map { data -> data.name })
        arrayAdapter?.notifyDataSetChanged()
        val item = arrayAdapter?.getItem(0) ?: ""
        onMethodNameSelected(item)
    }

    private fun onMethodNameSelected(key: String) {
        val methodParam = methodNameWithParamList.first { it.name == key }

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
        methodNameToBlock["fetchGreenMemberInformation"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchGreenMemberInformation(commonCallback)
        }
        methodNameToBlock["fetchIotMemberInformation"] = {
            val commonCallback = CallbackWithName(it)
            openApi.fetchIotMemberInformation(commonCallback)
        }
        methodNameToBlock["fetchUserInfo"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchUserInfo(commonCallback)
        }
        methodNameToBlock["createGreenOrder"] = {
            val callback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // matchId: 95B26A42074C906A
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
            // 测试trade_info: 123456781234567812345673__20210924
            openApi.queryGreenOrder(
                paramStr1!!, paramStr2!!, commonCallback
            )
        }
        methodNameToBlock["fetchAllRankGroup"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 测试trade_info: 123456781234567812345673__20210924
            openApi.fetchAllRankGroup(
                commonCallback
            )
        }

        methodNameToBlock["searchAlbum"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.search(
                paramStr1!!, SearchType.ALBUM, paramStr2?.toInt() ?: 0, paramStr3?.toInt() ?: 20, callback = commonCallback
            )
        }
        methodNameToBlock["searchSong"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.search(
                paramStr1!!, SearchType.SONG, paramStr2?.toInt() ?: 0, paramStr3?.toInt() ?: 20, callback = commonCallback
            )
        }
        methodNameToBlock["searchFolder"] = {
            val callback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.search(paramStr1!!, SearchType.FOLDER, paramStr2?.toInt() ?: 0, paramStr3?.toInt() ?: 20, callback = { data ->
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
                paramStr1!!, SearchType.RADIO, paramStr2?.toInt() ?: 0, paramStr3?.toInt() ?: 20, callback = commonCallback
            )
        }
        methodNameToBlock["searchLyric"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.search(
                paramStr1!!, SearchType.LYRIC, paramStr2?.toInt() ?: 0, paramStr3?.toInt() ?: 10, callback = commonCallback
            )
        }
        methodNameToBlock["searchSmart"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.searchSmart(paramStr1!!, commonCallback)
        }
        methodNameToBlock["createFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.createFolder(paramStr1!!, commonCallback)
        }
        methodNameToBlock["deleteFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.deleteFolder(paramStr1!!) { callback ->
                createdFolderDeleted = callback.data!!
                commonCallback.invoke(callback)
            }
        }
        methodNameToBlock["addSongToFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.addSongToFolder(
                paramStr1!!, paramStr2?.edtParamToList()?.map { v -> v.toLong() }, paramStr3?.edtParamToList(), paramStr4?.edtParamToList()
            ) {
                if (it.isSuccess()) {
                    folderSongAdded = it.data == true
                }
                commonCallback.invoke(it)
            }
        }
        methodNameToBlock["fetchSongOfFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 3805603854: 和周杰伦走过的21年，无与伦比
            openApi.fetchSongOfFolder(
                paramStr1!!, paramStr2?.toInt() ?: 0, paramStr3?.toInt() ?: 20, callback = commonCallback
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
            // 3805603854: 和周杰伦走过的21年，无与伦比
            openApi.collectFolder(paramStr1!!, commonCallback)
        }
        methodNameToBlock["unCollectFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 3805603854: 和周杰伦走过的21年，无与伦比
            openApi.unCollectFolder(paramStr1!!, commonCallback)
        }
        methodNameToBlock["deleteSongFromFolder"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.deleteSongFromFolder(
                paramStr1!!, paramStr2?.edtParamToList()?.map { v -> v.toLong() }, paramStr3?.edtParamToList(), paramStr4?.edtParamToList(), commonCallback
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
            // 3317: 官方歌单
            openApi.fetchFolderListByCategory(paramStr1!!.edtParamToList().map { v -> v.toInt() }, paramStr2?.toInt() ?: 0, paramStr3?.toInt() ?: 20, callback = {
                it.data?.forEach { folder ->
                    val creator = folder.creator
                }
                callback.invoke(it)
            })
        }
        methodNameToBlock["fetchFolderDetail"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 3805603854: 和周杰伦走过的21年，无与伦比
            openApi.fetchFolderDetail(paramStr1!!, commonCallback)
        }
        methodNameToBlock["fetchSongInfoBatch"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchSongInfoBatch(
                paramStr1?.edtParamToList()?.map { v -> v.toLong() }, paramStr2?.edtParamToList(), commonCallback
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
        methodNameToBlock["fetchAllLyric"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchAllLyric(paramStr1?.toLong(), paramStr2, commonCallback)
        }
        methodNameToBlock["fetchCategoryOfPublicRadio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchCategoryOfPublicRadio(commonCallback)
        }
        methodNameToBlock["fetchPublicRadioListByCategory"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 22: 乐器
            openApi.fetchPublicRadioListByCategory(
                paramStr1!!.edtParamToList().map { v -> v.toInt() }, commonCallback
            )
        }
        methodNameToBlock["fetchSongOfPublicRadio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 568: 综艺
            openApi.fetchSongOfPublicRadio(
                paramStr1!!.toInt(), paramStr2?.toInt() ?: 10, callback = commonCallback
            )
        }
        methodNameToBlock["fetchJustListenRadio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchJustListenRadio(commonCallback)
        }
        methodNameToBlock["fetchJustListenRank"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchJustListenRank(commonCallback)
        }
        methodNameToBlock["fetchSongOfJustListenRadio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 601: 随便听听-睡前
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
            // 巅峰榜->飙升榜
            openApi.fetchRankDetailByCategory(
                paramStr1!!.edtParamToList().map { v -> v.toInt() }, commonCallback
            )
        }
        methodNameToBlock["fetchSongOfRank"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 飙升榜
            openApi.fetchSongOfRank(
                paramStr1!!.toInt(), paramStr2?.toInt() ?: 0, paramStr2?.toInt() ?: 20, callback = commonCallback
            )
        }
        methodNameToBlock["fetchAlbumDetail"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 4805991: 追梦人
            openApi.fetchAlbumDetail(paramStr1, paramStr2, commonCallback)
        }
        methodNameToBlock["fetchSongOfAlbum"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 4805991: 追梦人
            openApi.fetchSongOfAlbum(
                paramStr1, paramStr2, paramStr3?.toInt() ?: 0, paramStr4?.toInt() ?: 20, callback = commonCallback
            )
        }
        methodNameToBlock["fetchSongOfSinger"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 19535: 大张伟
            openApi.fetchSongOfSinger(
                paramStr1!!.toInt(), paramStr2?.toInt() ?: 0, paramStr3?.toInt() ?: 20, paramStr4?.toInt() ?: 0, callback = commonCallback
            )
        }
        methodNameToBlock["fetchHotSingerList"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchHotSingerList(
                paramStr1?.toInt() ?: -100, paramStr2?.toInt() ?: -100, paramStr3?.toInt() ?: -100, callback = commonCallback
            )
        }
        methodNameToBlock["fetchAlbumOfSinger"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 19535: 大张伟
            openApi.fetchAlbumOfSinger(
                paramStr1!!.toInt(), paramStr2?.toInt() ?: 0, paramStr3?.toInt() ?: 20, paramStr4?.toInt() ?: 0, callback = commonCallback
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
                paramStr1?.toLong(), paramStr2, paramStr3?.edtParamToList()?.map { v -> v.toLong() }, callback = commonCallback
            )
        }
        methodNameToBlock["fetchHomepageRecommendation"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchHomepageRecommendation(
                paramStr1!!.edtParamToList().map { v -> v.toLong() }, callback = commonCallback
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
            // "猜你喜欢".hashCode = 898115530
            // "焦点图": 28753163
            openApi.fetchAlbumListOfRecommendLongAudioByCategory(
                paramStr1!!.edtParamToList().map { v -> v.toInt() }, commonCallback
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
            // 4013: 热播榜 937: 有声小说
            openApi.fetchAlbumListOfRankLongAudioByCategory(
                paramStr1!!.edtParamToList().map { v -> v.toInt() }, commonCallback
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
            // 1001: 有声书 1502: 悬疑
            openApi.fetchCategoryFilterOfLongAudio(
                paramStr1!!.edtParamToList().map { v -> v.toInt() }, commonCallback
            )
        }
        methodNameToBlock["fetchAlbumListOfLongAudioByCategory"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            // 1001: 有声书 1502: 悬疑
            // 2: 会员免费
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
                paramStr1!!, slotsMap, paramStr3 ?: "", paramStr4?.toLong(), paramStr5?.toInt() ?: 20, callback = commonCallback
            )
        }
        methodNameToBlock["reportRecentPlay"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.reportRecentPlay(paramStr1!!.toLong(), 2, callback = commonCallback)
        }

        methodNameToBlock["fetchHotKeyList"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchHotKeyList(type = paramStr1?.toIntOrNull() ?: 0, callback = commonCallback)
        }


//        methodNameToBlock["fetchBuyRecordb"] = {
//            val commonCallback = CallbackWithName(it)
//            fillDefaultParamIfNull(it)
////            openApi.fetchBuyRecord(
////                paramStr1?.toIntOrNull() ?: 0,
////                paramStr2.toString(),
////                callback =commonCallback )
//        }

        methodNameToBlock["fetchGetAiSongList"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchGetAiSongList(callback = commonCallback)
        }

        methodNameToBlock["fetchGetSongListSquare"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            val start: Int = paramStr1?.toIntOrNull() ?: 0
            val size: Int = paramStr2?.toIntOrNull() ?: 10
            val order = paramStr3 ?: "2"
            val categoryId: Int? = paramStr4?.toIntOrNull()
            openApi.fetchGetSongListSquare(start, size, order, categoryId, callback = commonCallback)
        }

        methodNameToBlock["fetchCollectedAlbum"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            val start: Int = paramStr1?.toIntOrNull() ?: 0
            val size: Int = paramStr2?.toIntOrNull() ?: 0
            openApi.fetchCollectedAlbum(start, size, commonCallback)
        }

        methodNameToBlock["collectAlbum"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            val isCollect = (paramStr1?.toIntOrNull() ?: 1) > 0
            openApi.collectAlbum(isCollect, paramStr2?.edtParamToList() ?: emptyList(), commonCallback)
        }

        methodNameToBlock["fetchCategoryPageOfLongAudio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            openApi.fetchCategoryPageOfLongAudio(commonCallback)
        }

        methodNameToBlock["fetchCategoryPageDetailOfLongAudio"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            val categoryId = paramStr1?.toIntOrNull() ?: 0
            val subCategoryId = paramStr2?.toIntOrNull() ?: 0
            val page = paramStr3?.toIntOrNull() ?: 0
            openApi.fetchCategoryPageDetailOfLongAudio(categoryId, subCategoryId, page, commonCallback)
        }

        methodNameToBlock["fetchAllTypeAiSongList"] = {
            val commonCallback = CallbackWithName(it)
            fillDefaultParamIfNull(it)
            val size = paramStr1?.toIntOrNull() ?: 0
            val page = paramStr2?.toIntOrNull() ?: 0
            openApi.fetchAllTypeAiSongList(size, page, commonCallback)
        }
    }

    private fun initMethodNameList() {
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryPageOfLongAudio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryPageDetailOfLongAudio", listOf("品类一级id", "品类二级id", "页码"), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchGreenMemberInformation", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchIotMemberInformation", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchUserInfo", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "createGreenOrder", listOf("matchId", "充值账号", "充值时长，单位月"), listOf(MustInitConfig.MATCH_ID, "17612107884285016458", "2")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "queryGreenOrder", listOf("matchId", "订单id"),
                //listOf("OApi_Baidu", "185412__20211115")
                listOf(MustInitConfig.MATCH_ID, "185412__20211115")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchAlbum", listOf("关键字", "页码（可不传）", "每页返回数量（可不传）"), listOf("周杰伦", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchSong", listOf("关键字", "页码（可不传）", "每页返回数量（可不传）"), listOf("周杰伦", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchFolder", listOf("关键字", "页码（可不传）", "每页返回数量（可不传）"), listOf("周杰伦", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchRadio", listOf("关键字", "页码（可不传）", "每页返回数量（可不传）"), listOf("周杰伦", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchLyric", listOf("关键字", "页码（可不传）", "每页返回数量（可不传）"), listOf("周杰伦", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "searchSmart", listOf("关键字"), listOf("周杰伦")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "createFolder", listOf("歌单名称"), listOf("OpenApi测试歌单")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "deleteFolder", listOf("歌单id"), listOf("8202113137")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfFolder", listOf("歌单id", "页码（可不传）", "每页返回数量（可不传）"), listOf("3805603854", null, null)
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
                "collectFolder", listOf("歌单id"), listOf("3805603854")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "unCollectFolder", listOf("歌单id"), listOf("3805603854")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "addSongToFolder", listOf("歌单id", "歌曲id列表", "歌曲mid列表", "歌曲类型"), listOf("8219435055", "314818717,317968884,316868744,291130348", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "deleteSongFromFolder", listOf("歌单id", "歌曲id列表", "歌曲mid列表", "歌曲类型"), listOf("8219435055", "314818717", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryOfFolder", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchFolderListByCategory", listOf("分类id列表", "页码（可不传）", "每页返回数量（可不传）"), listOf("0,3317", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchFolderDetail", listOf("歌单id"), listOf("3805603854")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongInfoBatch", listOf("歌曲id列表", "歌曲mid列表"), listOf("314818717,317968884,316868744,291130348", null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchNewSongRecommend", listOf("标签"), listOf("12")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchLyric", listOf("歌曲id", "歌曲mid"), listOf("314818717")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAllLyric", listOf("歌曲id", "歌曲mid"), listOf("335918510")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryOfPublicRadio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchPublicRadioListByCategory", listOf("分类id列表"), listOf("22")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfPublicRadio", listOf("电台id", "每页返回数量（可不传）"), listOf("568", null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchJustListenRadio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchJustListenRank", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfJustListenRadio", listOf("电台id"), listOf("601")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryOfRank", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAllRankGroup", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchRankDetailByCategory", listOf("分类id列表"), listOf("0,62")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfRank", listOf("榜单id", "页码（可不传）", "每页返回数量（可不传）"), listOf("62", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAlbumDetail", listOf("专辑id", "专辑mid"), listOf("4805991", null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfAlbum", listOf("专辑id", "专辑mid", "页码（可不传）", "每页返回数量（可不传）"), listOf("4805991", null, null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchSongOfSinger", listOf("歌手id", "页码（可不传）", "每页返回数量（可不传）", "排序类型（0按时间; 1按热度）"), listOf("19535", null, null, "0")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchHotSingerList", listOf("区域", "性别", "流派"), listOf("-100", "-100", "-100")
            )
        )

        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchNewAlbum", listOf("地区", "页码", "每页返回数量(可不传)", "专辑类型"), listOf("", null, null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "get111", listOf("地区"), listOf("")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "get222", listOf("地区"), listOf("")
            )
        )

        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAlbumOfSinger", listOf("歌手id", "页码（可不传）", "每页返回数量（可不传）", "排序类型（0按时间; 1按热度）"), listOf("19535", null, null, "0")
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
                "fetchSimilarSong", listOf("歌曲id", "歌曲mid", "已推荐歌曲id列表"), listOf("314818717", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchHomepageRecommendation", listOf("内容类型：200,500"), listOf("200,500")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryOfRecommendLongAudio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAlbumListOfRecommendLongAudioByCategory", listOf("分类id列表"), listOf(if (OpenApiSDK.getLoginApi().hasLogin()) "898115530" else "28753163")
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
                "fetchAlbumListOfRankLongAudioByCategory", listOf("分类id列表"), listOf("4013,937")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryOfLongAudio", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCategoryFilterOfLongAudio", listOf("分类id列表"), listOf("1001,1502")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAlbumListOfLongAudioByCategory",
                listOf("分类id列表", "过滤id列表", "页码（可不传）", "每页返回数量（可不传）", "排序类型（0按时间; 1按热度）"),
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
                "fetchRecentPlaySong", listOf("最后更新时间"), listOf("0")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchRecentPlayAlbum", listOf("最后更新时间"), listOf("0")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchRecentPlayFolder", listOf("最后更新时间"), listOf("0")
            )
        )
        //methodNameWithParamList.add(
        //    MethodNameWidthParam(
        //        "reportRecentPlay", listOf("资源id", "资源类型"), listOf("314818717", "2")
        //    )
        //)
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "musicSkill",
                listOf("意图", "槽位值(kv以冒号分隔，多个槽位值逗号分隔)", "原始语音", "当前在播歌曲id(可不传)", "返回数量（可不传）"),
                listOf("点歌播放", "歌手名:周杰伦,歌曲语言:中文", "播放感伤的歌曲", null, null)
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "reportRecentPlay", listOf("歌曲id"), listOf("316868744")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchHotKeyList", listOf("热词类型"), listOf("0")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchGetAiSongList", listOf(), listOf()
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchGetSongListSquare", listOf("起始位置", "歌单数量", "拉取方式，取值只能是2或者5，2：最新，5：最热", "分类id，可选参数"), listOf("0", "10", "2", "")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchCollectedAlbum", listOf("起始偏移量", "每页返回数量"), listOf("0", "20")
            )
        )
        methodNameWithParamList.add(
            MethodNameWidthParam(
                "collectAlbum", listOf("收藏(0：取消收藏，1：收藏)", "专辑id列表"), listOf("1", null)
            )
        )

        methodNameWithParamList.add(
            MethodNameWidthParam(
                "fetchAllTypeAiSongList", listOf("歌单分页数量", "取第几页"), listOf("12", "0")
            )
        )
        methodNameWithParamList.sortBy {
            it.name
        }
    }

}