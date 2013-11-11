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
import com.openxc.measurements.EngineSpeed;
import com.openxc.remote.VehicleServiceException;

public class StarterActivity extends Activity {

    private VehicleManager mVehicleManager;
    private TextView mEngineSpeedView;
    private Intent mVmIntent = null;
    private boolean mBound = false; // added for better example of Activity/Service lifecycle management.
    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the VehicleManager service is established, i.e. bound.
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i("openxc", "Bound to VehicleManager");
            mVehicleManager = ((VehicleManager.VehicleBinder) service)
                    .getService();

            try {
                mVehicleManager.addListener(EngineSpeed.class, mSpeedListener);
            } catch (VehicleServiceException e) {
                e.printStackTrace();
            } catch (UnrecognizedMeasurementTypeException e) {
                e.printStackTrace();
            }
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.w("openxc", "VehicleManager Service  disconnected unexpectedly");
            mVehicleManager = null;
            mBound = false;
        }
    };

    @Override
    /**
     * As usual for onCreate, we set up the (static) UI here.
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starter);
        mEngineSpeedView = (TextView) findViewById(R.id.vehicle_speed);
    }

    @Override
    /**
     * We bind the service here and set the boolean rather than rely on
     * onCreate, which gets called only when the whole Activity is created.
     * This was done for resolving  issue #1 on Github in openxc-starter project
     *
     * Depending on how we got to onStart, the service might still be running, so we check mBound.
     */
    protected void onStart() {
        super.onStart();
        if (!mBound) {
            mVmIntent = new Intent(this, VehicleManager.class);
            mBound = bindService(mVmIntent, mConnection, Context.BIND_AUTO_CREATE);
            if (!mBound)
                Log.e("openxc", "Failed to bind to VehicleManager");
        } // if we find it still bound, we do nothing.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.starter, menu);
        return true;
    }

    /**
     * We (conditionally) unbind the service here since in API level 8, we are
     * not guaranteed to get to onStop() under conditions of
     * "extreme memory pressure".
     */
    public void onPause() {
        super.onPause();
        if (mBound) {
            Log.i("openxc", "Unbinding from Vehicle Manager");
            unbindService(mConnection); // requires mConnection != null, which is true; it can't be null.
            mBound = false; // unbindService returns void, so we assume it worked.
        }
    }

    /**
     * It is rare that onResume() is called after onPause() w/o intervening
     * onStart(), but it can happen, so we reconnect to the service
     * conditionally here, too.
     */
    public void onResume() {
        super.onResume();
        if (!mBound) {
            mVmIntent = new Intent(this, VehicleManager.class);
            mBound = bindService(mVmIntent, mConnection, Context.BIND_AUTO_CREATE);
            if (mBound)
                Log.i("openxc", "Binding to Vehicle Manager");
            else
                Log.e("openxc", "Failed to bind to Vehicle Manager");
        }
    }

    EngineSpeed.Listener mSpeedListener = new EngineSpeed.Listener() {
        public void receive(Measurement measurement) {
            final EngineSpeed speed = (EngineSpeed) measurement;
            StarterActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    mEngineSpeedView.setText("Vehicle speed (km/h): "
                            + speed.getValue().doubleValue());
                }
            });
        }
    };
}
