package edu.ucsb.geog;


import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class GauchoSafe extends Activity implements OnClickListener, LocationListener, Runnable 
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
	
	// start the location section
	private LocationManager locationManager;
	private String locationProvider;
	private Thread locationThread;
	private double latitude = 0;
	private double longitude = 0;
	
	// phone number
	private String phoneNumber;
	 
	
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
	    phoneNumber = tm.getLine1Number();
	    androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
	    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
	    deviceId = deviceUuid.toString();
		
		
		
		buttonDoSomething = (Button) findViewById(R.id.btn1);
		buttonDoSomething.setOnClickListener(this);
		
		serviceIntent = new Intent(this, AccelService.class);
	    
		
	    
	    locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	    Criteria locationCriteria = new Criteria();
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
					prefsEditor.putBoolean("isEmergent", false);
					editor.putInt("lightOn", 0);
			        prefsEditor.commit();
			        
			        
			  	    // turn on location service
			        locationManager.requestLocationUpdates(locationProvider, 400, 1, this);	        
			        locationThread = new Thread(this);
			        locationThread.start();
			  } 
			  else 
			  {
				    stopService(serviceIntent);
					trackeron = false;
					buttonDoSomething.setText("Turn GauchoSafe ON");
					prefsEditor.putBoolean("stationary", true);
					prefsEditor.putBoolean("ucsb_tracker", trackeron);
					prefsEditor.putBoolean("isEmergent", false);
					editor.putInt("lightOn", 0);
			        prefsEditor.commit();
			        
			        
			        // cancel location service
			        locationManager.removeUpdates(this);		    
			  }
			  
			  try 
			  {
		        	camera.release();
		       } 
			  catch (Exception e) 
			  {
		        	// fix this
		      }
			  
			  buttonDoSomething.setEnabled(true);
			  editor.putBoolean("ucsb_tracker", trackeron);
			  editor.commit();
		  } 
		  else if (src.getId() == R.id.btn2) 
		  {
			  editor.putInt("lightOn", 0);
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


	@Override
	public void run() 
	{
		while(true)
		{
			trackeron = settings.getBoolean("ucsb_tracker", false);
			if(!trackeron)
				break;
			try 
			{
				Thread.sleep(2000);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
			
			sendDataToServer(latitude, longitude, phoneNumber);
			//System.out.println(latitude+","+longitude+","+phoneNumber);
			
		}	
	}


	@Override
	public void onLocationChanged(Location location) 
	{
		latitude = location.getLatitude();
		longitude = location.getLongitude();
	}


	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	
	public void sendDataToServer(double latitude, double longitude, String phoneNumber)
	{
		HttpClient httpclient = new DefaultHttpClient();
	    HttpResponse response;
		try 
		{
			String requestURL = "http://stko-work.geog.ucsb.edu/gauchosafe/handlers/track.php?lat="+latitude+"&lng="+longitude+"&id="+phoneNumber;
			System.out.println(requestURL);
			response = httpclient.execute(new HttpGet(requestURL));
			StatusLine statusLine = response.getStatusLine();
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK)
		    {
		        System.out.println("success");
		    }
		    else
		    {
		    	System.out.println("failed");
		    }
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
	}

}