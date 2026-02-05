package com.tencent.qqmusic.qplayer.ui.activity.player.lyric

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tencent.qqmusic.innovation.common.logging.MLog
import com.tencent.qqmusic.innovation.common.util.ToastUtils
import com.tencent.qqmusic.openapisdk.core.OpenApiSDK
import com.tencent.qqmusic.openapisdk.model.PlayerStyleData
import com.tencent.qqmusic.openapisdk.model.StyleData
import com.tencent.qqmusic.openapisdk.model.VipLevel
import com.tencent.qqmusic.openapisdk.playerui.LyricStyleManager
import com.tencent.qqmusic.openapisdk.playerui.PlayerStyleManager
import com.tencent.qqmusic.qplayer.R
import com.tencent.qqmusic.qplayer.baselib.util.AppScope
import com.tencent.qqmusic.qplayer.ui.activity.BaseActivity

/**
 * Created by silverfu on 2024/11/26.
 * 沉浸式歌词风格
 */
class PlayerImmersionLyricStyleActivity : BaseActivity() {


    private val openApiImpl = OpenApiSDK.getOpenApi()
    private val customAdapter = CustomAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_style)
        val recyclerView = findViewById<RecyclerView>(R.id.player_style_recycle_view)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = customAdapter
    }

    class CustomAdapter : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

        private val list: MutableList<PlayerStyleData> = arrayListOf()

        init {
            initData()
        }

        fun initData() {
            OpenApiSDK.getOpenApi().fetchPlayerImmersiveLyricList {
                MLog.i("PlayerStyleActivity", "styles $it")
                it.data?.let { it1 -> setData(it1) } ?: run {
                }
            }
        }

        fun setData(data: List<PlayerStyleData>) {
            list.clear()
            list.addAll(data)
            notifyDataSetChanged()
        }

        /**
         * Provide a reference to the type of views that you are using
         * (custom ViewHolder)
         */
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(R.id.textView)
            val btnUse: Button = view.findViewById(R.id.btnUse)
            val identity: TextView = view.findViewById(R.id.identity)
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            // Create a new view, which defines the UI of the list item
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.style_item, viewGroup, false)

            return ViewHolder(view)
        }

        // Replace the contents of a view (invoked by the layout manager)
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

            // Get element from your dataset at this position and replace the
            // contents of the view with that element
            val playerStyleData = list[position]

            viewHolder.textView.text = "${playerStyleData.name}-${playerStyleData.id}"
            if (LyricStyleManager.getLyricStyle().id == playerStyleData.id) {
                viewHolder.btnUse.text = "使用中"
                viewHolder.btnUse.setTextColor(Color.GREEN)
                if (playerStyleData.userProfit?.status == 2) {
                    viewHolder.btnUse.text = "试用中"
                }
            } else if (playerStyleData.canUse == true) {
                viewHolder.btnUse.text = "使用"
                viewHolder.btnUse.setTextColor(Color.BLACK)
            } else if (playerStyleData.userProfit?.status == 1) {
                viewHolder.btnUse.setTextColor(Color.BLACK)
                viewHolder.btnUse.text = "领取试用"
            } else {
                viewHolder.btnUse.setTextColor(Color.BLACK)
                viewHolder.btnUse.text = "不能使用(${playerStyleData.userProfit?.status})"
            }

            viewHolder.identity.text = when (playerStyleData.vipLevel) {
                VipLevel.GreenVip -> {
                    "GreenVip"
                }
                VipLevel.SuperVip -> {
                    "SuperVip"
                }
                else -> {
                    ""
                }
            }



            viewHolder.btnUse.setOnClickListener {
                if (LyricStyleManager.getLyricStyle().id == playerStyleData.id) {
                    viewHolder.itemView.context?.apply {
                        startActivity(Intent(this, PlayerImmersionLyricActivity::class.java))
                    }
                    return@setOnClickListener
                }

                val block = {
                    LyricStyleManager.setLyricStyle(playerStyleData,
                        object : PlayerStyleManager.PlayerStyleLoaderListener {
                            override fun onStart(playerStyleData: PlayerStyleData) {
                                MLog.i("PlayerStyleActivity", "onStart")
                            }

                            override fun onDownloading(
                                playerStyleData: StyleData,
                                progress: Float
                            ) {
                                AppScope.launchUI {
                                    val progress = String.format("%.2f", progress)
                                    viewHolder.btnUse.text = "下载中:$progress"
                                }
                            }

                            override fun onSuccess(styleData: StyleData) {
                                MLog.i("PlayerStyleActivity", "setStyle success $playerStyleData")
                                AppScope.launchUI {
                                    ToastUtils.showShort("设置成功")
                                    notifyDataSetChanged()
                                    viewHolder.itemView.context?.apply {
                                        startActivity(Intent(this, PlayerImmersionLyricActivity::class.java))
                                    }
                                }
                            }

                            override fun onFailed(
                                code: Int,
                                playerStyleData: PlayerStyleData,
                                errMsg: String?
                            ) {
                                ToastUtils.showShort("失败($errMsg)")
                                MLog.i("PlayerStyleActivity", "setStyle fail $errMsg")
                            }
                        })
                }

                //根据试用状态，领取试用逻辑
                if (playerStyleData.canUse == true) {
                    block.invoke()
                } else {
                    OpenApiSDK.getOpenApi().openFreeLimitedTimeByPlayStyle(playerStyleData.id.toString()) {
                        if (it.data == true) {
                            (viewHolder.bindingAdapter as CustomAdapter).initData()
                            ToastUtils.showShort("领取成功")
                        } else {
                            ToastUtils.showShort("领取失败(${it.errorMsg})")
                        }
                    }
                }
            }

        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = list.size

    }
}