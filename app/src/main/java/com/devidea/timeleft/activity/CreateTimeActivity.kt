package com.devidea.timeleft.activity

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.devidea.timeleft.ItemSave
import com.devidea.timeleft.R
import com.devidea.timeleft.alarm.AlarmReceiver.Companion.TAG
import com.devidea.timeleft.databinding.ActivityCreateTimeBinding
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CreateTimeActivity : AppCompatActivity() {
    lateinit var startTime: String
    lateinit var endTime: String

    var alarmFlag = false
    var weekendFlag = false

    private lateinit var binding: ActivityCreateTimeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_time)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_time)
        binding.activity = this

        /*
        binding.alarmSwitch.setOnCheckedChangeListener { compoundButton, b ->
            if (compoundButton.isChecked) {
                binding.alarmRateDateLayout.visibility = View.VISIBLE
                binding.alarmWeekendSwitch.visibility = View.VISIBLE
                alarmFlag = true
            } else {
                binding.alarmRateDateLayout.visibility = View.GONE
                binding.alarmWeekendSwitch.visibility = View.GONE
                alarmFlag = false
            }
        }

        binding.alarmWeekendSwitch.setOnCheckedChangeListener { compoundButton, b ->
            if (compoundButton.isChecked) {
                weekendFlag = true
            } else {
                Log.d(TAG, "0")
                weekendFlag = false
            }
        }

         */

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
            /*else {
                if (binding.alarmRateDateEditText.length() > 0) {

                    val start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("H:m"))
                    val end = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("H:m"))
                    Log.d(TAG, start.toString())
                    Log.d(TAG, end.toString())
                    Log.d(TAG, Duration.between(start, end).toMillis().toString())
                    Log.d(TAG, (binding.alarmRateDateEditText.text.toString().toInt() * 1000 * 3600).toString())
                    if (Duration.between(start, end).toMillis() > binding.alarmRateDateEditText.text.toString().toInt() * 1000 * 3600

                    ) {
                        ItemSave().saveTimeItem(
                            binding.inputSummery.text.toString(),
                            startTime,
                            endTime,
                            alarmFlag,
                            binding.alarmRateDateEditText.text.toString().toInt(),
                            weekendFlag
                        )
                        finish()
                    } else {
                        Toast.makeText(this, "알람시간이 설정시간보다 큽니다", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "알람시간을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
             */
        } else {
            Toast.makeText(this, "입력을 확인해주세요", Toast.LENGTH_SHORT).show()
        }
    }

}