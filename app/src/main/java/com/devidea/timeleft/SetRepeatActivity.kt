package com.devidea.timeleft

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup

class SetRepeatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_repeat)
        val radioGroup = findViewById<RadioGroup>(R.id.radio_group)
        val btnSave = findViewById<Button>(R.id.save)
        val inpLayout1 = findViewById<LinearLayout>(R.id.input_layout1)
        val inpLayout2 = findViewById<LinearLayout>(R.id.input_layout2)
        val inputValue = findViewById<EditText>(R.id.editText)
        val inputValue2 = findViewById<EditText>(R.id.editText2)
        var flag = 0

        //람다식 방법
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioButton -> {
                    flag = 0
                    inpLayout1.visibility = View.GONE
                    inpLayout2.visibility = View.GONE
                }
                R.id.radioButton2 -> {
                    flag = 1
                    inpLayout1.visibility = View.VISIBLE
                    inpLayout2.visibility = View.GONE
                }
                R.id.radioButton3 -> {
                    flag = 2
                    inpLayout1.visibility = View.GONE
                    inpLayout2.visibility = View.VISIBLE
                }
            }
        }

        btnSave.setOnClickListener {
            val intent = Intent().apply {
                putExtra("flag", flag)
                if (flag == 1) {
                    putExtra("value", inputValue.text.toString().toInt())
                } else {
                    putExtra("value", inputValue2.text.toString().toInt())

                }
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }


    }
}