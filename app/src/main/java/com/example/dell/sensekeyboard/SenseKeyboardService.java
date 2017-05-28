package com.example.dell.sensekeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.inputmethodservice.ExtractEditText;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.TextView;


public class SenseKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private boolean caps = false;

    // LWM feature flag, default value is false = off
    private Boolean mLwmFeatureActive = false;
    private Boolean mDeviceInMoveFlag = false; // copy of SignificantMotionSensor member, indicating if device is in move

    private SignificantMotionSensor mSignificantMotionSensor = null;
    private Keyboard mKeyboard = null;

    private AudioManager audioManager;
    private float defaultAudioVolume = (float) 0.1; // default key click audio volume

    private static final String CLASS_NAME = SenseKeyboardService.class.getSimpleName();

    private Integer mEditorInfoImeAction = null; // keep editor info object, because we will get it in onStartInputView() method but will need it in onKey() method
    private static final int IME_ACTION_DEFAULT_LOCAL = 999999;

    private Typeface mTypeface;
    private String mTypefaceName = "fonts/ubuntu/Ubuntu-R.ttf";//Jomolhari-alpha3c-0605331.ttf";//"DDC_Uchen.ttf"

    // http://android.okhelp.cz/java_android_code.php?o=%5Candroid-15%5CSoftKeyboard%5Csrc%5Ccom%5Cexample%5Candroid%5Csoftkeyboard%5CSoftKeyboard.java

    @Override
    public void onCreate() { // causes application fail (even empty method), don't know why...
        super.onCreate();

        Log.i("SenseKeyboard-Service","onCreate");

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mSignificantMotionSensor = new SignificantMotionSensor(getApplicationContext(), this);
        mTypeface = Typeface.createFromAsset(getAssets(), mTypefaceName);
    }


    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override
    public  void  onInitializeInterface() {
        /*if  (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int  displayWidth = getMaxWidth();
            if  (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new  LatinKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new  LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new  LatinKeyboard(this, R.xml.symbols_shift);*/

        mKeyboard = new Keyboard(this, R.xml.keys_positions);
    }


    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override
    public View onCreateInputView() {
        Log.i("SenseKeyboard-Service","onCreateInputView");

        KeyboardView keyboardView = (KeyboardView)getLayoutInflater().inflate(R.layout.keyboard, null);
        keyboardView.setKeyboard(mKeyboard);
        keyboardView.setOnKeyboardActionListener(this);
        //keyboardView.setupKeys(mTypeface);
        //keyboardView.addChildrenForAccessibility();
        //keyboardView.announceForAccessibility();
        //keyboardView.createAccessibilityNodeInfo();
        //keyboardView.getAccessibilityTraversalBefore();
        //keyboardView.getAccessibilityTraversalAfter();

        return keyboardView;

        /*
        MyKeyboardView inputView = (MyKeyboardView) getLayoutInflater().inflate(R.layout.input, null);

        inputView.setOnKeyboardActionListener(this);
        inputView.setKeyboard(mLatinKeyboard);

        return mInputView;

         */
    }


    @Override
    public View onCreateExtractTextView() {
        // TODO Auto-generated method stub
        Log.i("SenseKeyboard-Service","onCreateExtractTextView()");
        View view = super.onCreateExtractTextView();
        ExtractEditText textEdit = (ExtractEditText)view.findViewById(android.R.id.inputExtractEditText); // R.id.inputExtractEditText
        textEdit.setTypeface(mTypeface);
        textEdit.setTextSize(30);
        return view;
    }




    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    /*@Override public  View onCreateCandidatesView() {
        mCandidateView = new  CandidateView(this);
        mCandidateView.setService(this);
        return  mCandidateView;
    }*/


    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    /*@Override public  void  onStartInput(EditorInfo attribute, boolean  restarting) {

    }*/


    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        Log.i("SenseKeyboard-Service","onStartInputView");
        applyFeatureSettings();

        if(mLwmFeatureActive) {
            // LWM feature is active, register for move detection events
            mSignificantMotionSensor.register();
        }

        InputConnection ic = getCurrentInputConnection();

        switch (attribute.imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            case EditorInfo.IME_ACTION_GO:
                mEditorInfoImeAction = EditorInfo.IME_ACTION_GO;
                Log.i("SenseKeyboard-Service","onStartInputView-IME_ACTION_GO");
                break;
            case EditorInfo.IME_ACTION_NEXT:
                mEditorInfoImeAction = EditorInfo.IME_ACTION_NEXT;
                Log.i("SenseKeyboard-Service","onStartInputView-IME_ACTION_NEXT");
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                mEditorInfoImeAction = EditorInfo.IME_ACTION_SEARCH;
                Log.i("SenseKeyboard-Service","onStartInputView-IME_ACTION_SEARCH");
                break;
            case EditorInfo.IME_ACTION_SEND:
                mEditorInfoImeAction = EditorInfo.IME_ACTION_SEND;
                Log.i("SenseKeyboard-Service","onStartInputView-IME_ACTION_SEND");
                break;
            default:
                mEditorInfoImeAction = IME_ACTION_DEFAULT_LOCAL;
                Log.i("SenseKeyboard-Service","onStartInputView-Key Enter event");
                break;
        }
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return true;
    }

    @Override
    public void onUpdateExtractingVisibility(EditorInfo ei) {
        setExtractViewShown(true);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {

        KeyboardView view = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);

        //Log.e("SenseKeyboard-Service","onStartInputView-Key ^^^^ "+view.findViewById(R.id.keyboard_input));
//        EditText keyboardInput = (EditText)view.findViewById(R.id.keyboard_input);
//        keyboardInput.setText("Google is your friend.", TextView.BufferType.EDITABLE);

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
                break;*/
            case Keyboard.KEYCODE_DONE:
                switch (mEditorInfoImeAction) {
                    case EditorInfo.IME_ACTION_GO:
                        ic.performEditorAction(EditorInfo.IME_ACTION_GO);
                        Log.i("SenseKeyboard-Service","onKey-IME_ACTION_GO");
                        break;
                    case EditorInfo.IME_ACTION_NEXT:
                        ic.performEditorAction(EditorInfo.IME_ACTION_NEXT);
                        Log.i("SenseKeyboard-Service","onKey-IME_ACTION_NEXT");
                        break;
                    case EditorInfo.IME_ACTION_SEARCH:
                        ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH);
                        Log.i("SenseKeyboard-Service","onKey-IME_ACTION_SEARCH");
                        break;
                    case EditorInfo.IME_ACTION_SEND:
                        ic.performEditorAction(EditorInfo.IME_ACTION_SEND);
                        Log.i("SenseKeyboard-Service","onKey-IME_ACTION_SEND");
                        break;
                    case IME_ACTION_DEFAULT_LOCAL:
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                        Log.i("SenseKeyboard-Service","onKey-Key Enter event");
                        break;
                    default:
                        Log.i("SenseKeyboard-Service","onKey-DEFAULT-UNKNOWN ACTION!!!");
                        break;
                }
                /*ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                Log.e("SenseKeyboard-Service","onKey-Key Enter event");*/
                break;
            default:
                char code = (char)primaryCode;
                if(Character.isLetter(code) && caps) {
                    code = Character.toUpperCase(code);
                }
                ic.commitText(String.valueOf(code),1);
        }
    }


    private void playClick(int keyCode) {

        float audioVolume;

        // device is in move (LWM feature), adjust audio volume
        if(getDeviceInMoveFlag()) {
            audioVolume = (float) 1.0;
        } else {
            audioVolume = defaultAudioVolume;
        }

        switch(keyCode) {
            case 32: // SPACE BAR
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, audioVolume);
                break;
            case Keyboard.KEYCODE_DELETE:
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE, audioVolume);
                break;
            default:
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, audioVolume);
        }
    }


    private void applyFeatureSettings() {
        //SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // LWM feature variable is initialized to FALSE, so we can use here as default value if no entry found (second parameter)
        mLwmFeatureActive = sharedPref.getBoolean(getString(R.string.LWMFeature), mLwmFeatureActive);
        Log.i("SenseKeyboard-Service", "LWM:"+String.valueOf(mLwmFeatureActive));
    }


    public void setDeviceInMoveFlag(boolean deviceInMoveFlag) {
        mDeviceInMoveFlag = deviceInMoveFlag;
        Log.i("SenseKeyboard-Service", "setDeviceInMoveFlag setting flag to:"+mDeviceInMoveFlag);
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
