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
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class SenseKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener,
        SpellCheckerSession.SpellCheckerSessionListener {

    private static final int KEYCODE_SPACE = 32;

    private boolean caps = false;

    // LWM feature flag, default value is false = off
    private Boolean mLwmFeatureActive = false;
    private Boolean mDeviceInMoveFlag = false; // copy of SignificantMotionSensor member, indicating if device is in move
    private Boolean mDisplayComposingText;

    private SignificantMotionSensor mSignificantMotionSensor = null;
    private Keyboard mKeyboard = null;

    private AudioManager audioManager;
    private float defaultAudioVolume = (float) 0.1; // default key click audio volume

    private static final String CLASS_NAME = SenseKeyboardService.class.getSimpleName();

    private Integer mEditorInfoImeAction = null; // keep editor info object, because we will get it in onStartInputView() method but will need it in onKey() method
    private static final int IME_ACTION_DEFAULT_LOCAL = 999999;

    private Typeface mTypeface;
    private String mTypefaceName = "fonts/ubuntu/Ubuntu-R.ttf";//Jomolhari-alpha3c-0605331.ttf";//"DDC_Uchen.ttf"

    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions; // vybírá se z něj v pickSuggestionsManually
    private StringBuilder mComposing; // podtrzeny text v radku kam pisu, kdyz vyberu text z nabidky (mCompletions?), zameni se za podtrzeny text
    private List<String> mSuggestions;
    private SpellCheckerSession mSpellCheckerSession;

    private Timer keyDeleteTimer;

    public SenseKeyboardService() {
        mComposing = new StringBuilder();
        mSuggestions = new ArrayList<String>();
    }

    // http://android.okhelp.cz/java_android_code.php?o=%5Candroid-15%5CSoftKeyboard%5Csrc%5Ccom%5Cexample%5Candroid%5Csoftkeyboard%5CSoftKeyboard.java
    // http://www.blackcj.com/blog/2016/03/30/building-a-custom-android-keyboard/
    // https://blog.autsoft.hu/discovering-the-android-api-part-1/ - API differences

    @Override
    public void onCreate() { // causes application fail (even empty method), don't know why...
        super.onCreate();

        Log.i("SenseKeyboard-Service","onCreate");

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mSignificantMotionSensor = new SignificantMotionSensor(getApplicationContext(), this);
        mTypeface = Typeface.createFromAsset(getAssets(), mTypefaceName);
        keyDeleteTimer = new Timer();

        final TextServicesManager textServicesManager = (TextServicesManager) getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        mSpellCheckerSession = textServicesManager.newSpellCheckerSession(null, null, this, true);
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
    @Override
    public  View onCreateCandidatesView() {
        Log.i("SenseKeyboard-Service","onCreateCandidatesView()");
        //mCandidateView = super.onCreateCandidatesView();
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }


    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean  restarting) {
        Log.i("SenseKeyboard-Service","onStartInput()");
        super.onStartInput(attribute, restarting);

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mCompletions = null;
        mComposing.setLength(0);
        updateCandidates();

        mDisplayComposingText = false;

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                //mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                //mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                //mCurKeyboard = mQwertyKeyboard;
                mDisplayComposingText = true;

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mDisplayComposingText = false;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mDisplayComposingText = false;
                }

                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mDisplayComposingText = false;
                    //mCompletionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                //updateShiftKeyState(attribute);
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                //mCurKeyboard = mQwertyKeyboard;
                //updateShiftKeyState(attribute);
        }
    }


    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        Log.i("SenseKeyboard-Service","onStartInputView()");
        super.onStartInputView(attribute, restarting);

        applyFeatureSettings();

        if(mLwmFeatureActive) {
            // LWM feature is active, register for move detection events
            mSignificantMotionSensor.register();
        }

        //InputConnection ic = getCurrentInputConnection();

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


    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        Log.d("SenseKeyboard-Service","onUpdateSelection(oldSelStart="+oldSelStart+", oldSelEnd="+oldSelEnd+", newSelStart="+newSelStart+
                ", newSelEnd="+newSelEnd+", candidatesStart="+candidatesStart+", candidatesEnd="+candidatesEnd+")");

        /**
         * Called when the application has reported a new selection region of
         * the text.  This is called whether or not the input method has requested
         * extracted text updates, although if so it will not receive this call
         * if the extracted text has changed as well.
         *
         * <p>Be careful about changing the text in reaction to this call with
         * methods such as setComposingText, commitText or
         * deleteSurroundingText. If the cursor moves as a result, this method
         * will be called again, which may result in an infinite loop.
         *
         * <p>The default implementation takes care of updating the cursor in
         * the extract text, if it is being shown.
         */
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            //mComposing.setLength(0);
            updateCandidates();
            InputConnection inputConnection = getCurrentInputConnection();
            if (inputConnection != null) {
                inputConnection.finishComposingText();
            }
        }
    }
    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        Log.i("SenseKeyboard-Service","updateCandidates()");
        if (mComposing.length() > 0) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(mComposing.toString());
            //mSpellCheckerSession.getSentenceSuggestions(new TextInfo[] {new TextInfo(mComposing.toString())}, 5);
            setSuggestions(list, true, true);
        } else {
            setSuggestions(null, false, false);
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        // ZATIM JSEM JI NEVIDEL NIKDY ZAVOLANOU!
        Log.i("SenseKeyboard-Service","onDisplayCompletions() completions=" + completions);
        mCompletions = completions;
        if (completions == null) {
            setSuggestions(null, false, false);
            return;
        }

        List<String> stringList = new ArrayList<String>();
        for (int i = 0; i < (completions != null ? completions.length : 0); i++) {
            CompletionInfo completionInfo = completions[i];
            if (completionInfo != null) {
                stringList.add(completionInfo.getText().toString());
            }
        }
        stringList.add("test");
        setSuggestions(stringList, true, true);
    }

    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid) {
        Log.i("SenseKeyboard-Service","setSuggestions() suggestions=" + suggestions + ", completions=" + completions + ", typedWordValid=" + typedWordValid);
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }

        setSuggestions(suggestions);
        suggestions = getSuggestions(); // TEMP check !!

        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }


    /**
     *
     * @return true or false based on whether it should be fullscreen in the current environment
     */
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
        //TODO: when pressing one key twice, we receive first key event, then delete event and then second key event -> three clicks instead of two... :(

        Log.d("SenseKeyboard-Service","onKey(primaryCode="+primaryCode+", keyCodes="+keyCodes+")");
        KeyboardView view = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);

        InputConnection inputConnection = getCurrentInputConnection();

        // cancel timer - if new character was pressed before timer expired, no sound will be played, we want it because of behavior when second/third code of one key pressed
        keyDeleteTimer.cancel();
        keyDeleteTimer.purge();

        switch(primaryCode){
            case Keyboard.KEYCODE_DELETE:
                handleDelete();

                keyDeleteTimer = new Timer();
                keyDeleteTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        playClick(Keyboard.KEYCODE_DELETE);
                    }
                }, 50); // delay of 50ms

                break;

            case KEYCODE_SPACE:
                handleSpace();
                playClick(KEYCODE_SPACE);
                break;

            /*case Keyboard.KEYCODE_SHIFT:
                caps = !caps;
                keyboard.setShifted(caps);
                kv.invalidateAllKeys();
                break;*/

            case Keyboard.KEYCODE_DONE:
                playClick(Keyboard.KEYCODE_DONE);
                switch (mEditorInfoImeAction) {
                    case EditorInfo.IME_ACTION_GO:
                        inputConnection.performEditorAction(EditorInfo.IME_ACTION_GO);
                        Log.i("SenseKeyboard-Service","onKey-IME_ACTION_GO");
                        break;
                    case EditorInfo.IME_ACTION_NEXT:
                        inputConnection.performEditorAction(EditorInfo.IME_ACTION_NEXT);
                        Log.i("SenseKeyboard-Service","onKey-IME_ACTION_NEXT");
                        break;
                    case EditorInfo.IME_ACTION_SEARCH:
                        inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEARCH);
                        Log.i("SenseKeyboard-Service","onKey-IME_ACTION_SEARCH");
                        break;
                    case EditorInfo.IME_ACTION_SEND:
                        inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEND);
                        Log.i("SenseKeyboard-Service","onKey-IME_ACTION_SEND");
                        break;
                    case IME_ACTION_DEFAULT_LOCAL:
                        inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
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
                playClick(primaryCode);
                handleCharacter(primaryCode, keyCodes);
        }
    }



    /**
     * Use this to monitor hard key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("SenseKeyboard-Service","onKeyDown(keyCode="+keyCode+", event="+event+")");
        return super.onKeyDown(keyCode, event);
    }


    /**
     * Use this to monitor hard key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d("SenseKeyboard-Service","onKeyUp(keyCode="+keyCode+", event="+event+")");
        return super.onKeyUp(keyCode, event);
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
            case KEYCODE_SPACE:
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


    /**
     * http://www.tutorialspoint.com/android/android_spelling_checker.htm
     * @param results results
     */
    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
        Log.i("SenseKeyboard-Service", "onGetSuggestions(results="+results+")");
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < results.length; ++i) {
            // Returned suggestions are contained in SuggestionsInfo
            final int len = results[i].getSuggestionsCount();
            sb.append('\n');

            for (int j = 0; j < len; ++j) {
                sb.append("," + results[i].getSuggestionAt(j));
            }

            sb.append(" (" + len + ")");
        }
        Log.i("SenseKeyboard-Service", "onGetSuggestions: " + sb.toString());
    }
    private static final int NOT_A_LENGTH = -1;

    private void dumpSuggestionsInfoInternal(
            final List<String> sb, final SuggestionsInfo si, final int length, final int offset) {
        // Returned suggestions are contained in SuggestionsInfo
        final int len = si.getSuggestionsCount();
        for (int j = 0; j < len; ++j) {
            sb.add(si.getSuggestionAt(j));
        }
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        Log.i("SenseKeyboard-Service", "onGetSentenceSuggestions(results="+results+")");
        final List<String> sb = new ArrayList<>();
        for (int i = 0; i < results.length; ++i) {
            final SentenceSuggestionsInfo sentenceSuggestionsInfo = results[i];
            for (int j = 0; j < sentenceSuggestionsInfo.getSuggestionsCount(); ++j) {
                dumpSuggestionsInfoInternal(
                        sb, sentenceSuggestionsInfo.getSuggestionsInfoAt(j),
                        sentenceSuggestionsInfo.getOffsetAt(j), sentenceSuggestionsInfo.getLengthAt(j));
            }
        }
        Log.d("SenseKeyboard-Service", "onGetSentenceSuggestions SUGGESTIONS: " + sb.toString());
        setSuggestions(sb, true, true);
    }


    private void handleCharacter(int primaryCode, int[] keyCodes) {

        char code = (char)primaryCode;
        if(Character.isLetter(code) && caps) {
            code = Character.toUpperCase(code);
        }
        /*if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }*/
        if (mDisplayComposingText) {
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1); // Tohle mi vkládá podtrzeny text do radku kam pisu, kdyz vyberu text z nabidky, zameni se za podtrzeny text
            //updateShiftKeyState(getCurrentInputEditorInfo());
            updateCandidates();
        } else {
            getCurrentInputConnection().commitText(String.valueOf(code),1);
        }
    }


    private void handleDelete() {
        Log.i("SenseKeyboard-Service", "mComposing="+mComposing);

        final int length = mComposing.length();
        InputConnection inputConnection = getCurrentInputConnection();
        if (length > 1) {
            //mComposing.delete(0, (mComposing.length() - 1));
            //mComposing.append("COMP");

            mComposing.delete(length - 1, length);
            inputConnection.setComposingText(mComposing, 1); // Tohle mi vkládá podtrzeny text do radku kam pisu, kdyz vyberu text z nabidky, zameni se za podtrzeny text
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            inputConnection.commitText("", 0);
            updateCandidates();
        } else {
            //keyDownUp(KeyEvent.KEYCODE_DEL);
            inputConnection.deleteSurroundingText(1, 0);
        }
        //updateShiftKeyState(getCurrentInputEditorInfo());
        //inputConnection.deleteSurroundingText(1, 0); // odkomentovani zde zpusobi, ze se změna projevuje hned a ne do composing (podtrzeneho) textu
    }


    private void handleSpace() {
        commitComposingText();
        getCurrentInputConnection().commitText(String.valueOf((char) KEYCODE_SPACE),1);
    }


    public void pickSuggestionManually(int index) {
        Log.d("SenseKeyboard-Service", "pickSuggestionManually(index="+index+")");
        if (mCompletions != null && index >= 0 && index < mCompletions.length) {
            CompletionInfo completionInfo = mCompletions[index];
            getCurrentInputConnection().commitCompletion(completionInfo);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            //updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {
            Log.d("SenseKeyboard-Service", "pickSuggestionManually ELSE SECTION");
            if (mDisplayComposingText && getSuggestions() != null && index >= 0) {
                mComposing.replace(0, mComposing.length(), getSuggestions().get(index));
            }
            commitComposingText();
        }
    }


    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitComposingText() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }


    private boolean getDeviceInMoveFlag() {
        return mDeviceInMoveFlag;
    }

    private List<String> getSuggestions() {
        mSuggestions.add("dve");
        mSuggestions.add("tri");
        return mSuggestions;

    }

    private void setSuggestions(List<String> suggestions) {
        if (suggestions == null) {
            mSuggestions.clear();
        } else {
            mSuggestions = suggestions;
        }
        Log.d("SenseKeyboard-Service", "setSuggestions() mSuggestions="+mSuggestions);
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
