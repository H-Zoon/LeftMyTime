package com.devidea.timeleft;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.graphics.Color;
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
    ItemSave itemSave = new ItemSave();
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
                //버튼 색상변경. theme에서 변경방법 찾아 적용
                datePickerEnd.getButton(datePickerEnd.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                datePickerEnd.getButton(datePickerEnd.BUTTON_POSITIVE).setTextColor(Color.BLACK);
            }
        };
        TimePickerDialog datePickerStart = new TimePickerDialog(CreateTimeActivity.this, callbackMethodStart, 12, 0, false);
        datePickerStart.setTitle("시작시간");

        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerStart.show();

                //버튼 색상변경. theme에서 변경방법 찾아 적용
                datePickerStart.getButton(datePickerStart.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                datePickerStart.getButton(datePickerStart.BUTTON_POSITIVE).setTextColor(Color.BLACK);

            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(inputSummery.getText().toString().equals("")) && isValid) {
                    itemSave.saveTimeItem(inputSummery.getText().toString(), startTime, endTime, true);
                    MainActivity.GetDBItem();
                    finish();
                } else {
                    Toast.makeText(CreateTimeActivity.this, "시간과 이름을 확인해주세요", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    //TimePickerDialog 의 주, 야간 테마변경을 위한 함수
    /*

    private int getTimeTheme() {
        int value;
        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_YES:
                value = 2;
                break;
            default:
                value = 3;
                break;
        }
        return value;
    }

     */

}