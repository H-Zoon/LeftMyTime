package com.devidea.timeleft.activity

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.DatePickerDialog
import android.content.Intent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.devidea.timeleft.ItemSave
import com.devidea.timeleft.R
import com.devidea.timeleft.databinding.ActivityCreateDayBinding
import java.time.LocalDate

//사용자가 날짜 지정하기를 선택한 경우 진입하는 activity
class CreateDayActivity : AppCompatActivity() {

    lateinit var startDay : String
    lateinit var endDay : String
    var UPDATEFLAG = 0
    var UPDATEVALUE = 0

    private lateinit var binding: ActivityCreateDayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_day)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_day)
        binding.activity = this

    }

    private val preContractStartActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { a_result ->
            if (a_result.resultCode == Activity.RESULT_OK) {
                a_result.data?.let {
                    UPDATEFLAG = it.getIntExtra("flag", 0)
                    UPDATEVALUE = it.getIntExtra("value", 0)
                    when(UPDATEFLAG){
                        0 -> binding.inputUpdateRate.setText("반복없음")
                        1 -> binding.inputUpdateRate.setText("이후 $UPDATEVALUE 일마다 반복")
                        2 -> binding.inputUpdateRate.setText("매달 $UPDATEVALUE 일에 반복")
                    }
                }
            }
        }

    fun setStartDay(v: View) {
        val datePickerDialog = DatePickerDialog(
            this, {datePicker, year, month, day ->
                val monthis = month+1
                startDay = "$year-$monthis-$day"
                binding.inputStartDay.setText(startDay)
            },
            LocalDate.now().year, LocalDate.now().monthValue-1, LocalDate.now().dayOfMonth)
        datePickerDialog.show()
    }

    fun setEndDay(v: View) {
        val timePickerDialog = DatePickerDialog(
            this, {datePicker, year, month, day ->
                val monthis = month+1
                endDay = "$year-$monthis-$day"
                binding.inputEndDay.setText(endDay)
            },
            LocalDate.now().year, LocalDate.now().monthValue-1, LocalDate.now().dayOfMonth)
        timePickerDialog.show()
    }

    fun setRepeatRate(v: View) {
        val intent = Intent(application, SetRepeatActivity::class.java)
        preContractStartActivityResult.launch(intent)}

    //isInitialized is able instance variable, not a local variable.
    fun save(v: View) {
        if (::startDay.isInitialized && ::endDay.isInitialized && binding.inputSummery.length()>0){
            ItemSave().saveMonthItem(binding.inputSummery.text.toString(), startDay, endDay, UPDATEFLAG, UPDATEVALUE)
            finish()
        }else{
            Toast.makeText(this,"입력확인",Toast.LENGTH_SHORT).show()
        }}

}