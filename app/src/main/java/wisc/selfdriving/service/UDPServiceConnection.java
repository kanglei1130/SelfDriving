package wisc.selfdriving.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by wei on 4/4/17.
 */

public class UDPServiceConnection implements ServiceConnection {

    private UDPService.UDPBinder binder = null;
    private static final String TAG = "UDPServiceConnection";


    public void onServiceConnected(ComponentName className, IBinder service) {
        Log.d(TAG, "connected");
        binder = ((UDPService.UDPBinder) service);
    }
    public void onServiceDisconnected(ComponentName className) {
        binder = null;
        Log.d(TAG, "distconnected");
    }

    //send data form main to UDPService
    public void sendData(String n) {
        binder.sendData(n);
    }
}
