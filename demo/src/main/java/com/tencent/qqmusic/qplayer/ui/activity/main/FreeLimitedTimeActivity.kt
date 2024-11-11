package com.tencent.qqmusic.qplayer.ui.activity.main

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope

class FreeLimitedTimeActivity : AppCompatActivity() {

    private val tvResult: TextView by lazy { findViewById(R.id.tv_result) }
    private val btnGet: TextView by lazy { findViewById(R.id.btn_get) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_free_limited_time)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val spinner = findViewById<AppCompatSpinner>(R.id.spinner)
        val dataList = arrayListOf(Pair("AI歌词", 8), Pair("WANOS", 6), Pair("母带", 4))
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            android.R.id.text1,
            dataList
        )
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object :OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val select = spinner.selectedItem as Pair<*, *>
                getFreeLimitedTimeProfitInfo(type = select.second as Int)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        getFreeLimitedTimeProfitInfo(type = 8)

        btnGet.setOnClickListener {
            val select = spinner.selectedItem as Pair<*, *>
            val type = select.second as Int
            OpenApiSDK.getOpenApi().openFreeLimitedTimeAuth(type = type){
                if (it.isSuccess()){
                    getFreeLimitedTimeProfitInfo(type = type)
                }else{
                    tvResult.text = "get fail...(${it.errorMsg})"
                }
            }
        }
    }


    private fun getFreeLimitedTimeProfitInfo(type:Int){
        tvResult.text = "loading..."
        OpenApiSDK.getOpenApi().getFreeLimitedTimeProfitInfo(type) {
            AppScope.launchUI {
                tvResult.text = it.data?.toString()

                val canGet = it.data?.status == 0 && it.data?.used == 0
                btnGet.isClickable = canGet
                if (!canGet){
                    btnGet.text = "CANNOT GET"
                }else{
                    btnGet.text = "FREE GET"
                }
            }
        }
    }
}