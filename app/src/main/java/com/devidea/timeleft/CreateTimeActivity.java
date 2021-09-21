package com.devidea.timeleft;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.time.LocalTime;

import static android.content.DialogInterface.BUTTON_POSITIVE;

public class CreateTimeActivity extends AppCompatActivity {
    TextView timeRange;
    EditText inputSummery;
    Button clock, save;
    //CheckBox AutoUpdateCheck;
    ItemGenerator itemGenerator = new ItemGenerator();
    LocalTime startTime, endTime;
    boolean isValid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_time);
        inputSummery = findViewById(R.id.input_summery);
        timeRange = findViewById(R.id.time_range);
        clock = findViewById(R.id.clock);
        save = findViewById(R.id.summit_time);

        TimePickerDialog.OnTimeSetListener callbackMethodEnd = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                endTime = LocalTime.of(hourOfDay,minute);
                if(endTime.compareTo(startTime)<0){
                    Toast.makeText(CreateTimeActivity.this, "시간 범위를 확인해주세요", Toast.LENGTH_LONG).show();
                    isValid = false;
                }
                else {
                    isValid = true;
                }
            }
        };
        TimePickerDialog datePickerEnd = new TimePickerDialog(CreateTimeActivity.this, callbackMethodEnd, 12, 0, false);
        datePickerEnd.setTitle("종료시간");

        // TODO: 수정합시다
        TimePickerDialog.OnTimeSetListener callbackMethodStart = new TimePickerDialog.OnTimeSetListener() {

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                startTime = LocalTime.of(hourOfDay,minute);
                datePickerEnd.show();
            }
        };
        TimePickerDialog datePickerStart = new TimePickerDialog(CreateTimeActivity.this, callbackMethodStart, 12, 0, false);
        datePickerStart.setTitle("시작시간");

        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerStart.show();

            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(inputSummery.getText().toString().equals(""))&&isValid){
                    //TODO : booean값 가변형으로 수정
                    itemGenerator.saveTimeItem(inputSummery.getText().toString(), startTime, endTime, false);
                    MainActivity.refreshItem();
                    finish();
                }
            }
        });

    }

}