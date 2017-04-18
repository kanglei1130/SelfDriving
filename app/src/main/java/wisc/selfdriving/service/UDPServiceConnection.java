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
/*
    public String callFunction() {
        return binder.getIp();
    }
*/
    public void sendData(String n) {
        Log.d(TAG, n);
        binder.sendData(n);
    }

    public String getOrder() {
        return binder.getOrder();
    }
}
