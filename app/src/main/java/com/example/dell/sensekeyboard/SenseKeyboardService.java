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
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SpellCheckerSubtype;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;


public class SenseKeyboardService extends InputMethodService implements KeyboardView.OnKeyboardActionListener,
        SpellCheckerSession.SpellCheckerSessionListener {

    private static final String CLASS_NAME_STRING = SenseKeyboardService.class.getSimpleName();
    private static final int KEYCODE_SPACE = 32;

    private boolean caps = false;
    private Boolean mSkipKeyAfterLongpress = false;

    // LWM feature flag, default value is false = off
    private Boolean mLwmFeatureActive = false;
    private Boolean mDeviceInMoveFlag = false; // copy of SignificantMotionSensor member, indicating if device is in move
    private Boolean mDisplayComposingText;
    //private Boolean mCapitalLetterFlag;

    private SignificantMotionSensor mSignificantMotionSensor = null;
    private Keyboard mKeyboard = null;
    private MyKeyboardView mMyKeyboardView = null;

    private AudioManager audioManager;
    private float defaultAudioVolume = (float) 0.1; // default key click audio volume

    private static final String CLASS_NAME = SenseKeyboardService.class.getSimpleName();

    private Integer mEditorInfoImeAction = null; // keep editor info object, because we will get it in onStartInputView() method but will need it in onKey() method

    private static final int IME_ACTION_DEFAULT_LOCAL = 999999;
    private static final int NO_SUGGESTION_SELECTED = -1;
    private static final int FIRST_SUGGESTION_SELECTED = 0;


    private Typeface mTypeface;
    private String mTypefaceName = "fonts/ubuntu/Ubuntu-R.ttf";//Jomolhari-alpha3c-0605331.ttf";//"DDC_Uchen.ttf"

    private MyCandidatesView mMyCandidatesView;
    private CompletionInfo[] mCompletions; // vybírá se z něj v pickSuggestionsManually
    private StringBuilder mComposing; // podtrzeny text v radku kam pisu, kdyz vyberu text z nabidky (mCompletions?), zameni se za podtrzeny text
    private List<String> mSuggestions;
    private SpellCheckerSession mSpellCheckerSession;
    private Integer mSuggestionFocussed;

    private Timer keyDeleteTimer;
    private Timer mKeyPressTimer;

    public SenseKeyboardService() {
        mComposing = new StringBuilder();
        mSuggestions = new ArrayList<String>();
        //mCapitalLetterFlag = true;
    }

    // http://android.okhelp.cz/java_android_code.php?o=%5Candroid-15%5CSoftKeyboard%5Csrc%5Ccom%5Cexample%5Candroid%5Csoftkeyboard%5CSoftKeyboard.java
    // http://www.blackcj.com/blog/2016/03/30/building-a-custom-android-keyboard/
    // https://blog.autsoft.hu/discovering-the-android-api-part-1/ - API differences
    // https://code.tutsplus.com/tutorials/an-introduction-to-androids-spelling-checker-framework--cms-23754
    // https://developer.android.com/training/keyboard-input/style.html
    // https://nlp.stanford.edu/IR-book/html/htmledition/edit-distance-1.html
    // https://developer.android.com/guide/topics/text/creating-input-method.html
    // https://developer.android.com/guide/topics/ui/accessibility/apps.html
    // https://developer.android.com/guide/topics/ui/accessibility/index.html
    // https://developer.android.com/topic/libraries/support-library/features.html
    // https://developer.android.com/reference/android/view/accessibility/package-summary.html
    // https://developer.android.com/reference/android/graphics/Canvas.html
    // https://developer.android.com/training/gestures/detector.html
    // https://support.google.com/accessibility/android/answer/6151827?hl=en

    @Override
    public void onCreate() { // causes application fail (even empty method), don't know why...
        super.onCreate();

        Log.i(CLASS_NAME_STRING,"onCreate");

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mSignificantMotionSensor = new SignificantMotionSensor(getApplicationContext(), this);
        mTypeface = Typeface.createFromAsset(getAssets(), mTypefaceName);
        keyDeleteTimer = new Timer();

        //mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        //mWordSeparators = getResources().getString(R.string.word_separators);

        Locale locale = Locale.getDefault();
        Log.i(CLASS_NAME_STRING,"onCreate Locale="+locale.toString());
        final TextServicesManager textServicesManager = (TextServicesManager) getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);

        /*if (!mTextServicesManager.isSpellCheckerEnabled() || locale == null
                || mTextServicesManager.getCurrentSpellCheckerSubtype(true) == null) {*/
        //mSpellCheckerSession = textServicesManager.newSpellCheckerSession(null, locale, this, false);
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
        Log.i(CLASS_NAME_STRING,"onCreateInputView");

        mMyKeyboardView = (MyKeyboardView)getLayoutInflater().inflate(R.layout.keyboard, null);
        mMyKeyboardView.setKeyboardService(this); // not nice solution - must be set before first use of keyboard service
        mMyKeyboardView.getAccessibilityNodeProvider();
        mMyKeyboardView.setPreviewEnabled(false); // tímhle zakážu preview popup nad klávesami
        mMyKeyboardView.setKeyboard(mKeyboard);
        mMyKeyboardView.setOnKeyboardActionListener(this); // this class will be action listener since we handle onKey() events
        /*myKeyboardView.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        Log.i(CLASS_NAME_STRING,"ACTION_HOVER_ENTER");
                        break;
                    case MotionEvent.ACTION_HOVER_MOVE:
                        Log.i(CLASS_NAME_STRING,"ACTION_HOVER_MOVE");
                        v.announceForAccessibility("Experiment");
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        Log.i(CLASS_NAME_STRING,"ACTION_HOVER_EXIT");
                        break;
                }
                return false;
            }
        });*/
        //keyboardView.setupKeys(mTypeface);
        //keyboardView.addChildrenForAccessibility();
        //keyboardView.announceForAccessibility();
        //keyboardView.createAccessibilityNodeInfo();
        //keyboardView.getAccessibilityTraversalBefore();
        //keyboardView.getAccessibilityTraversalAfter();

        return mMyKeyboardView;
    }


    @Override
    public View onCreateExtractTextView() {
        // TODO Auto-generated method stub
        Log.i(CLASS_NAME_STRING,"onCreateExtractTextView()");
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
        Log.i(CLASS_NAME_STRING,"onCreateCandidatesView()");
        mSuggestionFocussed = NO_SUGGESTION_SELECTED;
        //mCandidateView = super.onCreateCandidatesView();
        mMyCandidatesView = new MyCandidatesView(this);
        mMyCandidatesView.setService(this);
        return mMyCandidatesView;
    }


    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean  restarting) {
        Log.i(CLASS_NAME_STRING,"onStartInput()");
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
        Log.i(CLASS_NAME_STRING,"onStartInputView()");
        super.onStartInputView(attribute, restarting);

        final TextServicesManager textServicesManager = (TextServicesManager) getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        Locale locale = Locale.getDefault();
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        InputMethodSubtype subtype = inputMethodManager.getCurrentInputMethodSubtype();
        Log.i(CLASS_NAME_STRING,"onCreate Locale="+locale.toString());
        //locale = new Locale("cs");
        mSpellCheckerSession = textServicesManager.newSpellCheckerSession(null, locale, this, false);

        applyFeatureSettings();

        if(mLwmFeatureActive) {
            // LWM feature is active, register for move detection events
            mSignificantMotionSensor.register();
        }

        //InputConnection ic = getCurrentInputConnection();

        switch (attribute.imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            case EditorInfo.IME_ACTION_GO:
                mEditorInfoImeAction = EditorInfo.IME_ACTION_GO;
                Log.i(CLASS_NAME_STRING,"onStartInputView-IME_ACTION_GO");
                break;
            case EditorInfo.IME_ACTION_NEXT:
                mEditorInfoImeAction = EditorInfo.IME_ACTION_NEXT;
                Log.i(CLASS_NAME_STRING,"onStartInputView-IME_ACTION_NEXT");
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                mEditorInfoImeAction = EditorInfo.IME_ACTION_SEARCH;
                Log.i(CLASS_NAME_STRING,"onStartInputView-IME_ACTION_SEARCH");
                break;
            case EditorInfo.IME_ACTION_SEND:
                mEditorInfoImeAction = EditorInfo.IME_ACTION_SEND;
                Log.i(CLASS_NAME_STRING,"onStartInputView-IME_ACTION_SEND");
                break;
            default:
                mEditorInfoImeAction = IME_ACTION_DEFAULT_LOCAL;
                Log.i(CLASS_NAME_STRING,"onStartInputView-Key Enter event");
                break;
        }
    }


    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        /*Log.d(CLASS_NAME_STRING,"onUpdateSelection(oldSelStart="+oldSelStart+", oldSelEnd="+oldSelEnd+", newSelStart="+newSelStart+
                ", newSelEnd="+newSelEnd+", candidatesStart="+candidatesStart+", candidatesEnd="+candidatesEnd+")");*/

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
        //Log.d(CLASS_NAME_STRING,"updateCandidates() mComposing="+mComposing.toString());
        //mSpellCheckerSession = mTextServicesManager.newSpellCheckerSession(null, null, this, true);

        if (mComposing.length() > 0) {
            ArrayList<String> list = new ArrayList<String>();
            list.add(mComposing.toString());
            if (isSentenceSpellCheckSupported()) {
                mSpellCheckerSession.getSentenceSuggestions(new TextInfo[] {new TextInfo(mComposing.toString())}, 5);
            } else {
                mSpellCheckerSession.getSuggestions(new TextInfo(mComposing.toString()), 3);
            }
            setSuggestions(list, true, true); // zakomentene kvuli padani protoze po pridani timeru volam z jineho threadu
        } else {
            setSuggestions(null, false, false); // zakomentene kvuli padani protoze po pridani timeru volam z jineho threadu
        }
        mSuggestionFocussed = FIRST_SUGGESTION_SELECTED;
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        // neznamo kdy a proc se vola, zatim videno jen na lenovo tabletu
        Log.e(CLASS_NAME_STRING,"onDisplayCompletions() !!!!! completions=" + completions);
        //throw new RuntimeException("onDisplayCompletions()");
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
        stringList.add("onDisplayCompletions");
        setSuggestions(stringList, true, true);
    }

    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid) {
        //Log.d(CLASS_NAME_STRING,"setSuggestions() suggestions=" + suggestions + ", completions=" + completions + ", typedWordValid=" + typedWordValid);
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        } else {
            setCandidatesViewShown(false);
        }

        setSuggestions(suggestions);
        suggestions = getSuggestions(); // TEMP check !!

        if (mMyCandidatesView != null) {
            mMyCandidatesView.setSuggestions(suggestions, completions, typedWordValid);
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
        try {
            sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.e(CLASS_NAME_STRING,"onKey(primaryCode="+primaryCode+", keyCodes="+keyCodes+")");

        if(mMyKeyboardView.isScrollGestureInProgress()) {
            Log.e(CLASS_NAME_STRING,"onKey - ScrollGestureInProgress - SKIP");
            return;
        }

        if(mSkipKeyAfterLongpress) {
            Log.e(CLASS_NAME_STRING,"onKey - mSkipKeyAfterLongpress - SKIP");
            mSkipKeyAfterLongpress = false;
            return;
        }

        KeyboardView view = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);

        // cancel timer - if new character was pressed before timer expired, no sound will be played, we want it because of behavior when second/third code of one key pressed
        keyDeleteTimer.cancel();
        keyDeleteTimer.purge();
        keyDeleteTimer = new Timer();

        if(mKeyPressTimer != null) {
            mKeyPressTimer.cancel();
            //mKeyPressTimer.purge();
        }
        mKeyPressTimer = new Timer();

        switch(primaryCode){
            case Keyboard.KEYCODE_DELETE:
                handleDelete();
                break;

            case KEYCODE_SPACE:
                handleSpace();
                break;

            /*case Keyboard.KEYCODE_SHIFT:
                caps = !caps;
                keyboard.setShifted(caps);
                kv.invalidateAllKeys();
                break;*/

            case Keyboard.KEYCODE_DONE:
                handleDone();
                /*ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                Log.e(CLASS_NAME_STRING,"onKey-Key Enter event");*/
                break;
            default:
                //mKeyPressTimer.schedule(new KeyPressTimerTask(primaryCode, keyCodes, this) {}, 50); // delay of 50ms
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
        //Log.d(CLASS_NAME_STRING,"onKeyDown(keyCode="+keyCode+", event="+event+")");
        return super.onKeyDown(keyCode, event);
    }


    /**
     * Use this to monitor hard key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //Log.d(CLASS_NAME_STRING,"onKeyUp(keyCode="+keyCode+", event="+event+")");
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
        Log.d(CLASS_NAME_STRING, "LWM:"+String.valueOf(mLwmFeatureActive));
    }


    public void setDeviceInMoveFlag(boolean deviceInMoveFlag) {
        mDeviceInMoveFlag = deviceInMoveFlag;
        Log.d(CLASS_NAME_STRING, "setDeviceInMoveFlag setting flag to:"+mDeviceInMoveFlag);
    }


    /**
     * http://www.tutorialspoint.com/android/android_spelling_checker.htm
     * @param results results
     */
    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
        //Log.d(CLASS_NAME_STRING, "onGetSuggestions(results="+results+")");
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
        //Log.d(CLASS_NAME_STRING, "onGetSuggestions: " + sb.toString());
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
        //Log.d(CLASS_NAME_STRING, "onGetSentenceSuggestions(results="+results.toString()+")");
        final List<String> sb = new ArrayList<>();
        for (int i = 0; i < results.length; ++i) {
            final SentenceSuggestionsInfo sentenceSuggestionsInfo = results[i];
            for (int j = 0; j < sentenceSuggestionsInfo.getSuggestionsCount(); ++j) {
                dumpSuggestionsInfoInternal(
                        sb, sentenceSuggestionsInfo.getSuggestionsInfoAt(j),
                        sentenceSuggestionsInfo.getOffsetAt(j), sentenceSuggestionsInfo.getLengthAt(j));
            }
        }
        //Log.d(CLASS_NAME_STRING, "onGetSentenceSuggestions SUGGESTIONS: " + sb.toString());
        setSuggestions(sb, true, true);

        /*
        To only show suggestions for misspelled words, we will have to look at the flags associated with each SuggestionsInfo object.
        A SuggestionsInfo object for a misspelled word has the RESULT_ATTR_LOOKS_LIKE_TYPO flag set.
        Therefore, we must add code to ignore SuggestionsInfo objects where this flag is not set.
        Add the following code to the onGetSentenceSuggestions method before the innermost loop begins:
        if((result.getSuggestionsInfoAt(i).getSuggestionsAttributes() &
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) != SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO )
            continue;
         */
    }


    /*@Override
    public SentenceSuggestionsInfo[] onGetSentenceSuggestionsMultiple (TextInfo[] textInfos, int suggestionsLimit) {

    }*/


    public void handleCharacter(int primaryCode, int[] keyCodes) {

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
        playClick(primaryCode);
    }


    private void handleDelete() {
        Log.d(CLASS_NAME_STRING, "handleDelete()");

        keyDeleteTimer = new Timer(); // timer here is because delete is sent by system when second/third char of key is pressed; new is because old one was canceled in calling function
        final int length = mComposing.length();
        InputConnection inputConnection = getCurrentInputConnection();

        if (length > 1) { // we delete text from composing text, which contains more than one character
            mComposing.delete(length - 1, length);
            inputConnection.setComposingText(mComposing, 1); // Tohle mi vkládá podtrzeny text do radku kam pisu, kdyz vyberu text z nabidky, zameni se za podtrzeny text
            updateCandidates();
            keyDeleteTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    playClick(Keyboard.KEYCODE_DELETE);
                }
            }, 50); // delay of 50ms
        } else if (length > 0) { // we handle text from composing text, which contains last one character
            mComposing.setLength(0);
            updateCandidates();
            keyDeleteTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    getCurrentInputConnection().commitText("", 0); // only one char in composing text, we have to commit empty text here
                    playClick(Keyboard.KEYCODE_DELETE);
                }
            }, 50); // delay of 50ms
        } else { // composing text is empty, we will directly remove one character from input text field
            inputConnection.deleteSurroundingText(1, 0);
            keyDeleteTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    playClick(Keyboard.KEYCODE_DELETE);
                }
            }, 50); // delay of 50ms
        }
        //updateShiftKeyState(getCurrentInputEditorInfo());
        //inputConnection.deleteSurroundingText(1, 0); // odkomentovani zde zpusobi, ze se změna projevuje hned a ne do composing (podtrzeneho) textu
    }


    private void handleDone() {
        InputConnection inputConnection = getCurrentInputConnection();
        commitComposingText(); // first of all, commit composing text, because enter means composing text should be put into edited textbox

        switch (mEditorInfoImeAction) {
            case EditorInfo.IME_ACTION_GO:
                inputConnection.performEditorAction(EditorInfo.IME_ACTION_GO);
                Log.i(CLASS_NAME_STRING,"onKey-IME_ACTION_GO");
                break;
            case EditorInfo.IME_ACTION_NEXT:
                inputConnection.performEditorAction(EditorInfo.IME_ACTION_NEXT);
                Log.i(CLASS_NAME_STRING,"onKey-IME_ACTION_NEXT");
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEARCH);
                Log.i(CLASS_NAME_STRING,"onKey-IME_ACTION_SEARCH");
                break;
            case EditorInfo.IME_ACTION_SEND:
                inputConnection.performEditorAction(EditorInfo.IME_ACTION_SEND);
                Log.i(CLASS_NAME_STRING,"onKey-IME_ACTION_SEND");
                break;
            case IME_ACTION_DEFAULT_LOCAL:
                inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                Log.i(CLASS_NAME_STRING,"onKey-Key Enter event");
                break;
            default:
                Log.i(CLASS_NAME_STRING,"onKey-DEFAULT-UNKNOWN ACTION!!!");
                break;
        }
        playClick(Keyboard.KEYCODE_DONE);
    }


    private void handleSpace() {
        commitComposingText(); // first of all, commit composing text, because space means composing text should be put into edited textbox
        getCurrentInputConnection().commitText(String.valueOf((char) KEYCODE_SPACE),1); // add also the space key
        playClick(KEYCODE_SPACE);

        mSuggestionFocussed = NO_SUGGESTION_SELECTED;
        super.setCandidatesViewShown(false);
    }


    public void pickSuggestionManually(int index) {
        Log.d(CLASS_NAME_STRING, "pickSuggestionManually(index="+index+")");
        if (mCompletions != null && index >= 0 && index < mCompletions.length) {
            CompletionInfo completionInfo = mCompletions[index];
            getCurrentInputConnection().commitCompletion(completionInfo);
            /*if (mMyCandidatesView != null) {
                mMyCandidatesView.clear();
            }*/
            //updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {
            Log.d(CLASS_NAME_STRING, "pickSuggestionManually ELSE SECTION");
            if (mDisplayComposingText && getSuggestions() != null && index >= 0) {
                mComposing.replace(0, mComposing.length(), getSuggestions().get(index));
            }
            commitComposingText();
        }
        if (mMyCandidatesView != null) {
            mMyCandidatesView.clear();
        }
        mSuggestionFocussed = NO_SUGGESTION_SELECTED;
        super.setCandidatesViewShown(false);
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

    private boolean isSentenceSpellCheckSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    private List<String> getSuggestions() {
        return mSuggestions;
    }

    private void setSuggestions(List<String> suggestions) {
        if (suggestions == null) {
            mSuggestions.clear();
        } else {
            mSuggestions = suggestions;
        }
        mSuggestions.add(mComposing.toString());

        // TODO temp for test purposes
        /*List<String> tmp = new ArrayList<String>();
        tmp.add(mComposing.toString());
        tmp.add("skakal");
        tmp.add("pes");
        tmp.add("pres");
        tmp.add("oves");
        mSuggestions = new ArrayList<String>(tmp);*/
        //Log.d(CLASS_NAME_STRING, "setSuggestions() mSuggestions="+mSuggestions);
    }


    @Override
    public void onFinishInput() {
        super.onFinishInput();
        Log.i(CLASS_NAME_STRING, "onFinishInput()");
    }


    /**
     * Method called from MyKeyboardView, originally came from MyGestureListener
     * Methods takes next word from suggestion list, sets it as highlighted (which forces redraw
     * of candidate view after it is invalidated) and returns newly focused word
     * (focus in meaning of this method is only virtual, because it is made by gestures)
     */
    public String onSwipeRightGesture() {

        String suggestion = "";

        if(mSuggestions != null && mSuggestions.size() > 0) {
            if(++mSuggestionFocussed < mSuggestions.size()) {
                suggestion = mSuggestions.get(mSuggestionFocussed);
            } else {
                mSuggestionFocussed = 0;
                suggestion = mSuggestions.get(mSuggestionFocussed);
            }
            if(mMyCandidatesView != null) {
                mMyCandidatesView.changeHighlightedSuggestion(mSuggestionFocussed);
            }
        }

        return suggestion;
    }


    /**
     * Method called from MyKeyboardView, originally came from MyGestureListener
     * Methods takes next word from suggestion list, sets it as highlighted (which forces redraw
     * of candidate view after it is invalidated) and returns newly focused word
     * (focus in meaning of this method is only virtual, because it is made by gestures)
     */
    public String onSwipeLeftGesture() {

        String suggestion = "";

        if(mSuggestions != null && mSuggestions.size() > 0) {
            if(--mSuggestionFocussed >= 0) {
                suggestion = mSuggestions.get(mSuggestionFocussed);
            } else {
                mSuggestionFocussed = mSuggestions.size() - 1;
                suggestion = mSuggestions.get(mSuggestionFocussed);
            }
            if(mMyCandidatesView != null) {
                mMyCandidatesView.changeHighlightedSuggestion(mSuggestionFocussed);
            }
        }

        return suggestion;
    }


    /**
     * Method called from MyKeyboardView, originally came from MyGestureListener
     *
     */
    public void onSwipeDownGesture() {
        pickSuggestionManually(mSuggestionFocussed);
    }


    public void onLongPressGesture() {
        //mSkipKeyAfterLongpress = true;
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

