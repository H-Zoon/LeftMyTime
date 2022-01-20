package com.devidea.timeleft

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CreateTimeActivity : AppCompatActivity() {
    lateinit var startTime : String
    lateinit var endTime : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_time)

        val inputTitle = findViewById<EditText>(R.id.input_summery)
        val inputStartTime = findViewById<EditText>(R.id.input_start_time)
        val inputEndTime = findViewById<EditText>(R.id.input_end_time)
        val btnSave = findViewById<Button>(R.id.save)

        val itemSave = ItemSave()

        inputStartTime.setOnClickListener { v ->
            val timePickerDialog = TimePickerDialog(
                this, { timePicker, time, minute ->
                    startTime = "$time:$minute"
                },
                0, 0, false)
            timePickerDialog.setTitle("시작시간 설정")
            timePickerDialog.show()
        }

        inputEndTime.setOnClickListener { v ->
            val timePickerDialog = TimePickerDialog(
                this, { timePicker, time, minute ->
                    endTime = "$time:$minute"}, 0, 0, false)
            timePickerDialog.setTitle("종료시간 설정")
            timePickerDialog.show()
        }

        //isInitialized is able instance variable, not a local variable.
        btnSave.setOnClickListener {
            if (::startTime.isInitialized && ::endTime.isInitialized && inputTitle.length()>0){
                itemSave.saveTimeItem(inputTitle.text.toString(), startTime, endTime, true)
                finish()
            }else{
                Toast.makeText(this,"입력확인",Toast.LENGTH_SHORT).show()
            }}

    }

}