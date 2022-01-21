package com.devidea.timeleft

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CreateTimeActivity : AppCompatActivity() {
    lateinit var startTime: String
    lateinit var endTime: String

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
                    if (time in 0..11) {
                        inputStartTime.setText("오전 $time 시 $minute 분")
                    } else {
                        inputStartTime.setText("오후 $time 시 $minute 분")
                    }
                },
                0, 0, false
            )
            timePickerDialog.setTitle("시작시간 설정")
            timePickerDialog.show()
        }

        inputEndTime.setOnClickListener { v ->
            val timePickerDialog = TimePickerDialog(
                this, { timePicker, time, minute ->
                    endTime = "$time:$minute"
                    if (time in 0..11) {
                        inputEndTime.setText("오전 $time 시 $minute 분")
                    } else {
                        inputEndTime.setText("오후 $time 시 $minute 분")
                    }
                }, 0, 0, false
            )
            timePickerDialog.setTitle("종료시간 설정")
            timePickerDialog.show()
        }

        //isInitialized is able instance variable, not a local variable.
        btnSave.setOnClickListener {
            if (::startTime.isInitialized && ::endTime.isInitialized && inputTitle.length() > 0) {
                itemSave.saveTimeItem(inputTitle.text.toString(), startTime, endTime)
                finish()
            } else {
                Toast.makeText(this, "입력확인", Toast.LENGTH_SHORT).show()
            }
        }

    }

}