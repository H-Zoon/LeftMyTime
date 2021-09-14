package com.devidea.timeleft;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialStyledDatePickerDialog;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class CreateItemActivity extends AppCompatActivity {

    EditText inputSummery, inputDay;
    Button calender, save;
    ItemGenerator itemGenerator = new ItemGenerator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_item);

        LocalDate now = LocalDate.now();

        inputSummery = findViewById(R.id.input_summery);
        inputDay = findViewById(R.id.input_day);
        calender = findViewById(R.id.calender);
        save = findViewById(R.id.summit);

        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat transFormat = new SimpleDateFormat("dd");

                Calendar start = new GregorianCalendar(now.getYear(), now.getMonthValue()-1, now.getDayOfMonth());
                Calendar end = new GregorianCalendar(year, month, dayOfMonth);
                int diffDay;

                if(start.compareTo(end) < 0){
                    diffDay=(int)((end.getTimeInMillis()-start.getTimeInMillis())/(24*60*60*1000));
                    inputDay.setText(String.valueOf(diffDay));
                }

                else Toast.makeText(CreateItemActivity.this, "오늘보다 먼 날을 선택해주세요", Toast.LENGTH_LONG).show();


            }
        };

        calender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(CreateItemActivity.this, dateSetListener, now.getYear(), now.getMonthValue()-1, now.getDayOfMonth());
                datePicker.show();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemGenerator.genDate(inputSummery.getText().toString(), Integer.parseInt(inputDay.getText().toString()));
                finish();
            }
        });

    }
}