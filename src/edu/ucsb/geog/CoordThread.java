package edu.ucsb.geog;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.R.string;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class CoordThread implements Runnable, LocationListener
{
	private double latitude = 0;
	private double longitude = 0;
	private SharedPreferences sharedPreferences;
	private String phoneNumber;
	private double accuracy;
	private LocationManager locationManager;
	private String locationProvider;
	
	public CoordThread(LocationManager locationManager, String locationProvider, SharedPreferences sharedPref, String phoneNumber)
	{
		sharedPreferences = sharedPref;
		this.phoneNumber = phoneNumber;
		this.locationManager = locationManager;
		this.locationProvider = locationProvider;
		
		Location location = locationManager.getLastKnownLocation(locationProvider);
		if(location!= null)
		{
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			accuracy = location.getAccuracy();
			if(accuracy < 500)
				sendDataToServer(latitude, longitude, "");
		}
			
	}

	
	public void run() 
	{
		while(true)
		{
			boolean trackeron = sharedPreferences.getBoolean("ucsb_tracker", false);
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
			
			//System.out.println("accuracy: "+accuracy);
			
			
			if(accuracy>500)
				continue;
			
			
			if(GauchoSafe.EMERGENCE.intValue() == 2 )
			{
				//System.out.println(latitude+","+longitude+","+phoneNumber);
				
				sendDataToServer(latitude, longitude, phoneNumber);
			}
			else 
			{
				//System.out.println(latitude+","+longitude+",");
				sendDataToServer(latitude, longitude, "");
			}
			//System.out.println(latitude+","+longitude+","+phoneNumber);
			
		}	
	}


	@Override
	public void onLocationChanged(Location location) 
	{
		latitude = location.getLatitude();
		longitude = location.getLongitude();
		accuracy = location.getAccuracy();
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
