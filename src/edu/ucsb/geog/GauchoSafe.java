package edu.ucsb.geog;


import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class GauchoSafe extends Activity implements OnClickListener {

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
		
		trackeron = settings.getBoolean("ucsb_tracker", false);
		
		buttonDoSomething = (Button) findViewById(R.id.btn1);
		buttonDoSomething.setOnClickListener(this);
		
		serviceIntent = new Intent(this, AccelService.class);
	    
	    if (trackeron) {
	    	buttonDoSomething.setText("Turn GauchoSafe OFF");
	    } else {
	    	buttonDoSomething.setText("Turn GauchoSafe ON");
	    }
	       
	}
	

	@Override
	  protected void onPause() {
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
		  if (src.getId() == R.id.btn1) {
			  buttonDoSomething.setEnabled(false);
			  if (!trackeron) { 
				  	startService(serviceIntent);
					trackeron = true;
					buttonDoSomething.setText("Turn GauchoSafe OFF");
					// prefsEditor.putBoolean("turnOnWifi", true);
					prefsEditor.putBoolean("stationary", true);
			        prefsEditor.commit();
			  } else {
				    stopService(serviceIntent);
					trackeron = false;
					buttonDoSomething.setText("Turn GauchoSafe ON");
					prefsEditor.putBoolean("stationary", true);
			        prefsEditor.commit();
			        
			  }
			  buttonDoSomething.setEnabled(true);
			  editor.putBoolean("ucsb_tracker", trackeron);
			  editor.commit();
		  } 
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