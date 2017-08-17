package com.example.dell.sensekeyboard;

import java.util.TimerTask;

/**
 * Created by Dell on 5.7.2017.
 * Class NOT used at this moment !!
 */
public class KeyPressTimerTask extends TimerTask {

    private final int mPrimaryCode;
    private final int[] mKeyCodes;
    private final SenseKeyboardService mSenseKeyboardService;

    KeyPressTimerTask(int primaryCode, int[] keyCodes, SenseKeyboardService senseKeyboardService) {
        this.mPrimaryCode = primaryCode;
        this.mKeyCodes = keyCodes;
        this.mSenseKeyboardService = senseKeyboardService;
    }

    public void run() {
        mSenseKeyboardService.handleCharacter(mPrimaryCode, mKeyCodes);
    }
}