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
            val intent = Intent().apply {
                putExtra("flag", flag)
                when(flag){
                    1 -> putExtra("value", binding.editText.text.toString().toInt())
                    2 -> putExtra("value", binding.editText2.text.toString().toInt())
                }
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }


    }
}