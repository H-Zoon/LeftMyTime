package com.devidea.timeleft.activity

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.devidea.timeleft.R
import com.devidea.timeleft.databinding.ActivityCreateTimeBinding
import com.devidea.timeleft.databinding.ActivitySetRepeatBinding

class SetRepeatActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetRepeatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_set_repeat)
        binding = ActivitySetRepeatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var flag = 0

        val radioGroup = findViewById<RadioGroup>(R.id.radio_group)

        //람다식 방법
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioButton -> {
                    flag = 0
                    binding.inputLayout1.visibility = View.GONE
                    binding.inputLayout2.visibility = View.GONE
                }
                R.id.radioButton2 -> {
                    flag = 1
                    binding.inputLayout1.visibility = View.VISIBLE
                    binding.inputLayout2.visibility = View.GONE
                }
                R.id.radioButton3 -> {
                    flag = 2
                    binding.inputLayout1.visibility = View.GONE
                    binding.inputLayout2.visibility = View.VISIBLE
                }
            }
        }

        binding.save.setOnClickListener {
            if (flag == 1 && binding.editText.length() > 0) {
                val intent = Intent().apply {
                    putExtra("flag", flag)
                    putExtra("value", binding.editText.text.toString().toInt())
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else if (flag == 2 && binding.editText2.length() > 0) {
                if (binding.editText2.text.toString().toInt() < 32) {
                    val intent = Intent().apply {
                        putExtra("flag", flag)
                        putExtra("value", binding.editText2.text.toString().toInt())
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