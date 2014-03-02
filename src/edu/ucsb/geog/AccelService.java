package edu.ucsb.geog;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

public class AccelService extends Service 
{
  
  private static AlarmReceiver alarmReceiver;
  private GenerateUserActivityThread generateUserActivityThread;
  private ScreenOffBroadcastReceiver screenOffBroadcastReceiver;
  private boolean samplingStarted = false;
  private static AlarmManager alarmManager;

  
  public void onCreate() 
  {	 
	  showNotification();
	  Log.v("AccelService", "onCreate");
  }
  
  public int onStartCommand(Intent intent, int flags, int startId) 
  {

		
	  if(alarmReceiver == null)
		  alarmReceiver = new AlarmReceiver();
	  
	  if(alarmManager == null)
		  alarmManager = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);

	  alarmReceiver.SetAlarm(getApplicationContext());
	  samplingStarted = true;
	 return START_STICKY;
  }

	
  	@Override
  	public void onDestroy() 
  	{
  		Log.v("AccelService", "onDestroy");
	  //Cancel alarm when the service is destroyed
	  if(alarmReceiver != null) { 
		  alarmReceiver.CancelAlarm(getApplicationContext());
		  // unregisterReceiver(alarmReceiver);
	  }

	  samplingStarted = false;
	  
	  //Unregister the screenOffreceiver when the service is destroyed
	  if(screenOffBroadcastReceiver != null)
		  unregisterReceiver( screenOffBroadcastReceiver );
	  
	  //Cancel the thread which is used to turn the screen on
	  if( generateUserActivityThread != null ) 
	  {
		  generateUserActivityThread.stopThread();
		  generateUserActivityThread = null;
	  }
	  
	  stopForeground(true); 
	  
  	}
  
  @Override
  	public IBinder onBind(Intent intent) 
  {
    return(null);
  }

	private void showNotification()
	{
		 Notification note=new Notification(R.drawable.iconnotification, getText(R.string.accel_started), System.currentTimeMillis());
	     
		 Intent notifyIntent = new Intent(Intent.ACTION_MAIN);
	     notifyIntent.setClass(getApplicationContext(), GauchoSafe.class);
	     notifyIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	     
	     PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_CANCEL_CURRENT);
	     
	     note.setLatestEventInfo(this, getText(R.string.local_service_label), getText(R.string.accel_started), contentIntent);
	 	
	     note.flags|=Notification.FLAG_NO_CLEAR;

	     startForeground(1337, note);
	     Log.v("AccelService", "showNotification");
	     	     
	}
	
	// This part defines the ScreenOffBroadcastReceiver---------------
	class ScreenOffBroadcastReceiver extends BroadcastReceiver 
	{
		public void onReceive(Context context, Intent intent) 
		{
			
			if(samplingStarted) 
			{
				if( generateUserActivityThread != null ) 
				{
					generateUserActivityThread.stopThread();
					generateUserActivityThread = null;
				}
				
				generateUserActivityThread = new GenerateUserActivityThread();
				generateUserActivityThread.start();
			}
		}
	}

	class GenerateUserActivityThread extends Thread 
	{
		public void run() 
		{
			try 
			{
				Thread.sleep( 2000L );
			} 
			catch( InterruptedException ex ) {}
			
			Log.d( "screenoff", "User activity generation thread started" );

			PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
			userActivityWakeLock =  pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "GenerateUserActivity");
			userActivityWakeLock.acquire();
			Log.v("AccelService", "ActivityThread Run");
		}

		public void stopThread() 
		{
			userActivityWakeLock.release();
			userActivityWakeLock = null;
		}

		PowerManager.WakeLock userActivityWakeLock;
	}
	
	// AlarmReceiver inner Class
	public static class AlarmReceiver extends BroadcastReceiver implements Observer
	{
		private long msInterval = 10000;
		private Context alrmContext;

		@Override
		public void onReceive(Context context, Intent intent) 
	    {   
			// Log.v("AlarmReceiver", "onReceive"); 
			this.alrmContext = context;
	        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
	        wl.acquire();
	        Log.v("AccelService", "onReceive");
	        AcclThread acclThread = new AcclThread(context);
	        Thread thread = new Thread(acclThread);
	        thread.start();
	        acclThread.addObserver(this);
	        
	        wl.release();    
	    }

		public void SetAlarm(Context context)
		{
			// Log.v("AlarmReceiver", "setAlarm"); 
			//if(alarmManager == null)
				//alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			
			Intent i = new Intent(context, AlarmReceiver.class);
			PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
			
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), msInterval, pi);
			// Log.v("AlarmReceiver", "setRepeating done"); 

		}

		public void CancelAlarm(Context context)
		{
			//if(alarmManager == null)
				//alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			
			Intent intent = new Intent(context, AlarmReceiver.class);
			PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
			alarmManager.cancel(sender);
			// Log.v("AccelService","End Cancel Alarm");
		}

		// Receives notifications from the observable (the thread)
		@Override
		public void update(Observable observable, Object data) {
			if(observable instanceof AcclThread) {
				boolean stationary = ((AcclThread) observable).stationary;
				boolean stationarityChanged = ((AcclThread) observable).stationarityChanged;
				if(!stationary && stationarityChanged) {
					Log.v("AccelService", "STARTED MOVING");
				} else if (stationary && stationarityChanged) {
					Log.v("AccelService", "BECAME STATIONARY");
				}
			}
		}

	}

}
