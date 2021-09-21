package com.devidea.timeleft;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class CreateMonthActivity extends AppCompatActivity {

    EditText inputSummery, inputDay;
    Button calender, save;
    CheckBox AutoUpdateCheck;
    ItemGenerator itemGenerator = new ItemGenerator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_month);

        LocalDate now = LocalDate.now();

        inputSummery = findViewById(R.id.input_summery);
        inputDay = findViewById(R.id.input_day);
        calender = findViewById(R.id.calender);
        save = findViewById(R.id.summit);
        AutoUpdateCheck = findViewById(R.id.auto_update_check);

        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                LocalDate startDate = LocalDate.now();
                LocalDate endDate = LocalDate.of(year, month, dayOfMonth);

                if (startDate.isAfter(endDate)) {
                    inputDay.setText("");
                    inputDay.setText(String.valueOf(endDate.minusDays(startDate.getDayOfMonth()).getDayOfMonth()));
                    inputDay.setSelection(inputDay.length());
                } else {
                    Toast.makeText(CreateMonthActivity.this, "오늘보다 먼 날을 선택해주세요", Toast.LENGTH_LONG).show();
                }

            }
        };

        calender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(CreateMonthActivity.this, dateSetListener, now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth());
                datePicker.show();
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(inputSummery.getText().toString().equals("")) && !(inputDay.getText().toString().equals(""))) {
                    if (Integer.parseInt(inputDay.getText().toString()) > 1826) {
                        Toast.makeText(CreateMonthActivity.this, "흠..감당하기엔 너무 멀지 않나요..?", Toast.LENGTH_LONG).show();
                    } else {
                        itemGenerator.saveMonthItem(inputSummery.getText().toString(), Integer.parseInt(inputDay.getText().toString()), AutoUpdateCheck.isChecked());
                        MainActivity.refreshItem();
                        finish();
                    }

                } else {
                    Toast.makeText(CreateMonthActivity.this, "제목과 날짜를 확인해주세요", Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}