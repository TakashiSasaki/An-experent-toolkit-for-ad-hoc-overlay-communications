package jp.ks.quality;

import java.util.List;

import android.app.Activity;
import android.hardware.Sensor;    // 1.5
import android.hardware.SensorEvent;    // 1.5
import android.hardware.SensorEventListener;    // 1.5
//import android.hardware.SensorListener; // 1.1
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

public class SensorTestActivity extends Activity
    implements SensorEventListener { // 1.5
//    implements SensorListener { // 1.1
    
    private boolean mRegisteredSensor;
    private SensorManager mSensorManager;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mRegisteredSensor = false;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // 1.5
        {
            List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
            
            if (sensors.size() > 0) {
                Sensor sensor = sensors.get(0);
                mRegisteredSensor = mSensorManager.registerListener(this,
                    sensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
            }
        }
        
//        // 1.1
//        {
//            int sensors = mSensorManager.getSensors();
//            if ((sensors | SensorManager.SENSOR_ORIENTATION) != 0) {
//                mRegisteredSensor = mSensorManager.registerListener(this,
//                        SensorManager.SENSOR_ORIENTATION,
//                        SensorManager.SENSOR_DELAY_FASTEST);
//            }
//        }
    }

    @Override
    protected void onPause() {
        if (mRegisteredSensor) {
            mSensorManager.unregisterListener(this);
            mRegisteredSensor = false;
        }
        
        super.onPause();
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { // 1.5
//    public void onAccuracyChanged(int sensor, int accuracy) { // 1.1
    }

    @Override
    public void onSensorChanged(SensorEvent event) { // 1.5
//    public void onSensorChanged(int sensor, float[] values) { // 1.1

        // 1.5
        {
            if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                // values[0]:
                // Azimuth, angle between the magnetic north direction and the Y axis,
                // around the Z axis (0 to 359). 0=North, 90=East, 180=South, 270=West
                // values[1]:
                // Pitch, rotation around X axis (-180 to 180),
                // with positive values when the z-axis moves toward the y-axis.
                // values[2]:
                // Roll, rotation around Y axis (-90 to 90),
                // with positive values when the x-axis moves away from the z-axis.             
                Log.v("ORIENTATION",
                    String.valueOf(event.values[0]) + ", " +
                    String.valueOf(event.values[1]) + ", " +
                    String.valueOf(event.values[2]));
            }
        }
        
//        // 1.1
//        {
//            if (sensor == SensorManager.SENSOR_ORIENTATION) {
//                // values[0]:
//                // Azimuth, rotation around the Z axis (0<=azimuth<360).
//                // 0 = North, 90 = East, 180 = South, 270 = West
//                // values[1]:
//                // Pitch, rotation around X axis (-180<=pitch<=180),
//                // with positive values when the z-axis moves toward the y-axis.
//                // values[2]:
//                // Roll, rotation around Y axis (-90<=roll<=90),
//                // with positive values when the z-axis moves toward the x-axis.
//                Log.v("ORIENTATION",
//                    String.valueOf(values[0]) + ", " +
//                    String.valueOf(values[1]) + ", " +
//                    String.valueOf(values[2]));
//            }
//        }
    }
}