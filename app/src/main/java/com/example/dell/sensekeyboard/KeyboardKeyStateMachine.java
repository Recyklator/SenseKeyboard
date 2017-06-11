package com.example.dell.sensekeyboard;

import android.inputmethodservice.Keyboard;

/**
 * Created by Dell on 9.6.2017.
 */

public class KeyboardKeyStateMachine {

    public static final int STATE_INITIALIZED = 0;
    public static final int STATE_FIRST_CHAR_OF_BUTTON_PRESSED = 1;
    public static final int STATE_NON_FIRST_CHAR_OF_BUTTON_PRESSED = 2;
    public static final int STATE_DELETE_KEY_PRESSED = 3;
    public static final int STATE_CORRUPTED = 9;

    private int currentState;

    private int[] states = {STATE_INITIALIZED, STATE_FIRST_CHAR_OF_BUTTON_PRESSED,
            STATE_NON_FIRST_CHAR_OF_BUTTON_PRESSED, STATE_DELETE_KEY_PRESSED, STATE_CORRUPTED};

    private int[][] transition = {
            // STATE_INITIALIZED
            {STATE_CORRUPTED, STATE_FIRST_CHAR_OF_BUTTON_PRESSED, STATE_CORRUPTED, STATE_DELETE_KEY_PRESSED, -1, -1, -1, -1, -1, STATE_CORRUPTED},
            // STATE_FIRST_CHAR_OF_BUTTON_PRESSED
            {STATE_CORRUPTED, STATE_FIRST_CHAR_OF_BUTTON_PRESSED, STATE_NON_FIRST_CHAR_OF_BUTTON_PRESSED, STATE_DELETE_KEY_PRESSED, -1, -1, -1, -1, -1, STATE_CORRUPTED},
            // STATE_NON_FIRST_CHAR_OF_BUTTON_PRESSED
            {STATE_CORRUPTED, STATE_FIRST_CHAR_OF_BUTTON_PRESSED, STATE_NON_FIRST_CHAR_OF_BUTTON_PRESSED, STATE_DELETE_KEY_PRESSED, -1, -1, -1, -1, -1, STATE_CORRUPTED},
            // STATE_DELETE_KEY_PRESSED
            {STATE_CORRUPTED, STATE_FIRST_CHAR_OF_BUTTON_PRESSED, STATE_CORRUPTED, STATE_DELETE_KEY_PRESSED, -1, -1, -1, -1, -1, STATE_CORRUPTED},
            // state not implemented
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            // state not implemented
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            // state not implemented
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            // state not implemented
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            // state not implemented
            {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
            // STATE_CORRUPTED - lets implement state transitions to not let the app freeze, if it occurs in wrong state
            {STATE_INITIALIZED, STATE_FIRST_CHAR_OF_BUTTON_PRESSED, STATE_NON_FIRST_CHAR_OF_BUTTON_PRESSED, STATE_DELETE_KEY_PRESSED, -1, -1, -1, -1, -1, STATE_CORRUPTED}
    };

    KeyboardKeyStateMachine() {
        currentState = STATE_INITIALIZED;
    }

    public void handleNewKey(int keyPressedCode) {
        int newStateCode;

        switch(keyPressedCode) {
            case Keyboard.KEYCODE_DELETE:
                newStateCode = STATE_DELETE_KEY_PRESSED;
                break;

            case 44: // ","
            case 97: // a
            case 100: // d
            case 103: // g
            case 106: // j
            case 109: // m
            case 112: // p
            case 116: // t
            case 119: // w
            case 32: // space
            case -4: // enter
                newStateCode = STATE_FIRST_CHAR_OF_BUTTON_PRESSED;
                break;

            default: // here should be the error state, but to avoid enumeration of all non-first characters of all buttons, we used this way
                newStateCode = STATE_NON_FIRST_CHAR_OF_BUTTON_PRESSED;
                break;
        }
        changeState(newStateCode);
    }

    private void changeState(int newStateCode) {
        currentState = transition[currentState][newStateCode];
    }
}

/*class FSM {
    // 2. states
    private State[] states = {new A(), new B(), new C()};
    // 4. transitions
    private int[][] transition = {{2,1,0}, {0,2,1}, {1,2,2}};
    // 3. current
    private int current = 0;

    private void next(int msg) {
        current = transition[current][msg];
    }

    // 5. All client requests are simply delegated to the current state object
    public void on() {
        states[current].on();
        next(0);
    }

    public void off() {
        states[current].off();
        next(1);
    }

    public void ack() {
        states[current].ack();
        next(2);
    }
}

abstract class State {
    public void on() {
        System.out.println("error");
    }

    public void off() {
        System.out.println("error");
    }

    public void ack() {
        System.out.println("error");
    }
}

class A extends State {
    public void on() {
        System.out.println("A + on  = C");
    }

    public void off() {
        System.out.println("A + off = B");
    }

    public void ack() {
        System.out.println("A + ack = A");
    }
}

class B extends State {
    public void on() {
        System.out.println("B + on  = A");
    }

    public void off() {
        System.out.println("B + off = C");
    }
}

class C extends State {
    // 8. The State derived classes only override the messages they need to
    public void on() {
        System.out.println("C + on  = B");
    }
}

public class StateDemo {
    public static void main(String[] args) {
        FSM fsm = new FSM();
        int[] msgs = {2, 1, 2, 1, 0, 2, 0, 0};
        for (int msg : msgs) {
            if (msg == 0) {
                fsm.on();
            } else if (msg == 1) {
                fsm.off();
            } else if (msg == 2) {
                fsm.ack();
            }
        }
    }
}*/