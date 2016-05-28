package com.example.dell.sensekeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;


public class SenseKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private boolean caps = false;

    // LWM feature flag, default value is false = off
    private Boolean mLwmFeatureActive = false;
    private Boolean mDeviceInMoveFlag = false; // copy of SignificantMotionSensor member, indicating if device is in move

    private SignificantMotionSensor mSignificantMotionSensor = null;

    private static final String CLASS_NAME = SenseKeyboardService.class.getSimpleName();


    @Override
    public void onCreate() { // causes application fail (even empty method), don't know why...
        super.onCreate();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.e("SenseKeyboard-Service","On Create input activity upper to 4.3 version detected");
            mSignificantMotionSensor = new SignificantMotionSensor(getApplicationContext(), this);
        }
    }


    @Override
    public View onCreateInputView() {
        Log.e("SenseKeyboard-Service","onCreateInputView");

        Keyboard keyboard = new Keyboard(this, R.xml.keys_positions);

        KeyboardView keyboardView = (KeyboardView)getLayoutInflater().inflate(R.layout.keyboard, null);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);

        return keyboardView;
    }


    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        Log.e("SenseKeyboard-Service","onStartInputView");
        applyFeatureSettings();

        if(mLwmFeatureActive) {
            // LWM feature is active, register for move detection events
            mSignificantMotionSensor.register();
        }
    }


    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        playClick(primaryCode);
        switch(primaryCode){
            case Keyboard.KEYCODE_DELETE :
                ic.deleteSurroundingText(1, 0);
                break;
            /*case Keyboard.KEYCODE_SHIFT:
                caps = !caps;
                keyboard.setShifted(caps);
                kv.invalidateAllKeys();
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;*/
            default:
                char code = (char)primaryCode;
                if(Character.isLetter(code) && caps){
                    code = Character.toUpperCase(code);
                }
                ic.commitText(String.valueOf(code),1);
        }
    }


    private void playClick(int keyCode){
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE); /*am = (AudioManager)getSystemService(AUDIO_SERVICE);*/
        switch(keyCode){
            case 32:

                //am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
                break;
            /*case Keyboard.KEYCODE_DONE:
            case 10:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                break;*/
            case Keyboard.KEYCODE_DELETE:

                //am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                break;
            default:
                /*try { // toto funguje
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
                audioManager.playSoundEffect(SoundEffectConstants.CLICK);
                //am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }
    }


    private void applyFeatureSettings() {
        //SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // LWM feature variable is initialized to FALSE, so we can use here as default value if no entry found (second parameter)
        mLwmFeatureActive = sharedPref.getBoolean(getString(R.string.LWMFeature), mLwmFeatureActive);
        Log.e("SenseKeyboard-Service", "LWM:"+String.valueOf(mLwmFeatureActive));
    }


    public void setDeviceInMoveFlag(boolean deviceInMoveFlag) {
        mDeviceInMoveFlag = deviceInMoveFlag;
        Log.e("SenseKeyboard-Service", "setDeviceInMoveFlag setting flag to:"+mDeviceInMoveFlag);
    }


    private boolean getDeviceInMoveFlag() {
        return mDeviceInMoveFlag;
    }


    @Override
    public void onPress(int primaryCode) {
    }

    @Override
    public void onRelease(int primaryCode) {
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeUp() {
    }
}
