package com.devidea.timeleft

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.DatePickerDialog.OnDateSetListener
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import java.time.LocalDate
import java.time.temporal.ChronoUnit

//사용자가 날짜 지정하기를 선택한 경우 진입하는 activity
class CreateMonthActivity : AppCompatActivity() {

    lateinit var startDay : String
    lateinit var endDay : String
    var UPDATEFLAG = 0
    var UPDATEVALUE = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_month)

        val inputTitle = findViewById<EditText>(R.id.input_summery)
        val inputStartDay = findViewById<EditText>(R.id.input_start_day)
        val inputEndDay = findViewById<EditText>(R.id.input_end_day)
        val inputUpdate = findViewById<EditText>(R.id.input_update_rate)
        val btnSave = findViewById<Button>(R.id.save)

        val itemSave = ItemSave()

        val preContractStartActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { a_result ->
                if (a_result.resultCode == Activity.RESULT_OK) {
                    a_result.data?.let {
                        UPDATEFLAG = it.getIntExtra("flag", 0)
                        UPDATEVALUE = it.getIntExtra("value", 0)
                        when(UPDATEFLAG){
                            0 -> inputUpdate.setText("반복없음")
                            1 -> inputUpdate.setText("이후 $UPDATEVALUE 일마다 반복")
                            2 -> inputUpdate.setText("매달 $UPDATEVALUE 일에 반복")
                        }
                    }
                }
            }

        inputStartDay.setOnClickListener { v ->
            val datePickerDialog = DatePickerDialog(
                this, {datePicker, year, month, day ->
                    var monthis = month+1
                    startDay = "$year-$monthis-$day"
                    inputStartDay.setText(startDay)
                      },
                LocalDate.now().year, LocalDate.now().monthValue-1, LocalDate.now().dayOfMonth)
            datePickerDialog.show()
        }

        inputEndDay.setOnClickListener { v ->
            val timePickerDialog = DatePickerDialog(
                this, {datePicker, year, month, day ->
                    val monthis = month+1
                    endDay = "$year-$monthis-$day"
                    inputEndDay.setText(endDay)
                      },
                LocalDate.now().year, LocalDate.now().monthValue-1, LocalDate.now().dayOfMonth)
            timePickerDialog.show()
        }

        inputUpdate.setOnClickListener { v ->
            val intent = Intent(application, SetRepeatActivity::class.java)
            preContractStartActivityResult.launch(intent)}

        //isInitialized is able instance variable, not a local variable.
        btnSave.setOnClickListener {
            if (::startDay.isInitialized && ::endDay.isInitialized && inputTitle.length()>0){
                itemSave.saveMonthItem(inputTitle.text.toString(), startDay, endDay, UPDATEFLAG, UPDATEVALUE)
                finish()
            }else{
                Toast.makeText(this,"입력확인",Toast.LENGTH_SHORT).show()
            }}

    }
}