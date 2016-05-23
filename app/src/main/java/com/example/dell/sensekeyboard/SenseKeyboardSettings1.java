package com.example.dell.sensekeyboard;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SenseKeyboardSettings1 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings1);

        Button nextButton = (Button)findViewById(R.id.main_settings_next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SenseKeyboardSettings1.this, SenseKeyboardSettings2.class);
                intent.setFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
                //intent.putExtra("data", mzdaVystup);
                startActivity(intent);
                finish();
            }
        });

        Button exitButton = (Button)findViewById(R.id.main_settings_exit_button);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
