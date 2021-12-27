package com.devidea.timeleft

import android.content.Intent
import androidx.room.Room
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import me.relex.circleindicator.CircleIndicator2
import android.content.DialogInterface
import android.os.Looper
import android.app.*
import android.os.Handler
import android.view.View
import android.widget.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList

class MainActivity() : AppCompatActivity() {
    private val topItemListArray = ArrayList<AdapterItem?>()

    private var backPressedTime: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {

        //database 객체 초기화
        appDatabase = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "ItemData")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        explanation = findViewById(R.id.explanation)
        timeText = findViewById(R.id.time)
        dayText = findViewById(R.id.day)

        var recyclerView: RecyclerView = findViewById(R.id.recyclerview)

        recyclerView.layoutManager = LinearLayoutManager(
            this,
            RecyclerView.HORIZONTAL,
            false
        ) // 좌우 스크롤 //


        topItemListArray.add(ITEM_GENERATE.timeItem())
        topItemListArray.add(ITEM_GENERATE.monthItem())
        topItemListArray.add(ITEM_GENERATE.yearItem())

        //recyclerView 관련 객체
        var topItemAdapter = TopRecyclerView(topItemListArray)
        recyclerView.setAdapter(topItemAdapter)

        // PagerSnapHelper 추가 꼭 공부하기 !!
        val pagerSnapHelper = PagerSnapHelper()
        pagerSnapHelper.attachToRecyclerView(recyclerView)

        //인디케이터 활성화 꼭 공부하기!!
        val indicator = findViewById<CircleIndicator2>(R.id.indicator)
       //indicator.attachToRecyclerView(recyclerView, pagerSnapHelper)

        //인디케이터 활성화 꼭 공부하기!!
        topItemAdapter.registerAdapterDataObserver(indicator.adapterDataObserver)

        //커스텀 항목에 대한 추가

        var customItemRecyclerView: RecyclerView = findViewById(R.id.recyclerview2)
        customItemRecyclerView.layoutManager = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        ) // 상하 스크롤 //


        //GetDBItem()
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
        val handler = Handler(Looper.getMainLooper())
        //초단위, 현재시간 update Thread
        Thread {
            while (true) {
                //핸들러
                handler.post {
                    clock()
                    topItemAdapter!!.notifyItemChanged(0, "update")
                    for (i in position.indices) {
                        bottomItemAdapter!!.notifyItemChanged(
                            position[i], itemID[i]
                        )
                    }
                }
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    fun clock() {
        val currentDateTime = LocalDateTime.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
        val TimeFormatter = DateTimeFormatter.ofPattern("a h:m:ss")
        dayText!!.text = currentDateTime.format(dateFormatter)
        timeText!!.text = currentDateTime.format(TimeFormatter)
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

        //하단 recyclerView 객체

        private var customItemRecyclerView: RecyclerView? = null
        private var bottomItemAdapter: BottomRecyclerView? = null


        val ITEM_GENERATE: InterfaceItem = ItemGenerate()
        var appDatabase: AppDatabase? = null
        private val position = ArrayList<Int>()
        private val itemID = ArrayList<Int>()
        private var explanation: TextView? = null
        private var dayText: TextView? = null
        private var timeText: TextView? = null
        fun GetDBItem() {
            bottomItemListArray.clear()
            position.clear()
            itemID.clear()

            if (appDatabase!!.DatabaseDao().item.size != 0) {
                explanation!!.visibility = View.INVISIBLE
                for (i in appDatabase!!.DatabaseDao().item.indices) {
                    if ((appDatabase!!.DatabaseDao().item[i]?.type == "Time")) {
                        bottomItemListArray.add(
                            ITEM_GENERATE.customTimeItem(
                                appDatabase!!.DatabaseDao().item[i]
                            )
                        )
                        position.add(i)
                        bottomItemListArray[i]?.let { itemID.add(it.id) }
                    } else {
                        bottomItemListArray.add(
                            ITEM_GENERATE.customMonthItem(
                                appDatabase!!.DatabaseDao().item[i]
                            )
                        )
                    }
                }
            } else {
                explanation!!.visibility = View.VISIBLE
            }


            //사용자가 추가한 부분의 아이템
            bottomItemAdapter = BottomRecyclerView(bottomItemListArray)
            customItemRecyclerView!!.adapter = bottomItemAdapter
        }
    }
}