package com.example.dell.sensekeyboard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

public class SenseKeyboardSettingsFinal extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings_final);

        Button activateKeyboardButton = (Button)findViewById(R.id.final_settings_activate_keyboard_button);
        if (activateKeyboardButton != null) {

            activateKeyboardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS), 0);
                }
            });
        }

        Button chooseKeyboardButton = (Button)findViewById(R.id.final_settings_choose_keyboard_button);
        if (chooseKeyboardButton != null) {

            chooseKeyboardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Context context = getApplicationContext();
                    InputMethodManager imeManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imeManager != null) {
                        imeManager.showInputMethodPicker();
                        //finish(); // does not do a good job, cancels only this settings page
                    } else {
                        Toast.makeText(context ,"Error", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }


        //new MzdyServiceTask().execute(vstup);

        Button backButton = (Button)findViewById(R.id.final_settings_back_button);
        if (backButton != null) {

            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }
    }
}
