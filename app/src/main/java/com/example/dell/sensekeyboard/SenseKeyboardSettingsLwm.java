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
                    //SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = sharedPref.edit();

                    Log.e("SenseKeyboard", "Settings - LWM:"+String.valueOf(isChecked));
                    editor.putBoolean(getString(R.string.LWMFeature), isChecked);
                    editor.commit();
                }
            });

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            Boolean defaultLVMValue = false;
            Boolean lwmFeatureActive = sharedPref.getBoolean(getString(R.string.LWMFeature), defaultLVMValue);

            lwmFeatureSwitch.setChecked(lwmFeatureActive);
        }

        Button nextButton = (Button)findViewById(R.id.lwm_settings_next_button);
        if (nextButton != null) {
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(SenseKeyboardSettingsLwm.this, SenseKeyboardSettingsFinal.class);
                    //intent.setFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //intent.putExtra("data", mzdaVystup);
                    startActivity(intent);
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

    /*
    Performing stop of activity that is not resumed: {com.example.dell.sensekeyboard/com.example.dell.sensekeyboard.SenseKeyboardSettingsLwm}
    java.lang.RuntimeException: Performing stop of activity that is not resumed: {com.example.dell.sensekeyboard/com.example.dell.sensekeyboard.SenseKeyboardSettingsLwm}
    at android.app.ActivityThread.performStopActivityInner(ActivityThread.java:3607)
    */
}
