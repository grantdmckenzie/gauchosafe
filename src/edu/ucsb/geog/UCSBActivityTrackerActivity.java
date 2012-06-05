package edu.ucsb.geog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import java.util.Iterator;
public class UCSBActivityTrackerActivity extends Activity implements Observer {

	//declare widgets
	private TextView mAccelerometerDisplay;
	private TextView mWifiDisplay;
	private TextView mCoordinateDisplay;

	//declare variables for accelerometer
	private SensorManager mSensorManager;
	private Accelerometer accelerometer;
	private Coordinates coordinate;
	private Thread accelThread;
	private Handler accelHandler = null;
	private LocationManager locationManager;
	private ArrayList<HashMap<String,Double>> fixList;
	private WifiManager wifiManager;
	//declare a hashmap to store the values from sensors
	private HashMap fix;
	private Wifi wifi;
	private Thread wifithread;
	private Thread coordthread;
	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// initiate GUI
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);

		
		// initiate display textviews
		mAccelerometerDisplay = (TextView)findViewById(R.id.accelerometerDisplay);
		mWifiDisplay = (TextView)findViewById(R.id.wifiDisplay);
		mCoordinateDisplay = (TextView)findViewById(R.id.coordinateDisplay);

		// initiate variables for accelerometer
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = new Accelerometer(mSensorManager, 5000); // the rate for accelerometer is 5 sec
		accelerometer.addObserver(this);
		accelThread = new Thread(accelerometer);
		// we will not be able to directly change the value of textview in another thread
	    // so I used the handler mode
		accelHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				String value = msg.getData().getString("value");
				mAccelerometerDisplay.setText(value);
			}

		};  
		
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		coordinate = new Coordinates(locationManager);
		coordinate.addObserver(this);
		coordthread = new Thread(coordinate);
		
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifi = new Wifi(wifiManager, 10000);
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifi.addObserver(this);   
        wifithread = new Thread(wifi);
        
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
	}
	
	@Override
	protected void onStart() 
	{
		// TODO Auto-generated method stub
		super.onStart();
		accelerometer.startRecording();
		accelThread.start();
		wifithread.start();
		coordinate.startRecording();
		coordthread.start();
		
	}
	

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		// wifithread.suspend();
	}
	
	@Override
	protected void onStop() 
	{
		// TODO Auto-generated method stub
		super.onStop();
		accelerometer.stopRecording();  
		accelThread.stop();
		wifithread.stop();
		coordinate.stopRecording();
		coordthread.stop();
	}


	@Override
	public void update(Observable observable, Object data) 
	{
		// use fix to handle the data from all sensors
		fix  = (HashMap)data;
		
		Message message = new Message();
		Bundle bundle = new Bundle();
		
		// if the values come from accelerometer do the following actions
		if(observable instanceof Accelerometer)
		{		
			fix = accelerometer.getFix();  // Grant Edit
			bundle.putCharSequence("value", "Accelerometer: x:"+fix.get("accelx")+", y:"+fix.get("accely")+", z:"+fix.get("accelz"));
			message.setData(bundle);
			accelHandler.sendMessage(message);
		}	
		else if(observable instanceof Coordinates) {
			// Log.v("Location", "Location");
			fix = coordinate.getFix();
			// mCoordinateDisplay.setText(fix.get("lat").toString());
			// mCoordinateDisplay.setText("test");
		} 
		else if (observable instanceof Wifi){
			fix = wifi.getFix();
			// mWifiDisplay.setText(fix.get("sensor").toString());
			
			// System.out.println("==========Number of wifi: "+fix.size()+"==============");
			/* Iterator it = fix.keySet().iterator();
			while(it.hasNext())
			{
				ScanResult sr = (ScanResult)fix.get(it.next());
				
				// System.out.println(sr.SSID+","+sr.level+","+sr.frequency);
			} */	
		}
		
		// Add the fix to the fixlist (arraylist of hashmaps)
		// fixList.add(fix);
		Log.v("New Fix Added", fix.toString());
	}
}