package com.devidea.timeleft

import com.devidea.timeleft.EntityItemInfo
import com.devidea.timeleft.MainActivity
import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.appwidget.AppWidgetManager
import androidx.room.Room
import com.devidea.timeleft.AppDatabase
import com.devidea.timeleft.AppWidget
import android.app.PendingIntent
import android.app.AlarmManager
import android.widget.RemoteViews
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
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import me.relex.circleindicator.CircleIndicator2
import android.content.DialogInterface
import com.devidea.timeleft.CreateTimeActivity
import com.devidea.timeleft.CreateMonthActivity
import android.widget.Toast
import android.os.Looper
import com.devidea.timeleft.BottomRecyclerView
import com.devidea.timeleft.ItemGenerate
import androidx.room.PrimaryKey
import android.view.ViewGroup
import android.view.LayoutInflater
import android.animation.ObjectAnimator
import android.widget.ProgressBar
import android.app.Activity
import android.widget.Spinner
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.util.SparseBooleanArray
import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.widget.EditText
import com.devidea.timeleft.ItemSave
import android.app.TimePickerDialog.OnTimeSetListener
import android.widget.TimePicker
import android.app.TimePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.widget.DatePicker
import android.app.DatePickerDialog
import android.view.View
import java.util.ArrayList

internal class TopRecyclerView     //CustomAdapter 생성자
constructor(  //array list
    private val arrayList: ArrayList<AdapterItem?>
) : RecyclerView.Adapter<TopRecyclerView.ViewHolder>() {
    // Create new views (invoked by the layout manager)
    public override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view: View = LayoutInflater.from(viewGroup.getContext())
            .inflate(R.layout.item_recyclerview_top, viewGroup, false)
        return ViewHolder(view)
    }

    public override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        /*
        viewHolder.summery.setText(arrayList.get(position).getSummery())
        val percent_buf: String? = arrayList.get(position).getPercentString()
        viewHolder.percent.setText(percent_buf + "%")
        viewHolder.leftValue.setText(arrayList.get(position).getLeftDay())
        val percent: Int = percent_buf!!.toFloat().toInt()
        ObjectAnimator.ofInt(viewHolder.progressBar, "progress", percent)
            .setDuration(1500)
            .start()

         */
    }

    public override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        /*
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val adapterItem: AdapterItem = MainActivity.Companion.ITEM_GENERATE.timeItem()
            holder.percent.setText(adapterItem.getPercentString() + "%")
            holder.progressBar.setProgress(adapterItem.getPercentString().toFloat() as Int)
            holder.leftValue.setText(adapterItem.getLeftDay())
        }

         */
    }

    public override fun getItemCount(): Int {
        return arrayList.size
    }

    class ViewHolder constructor(view: View) : RecyclerView.ViewHolder(view) {
        val summery: TextView
        val percent: TextView
        val progressBar: ProgressBar
        val leftValue: TextView

        //ViewHolder
        init {
            summery = view.findViewById<View>(R.id.summery) as TextView
            percent = view.findViewById<View>(R.id.percent_text) as TextView
            progressBar = view.findViewById<View>(R.id.progress) as ProgressBar
            leftValue = view.findViewById(R.id.left_day)
        }
    }
}