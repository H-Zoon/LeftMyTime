package com.devidea.timeleft.activity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.devidea.timeleft.R
import com.devidea.timeleft.databinding.ActivitySetRepeatBinding

class SetRepeatActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetRepeatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetRepeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var flag = 0

        binding.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.alarm_rate_unable_radio_button -> {
                    flag = 0
                    binding.alarmRateDateLayout.visibility = View.GONE
                    binding.alarmRateMonthLayout.visibility = View.GONE
                }
                R.id.alarm_rate_date_radio_button -> {
                    flag = 1
                    binding.alarmRateDateLayout.visibility = View.VISIBLE
                    binding.alarmRateMonthLayout.visibility = View.GONE
                }
                R.id.alarm_rate_month_radio_button -> {
                    flag = 2
                    binding.alarmRateDateLayout.visibility = View.GONE
                    binding.alarmRateMonthLayout.visibility = View.VISIBLE
                }
            }
        }

        binding.save.setOnClickListener {
            if (flag == 0) {
                val intent = Intent().apply {
                    putExtra("flag", flag)
                    putExtra("value", 0)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else if (flag == 1 && binding.alarmRateDateEditText.length() > 0) {
                val intent = Intent().apply {
                    putExtra("flag", flag)
                    putExtra("value", binding.alarmRateDateEditText.text.toString().toInt())
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else if (flag == 2 && binding.alarmRateMonthEditText.length() > 0) {
                if (binding.alarmRateMonthEditText.text.toString().toInt() < 32) {
                    val intent = Intent().apply {
                        putExtra("flag", flag)
                        putExtra(
                            "value",
                            binding.alarmRateMonthEditText.text.toString().toInt()
                        )
                    }

                    setResult(Activity.RESULT_OK, intent)
                    finish()
                } else {
                    Toast.makeText(this, "31보다 큰 값이 입력되었습니다.", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "입력을 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
