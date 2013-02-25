// Author: Grant McKenzie grant.mckenzie@geog.ucsb.edu
// Date: January 2013
// Project: Android Activity
// Client: UCSB Geography

package edu.ucsb.geog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class BurstSD {
	private ArrayList<Double> setOfVectors;
	private double vSum;
	private float mean;
	private ArrayList<Double> currentVector;
	private ArrayList<Double> previousVector;
	
	// Incoming Burst of accel values
	public BurstSD(Vector<JSONObject> vBurst) {
		
		this.setOfVectors = new ArrayList<Double>(vBurst.size()-1);
		this.currentVector = new ArrayList<Double>(3);
		this.previousVector = null;
		
		double v = 0;
		
		for(JSONObject r: vBurst) {
			try {
				// get x, y, z from JSON
				this.currentVector.add(0, (Double) r.get("accelx"));
				this.currentVector.add(1, (Double) r.get("accely"));
				this.currentVector.add(2, (Double) r.get("accelz"));
				// Only calculate the sum vector length if we have two vectors
				if (this.previousVector != null) {
					v = Math.sqrt(Math.pow((this.previousVector.get(0)+this.currentVector.get(0)), 2) + Math.pow((this.previousVector.get(1)+this.currentVector.get(1)), 2) + Math.pow((this.previousVector.get(2)+this.currentVector.get(2)), 2));
					this.setOfVectors.add(v);
					this.vSum += v;
				}
				// Assign the current vector as the previous vector
				this.previousVector = this.currentVector;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		// The mean is just the sum divided by size 
		this.mean = (float) this.vSum / this.setOfVectors.size();	
	}
	// return the Standard Deviation
	public double getSD() {
		double sumsquares = 0;
		for(int i = 0; i < this.setOfVectors.size(); i++) {
			sumsquares += Math.pow((this.setOfVectors.get(i) - this.mean),2); 
		}
		double result = sumsquares / (this.setOfVectors.size()-1);
		
		return Math.sqrt(result);
	}
}







