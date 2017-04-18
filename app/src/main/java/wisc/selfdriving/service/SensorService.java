package wisc.selfdriving.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import wisc.selfdriving.utility.Trace;

import java.util.concurrent.atomic.AtomicBoolean;

public class SensorService extends Service implements LocationListener {

    private final String TAG = "Sensor Service";
    private final Binder binder_ = new SensorBinder();

    public class SensorBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
        public String callTest() {
            Log.d(TAG, "this is called");
            return "success";
        }
    }

    private AtomicBoolean isRunning_ = new AtomicBoolean(false);
    private LocationManager locationManager;

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "location update speed:" + String.valueOf(location.getSpeed()));
        // TODO Auto-generated method stub
        if(location != null){
            Trace trace = new Trace(4);
            trace.time = System.currentTimeMillis();
            trace.values[0] = location.getLatitude();
            trace.values[1] = location.getLongitude();
            trace.values[2] = location.getAltitude();
            trace.values[3] = location.getSpeed();
            trace.type = Trace.GPS;

            sendTrace(trace);
        }

    }

    @Override
    public void onProviderDisabled(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        // TODO Auto-generated method stub

    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return binder_;
    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return START_STICKY;
    }

    public void onDestroy() {
        Log.d(TAG, "stop service");

        if(locationManager != null) {
            locationManager.removeUpdates(this);
        }
        isRunning_.set(false);
        stopSelf();
    }

    private void startService() {
        Log.d(TAG, "start service");
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        isRunning_.set(true);
    }

    private void sendTrace(Trace trace) {
        Log.d(TAG, trace.toJson());
        Intent intent = new Intent("sensor");
        intent.putExtra("trace", trace.toJson());

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
///////////////////////////////////////////////////////////////////////////////////////////
    public void test() {
        Log.d(TAG, "this is in test");
    }
    public static final String ACTION = "com.hariharan.arduinousb";
}