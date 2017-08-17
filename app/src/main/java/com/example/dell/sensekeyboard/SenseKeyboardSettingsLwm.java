package com.example.dell.sensekeyboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

public class SenseKeyboardSettingsLwm extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings_lwm);

        // put LWM feature button into activity, try to restore its value from previous configuration
        Switch lwmFeatureSwitch = (Switch) findViewById(R.id.lwm_settings_switch);
        if (lwmFeatureSwitch != null) {

            lwmFeatureSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPref.edit();

                Log.d("SenseKeyboard", "Settings - LWM:"+String.valueOf(isChecked));
                editor.putBoolean(getString(R.string.LWMFeatureId), isChecked);
                editor.commit();
                }
            });

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            Boolean defaultLVMValue = false;
            Boolean lwmFeatureActive = sharedPref.getBoolean(getString(R.string.LWMFeatureId), defaultLVMValue);

            lwmFeatureSwitch.setChecked(lwmFeatureActive);
        }

        Button nextButton = (Button)findViewById(R.id.lwm_settings_next_button);
        if (nextButton != null) {
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                Intent intent = new Intent(SenseKeyboardSettingsLwm.this, SenseKeyboardSettingsFinal.class);
                intent.setFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                }
            });
        }

        Button exitButton = (Button)findViewById(R.id.lwm_settings_exit_button);
        if (exitButton != null) {
            exitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
}
