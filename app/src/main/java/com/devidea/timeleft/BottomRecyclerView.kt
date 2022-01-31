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
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import com.devidea.timeleft.datadase.AppDatabase
import java.util.*

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

        val view: View = LayoutInflater.from(activityContext)
            .inflate(R.layout.item_recyclerview_bottom, viewGroup, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {

        viewHolder.startValue.text = items[position].startDay
        viewHolder.endValue.text = items[position].endDay
        viewHolder.leftValue.text = items[position].leftDay

        when (items[position].autoUpdateFlag) {
            1 -> viewHolder.autoUpdate.text =
                ((items[position].updateRate).toString() + "일후 반복되는 일정이에요")
            2 -> viewHolder.autoUpdate.text =
                ("매 달" + (items[position].updateRate).toString() + "일에 반복되는 일정이에요")
            else -> viewHolder.autoUpdate.text = "100% 달성후 끝나는 일정이에요"
        }

        viewHolder.startValue.visibility = View.GONE
        viewHolder.endValue.visibility = View.GONE
        viewHolder.leftValue.visibility = View.GONE
        viewHolder.autoUpdate.visibility = View.GONE
        viewHolder.deleteButton.visibility = View.GONE
        with(viewHolder) {
            deleteButton.setOnClickListener {
                val builder: AlertDialog.Builder = AlertDialog.Builder(activityContext)
                builder.setMessage("정말 삭제할까요?")
                builder.setPositiveButton("OK") { _, _ ->
                    appDatabase.itemDao()
                        .deleteItem(items[position].id)
                    appDatabase.itemDao().deleteCustomWidget(
                        items[position].id
                    )
                    Toast.makeText(App.context(), "삭제되었습니다.", Toast.LENGTH_LONG).show()
                }
                builder.setNegativeButton("Cancel") { dialog, id -> }
                selectedItems!!.put(position, false)
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
            }
        }
        val summery_buf: String? = items[position].summery
        viewHolder.summery.text = summery_buf
        val percent_buf: String? = items[position].percentString
        viewHolder.percent.text = percent_buf + "%"
        val percent: Int = percent_buf!!.toFloat().toInt()
        ObjectAnimator.ofInt(viewHolder.progressBar, "progress", percent)
            .setDuration(1500)
            .start()


    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val adapterItem: AdapterItem = payloads[0] as AdapterItem

            holder.leftValue.text = adapterItem.leftDay
            holder.percent.text = adapterItem.percentString + "%"
            holder.progressBar.progress = adapterItem.percentString!!.toFloat().toInt()

        }

    }



    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        val summery: TextView
        val percent: TextView
        val progressBar: ProgressBar
        val startValue: TextView
        val endValue: TextView
        val leftValue: TextView
        val autoUpdate: TextView
        val deleteButton: Button
        private val imageButton: ImageView

        private fun changeVisibility(isExpanded: Boolean) {
            // height 값을 dp로 지정
            val dpValue: Int = 200
            val d: Float = App.context().getResources().getDisplayMetrics().density
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
                startValue.visibility = if (isExpanded) View.VISIBLE else View.GONE
                endValue.visibility = if (isExpanded) View.VISIBLE else View.GONE
                leftValue.visibility = if (isExpanded) View.VISIBLE else View.GONE
                autoUpdate.visibility = if (isExpanded) View.VISIBLE else View.GONE
                deleteButton.visibility = if (isExpanded) View.VISIBLE else View.GONE
                imageButton.setBackgroundResource(if (isExpanded) R.drawable.baseline_expand_less_black_36 else R.drawable.baseline_expand_more_black_36)
            }
            // Animation start
            va.start()
        }


        //ViewHolder
        init {
            summery = view.findViewById(R.id.summery)
            percent = view.findViewById(R.id.percent_text)
            progressBar = view.findViewById(R.id.progress)
            startValue = view.findViewById(R.id.start_day)
            endValue = view.findViewById(R.id.end_day)
            autoUpdate = view.findViewById(R.id.update_is)
            leftValue = view.findViewById(R.id.left_day)
            imageButton = view.findViewById(R.id.imageButton)
            deleteButton = view.findViewById(R.id.delete_button)

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
}