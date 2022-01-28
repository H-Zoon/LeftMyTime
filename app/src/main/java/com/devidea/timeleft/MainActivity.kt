package com.devidea.timeleft

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import android.app.*
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.devidea.timeleft.databinding.ActivityMainBinding
import com.devidea.timeleft.datadase.AppDatabase
import com.devidea.timeleft.datadase.itemdata.ItemEntity
import com.devidea.timeleft.viewmodels.TimeLeftViewModel
import kotlinx.coroutines.*
import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    private val topItemListArray = ArrayList<AdapterItem?>()
    private var backPressedTime: Long = 0

    private lateinit var binding: ActivityMainBinding //activity_main.xml
    private lateinit var viewModel : TimeLeftViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(TimeLeftViewModel::class.java)
        val view = binding.root
        setContentView(view)

        initTopRecyclerView()
        initBottomRecyclerView()


        viewModel.currentValue.observe(this, Observer {
            binding.day.text = it.toString()
        })

        viewModel.currentValue2.observe(this, Observer {
            binding.time.text = it.toString()
        })



        binding.timeAdd.setOnClickListener {
            val itemName = arrayOfNulls<String>(2)
            itemName[0] = "시간범위 지정하기"
            itemName[1] = "날짜 지정하기"
            AlertDialog.Builder(this)
                .setTitle("원하시는 종류를 선택해주세요")
                .setItems(itemName) { _, which ->
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
                }
                .create().show()
        }

        /* 이 함수 ViewModel 로 옮기면 성공.
        topItemAdapter.notifyItemChanged(0, ITEM_GENERATE.timeItem())

                for (i in position.indices) {
                    bottomItemAdapter.notifyItemChanged(
                        position[i],
                        ITEM_GENERATE.customTimeItem(
                            itemList[position[i]]
                        )
                    )

         */

    }

    private fun initTopRecyclerView() {
        binding.recyclerview.layoutManager = LinearLayoutManager(
            this, RecyclerView.HORIZONTAL,
            false
        )

        topItemListArray.add(ITEM_GENERATE.timeItem())
        topItemListArray.add(ITEM_GENERATE.monthItem())
        topItemListArray.add(ITEM_GENERATE.yearItem())

        val topItemAdapter = TopRecyclerView(topItemListArray)
        binding.recyclerview.adapter = topItemAdapter

        val pagerSnapHelper = PagerSnapHelper()
        pagerSnapHelper.attachToRecyclerView(binding.recyclerview)

        binding.indicator.attachToRecyclerView(binding.recyclerview, pagerSnapHelper)
        topItemAdapter.registerAdapterDataObserver(binding.indicator.adapterDataObserver)
    }

    private fun initBottomRecyclerView() {
        binding.recyclerview2.layoutManager = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        )
        val bottomItemAdapter = BottomRecyclerView(refreshItem())
        binding.recyclerview2.adapter = bottomItemAdapter

    }

    override fun onBackPressed() {
        val tempTime = System.currentTimeMillis()
        val intervalTime = tempTime - backPressedTime

        //뒤로가기 버튼 리스너에 쓰이는 변수
        val FINISH_INTERVAL_TIME: Long = 2000
        if (intervalTime in 0..FINISH_INTERVAL_TIME) {
            finish()
        } else {
            backPressedTime = tempTime
            Toast.makeText(applicationContext, "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private val bottomItemListArray = ArrayList<AdapterItem?>()
        private val appDatabase = AppDatabase.getDatabase(App.context())
        lateinit var itemList: List<ItemEntity>
        // 기존 database instance의 싱글톤 디자인패턴 사용

        val ITEM_GENERATE: InterfaceItem = ItemGenerate()
        private val position = ArrayList<Int>()

// 이 함수 ViewModel 로 옮기면 성공.
        fun refreshItem(): ArrayList<AdapterItem?> {
            position.clear()
            itemList = appDatabase!!.itemDao().item

            for (i in itemList.indices) {
                if ((itemList[i].type == "Time")) {
                    bottomItemListArray.add(
                        ITEM_GENERATE.customTimeItem(
                            itemList[i]
                        )
                    )
                    position.add(i)
                } else {
                    bottomItemListArray.add(
                        ITEM_GENERATE.customMonthItem(
                            itemList[i]
                        )
                    )
                }
            }
            return bottomItemListArray
        }
    }


}
