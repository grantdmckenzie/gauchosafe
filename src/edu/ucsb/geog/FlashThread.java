package edu.ucsb.geog;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaPlayer;

public class FlashThread implements Runnable
{
	
	 private Context context;
	 private static Camera camera;
	 private static Parameters p;
	 private Editor prefsEditor;
	 private static boolean cameraIsOn = false;
	 private SharedPreferences appSharedPrefs;
	  
	  public FlashThread(SharedPreferences sp, Context context)
	  {
		  this.context = context;
		  appSharedPrefs = sp;
		  prefsEditor = appSharedPrefs.edit();
		  prefsEditor.putInt("lightOn", 2);
		  prefsEditor.putBoolean("isEmergent", true);
		  prefsEditor.commit();
		  System.out.println("flash"+appSharedPrefs);
		  
		  GauchoSafe.EMERGENCE = new Integer(2);
		  
	  }
	  
		private void turnOnFlashLight() {
			camera = Camera.open();
			p = camera.getParameters();
			p.setFlashMode(Parameters.FLASH_MODE_TORCH);
			camera.setParameters(p);
			camera.startPreview();
			cameraIsOn = true;
			
	}
	private void turnOffFlashLight() {
		cameraIsOn = false;
		prefsEditor.putInt("lightOn", 0);
		prefsEditor.commit();
		camera.release();
	}
	  

		@Override	
		public void run() 
		{
			MediaPlayer mp = MediaPlayer.create(context, R.raw.siren);
			mp.start();
			int i=0;
			while(i<20){
				try {
					turnOnFlashLight();
					Thread.sleep(100);
					turnOffFlashLight();
					i++;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}		
}
