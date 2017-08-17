package com.example.dell.sensekeyboard;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by Dell on 16.7.2017.
 */

class MyGestureListener implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private static final String DEBUG_TAG = "SenseKeyboard";
    private static MyKeyboardView mMyKeyboardView;

    MyGestureListener(MyKeyboardView myKeyboardView) {
        this.mMyKeyboardView = myKeyboardView;
    }


    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDown: " + event.toString());
        return false;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        // it is called several times during finger move over display
        Log.d(DEBUG_TAG, "onScroll: " + event1.toString()+event2.toString());
        mMyKeyboardView.setGestureInProgressFlag(true);

        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
        //mMyKeyboardView.onLongPressGesture();
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        // it is called at the end of finger move over display
        Log.d(DEBUG_TAG, "onFling: " + event1.toString());
        Log.d(DEBUG_TAG, "onFling: " + event2.toString());
        Log.d(DEBUG_TAG, "onFling velocityX: " + velocityX + " velocityY: " + velocityY);

        float absVelocityX = Math.abs(velocityX);
        float absVelocityY = Math.abs(velocityY);

        if(velocityX > 0 && absVelocityX > absVelocityY) {
            mMyKeyboardView.onSwipeRightGesture();
        } else if(velocityX < 0 && absVelocityX > absVelocityY) {
            mMyKeyboardView.onSwipeLeftGesture();
        } else if(velocityY > 0 && absVelocityX < absVelocityY) {
            mMyKeyboardView.onSwipeDownGesture();
        } else if(velocityY < 0 && absVelocityX < absVelocityY) {
            //mMyKeyboardView.onSwipeUpGesture();
        }

        mMyKeyboardView.setGestureInProgressFlag(false);

        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return false;
    }
}

