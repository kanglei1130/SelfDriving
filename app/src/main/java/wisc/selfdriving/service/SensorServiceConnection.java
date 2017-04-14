package wisc.selfdriving.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by wei on 2/23/17.
 */

public class SensorServiceConnection implements ServiceConnection {
    private SensorService.SensorBinder binder = null;
    private static final String TAG = "SensorServiceConnection";


    public void onServiceConnected(ComponentName className, IBinder service) {
        Log.d(TAG, "connected");
        binder = ((SensorService.SensorBinder) service);

    }
    public void onServiceDisconnected(ComponentName className) {
        binder = null;
        Log.d(TAG, "distconnected");

    }

    public String calllFunction() {
        return binder.callTest();
    }
};