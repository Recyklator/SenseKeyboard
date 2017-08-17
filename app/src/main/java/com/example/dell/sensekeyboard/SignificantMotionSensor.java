package com.example.dell.sensekeyboard;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class for Significant motion sensor manipulation - available since API 18 (4.3)
 */

public class SignificantMotionSensor implements SensorEventListener {

    // TODO: consider using semaphore protection !!
    private boolean mDeviceInMove; // this is variable we will provide using getter to the rest of world

    SenseKeyboardService mSenseKeyboardService; // TODO: find better way to actualize flag in main service task rather than taking whole class and call setter.

    private static final Integer SENSOR_DELAY = Integer.MAX_VALUE; // delays between events from sensor informing us about changes
    private static final Integer MOVEMENT_TURNOFF_TIMER = 120000; // 120s - timer between last movement detection and deviceInMove flag change to false
    private static final Integer MOVEMENT_CALCULATION_THRESHOLD = 4; // how intensive movement is classified as silence break
    private static final Integer SILENCE_BREAKS_THRESHOLD = 4; // how many silence breaks in short time needed to classify device as 'in move'
    private static final long MIN_PERIOD_BETWEEN_SILENCE_BREAKS = 2000; // 2s - all silence breaks in less than THIS millis from last one will be ignored
    private static final long MAX_PERIOD_BETWEEN_SILENCE_BREAKS = 15000; // 15s - if no silence break in THIS millis, silenceBreaksInRowCounter will be cleared
    private final SensorManager mSensorManager;
    private Integer silenceBreaksInRowCounter = 0;
    private Sensor mSensor = null;
    private Timer timer; //
    private long timeOfLastSilenceBreak = 1; // time in millis of last silence break

    // variables used for motion detection algorithm
    private float mAccel = 0.00f;
    private float mAccelCurrent = SensorManager.GRAVITY_EARTH;


    public SignificantMotionSensor(Context context, SenseKeyboardService senseKeyboardService) {
        Log.e("SenseKeyboard-SMSensor", "Significant Motion CONSTRUCTOR");

        mSenseKeyboardService = senseKeyboardService; // TEMPORARY
        setDeviceInMoveFlag(false); // lets initialize move flag to false since at beginning we have no info about device move
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // if we are on device with API 18 and above, we can try to look for system implementation of SMSensor, it should be more precise
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        }

        if(mSensor == null) {
            // significant motion sensor is not available on running device (either not implemented or lower API version), use accelerometer instead
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.i("SenseKeyboard", "onSensorChanged");

        if (event.sensor.getType() == Sensor.TYPE_SIGNIFICANT_MOTION) {
            // TODO: This part not tested yet since no phone had SMSensor

            // here we change move flag, because we received event that significant motion state changed
            setDeviceInMoveFlag(!getDeviceInMoveFlag()); // just flag negation in fact
        }
        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float[] mGravity = event.values.clone();
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            float mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x*x + y*y + z*z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            Log.e("SenseKeyboard", "x="+x+", y="+y+", z="+z+", mAccelCurrent="+mAccelCurrent+", delta="+delta+", mAccel="+mAccel);
            if(mAccel > MOVEMENT_CALCULATION_THRESHOLD) {
                // first of all, check when last threshold break happened (if not at least MIN_PERIOD_..., then ignore it)
                long currentTime = System.currentTimeMillis();
                if(currentTime < (timeOfLastSilenceBreak + MIN_PERIOD_BETWEEN_SILENCE_BREAKS)) {
                    return;
                }
                else if(currentTime > (timeOfLastSilenceBreak + MAX_PERIOD_BETWEEN_SILENCE_BREAKS)) {
                    silenceBreaksInRowCounter = 0; // reset silence breaks counter, because more than MAX_PERIOD... since last break
                }
                timeOfLastSilenceBreak = currentTime;

                silenceBreaksInRowCounter++;
                Log.e("SenseKeyboard-SMSensor", "silence break in row:"+silenceBreaksInRowCounter);

                if(silenceBreaksInRowCounter >= SILENCE_BREAKS_THRESHOLD) {
                    silenceBreaksInRowCounter = 0; // reset counter

                    setDeviceInMoveFlag(true); // now device is considered to be in move

                    DropDeviceInMoveFlag dropDeviceInMoveFlag = new DropDeviceInMoveFlag(this);
                    if(timer != null) {
                        timer.cancel();
                    }
                    timer = new Timer();
                    // device in move flag set, lets create timer for its dropdown after some period of silent time
                    timer.schedule(dropDeviceInMoveFlag, MOVEMENT_TURNOFF_TIMER);
                }
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.e("SenseKeyboard-SMSensor", "onAccuracyChanged");
    }


    public void register() {
        Log.e("SenseKeyboard-SMSensor", "Significant Motion registered, available sensors:"+String.valueOf(mSensorManager.getSensorList(-1)));
        mSensorManager.registerListener(this, mSensor, SENSOR_DELAY);
    }


    public boolean getDeviceInMoveFlag() {
        return mDeviceInMove;
    }

    public void setDeviceInMoveFlag(boolean deviceInMove) {
        mDeviceInMove = deviceInMove;
        Log.i("SenseKeyboard-SMSensor", "device move flag changed to:"+mDeviceInMove);
        mSenseKeyboardService.setDeviceInMoveFlag(mDeviceInMove);
    }

    public void resetDeviceInMoveFlag() {
        Log.e("SenseKeyboard-SMSensor", "resetDeviceInMoveFlag setting flag to FALSE");
        setDeviceInMoveFlag(false);
    }

}


class DropDeviceInMoveFlag extends TimerTask {

    private SignificantMotionSensor mSignificantMotionSensor;

    public DropDeviceInMoveFlag(SignificantMotionSensor significantMotionSensor) {
        mSignificantMotionSensor = significantMotionSensor;
    }

    @Override
    public void run() {
        mSignificantMotionSensor.resetDeviceInMoveFlag();
    }

}


/*@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class TriggerListener extends TriggerEventListener {
    public void onTrigger(TriggerEvent event) {
        Log.e("SenseKeyboard", "Significant Motion detected!!");
    }
}*/



/**
 * Class for Significant motion sensor manipulation - available since API 18 (4.3)
 */
/*public class SignificantMotionSensor extends Activity {

    private SensorManager mSensorManager;
    private Sensor mSigMotion;
    private TriggerEventListener mTriggerEventListener;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e("SenseKeyboard", "Significant Motion onCreate()");
    }


    @Override
    public void onStart() {
        super.onStart();

        Log.e("SenseKeyboard", "Significant Motion onStart()");
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.e("SenseKeyboard", "Significant Motion trigger started");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mSensorManager.requestTriggerSensor(mTriggerEventListener, mSigMotion);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.e("SenseKeyboard", "Significant Motion trigger Paused");
        // Call disable to ensure that the trigger request has been canceled.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mSensorManager.cancelTriggerSensor(mTriggerEventListener, mSigMotion);
        }
    }


    public void initialize() {
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSigMotion = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

        // TriggerEventListener feature is available since API 18 (4.3)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // lets create instance of our own class, which extends TrigerEventListener() so we could put our own event handle code there
            mTriggerEventListener = new TriggerListener();
        }
    }


    public void activate() {
        Log.e("SenseKeyboard", "Significant Motion activate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mSensorManager.requestTriggerSensor(mTriggerEventListener, mSigMotion);
        }
    }

}*/