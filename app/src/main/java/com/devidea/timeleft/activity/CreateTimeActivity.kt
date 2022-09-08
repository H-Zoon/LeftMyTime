package com.devidea.timeleft.activity

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.devidea.timeleft.ItemSave
import com.devidea.timeleft.R
import com.devidea.timeleft.databinding.ActivityCreateTimeBinding

class CreateTimeActivity : AppCompatActivity() {
    lateinit var startTime: String
    lateinit var endTime: String

    var alarmFlag = false

    private lateinit var binding: ActivityCreateTimeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_time)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_time)
        binding.activity = this

    }

    fun setStartTime(v: View) {
        val timePickerDialog = TimePickerDialog(
            this, { timePicker, time, minute ->
                startTime = "$time:$minute"
                if (time in 0..11) {
                    binding.inputStartTime.setText("오전 $time 시 $minute 분")
                } else {
                    binding.inputStartTime.setText("오후 $time 시 $minute 분")
                }
            },
            0, 0, false
        )
        timePickerDialog.setTitle("시작시간 설정")
        timePickerDialog.show()
    }

    fun setEndTime(v: View) {
        val timePickerDialog = TimePickerDialog(
            this, { timePicker, time, minute ->
                endTime = "$time:$minute"
                if (time in 0..11) {
                    binding.inputEndTime.setText("오전 $time 시 $minute 분")
                } else {
                    binding.inputEndTime.setText("오후 $time 시 $minute 분")
                }
            }, 0, 0, false
        )
        timePickerDialog.setTitle("종료시간 설정")
        timePickerDialog.show()
    }

    //isInitialized is able instance variable, not a local variable.
    fun save(v: View) {
        if (::startTime.isInitialized && ::endTime.isInitialized && binding.inputSummery.length() > 0) {
            if (!alarmFlag) {
                ItemSave().saveTimeItem(
                    binding.inputSummery.text.toString(),
                    startTime,
                    endTime,
                    false,
                    0,
                    false
                )
                finish()
            }

        } else {
            Toast.makeText(this, "입력을 확인해주세요", Toast.LENGTH_SHORT).show()
        }
    }

}