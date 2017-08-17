package com.example.dell.sensekeyboard;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;

/**
 * Created by Dell on 7.7.2017.
 */
public class MyKeyboardView extends KeyboardView {

    private static final int KEYCODE_DEL = -5;
    private static final int KEYCODE_SPACE = 32;
    private static final int KEYCODE_ENTER = -4;

    private AccessibilityManager mAccessibilityManager;
    private AccessibilityNodeProvider mAccessibilityNodeProvider;
    private GestureDetector mGestureDetector;
    private SenseKeyboardService mSenseKeyboardService;
    private Integer mLastFocusedKeyCode;
    private Integer mLastReportedCode;
    private Boolean mScrollGestureInProgress;
    private Boolean mLongPressInProgress;
    private Timer mLongPressTimer;

    public MyKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mScrollGestureInProgress = false;
        mLongPressInProgress = false;

        mLongPressTimer = new Timer();

        // Create an object of our Custom Gesture Detector Class
        MyGestureListener myGestureListener = new MyGestureListener(this);
        // Create a GestureDetector
        mGestureDetector = new GestureDetector(this.getContext(), myGestureListener);
        // Attach listeners that'll be called for double-tap and related gestures
        //mGestureDetector.setOnDoubleTapListener(customGestureDetector);
        mSenseKeyboardService = (SenseKeyboardService) context;
        mAccessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    public void setKeyboardService(SenseKeyboardService senseKeyboardService) {
        //mSenseKeyboardService = senseKeyboardService;
    }

   /*
    * (API Level 4) The system calls this method when your custom view generates an accessibility event.
    * As of API Level 14, the default implementation of this method calls onPopulateAccessibilityEvent()
    * for this view and then the dispatchPopulateAccessibilityEvent() method for each child of this view.
    * In order to support accessibility services on revisions of Android prior to 4.0 (API Level 14) you
    * must override this method and populate getText() with descriptive text for your custom view,
    * which is spoken by accessibility services, such as TalkBack.
    */
    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // called after onPopulateAccessibilityEvent(), if I add text here, it will be attached at the end of accessibility notification

        //event.getText().add("any attached string");

        // Call the super implementation to populate its text to the event, which
        // calls onPopulateAccessibilityEvent() on API Level 14 and up.
        boolean completed = super.dispatchPopulateAccessibilityEvent(event);

