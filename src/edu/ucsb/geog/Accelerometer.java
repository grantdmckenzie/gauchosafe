package edu.ucsb.geog;

import java.util.HashMap;
import java.util.Observable;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class Accelerometer extends Observable implements SensorEventListener, Runnable, Fix
{

	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private long msInterval; 
	private HashMap<String, Double> fix;
	private double accelx = 0;
	private double accely = 0;
	private double accelz = 0;
	
	
	public Accelerometer(SensorManager mSensorManager, long msInterval) {
					
		this.mSensorManager = mSensorManager;
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		this.msInterval = msInterval;
		fix =  new HashMap<String, Double>();
		
	}
	
	public void startRecording()
	{
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	public void stopRecording()
	{
		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
		accelx = event.values[0];
		accely = event.values[1];
		accelz = event.values[2];			
	}
	

	
	@Override
	public void run() 
	{		
		while(true)
		{	
			
			fix.put("sensor", 1.0);	// Grant edit: for identifying which sensor it is.
			fix.put("accelx", accelx);
			fix.put("accely", accely);
			fix.put("accelz", accelz);
			setChanged();
			notifyObservers(fix);
			try 
			{
				Thread.sleep(msInterval);             
			} 
			catch (InterruptedException ex) 
			{
			}			
		}		
	}

	@Override
	public HashMap<String, Double> getFix() {
		return fix;
	}
}
