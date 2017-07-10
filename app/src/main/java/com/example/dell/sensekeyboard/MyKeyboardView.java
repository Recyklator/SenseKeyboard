package com.example.dell.sensekeyboard;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Dell on 7.7.2017.
 */

public class MyKeyboardView extends KeyboardView {

    private AccessibilityNodeProvider mAccessibilityNodeProvider;
    //private Keyboard.Key key;
    private String mLastFocusedKeyCode;
    private String mLastReportedCode;

    public MyKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mLastFocusedKeyCode = "";
        mLastReportedCode = "";
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
        // volá se po onPopulateAccessibilityEvent(), "bazingaaa" se přidá na konec zvukového výstupu

        // Call the super implementation to populate its text to the event, which
        // calls onPopulateAccessibilityEvent() on API Level 14 and up.
        boolean completed = super.dispatchPopulateAccessibilityEvent(event);

        event.getText().add("bazingaaa");

        return completed;
    }


    /*
     * (API Level 14) This method sets the spoken text prompt of the AccessibilityEvent for your view.
     * This method is also called if the view is a child of a view which generates an accessibility event.
     */
    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        // volá se když ručně vyberu z nabídky našeptávače
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
        // Add a record for ourselves as well.
        AccessibilityEvent record = AccessibilityEvent.obtain();

        super.onInitializeAccessibilityEvent(event);
        //event.setPassword(true);
        if(event.getEventType() == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER && !isFocusedCodeEqualToReported()) {


            /*event.getText().add(getFocusedKeyString());
            event.setBeforeText(getFocusedKeyString());
            event.setContentDescription(getFocusedKeyString());
            //event.setAction(AccessibilityNodeInfo.ACTION_FOCUS); // no effect
            record.setContentDescription("maslo");
            record.getText().add("sadlo");
            event.appendRecord(record);*/

            /*super.announceForAccessibility(getLastFocusedKeyCode());
            setLastReportedCode(getLastFocusedKeyCode());*/
        }
    }


    /*
     * (API Level 14) This method provides accessibility services with information about the state of the view.
     * The default View implementation has a standard set of view properties, but if your custom view provides
     * interactive control beyond a simple TextView or Button, you should override this method and set the
     * additional information about your view into the AccessibilityNodeInfo object handled by this method.
     */
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        info.setText("Gool");
        info.setContentDescription("DvaGoly");
        super.onInitializeAccessibilityNodeInfo(info);
        info.setText("Gool");
        info.setContentDescription("DvaGoly");
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        // Listening for the down and up touch events
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e("SenseKeyboard", "MyKeyboardView onTouchEvent() DOWN");
                return true;

            case MotionEvent.ACTION_UP:
                Log.e("SenseKeyboard", "MyKeyboardView onTouchEvent() UP");
                return true;

            case MotionEvent.ACTION_HOVER_MOVE:
                Log.e("SenseKeyboard", "MyKeyboardView onTouchEvent() HOVER_MOVE");
                return true;

            case MotionEvent.ACTION_MOVE:
                //Log.d("SenseKeyboard", "MyKeyboardView onTouchEvent() MOVE");
                float fx = event.getX();
                float fy = event.getY();
                int x = Math.round(fx);
                int y = Math.round(fy);

                Keyboard keyboard = super.getKeyboard();
                List<Keyboard.Key> keysList = keyboard.getKeys();
                for(Keyboard.Key key : keysList) {
                    if(key.isInside(x, y)) {
                        char code = (char)key.codes[0];
                        setLastFocusedKeyCode(String.valueOf(code));
                        break;
                    }
                }
                if(!isFocusedCodeEqualToReported()){
                    super.announceForAccessibility(getLastFocusedKeyCode());
                    setLastReportedCode(getLastFocusedKeyCode());
                }
                /*Log.e("SenseKeyboard", "MyKeyboardView announceForAccessibility():"+getFocusedKeyCode());
                super.announceForAccessibility(getFocusedKeyCode());
                setLastReportedCode(getFocusedKeyCode());*/
                return true;
        }
        return false; // Return false for other touch events
    }


    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {

        //if (mAccessibilityNodeProvider == null) {

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
                    Log.i("SenseKeyboard", "MyKeyboardView createAccessibilityNodeInfo()");
                    AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityNodeInfo.obtain();
                    /*accessibilityNodeInfo.setContentDescription(getFocusedKeyString());
                    accessibilityNodeInfo.setText(getFocusedKeyString());*/
                    //accessibilityNodeInfo.
                    /*Bundle arguments = new Bundle();
                    arguments.putInt(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, 1);
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, arguments);*/

                    return accessibilityNodeInfo;
                }

                public AccessibilityNodeInfo findFocus(int focus) {
                    AccessibilityNodeInfo accessibilityNodeInfo = AccessibilityNodeInfo.obtain();
                    return accessibilityNodeInfo;
                }
            };
        //}
        return mAccessibilityNodeProvider;
    }


    private String getLastFocusedKeyCode() {
        return mLastFocusedKeyCode;
    }

    private void setLastFocusedKeyCode(String lastFocusedKeyCode) {
        Log.e("SenseKeyboard", "MyKeyboardView setLastFocusedKeyCode(lastFocusedKeyCode = "+lastFocusedKeyCode+")");
        mLastFocusedKeyCode = lastFocusedKeyCode;
    }

    private String getLastReportedCode() {
        return mLastReportedCode;
    }

    private void setLastReportedCode(String lastReportedCode) {
        mLastReportedCode = lastReportedCode;
    }

    private Boolean isFocusedCodeEqualToReported() {
        return mLastFocusedKeyCode.equals(mLastReportedCode);
    }

}