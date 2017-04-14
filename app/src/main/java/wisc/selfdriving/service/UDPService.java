package wisc.selfdriving.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.os.Handler;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;

public class UDPService extends Service {

    private static final String TAG = "UDPService";
    UDPServer udpserver = new UDPServer();
    private final Binder binder_ = new UDPService.UDPBinder();
    public String ip = "";
    public String order = "";
    public String rotation = "0";

    public class UDPBinder extends Binder {
        public UDPService getService() {
            return UDPService.this;
        }
        public String getIp() {
            Log.d(TAG, "getIp");
            getIpAddress();
            return ip;
        }
        public void sendData(String n){
            rotation = n;
            udpserver.send(n);
        }

        public String getOrder(){
            return order;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
       // throw new UnsupportedOperationException("Not yet implemented");
        return binder_;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return START_STICKY;
    }

    private void startService() {
        Log.d(TAG,"Start UDP server");
        (new Thread(udpserver)).start();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            udpserver.send("server send you now rotation is: " + rotation);
    }

    public void onDestroy() {
        if(udpserver != null){
            udpserver = null;
            Log.d(TAG,"udpserver connection is closed");
        }
        stopSelf();
    }

    private void getIpAddress() {
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();
                    if (inetAddress.isSiteLocalAddress()) {
                        ip += inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }
    }

    private void sendUDPServerData(String rotation) {

        UDPService.dataReading obj = new UDPService.dataReading(rotation);
        Gson gson = new Gson();
        String json = gson.toJson(obj);

        Intent intent = new Intent("UDPserver");
        intent.putExtra("rotationStatus", json);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class dataReading {
        private String rotation;

        public dataReading(String data){
            this.rotation = data;
        }
    }
}
