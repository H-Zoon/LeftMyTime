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
import android.app.Activity
import android.util.SparseBooleanArray
import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import com.devidea.timeleft.ItemSave
import android.app.TimePickerDialog.OnTimeSetListener
import android.app.TimePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.DatePickerDialog
import android.view.View
import android.widget.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class CreateMonthActivity constructor() : AppCompatActivity() {
    /*
    var inputSummery: EditText? = null
    var inputDay: EditText? = null
    var calender: Button? = null
    var save: Button? = null
    var AutoUpdateCheck: CheckBox? = null
    var itemSave: ItemSave = ItemSave()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_month)
        val now: LocalDate = LocalDate.now()
        inputSummery = findViewById(R.id.input_summery)
        inputDay = findViewById(R.id.input_day)
        calender = findViewById(R.id.calender)
        save = findViewById(R.id.summit)
        AutoUpdateCheck = findViewById(R.id.auto_update_check)
        val dateSetListener: OnDateSetListener = object : OnDateSetListener {
            public override fun onDateSet(
                view: DatePicker,
                year: Int,
                month: Int,
                dayOfMonth: Int
            ) {
                val startDate: LocalDate = LocalDate.now()
                val endDate: LocalDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (endDate.isAfter(startDate)) {
                    inputDay.setText("")
                    inputDay.setText(ChronoUnit.DAYS.between(startDate, endDate).toString())
                    inputDay.setSelection(inputDay.length())
                } else {
                    Toast.makeText(this@CreateMonthActivity, "오늘보다 먼 날을 선택해주세요", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
        calender.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View) {
                val datePicker: DatePickerDialog = DatePickerDialog(
                    this@CreateMonthActivity,
                    dateSetListener,
                    now.getYear(),
                    now.getMonthValue() - 1,
                    now.getDayOfMonth()
                )
                datePicker.show()
            }
        })
        save.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View) {
                if (!((inputSummery.getText().toString() == "")) && !((inputDay.getText()
                        .toString() == ""))
                ) {
                    if (inputDay.getText().toString().toInt() > 1826) {
                        Toast.makeText(
                            this@CreateMonthActivity,
                            "흠..감당하기엔 너무 멀지 않나요..?",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        itemSave.saveMonthItem(
                            inputSummery.getText().toString(),
                            inputDay.getText().toString().toInt(),
                            AutoUpdateCheck.isChecked()
                        )
                        MainActivity.Companion.GetDBItem()
                        finish()
                    }
                } else {
                    Toast.makeText(this@CreateMonthActivity, "제목과 날짜를 확인해주세요", Toast.LENGTH_LONG)
                        .show()
                }
            }
        })
    }

     */
}