        return completed;
    }


    /*
     * (API Level 14) This method sets the spoken text prompt of the AccessibilityEvent for your view.
     * This method is also called if the view is a child of a view which generates an accessibility event.
     */
    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        // called when I manually select item from autocomplete menu
        super.onPopulateAccessibilityEvent(event);

        int mActiveSelection = 5;
        int SELECTION_COUNT = 3;

        // Detect what type of accessibility event is being passed in.
        int eventType = event.getEventType();

        // Common case: The user has interacted with our view in some way. State may or may not
        // have been changed. Read out the current status of the view.
        //
        // We also set some other metadata which is not used by TalkBack, but could be used by
        // other TTS engines.
        if (eventType == AccessibilityEvent.TYPE_VIEW_SELECTED ||
                eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
            event.getText().add("Mode selected: " + Integer.toString(mActiveSelection + 1));
            event.setItemCount(SELECTION_COUNT);
            event.setCurrentItemIndex(mActiveSelection);
        }

        // When a user first focuses on our view, we'll also read out some simple instructions to
        // make it clear that this is an interactive element.
        if (eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
            event.getText().add("Tap to change");
        }
    }


    /*
     * (API Level 14) The system calls this method to obtain additional information about the state of the view,
     * beyond text content. If your custom view provides interactive control beyond a simple TextView or Button,
     * you should override this method and set the additional information about your view into the event using this method,
     * such as password field type, checkbox type or states that provide user interaction or feedback.
     * If you do override this method, you must call its super implementation and then only modify
     * properties that have not been set by the super class.
     */
    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
    }


    /*
     * (API Level 14) This method provides accessibility services with information about the state of the view.
     * The default View implementation has a standard set of view properties, but if your custom view provides
     * interactive control beyond a simple TextView or Button, you should override this method and set the
     * additional information about your view into the AccessibilityNodeInfo object handled by this method.
     */
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        Boolean handledByGestureDetector = this.mGestureDetector.onTouchEvent(event);

        try {
            sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(!handledByGestureDetector && !mScrollGestureInProgress) {
            // Listening for the down and up touch events
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d("SenseKeyboard", "MyKeyboardView onTouchEvent() DOWN");
                    mLongPressTimer = new Timer();
                    mLongPressTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mSenseKeyboardService.setIsLongPress(true);
                        }
                    }, 2000); // delay of 2s
                    //handleMotionEventForAccessibility(event);
                    return true;

                case MotionEvent.ACTION_UP:
                    //Log.d("SenseKeyboard", "MyKeyboardView onTouchEvent() UP");
                    mLongPressTimer.cancel();
                    mLongPressTimer.purge();
                    return true;

                case MotionEvent.ACTION_HOVER_MOVE:
                    //Log.d("SenseKeyboard", "MyKeyboardView onTouchEvent() HOVER_MOVE");
                    return true;

                case MotionEvent.ACTION_MOVE:
                    //Log.d("SenseKeyboard", "MyKeyboardView onTouchEvent() MOVE");

                    boolean newCharHandled = handleMotionEventForAccessibility(event);
                    if(newCharHandled) {
                        // even if there was long press detected on previous key, clear the flag, because we moved to another key (explore by touch)
                        mSenseKeyboardService.setIsLongPress(false);
                        mLongPressTimer.cancel();
                        mLongPressTimer.purge();
                        mLongPressTimer = new Timer();
                        mLongPressTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                mSenseKeyboardService.setIsLongPress(true);
                            }
                        }, 2000); // delay of 3s
                    }
                    return true;

                default:
                    return false; // Return false for other touch events
            }
        }
        return true;
    }


    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {

        if (mAccessibilityNodeProvider == null) {

            mAccessibilityNodeProvider = new AccessibilityNodeProvider() {
                public boolean performAction(int action, int virtualDescendantId, Bundle bundle) {
                    Log.d("SenseKeyboard", "MyKeyboardView performAction()");
                    // Implementation.
                    return false;
                }

                public List findAccessibilityNodeInfosByText(String text, int virtualDescendantId) {
                    Log.d("SenseKeyboard", "MyKeyboardView findAccessibilityNodeInfosByText()");
                    // Implementation.
                    return null;
                }

                public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualDescendantId) {
                    Log.d("SenseKeyboard", "MyKeyboardView createAccessibilityNodeInfo()");
                    AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityNodeInfo.obtain();
                    return accessibilityNodeInfo;
                }

                public AccessibilityNodeInfo findFocus(int focus) {
                    AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityNodeInfo.obtain();
                    return accessibilityNodeInfo;
                }
            };
        }
        return mAccessibilityNodeProvider;
    }


    public void onLongPressGesture() {
        mSenseKeyboardService.onLongPressGesture();
    }


    public void onSwipeRightGesture() {
        String suggestion = mSenseKeyboardService.onSwipeRightGesture();
        sendAccessibilityText(suggestion);
    }


    public void onSwipeLeftGesture() {
        String suggestion = mSenseKeyboardService.onSwipeLeftGesture();
        sendAccessibilityText(suggestion);
    }


    public void onSwipeDownGesture() {
        mSenseKeyboardService.onSwipeDownGesture();
    }


    public void setGestureInProgressFlag(Boolean gestureInProgressFlag) {
        if(mScrollGestureInProgress != gestureInProgressFlag) {
            Log.e("SenseKeyboard", "MyKeyboardView setGestureInProgressFlag(gestureInProgressFlag=" + gestureInProgressFlag + ")");
            mScrollGestureInProgress = gestureInProgressFlag;
        }
    }

    public Boolean isScrollGestureInProgress() {
        return mScrollGestureInProgress;
    }


    public void sendAccessibilityText(Integer keyCode) {
        String accessibilityString;

        if(keyCode == KEYCODE_DEL) {
            accessibilityString = "smazat";
        } else if(keyCode == KEYCODE_ENTER) {
            accessibilityString = "entr";
        } else if(keyCode == KEYCODE_SPACE) {
            accessibilityString = "mezerník";
        } else {
            // cast keyCode Integer to int, then to char and then make String from it
            accessibilityString = String.valueOf((char)(int) keyCode);
        }
        sendAccessibilityText(accessibilityString);
    }


    public void sendAccessibilityText(String accessibilityText) {

        if (mAccessibilityManager.isEnabled() && accessibilityText != null && !accessibilityText.isEmpty()) {
            Log.e("SenseKeyboard", "MyKeyboardView setAccessibilityText(accessibilityText="+accessibilityText+")");
            AccessibilityEvent e = AccessibilityEvent.obtain();
            e.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            e.setClassName(getClass().getName());
            e.setPackageName(this.getContext().getPackageName());
            e.getText().add(accessibilityText);
            mAccessibilityManager.sendAccessibilityEvent(e);
        }
        /*if(accessibilityText != null && !accessibilityText.isEmpty()) {
            Log.e("SenseKeyboard", "MyKeyboardView setAccessibilityText(accessibilityText="+accessibilityText+")");
            super.announceForAccessibility(accessibilityText);
        }*/
    }


    private Integer extractKeyCodeFromMotionEvent(MotionEvent event) {

        Integer code = mLastFocusedKeyCode; // we will return last focused key in case that we were unable to detect recently focused key
        float fx = event.getX();
        float fy = event.getY();
        int x = Math.round(fx);
        int y = Math.round(fy);

        Keyboard keyboard = super.getKeyboard();
        List<Keyboard.Key> keysList = keyboard.getKeys();
        for(Keyboard.Key key : keysList) {
            if(key.isInside(x, y)) {
                code = key.codes[0];
                break;
            }
        }
        return code;
    }


    /**
     * Function
     * @param event
     * @return true if new char was focused and new accessibility text was sent, false otherwise
     */
    private boolean handleMotionEventForAccessibility(MotionEvent event) {
        boolean newCodeReported = false;
        Integer extractedKeyCode = extractKeyCodeFromMotionEvent(event);
        setLastFocusedKeyCode(extractedKeyCode);

        if(!isFocusedCodeEqualToReported()){
            sendAccessibilityText(getLastFocusedKeyCode());
            setLastReportedCode(getLastFocusedKeyCode());
            newCodeReported = true;
        }
        return newCodeReported;
    }


    private Integer getLastFocusedKeyCode() {
        return mLastFocusedKeyCode;
    }


    private void setLastFocusedKeyCode(Integer lastFocusedKeyCode) {
        //Log.d("SenseKeyboard", "MyKeyboardView setLastFocusedKeyCode(lastFocusedKeyCode = "+lastFocusedKeyCode+")");
        mLastFocusedKeyCode = lastFocusedKeyCode;
    }


    private Integer getLastReportedCode() {
        return mLastReportedCode;
    }


    private void setLastReportedCode(Integer lastReportedCode) {
        mLastReportedCode = lastReportedCode;
    }


    private Boolean isFocusedCodeEqualToReported() {
        return mLastFocusedKeyCode == mLastReportedCode;
    }

}