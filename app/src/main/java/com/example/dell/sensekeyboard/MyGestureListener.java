package com.example.dell.sensekeyboard;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by Dell on 16.7.2017.
 */

class MyGestureListener implements GestureDetector.OnGestureListener {
    private static final String DEBUG_TAG = "Gestures";


    @Override
    public boolean onDown(MotionEvent event) {
        Log.e("SenseKeyboard", "onDown: " + event.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.e("SenseKeyboard", "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.e("SenseKeyboard", "onSingleTapUp: " + event.toString());
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
        Log.e("SenseKeyboard", "onScroll: " + event1.toString()+event2.toString());
        return false;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.e("SenseKeyboard", "onLongPress: " + event.toString());
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        Log.e("SenseKeyboard", "onFling: " + event1.toString()+event2.toString());
        return true;
    }
}

