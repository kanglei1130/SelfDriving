package wisc.selfdriving.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class UDPService extends Service implements Runnable {

    private static final String TAG = "UDPService";
    private final Binder binder_ = new UDPService.UDPBinder();
    public String localIP = "";
    public String order = "";
    public String rotation = "0";
    public DatagramSocket localSocket = null;
    public InetAddress remoteIPAddress = null;
    public int remotePort = 5000;
    public int localPort = 55555;
    //this IP need to be changed if you change your WiFi connection
    String remoteIPName = "192.168.10.103";
    private Boolean UDPThreadRunning = null;
    CarControl control;

    public class UDPBinder extends Binder {
        public UDPService getService() {
            return UDPService.this;
        }
        public void sendData(String n){
            rotation = n;
            send(n);
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
        //get local ip
        localIP = getIpAddress();
        Log.d(TAG, localIP);
        return START_STICKY;
    }

    WifiManager wifiManager;
    WifiManager.WifiLock lockHigh;

    private void startService() {

        wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        lockHigh = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "HIGH_WIFI");
        lockHigh.acquire();

        Log.d(TAG,"Start UDP server");
        try {
            localSocket = new DatagramSocket(localPort);
            remoteIPAddress = InetAddress.getByName(remoteIPName);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        (new Thread(this)).start();
        //send("UDPServiceSever send: ");
    }



    public void onDestroy() {
        Log.d(TAG,"udpserver connection is closed");
        stopSelf();
        UDPThreadRunning = false;

        lockHigh.release();
    }

    //get local ip for smoothing user
    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();
                    String sAddr = inetAddress.getHostAddress();
                    boolean isIPv4 = sAddr.indexOf(':') < 0;
                    if (!inetAddress.isLoopbackAddress() && isIPv4) {
                        ip = inetAddress.getHostAddress();
                        Log.d(TAG, ip);
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip = "Something Wrong! " + e.toString() + "\n";
        }
        return ip;
    }

    //send order achieved from UDP client to main
    /*private void sendUDPServerOrder(CarControl control) {

        Gson gson = new Gson();
        String json = gson.toJson(control);
        Intent intent = new Intent("UDPserver");
        intent.putExtra("order", json);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }*/
/////////////////////////////////////////////////////////////////////////////
    //send order achieved from UDP client directly to main without transfer it to json
    private void sendUDPServerOrder(String string) {

        Intent intent = new Intent("UDPserver");
        intent.putExtra("order", string);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
//////////////////////////////////////////////////////////////////////////////////

    public void run() {
        control = new CarControl();
        // TODO Auto-generated method stub
        Log.d(TAG, "start receiving thread");
        byte[] receiveData = new byte[1024];
        UDPThreadRunning = true;
        String duplicater = "";
        while (UDPThreadRunning.booleanValue()) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                localSocket.receive(receivePacket);
                String sentence = new String(receiveData, 0, receivePacket.getLength());

                //get UDPClient ip and port
                remoteIPAddress = receivePacket.getAddress();
                remotePort = receivePacket.getPort();

/*                //receive gson data from UDPClient
                Gson gson = new GsonBuilder().create();
                CarControl command = gson.fromJson(sentence, CarControl.class);
q
                control speed and turn rotation angle
                sendUDPServerOrder(command);*/

                //////////////////////////////////////////////////////
                //if received changed commend, then send it to main
                Log.d(TAG,sentence);
                if (duplicater != sentence && sentence.contains("steering")) {
                    sendUDPServerOrder(sentence);
                    duplicater = sentence;
                } else if (sentence.contains("length")){
                    double imageSize = Double.parseDouble(sentence.substring(sentence.indexOf(":")+1))/1024;
                    long sentTime = Long.parseLong(sentence.substring(sentence.indexOf("-")+1,sentence.indexOf(",")-1));
                    long tranTime = System.currentTimeMillis()-sentTime;
                    Log.d(TAG,"IMAGE return back infor: " + sentence);
                    Log.d(TAG,"Image size is: " + imageSize + "KB, transmission time is: " + tranTime);
                } else{
                }

                /////////////////////////////////////////////////////

                //this.send(sentence);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    //send data back to UDPClient
    public void send(String data) {

        byte[] sendData = data.getBytes();
        Log.d(TAG,String.valueOf(remoteIPAddress));
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteIPAddress, remotePort);
        try {
            localSocket.send(sendPacket);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String[] params = new String[]{data};
        UDPSendRequest sendRequest = new UDPSendRequest();
        sendRequest.execute(params);
    }


    private class UDPSendRequest extends AsyncTask<String, Void, String> {

        protected void onPostExecute(String result) {
            Log.d(TAG, "uploading result:" + result);
            this.cancel(true);
        }
        protected String doInBackground(String... params) {
            String data = params[0];
            byte[] sendData = data.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteIPAddress, remotePort);
            try {
                localSocket.send(sendPacket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return "";
        }
    }

}
