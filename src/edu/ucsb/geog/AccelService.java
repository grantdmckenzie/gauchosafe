package edu.ucsb.geog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;

import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;

public class AccelService extends Service implements SensorEventListener
{
  private SensorManager mSensorManager;
  private Sensor mAccelerometer;
  private static long msInterval = 45000;
  private Vector<JSONObject> fixes;
  
  private static final String PREFERENCE_NAME = "ucsbprefs";
  private int filenum;
  private String deviceId;
  private TelephonyManager tm;
  private Timer timer;

 
  
  
  public void onCreate() 
  {	 
	  showNotification();
	  
	  	tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String tmDevice, tmSerial, androidId;
		tmDevice = "" + tm.getDeviceId();
		tmSerial = "" + tm.getSimSerialNumber();
		androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
		UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
		deviceId = deviceUuid.toString();
		  	  
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
	    fixes = new Vector<JSONObject>();
	    
	    //timer = new Timer();
	        
  }
  
  public int onStartCommand(Intent intent, int flags, int startId) 
  {	 
	 mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);	
	 
	 return START_STICKY;
  }
  
  

  
  @Override
  public void onDestroy() 
  {
	  mSensorManager.unregisterListener(this);
	  timer.cancel();
	  stopForeground(true); 
	  
	  
  }
  
  @Override
  public IBinder onBind(Intent intent) 
  {
    return(null);
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
  			fix.put("ts", new Long(System.currentTimeMillis()/1000));
		}
  		catch (Exception e) 
  		{
			
		}
  		fixes.add(fix);
  		Log.v("Vector Size", "Vector Size: "+ fixes.size());
  		
  		if(fixes.size()==50)
  		{
  			mSensorManager.unregisterListener(this);
  			
  			writeToFile();
  			
  			SystemClock.sleep(msInterval);
  			
  			mSensorManager.registerListener(AccelService.this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);		
  			
//  			timer.schedule(new TimerTask() {
//				public void run() {
//					mSensorManager.registerListener(AccelService.this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);					
//				}
//			}, msInterval);
  			                 			
  		}
	
  	}
  	
  	
	private void showNotification()
	{
		 Notification note=new Notification(R.drawable.iconnotification, getText(R.string.accel_started), System.currentTimeMillis());
	     
		 Intent notifyIntent = new Intent(Intent.ACTION_MAIN);
	     notifyIntent.setClass(getApplicationContext(), UCSBActivityTrackerActivity.class);
	     notifyIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	     
	     PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_CANCEL_CURRENT);
	     
	     note.setLatestEventInfo(this, getText(R.string.local_service_label), getText(R.string.accel_started), contentIntent);
	 	
	     note.flags|=Notification.FLAG_NO_CLEAR;

	     startForeground(1337, note);
	     	     
	}
	
	private void writeToFile() {
		Vector<JSONObject> fixVector2 = fixes;
		fixes = new Vector<JSONObject>();
		SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, MODE_WORLD_READABLE);
		filenum = settings.getInt("ucsb_filenum", 0);
		File logFile = new File("sdcard/ucsbat_"+deviceId+"-"+filenum+".log");
		Log.v("Path to file", "Path to file (service): "+logFile);
	   if (!logFile.exists()) 
	   {
	      try {
	         logFile.createNewFile();
	      } 
	      catch (IOException e) {
	         e.printStackTrace();
	      }
	   }
	   if (fixVector2.size() > 0) {
           try 
           {
	    	   BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
	           for (int i=0; i<fixVector2.size(); i++) {
	                   buf.append(fixVector2.get(i).toString());
	                   //Log.v("logs", fixVector2.get(i).toString());
	                   buf.newLine();
	           }
	 	       buf.close();    
            } 
           catch (IOException e) 
           {
                Log.e("TAG", "Could not write file " + e.getMessage());
            }
           //running = true;
	   }
	}


	

}