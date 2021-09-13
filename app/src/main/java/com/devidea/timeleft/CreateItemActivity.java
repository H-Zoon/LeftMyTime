package com.devidea.timeleft;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class CreateItemActivity extends AppCompatActivity {

    EditText inputTitle, inputDay;
    TextView explanDay;
    Button save;
    ItemGenerator itemGenerator = new ItemGenerator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_item);

        inputTitle = findViewById(R.id.input_title);
        inputDay = findViewById(R.id.input_day);
        explanDay = findViewById(R.id.cal);
        save = findViewById(R.id.button);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemGenerator.genDate(inputTitle.getText().toString(), Integer.parseInt(inputDay.getText().toString()));
            }
        });

    }
}