package com.devidea.timeleft

import com.devidea.timeleft.EntityItemInfo
import com.devidea.timeleft.MainActivity
import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.appwidget.AppWidgetManager
import androidx.room.Room
import com.devidea.timeleft.AppDatabase
import com.devidea.timeleft.AppWidget
import com.devidea.timeleft.R
import com.devidea.timeleft.AdapterItem
import androidx.room.Database
import com.devidea.timeleft.EntityWidgetInfo
import androidx.room.RoomDatabase
import com.devidea.timeleft.DatabaseDao
import androidx.room.Dao
import com.devidea.timeleft.InterfaceItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.devidea.timeleft.TopRecyclerView
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import me.relex.circleindicator.CircleIndicator2
import android.content.DialogInterface
import com.devidea.timeleft.CreateTimeActivity
import com.devidea.timeleft.CreateMonthActivity
import android.os.Looper
import com.devidea.timeleft.BottomRecyclerView
import com.devidea.timeleft.ItemGenerate
import androidx.room.PrimaryKey
import android.view.ViewGroup
import android.view.LayoutInflater
import android.animation.ObjectAnimator
import android.util.SparseBooleanArray
import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.app.*
import com.devidea.timeleft.ItemSave
import android.app.TimePickerDialog.OnTimeSetListener
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Context
import android.view.View
import android.widget.*
import java.util.ArrayList

class BottomRecyclerView     //CustomAdapter 생성자
constructor(  //array list
    private val arrayList: ArrayList<AdapterItem?>
) : RecyclerView.Adapter<BottomRecyclerView.ViewHolder>() {
    // Item의 클릭 상태를 저장할 array 객체
    private var selectedItems: SparseBooleanArray? = null

    // Create new views (invoked by the layout manager)
    public override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        context = viewGroup.getContext()
        selectedItems = SparseBooleanArray(getItemCount())
        // Create a new view, which defines the UI of the list item
        val view: View = LayoutInflater.from(viewGroup.getContext())
            .inflate(R.layout.item_recyclerview_bottom, viewGroup, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    public override fun onBindViewHolder(viewHolder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {

        viewHolder.startValue.text = arrayList[position]!!.startDay
        viewHolder.endValue.text = arrayList[position]!!.endDay
        viewHolder.leftValue.text = arrayList[position]!!.leftDay
        if (arrayList[position]!!.isAutoUpdate) {
            viewHolder.autoUpdate.setText("이후 반복되는 일정이에요")
        } else {
            viewHolder.autoUpdate.setText("100% 달성후 끝나는 일정이에요")
        }
        viewHolder.startValue.setVisibility(View.GONE)
        viewHolder.endValue.setVisibility(View.GONE)
        viewHolder.leftValue.setVisibility(View.GONE)
        viewHolder.autoUpdate.setVisibility(View.GONE)
        viewHolder.deleteButton.setVisibility(View.GONE)
        viewHolder.deleteButton.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View) {
                val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                builder.setMessage("정말 삭제할까요?")
                builder.setPositiveButton("OK", object : DialogInterface.OnClickListener {
                    public override fun onClick(dialog: DialogInterface, id: Int) {
                        MainActivity.Companion.appDatabase!!.DatabaseDao()
                            .deleteItem(arrayList.get(position)!!.id)
                        MainActivity.Companion.appDatabase!!.DatabaseDao().deleteCustomWidget(
                            arrayList.get(position)!!.id
                        )
                        MainActivity.Companion.GetDBItem()
                        Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_LONG).show()
                    }
                })
                builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
                    public override fun onClick(dialog: DialogInterface, id: Int) {}
                })
                selectedItems!!.put(position, false)
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
            }
        })
        val summery_buf: String? = arrayList.get(position)!!.summery
        viewHolder.summery.setText(summery_buf)
        val percent_buf: String? = arrayList.get(position)!!.percentString
        viewHolder.percent.setText(percent_buf + "%")
        val percent: Int = percent_buf!!.toFloat().toInt()
        ObjectAnimator.ofInt(viewHolder.progressBar, "progress", percent)
            .setDuration(1500)
            .start()


    }

    public override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            for (payload: Any in payloads) {
                val itemID: Int = payload as Int
                val adapterItem: AdapterItem = MainActivity.Companion.ITEM_GENERATE.customTimeItem(
                    MainActivity.Companion.appDatabase!!.DatabaseDao().getSelectItem(itemID)
                )
                holder.leftValue.setText(adapterItem.leftDay)
                holder.percent.setText(adapterItem.percentString + "%")
                holder.progressBar.setProgress(adapterItem.percentString!!.toFloat() as Int)
            }
        }


    }

    public override fun getItemCount(): Int {
        return arrayList.size
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
        private val imageButton: Button
        private fun changeVisibility(isExpanded: Boolean) {
            // height 값을 dp로 지정
            val dpValue: Int = 200
            val d: Float = context!!.getResources().getDisplayMetrics().density
            val height: Int = (dpValue * d).toInt()

            // ValueAnimator.ofInt(int... values)는 View가 변할 값을 지정, 인자는 int 배열
            val va: ValueAnimator =
                if (isExpanded) ValueAnimator.ofInt(0, height) else ValueAnimator.ofInt(height, 0)
            // Animation이 실행되는 시간, n/1000초
            va.setDuration(600)
            va.addUpdateListener(object : AnimatorUpdateListener {
                public override fun onAnimationUpdate(animation: ValueAnimator) {
                    // value는 height 값
                    // imageView의 높이 변경
                    itemView.findViewById<View>(R.id.view).getLayoutParams().height =
                        animation.getAnimatedValue() as Int
                    itemView.findViewById<View>(R.id.view).requestLayout()
                    // imageView가 실제로 사라지게하는 부분
                    startValue.setVisibility(if (isExpanded) View.VISIBLE else View.GONE)
                    endValue.setVisibility(if (isExpanded) View.VISIBLE else View.GONE)
                    leftValue.setVisibility(if (isExpanded) View.VISIBLE else View.GONE)
                    autoUpdate.setVisibility(if (isExpanded) View.VISIBLE else View.GONE)
                    deleteButton.setVisibility(if (isExpanded) View.VISIBLE else View.GONE)
                    imageButton.setBackgroundResource(if (isExpanded) R.drawable.baseline_expand_less_black_36 else R.drawable.baseline_expand_more_black_36)
                }
            })
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
            itemView.setOnClickListener(object : View.OnClickListener {
                public override fun onClick(v: View) {
                    val pos: Int = getAdapterPosition()
                    if (selectedItems!!.get(getAdapterPosition())) {
                        // 펼쳐진 Item을 클릭 시
                        selectedItems!!.put(getAdapterPosition(), false)
                    } else {
                        // 직전의 클릭됐던 Item의 클릭상태를 지움
                        selectedItems!!.put(pos, false)
                        prePosition = pos
                        // 클릭한 Item의 position을 저장
                        selectedItems!!.put(getAdapterPosition(), true)
                    }
                    changeVisibility(selectedItems!!.get(getAdapterPosition()))
                }
            })
        }
    }

    companion object {
        // 직전에 클릭됐던 Item의 position
        private var prePosition: Int = -1

        //context
        private var context: Context? = null
    }
}