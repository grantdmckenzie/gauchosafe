package edu.ucsb.geog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.UUID;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AcclThread extends Observable implements Runnable, SensorEventListener
{
	
	 private Context context;
	 private SensorManager mSensorManager;
	 private Sensor mAccelerometer;
	 private Vector<JSONObject> fixes;
	  
	 private static final String PREFERENCE_NAME = "ucsbprefs";
	 private SharedPreferences appSharedPrefs;
	 private Editor prefsEditor;
	 private int filenum;
	 private String deviceId;
	 private TelephonyManager tm;
	 private BurstSD burstSD;
	 private double standardDeviation;
	 private double callibrationSD;
	 private double callibrationMean;
	 private float sddif;
	 private WakeLock wakeLock;
	 public WifiManager wifiManager;
	 // private BroadcastReceiver wifiReceiver;
	 private JSONObject prevfix;
	 private double vecLength;
	 private ArrayList<Double> previousVector;
	 private int fixcount;
	 private SimpleDateFormat simpleDateFormat;
	 public boolean stationarityChanged;
	 public boolean stationary;
	  
	  public AcclThread(Context context)
	  {
		 
			
		  //Log.v("AcclThread", "Constructor"); 
		  this.context = context;
		  tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		  String tmDevice, tmSerial, androidId;
		  tmDevice = "" + tm.getDeviceId();
		  tmSerial = "" + tm.getSimSerialNumber();
		  androidId = "" + android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
		  UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
		  deviceId = deviceUuid.toString();
		  		  
		  mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		  mAccelerometer = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
		  fixes = new Vector<JSONObject>();		  
		  if(wakeLock == null)
		  {
		       PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		       wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "tag");	        
		  }
		  
		  // Get Calibration SD from Shared Preferences
		  this.appSharedPrefs = context.getSharedPreferences("edu.ucsb.geog", Context.MODE_WORLD_READABLE);
	      this.prefsEditor = appSharedPrefs.edit();
	      
		  this.callibrationSD = appSharedPrefs.getFloat("callibrationSD", -99);
		  this.callibrationMean = appSharedPrefs.getFloat("callibrationMean", -99);
		  this.previousVector = new ArrayList<Double>(3);
		  this.vecLength = 0;
		  
		  // prevfix = null;
		  this.fixcount = 0;
		  // Log.v("AcclThread", "Constructor END"); 
		  simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	  }
	  
	   
	  @Override
	  public void onAccuracyChanged(Sensor sensor, int accuracy) 
	  {
		
	  }

	  	@Override
	  public void onSensorChanged(SensorEvent event) 
	  {	  		
	  		JSONObject fix = new JSONObject();
	   		
		  	  try 
		  	  {
		  		  fix.put("sensor", 1.0);
		  		  fix.put("accelx", event.values[0]);
		  		  fix.put("accely", event.values[1]);
		  		  fix.put("accelz", event.values[2]);
		  		  String dateString = simpleDateFormat.format(new Date(System.currentTimeMillis()));
		  		  fix.put("ts", dateString);
		  		  
		  	  }
		  	  catch (Exception e) 
		  	  {
		  		  e.printStackTrace();
		  	  }
		  	  
		  	 
		  	  fixes.add(fix);
	
		  	  if(fixes.size()==30) 
		  	  {
		  		mSensorManager.unregisterListener(this);
		  		
		  		BurstSD thisBurstSD = new BurstSD(fixes);
				double thisSD = thisBurstSD.getSD();
				
				boolean isStationary = true;
				double sdDiff = Math.abs(thisSD);
				Log.v("SD difference", sdDiff+"");
				
				if(sdDiff>6.0)
					isStationary = false;
		  		
				if(isStationary)
				{
					if(appSharedPrefs.getBoolean("stationary", true))
					{
						returnStatus(false,true);
						//Log.v("AccelService","STATIONARY => STATIONARY");
					}
					else
					{
						prefsEditor.putBoolean("stationary", true);
						returnStatus(true,true);
						//Log.v("AccelService","ACTIVE => STATIONARY");
					}
				}
				else
				{
					if(appSharedPrefs.getBoolean("stationary", true))
					{
						prefsEditor.putBoolean("stationary", false);
						returnStatus(true,false);
						//Log.v("AccelService","ACTIVE => STATIONARY");
						
					}
					else
					{
						returnStatus(false,false);
						//Log.v("AccelService","ACTIVE => ACTIVE");
					}
					/* Log.v("AccelThread", "LightOn: "+appSharedPrefs.getBoolean("lighton", false));
					if(appSharedPrefs.getInt("lighton", 1) == 1) {
						Camera camera = Camera.open();
						Parameters p = camera.getParameters();
						p.setFlashMode(Parameters.FLASH_MODE_TORCH);
						camera.setParameters(p);
						camera.startPreview();
						
						p.setFlashMode(Parameters.FLASH_MODE_TORCH);
						camera.setParameters(p);
						camera.stopPreview();
						
						p.setFlashMode(Parameters.FLASH_MODE_TORCH);
						camera.setParameters(p);
						camera.startPreview();
						
						p.setFlashMode(Parameters.FLASH_MODE_TORCH);
						camera.setParameters(p);
						camera.stopPreview();
						camera.release();
						prefsEditor.putInt("lighton", 2);
						prefsEditor.commit();
					} */
				}
				
		  	  }
	  } 
	  
	  	

	  		
	 
		@Override
		public void run() 
		{
			wakeLock.acquire();
			mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);	
		}
		
		// Input: Has stationarity changed?  Are we stationary?
		private void returnStatus(boolean changed, boolean stationary) {
			  	
			 this.stationarityChanged = changed;
			 this.stationary = stationary;
			
			 // store state
	  		  prefsEditor.commit();  
	  		  setChanged();
			  notifyObservers();
			
			  // unregister listener
	  		 // mSensorManager.unregisterListener(this);
	  		  try {
	  			  if(wakeLock.isHeld()) {
	  				wakeLock.release();
	  			  }
	  		    } catch (Exception e) {
	  		    	e.printStackTrace();
	  		    }
		}
		
}
