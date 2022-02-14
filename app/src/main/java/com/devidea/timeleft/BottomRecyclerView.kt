package com.devidea.timeleft

import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.animation.ObjectAnimator
import android.util.SparseBooleanArray
import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import com.devidea.timeleft.alarm.AlarmReceiver.Companion.TAG
import com.devidea.timeleft.alarm.ItemAlarmManager
import com.devidea.timeleft.databinding.ItemRecyclerviewBottomBinding
import com.devidea.timeleft.datadase.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
이 BottomRecyclerView 는 ViewBinding 을 통해 작성되었습니다.
 */

class BottomRecyclerView     //CustomAdapter 생성자
constructor(  //array list
    private val items: ArrayList<AdapterItem>
) : RecyclerView.Adapter<BottomRecyclerView.ViewHolder>() {

    // Item의 클릭 상태를 저장할 array 객체
    private var selectedItems: SparseBooleanArray? = null
    private val appDatabase = AppDatabase.getDatabase(App.context())
    var activityContext: Context? = null

    fun updateList(items: ArrayList<AdapterItem>) {
        val subjectDiffUtilCallback = DiffutilClass(this.items, items, this)
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(subjectDiffUtilCallback)

        this.items.clear()
        this.items.addAll(items)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        activityContext = viewGroup.context
        selectedItems = SparseBooleanArray(itemCount)

        val binding = ItemRecyclerviewBottomBinding.inflate(
            LayoutInflater.from(viewGroup.context),
            viewGroup,
            false
        )
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {

        viewHolder.binding.startDay.text = items[position].startDay
        viewHolder.binding.endDay.text = items[position].endDay
        viewHolder.binding.leftDay.text = items[position].leftDay


        when (items[position].autoUpdateFlag) {
            0 -> viewHolder.binding.updateIs.text = "100% 달성후 끝나는 일정."
            1 -> viewHolder.binding.updateIs.text = "종료 후 " +
                    ((items[position].updateRate).toString() + "일 뒤 반복되는 일정.")
            2 -> viewHolder.binding.updateIs.text =
                ("매 달" + (items[position].updateRate).toString() + "일에 반복되는 일정.")
            3 -> viewHolder.binding.updateIs.text = "매일 시작시간이 되면 반복."
        }

        Log.d("Viewflag", items[position].alarmFlag.toString())

        when (items[position].alarmFlag) {
            0 -> viewHolder.binding.alarmIs.text = "알림 설정되지않음."

            1 -> viewHolder.binding.alarmIs.text = items[position].alarmRate + "전 알림 설정됨."

            2 -> viewHolder.binding.alarmIs.text =
                items[position].alarmRate + "전 주말에도 알림."
        }

        with(viewHolder) {
            viewHolder.binding.deleteButton.setOnClickListener {
                val builder: AlertDialog.Builder = AlertDialog.Builder(activityContext)
                builder.setMessage("정말 삭제할까요?")
                builder.setPositiveButton("OK") { _, _ ->
                    delete(items[viewHolder.adapterPosition].id)
                    Toast.makeText(App.context(), "삭제되었습니다.", Toast.LENGTH_LONG).show()
                }
                builder.setNegativeButton("Cancel") { dialog, id -> }
                selectedItems!!.put(position, false)
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
            }
        }
        val summery_buf: String? = items[position].summery
        viewHolder.binding.summery.text = summery_buf
        val percent_buf: String? = items[position].percentString
        viewHolder.binding.percentText.text = percent_buf + "%"
        val percent: Int = percent_buf!!.toFloat().toInt()
        ObjectAnimator.ofInt(viewHolder.binding.progress, "progress", percent)
            .setDuration(1500)
            .start()


    }


    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int, payloads: List<Any>) {

        if (payloads.isEmpty()) {
            super.onBindViewHolder(viewHolder, position, payloads)
        } else {
            val adapterItem: AdapterItem = payloads[0] as AdapterItem

            viewHolder.binding.leftDay.text = adapterItem.leftDay
            viewHolder.binding.percentText.text = adapterItem.percentString + "%"
            viewHolder.binding.progress.progress = adapterItem.percentString!!.toFloat().toInt()

        }

    }


    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(val binding: ItemRecyclerviewBottomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private fun changeVisibility(isExpanded: Boolean) {
            // height 값을 dp로 지정
            val dpValue = 150
            val d: Float = App.context().resources.displayMetrics.density
            val height: Int = (dpValue * d).toInt()

            // ValueAnimator.ofInt(int... values)는 View가 변할 값을 지정, 인자는 int 배열
            val va: ValueAnimator =
                if (isExpanded) ValueAnimator.ofInt(0, height) else ValueAnimator.ofInt(height, 0)
            // Animation이 실행되는 시간, n/1000초
            va.duration = 600
            va.addUpdateListener { animation -> // value는 height 값
                // imageView의 높이 변경
                itemView.findViewById<View>(R.id.view).layoutParams.height =
                    animation.animatedValue as Int
                itemView.findViewById<View>(R.id.view).requestLayout()
                // imageView가 실제로 사라지게하는 부분
                binding.startDay.visibility = if (isExpanded) View.VISIBLE else View.GONE
                binding.endDay.visibility = if (isExpanded) View.VISIBLE else View.GONE
                binding.leftDay.visibility = if (isExpanded) View.VISIBLE else View.GONE
                //autoUpdate.visibility = if (isExpanded) View.VISIBLE else View.GONE
                binding.deleteButton.visibility = if (isExpanded) View.VISIBLE else View.GONE
                binding.imageView.setBackgroundResource(if (isExpanded) R.drawable.baseline_expand_less_black_36 else R.drawable.baseline_expand_more_black_36)
            }
            // Animation start
            va.start()
        }


        //ViewHolder
        init {

            itemView.setOnClickListener {
                val pos: Int = adapterPosition
                if (selectedItems!!.get(adapterPosition)) {
                    // 펼쳐진 Item을 클릭 시
                    selectedItems!!.put(adapterPosition, false)
                } else {
                    // 직전의 클릭됐던 Item의 클릭상태를 지움
                    selectedItems!!.put(pos, false)
                    prePosition = pos
                    // 클릭한 Item의 position을 저장
                    selectedItems!!.put(adapterPosition, true)
                }
                changeVisibility(selectedItems!!.get(adapterPosition))
            }
        }

    }

    companion object {
        // 직전에 클릭됐던 Item의 position
        private var prePosition: Int = -1

    }

    private fun delete(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            ItemAlarmManager().alarmDelete(id)
            appDatabase.itemDao()
                .deleteItem(id)
            //appDatabase.itemDao().deleteCustomWidget(id)
        }
    }
}