package com.openxc.openxcstarter;
 
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import com.openxc.VehicleManager;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.UnrecognizedMeasurementTypeException;
import com.openxc.measurements.VehicleSpeed;
import com.openxc.remote.VehicleServiceException;
 
public class StarterActivity extends Activity {
	
	private VehicleManager mVehicleManager;
	private TextView mVehicleSpeedView;
 
	private ServiceConnection mConnection = new ServiceConnection() {
	    // Called when the connection with the service is established
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        Log.i("openxc", "Bound to VehicleManager");
	        mVehicleManager = ((VehicleManager.VehicleBinder)service).getService();
	        
	        try {
				mVehicleManager.addListener(VehicleSpeed.class, mSpeedListener);
			} catch (VehicleServiceException e) {
				e.printStackTrace();
			} catch (UnrecognizedMeasurementTypeException e) {
				e.printStackTrace();
			}
	    }
 
	    // Called when the connection with the service disconnects unexpectedly
	    public void onServiceDisconnected(ComponentName className) {
	        Log.w("openxc", "VehicleService disconnected unexpectedly");
	        mVehicleManager = null;
	    }
	};
 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_starter);
		
		mVehicleSpeedView = (TextView) findViewById(R.id.vehicle_speed);
		
		Intent intent = new Intent(this, VehicleManager.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);   
	}
 
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.starter, menu);
		return true;
	}
	
	public void onPause() {
	    super.onPause();
	    Log.i("openxc", "Unbinding from vehicle service");
	    unbindService(mConnection);
	}
 
	VehicleSpeed.Listener mSpeedListener = new VehicleSpeed.Listener() {
	    public void receive(Measurement measurement) {
	    	final VehicleSpeed speed = (VehicleSpeed) measurement;
	        StarterActivity.this.runOnUiThread(new Runnable() {
	            public void run() {
	                mVehicleSpeedView.setText(
	                    "Vehicle speed (km/h): " + speed.getValue().doubleValue());
	            }
	        });
	    }
	};
}