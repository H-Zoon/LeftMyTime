package com.devidea.timeleft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import me.relex.circleindicator.CircleIndicator2
import android.content.DialogInterface
import android.app.*
import android.widget.*
import com.devidea.timeleft.datadase.AppDatabase
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList

class MainActivity() : AppCompatActivity() {
    private val topItemListArray = ArrayList<AdapterItem?>()
    private var backPressedTime: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        explanation = findViewById(R.id.explanation)

        timeText = findViewById(R.id.time)
        dayText = findViewById(R.id.day)

        bottomItemAdapter = BottomRecyclerView(bottomItemListArray)
        bottomRecyclerView = findViewById(R.id.recyclerview2)
        bottomRecyclerView!!.adapter = bottomItemAdapter
        bottomRecyclerView!!.isNestedScrollingEnabled = false

        val topRecyclerView: RecyclerView = findViewById(R.id.recyclerview)

        topRecyclerView.layoutManager = LinearLayoutManager(
            this,
            RecyclerView.HORIZONTAL,
            false
        ) // 좌우 스크롤 //


        topItemListArray.add(ITEM_GENERATE.timeItem())
        topItemListArray.add(ITEM_GENERATE.monthItem())
        topItemListArray.add(ITEM_GENERATE.yearItem())

        //recyclerView 관련 객체
        val topItemAdapter = TopRecyclerView(topItemListArray)
        topRecyclerView.adapter = topItemAdapter

        // PagerSnapHelper 추가 꼭 공부하기 !!
        val pagerSnapHelper = PagerSnapHelper()
        pagerSnapHelper.attachToRecyclerView(topRecyclerView)

        //인디케이터 활성화 꼭 공부하기!!
        val indicator = findViewById<CircleIndicator2>(R.id.indicator)
        //indicator.attachToRecyclerView(recyclerView, pagerSnapHelper)

        //인디케이터 활성화 꼭 공부하기!!
        topItemAdapter.registerAdapterDataObserver(indicator.adapterDataObserver)

        //커스텀 항목에 대한 추가
        refreshItem()

        bottomRecyclerView!!.layoutManager = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        ) // 상하 스크롤 //

        val button = findViewById<Button>(R.id.time_add)
        button.setOnClickListener {
            val itemName = arrayOfNulls<String>(2)
            itemName[0] = "시간범위 지정하기"
            itemName[1] = "날짜 지정하기"
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("원하시는 종류를 선택해주세요")
            builder.setItems(itemName, DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    0 -> startActivity(
                        Intent(
                            applicationContext,
                            CreateTimeActivity::class.java
                        )
                    )
                    1 -> startActivity(
                        Intent(
                            applicationContext,
                            CreateMonthActivity::class.java
                        )
                    )
                }
            })
            builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    Toast.makeText(applicationContext, "취소", Toast.LENGTH_SHORT).show()
                }
            })
            val alertDialog = builder.create()
            alertDialog.show()
        }

        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                //상단 시계
                dayText!!.text =
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
                timeText!!.text =
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("a h:m:ss"))

                topItemAdapter.notifyItemChanged(0, ITEM_GENERATE.timeItem())

                for (i in position.indices) {
                    bottomItemAdapter!!.notifyItemChanged(
                        position[i],
                        ITEM_GENERATE.customTimeItem(
                            appDatabase!!.itemDao().getSelectItem(itemID[i])
                        )
                    )
                }
                delay(1000)
            }
        }

    }

    override fun onBackPressed() {
        val tempTime = System.currentTimeMillis()
        val intervalTime = tempTime - backPressedTime

        //뒤로가기 버튼 리스너에 쓰이는 변수
        val FINISH_INTERVAL_TIME: Long = 2000
        if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
            finish()
        } else {
            backPressedTime = tempTime
            Toast.makeText(applicationContext, "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private val bottomItemListArray = ArrayList<AdapterItem?>()
        private val appDatabase = AppDatabase.getDatabase(App.context())
        // 기존 database instance의 싱글톤 디자인패턴 사용

        val ITEM_GENERATE: InterfaceItem = ItemGenerate()
        private val position = ArrayList<Int>()
        private val itemID = ArrayList<Int>()

        private var timeText: TextView? = null
        private var dayText: TextView? = null
        private var explanation: TextView? = null
        private var bottomRecyclerView: RecyclerView? = null
        private var bottomItemAdapter: BottomRecyclerView? = null

        fun refreshItem() {

            bottomItemListArray.clear()
            position.clear()
            itemID.clear()

            if (appDatabase!!.itemDao().item.isNotEmpty()) {
                //explanation!!.visibility = View.INVISIBLE
                for (i in appDatabase.itemDao().item.indices) {
                    if ((appDatabase.itemDao().item[i]?.type == "Time")) {
                        bottomItemListArray.add(
                            ITEM_GENERATE.customTimeItem(
                                appDatabase.itemDao().item[i]
                            )
                        )
                        position.add(i)
                        bottomItemListArray[i]?.let { itemID.add(it.id) }
                    } else {
                        bottomItemListArray.add(
                            ITEM_GENERATE.customMonthItem(
                                appDatabase.itemDao().item[i]
                            )
                        )
                    }
                }
            }
            bottomItemAdapter = BottomRecyclerView(bottomItemListArray)
            bottomRecyclerView?.adapter = bottomItemAdapter
        }
    }
}