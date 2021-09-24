package com.devidea.timeleft;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.time.LocalTime;

public class CreateTimeActivity extends AppCompatActivity {
    TextView timeRange;
    EditText inputSummery;
    Button clock, save;
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
            @SuppressLint("SetTextI18n")
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                endTime = LocalTime.of(hourOfDay, minute);
                if (endTime.isBefore(startTime)) {
                    Toast.makeText(CreateTimeActivity.this, "시작시간 이후로 설정해주세요", Toast.LENGTH_LONG).show();
                    isValid = false;
                } else {
                    timeRange.setText(startTime + " 부터 " + endTime + " 까지 계산할께요.");
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
                startTime = LocalTime.of(hourOfDay, minute);
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
                if (!(inputSummery.getText().toString().equals("")) && isValid) {
                    itemGenerator.saveTimeItem(inputSummery.getText().toString(), startTime, endTime, true);
                    MainActivity.GetDBItem();
                    finish();
                } else {
                    Toast.makeText(CreateTimeActivity.this, "시간과 이름을 확인해주세요", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

}