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
import android.graphics.Color
import android.view.View
import android.widget.*
import java.time.LocalTime

class CreateTimeActivity constructor() : AppCompatActivity() {
    /*
    var timeRange: TextView? = null
    var inputSummery: EditText? = null
    var clock: Button? = null
    var save: Button? = null
    var itemSave: ItemSave = ItemSave()
    var startTime: LocalTime? = null
    var endTime: LocalTime? = null
    var isValid: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_time)
        inputSummery = findViewById(R.id.input_summery)
        timeRange = findViewById(R.id.time_range)
        clock = findViewById(R.id.clock)
        save = findViewById(R.id.summit_time)
        val callbackMethodEnd: OnTimeSetListener = object : OnTimeSetListener {
            @SuppressLint("SetTextI18n")
            public override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
                endTime = LocalTime.of(hourOfDay, minute)
                if (endTime.isBefore(startTime)) {
                    Toast.makeText(this@CreateTimeActivity, "시작시간 이후로 설정해주세요", Toast.LENGTH_LONG)
                        .show()
                    isValid = false
                } else {
                    timeRange.setText(startTime.toString() + " 부터 " + endTime + " 까지 계산할께요.")
                    isValid = true
                }
            }
        }
        val datePickerEnd: TimePickerDialog =
            TimePickerDialog(this@CreateTimeActivity, callbackMethodEnd, 12, 0, false)
        datePickerEnd.setTitle("종료시간")

        // TODO: 수정합시다
        val callbackMethodStart: OnTimeSetListener = object : OnTimeSetListener {
            public override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
                startTime = LocalTime.of(hourOfDay, minute)
                datePickerEnd.show()
                //버튼 색상변경. theme에서 변경방법 찾아 적용
                datePickerEnd.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
                datePickerEnd.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK)
            }
        }
        val datePickerStart: TimePickerDialog =
            TimePickerDialog(this@CreateTimeActivity, callbackMethodStart, 12, 0, false)
        datePickerStart.setTitle("시작시간")
        clock.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View) {
                datePickerStart.show()

                //버튼 색상변경. theme에서 변경방법 찾아 적용
                datePickerStart.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
                datePickerStart.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.BLACK)
            }
        })
        save.setOnClickListener(object : View.OnClickListener {
            public override fun onClick(v: View) {
                if (!((inputSummery.getText().toString() == "")) && isValid) {
                    itemSave.saveTimeItem(
                        inputSummery.getText().toString(),
                        startTime,
                        endTime,
                        true
                    )
                    MainActivity.Companion.GetDBItem()
                    finish()
                } else {
                    Toast.makeText(this@CreateTimeActivity, "시간과 이름을 확인해주세요", Toast.LENGTH_LONG)
                        .show()
                }
            }
        })
    } //TimePickerDialog 의 주, 야간 테마변경을 위한 함수
    /*

    private int getTimeTheme() {
        int value;
        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_YES:
                value = 2;
                break;
            default:
                value = 3;
                break;
        }
        return value;
    }

     */

     */
}