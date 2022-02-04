package com.devidea.timeleft.activity

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.devidea.timeleft.ItemSave
import com.devidea.timeleft.R
import com.devidea.timeleft.alarm.AlarmReceiver.Companion.TAG
import com.devidea.timeleft.databinding.ActivityCreateTimeBinding
import com.devidea.timeleft.databinding.ActivityMainBinding

class CreateTimeActivity : AppCompatActivity() {
    lateinit var startTime: String
    lateinit var endTime: String

    var alarmFlag = 0

    var flag = 0
    private lateinit var binding: ActivityCreateTimeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_time)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_time)
        binding.activity = this

        binding.switch1.setOnCheckedChangeListener { compoundButton, b ->
            if (compoundButton.isChecked) {
                binding.inputLayout1.visibility = View.VISIBLE
                binding.switch2.visibility = View.VISIBLE
                alarmFlag = 2
            } else {
                binding.inputLayout1.visibility = View.GONE
                binding.switch2.visibility = View.GONE
                alarmFlag = 0
            }
        }

        binding.switch2.setOnCheckedChangeListener { compoundButton, b ->
            alarmFlag = if (compoundButton.isChecked) {
                Log.d(TAG, "1")
                1
            } else {
                Log.d(TAG, "0")
                2
            }
        }

    }

    fun setStartTime (v: View){
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

   fun setEndTime(v: View){
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
    fun save(v: View){
        if (::startTime.isInitialized && ::endTime.isInitialized && binding.inputSummery.length() > 0) {
            Log.d("flag", flag.toString())
            ItemSave().saveTimeItem(binding.inputSummery.text.toString(), startTime, endTime, alarmFlag, binding.editText.text.toString().toInt())
            finish()
        } else {
            Toast.makeText(this, "입력확인", Toast.LENGTH_SHORT).show()
        }
    }

}