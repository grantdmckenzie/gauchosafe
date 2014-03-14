package edu.ucsb.geog;


import java.util.UUID;

import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.location.Criteria;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class GauchoSafe extends Activity implements OnClickListener
{
	private Button buttonDoSomething;
	private Button buttonCalibrate;
	private SharedPreferences settings;
	private Editor prefsEditor;
	private boolean trackeron;
	private Intent serviceIntent;
	private TelephonyManager tm;
	private ConnectivityManager connectivity;
	private String deviceId;
	private static final String PREFERENCE_NAME = "ucsbprefs";
	private Camera camera;
	
	
	private Intent coordIntent;
	private String phoneNumber;
	
	
	private LocationManager locationManager;
	private String locationProvider;
	private Thread locationThread;
	private CoordThread coordThreadContainer;
	
	public static Integer EMERGENCE = new Integer(0);
	 
	
	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// initiate GUI
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		settings = getSharedPreferences(PREFERENCE_NAME, MODE_WORLD_READABLE);
		prefsEditor = settings.edit();
		
		// For defining unique device id
		tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		String tmDevice, tmSerial, androidId;
	    tmDevice = "" + tm.getDeviceId();
	    tmSerial = "" + tm.getSimSerialNumber();
	    androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
	    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
	    deviceId = deviceUuid.toString();
		phoneNumber = tm.getLine1Number();
		prefsEditor.commit();
		
		buttonDoSomething = (Button) findViewById(R.id.btn1);
		buttonDoSomething.setOnClickListener(this);
		
		serviceIntent = new Intent(this, AccelService.class);
		
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Criteria locationCriteria = new Criteria();
		//locationCriteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
	    
		locationProvider = locationManager.getBestProvider(locationCriteria, false);
		
	    
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		trackeron = settings.getBoolean("ucsb_tracker", false);
	    if (trackeron) 
	    {
	    	buttonDoSomething.setText("Turn GauchoSafe OFF");
	    } 
	    else 
	    {
	    	buttonDoSomething.setText("Turn GauchoSafe ON");
	    }
	}
	

	@Override
	  protected void onPause() 
	  {
	      super.onPause();
	      saveState();
	  }
	
	  public void onSaveInstanceState(Bundle savedInstanceState) {
		  super.onSaveInstanceState(savedInstanceState);
		  saveState();
	  }
	 
	  @Override
	  public void onRestoreInstanceState(Bundle savedInstanceState) {
	    super.onRestoreInstanceState(savedInstanceState);
	    settings = PreferenceManager.getDefaultSharedPreferences(this);
	  }
	  private void saveState() {
		  SharedPreferences preferences = getSharedPreferences(PREFERENCE_NAME, MODE_WORLD_READABLE);
		  SharedPreferences.Editor editor = preferences.edit(); 
		  editor.putBoolean("ucsb_tracker", trackeron);
		  editor.commit();
	  }

	@Override
	public void onClick(View src) {
		  SharedPreferences preferences = getSharedPreferences(PREFERENCE_NAME, MODE_WORLD_READABLE);
		  SharedPreferences.Editor editor = preferences.edit(); 
		  if (src.getId() == R.id.btn1) 
		  {
			  buttonDoSomething.setEnabled(false);
			  if (!trackeron) 
			  { 
				  	startService(serviceIntent);
				  	
					trackeron = true;
					buttonDoSomething.setText("Turn GauchoSafe OFF");
					// prefsEditor.putBoolean("turnOnWifi", true);
					prefsEditor.putBoolean("stationary", true);
					prefsEditor.putBoolean("ucsb_tracker", trackeron);
					editor.putInt("lightOn", 0);
			        prefsEditor.commit();
			        
			        coordThreadContainer = new CoordThread(locationManager,locationProvider, preferences, phoneNumber);
			        locationManager.requestLocationUpdates(locationProvider, 400, 1, coordThreadContainer);	        
			        locationThread = new Thread(coordThreadContainer);
			        locationThread.start();
			  } 
			  else 
			  {
				    stopService(serviceIntent);
				    
					trackeron = false;
					buttonDoSomething.setText("Turn GauchoSafe ON");
					prefsEditor.putBoolean("stationary", true);
					prefsEditor.putBoolean("ucsb_tracker", trackeron);
					editor.putInt("lightOn", 0);
			        prefsEditor.commit();	
			        
			        locationManager.removeUpdates(coordThreadContainer);
			        GauchoSafe.EMERGENCE = new Integer(0);
			  }
			  
			  try 
			  {
		        	camera.release();
		      } 
			  catch (Exception e) 
			  {
		        	//e.printStackTrace();
		      }
			  
			  buttonDoSomething.setEnabled(true);
			  editor.putBoolean("ucsb_tracker", trackeron);
			  editor.commit();
		  } 
		  /* else if (src.getId() == R.id.btn2) 
		  {
			  editor.putInt("lightOn", 0);
			  editor.commit();
		  } */
	}
	
	public void errorDialog(String msg) {
		AlertDialog.Builder adb=new AlertDialog.Builder(GauchoSafe.this);
        adb.setTitle("GauchoSafe");
        adb.setMessage(msg);
        adb.setNegativeButton("OK", new DialogInterface.OnClickListener() {  
  	      public void onClick(DialogInterface dialog, int which) {  
  	    	finish();
  	        return;  
  	   } });
        adb.show(); 
	}

}