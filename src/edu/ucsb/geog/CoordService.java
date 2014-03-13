package edu.ucsb.geog;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import edu.ucsb.geog.AccelService.AlarmReceiver;
import android.R.string;
import android.app.AlarmManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class CoordService extends Service implements Runnable, LocationListener
{
	private SharedPreferences settings;
	private Editor prefsEditor;
	private static final String PREFERENCE_NAME = "ucsbprefs";
	
	// start the location section
	private LocationManager locationManager;
	private String locationProvider;
	private Thread locationThread;
	private double latitude = 0;
	private double longitude = 0;
	
	private String phoneNumber;
	
	
	
	public void onCreate() 
    {	 
		settings = getSharedPreferences(PREFERENCE_NAME, MODE_WORLD_READABLE);
		prefsEditor = settings.edit();
		
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Criteria locationCriteria = new Criteria();
		locationProvider = locationManager.getBestProvider(locationCriteria, false);
		
		phoneNumber = settings.getString("phoneNumber", "");
    }
	  
    public int onStartCommand(Intent intent, int flags, int startId) 
    {
    	 // turn on location service
        locationManager.requestLocationUpdates(locationProvider, 400, 1, this);	        
        locationThread = new Thread(this);
        locationThread.start();
		  
		return START_STICKY;
    }

    
  	@Override
  	public void onDestroy() 
  	{
  		 // cancel location service
        locationManager.removeUpdates(this);
  	}
	
	

	@Override
	public IBinder onBind(Intent arg0) 
	{
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public void run() 
	{
		while(true)
		{
			boolean trackeron = settings.getBoolean("ucsb_tracker", false);
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
			
			if(settings.getInt("lightOn", 0) == 2)
			{
				System.out.println(latitude+","+longitude+","+phoneNumber);
				//sendDataToServer(latitude, longitude, phoneNumber);
			}
			else 
			{
				System.out.println(latitude+","+longitude+",");
				//sendDataToServer(latitude, longitude, "");
			}
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
