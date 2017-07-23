package com.example.dell.sensekeyboard;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by Dell on 16.7.2017.
 */

class MyGestureListener implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private static final String DEBUG_TAG = "Gestures";
    private static MyKeyboardView mMyKeyboardView;

    MyGestureListener(MyKeyboardView myKeyboardView) {
        this.mMyKeyboardView = myKeyboardView;
    }


    @Override
    public boolean onDown(MotionEvent event) {
        Log.d("SenseKeyboard", "onDown: " + event.toString());
        return false;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d("SenseKeyboard", "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d("SenseKeyboard", "onSingleTapUp: " + event.toString());
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        // vola se nekolikrat behem pohybu prstu po displeji
        Log.d("SenseKeyboard", "onScroll: " + event1.toString()+event2.toString());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.d("SenseKeyboard", "onLongPress: " + event.toString());
        mMyKeyboardView.onLongPressGesture();
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        // vola se na konci pohybu prstu po displeji
        Log.e("SenseKeyboard", "onFling: " + event1.toString()+event2.toString());
        mMyKeyboardView.onScrollRightGesture();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.e("SenseKeyboard", "onSingleTapConfirmed: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.e("SenseKeyboard", "onDoubleTap: " + event.toString());
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.e("SenseKeyboard", "onDoubleTapEvent: " + event.toString());
        return false;
    }
}

