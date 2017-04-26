package wisc.selfdriving.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import wisc.selfdriving.utility.CarControl;

public class UDPService extends Service implements Runnable {

    private static final String TAG = "UDPService";
    //UDPServer udpserver = new UDPServer();
    private final Binder binder_ = new UDPService.UDPBinder();
    public String localIP = "";
    public String order = "";
    public String rotation = "0";
    public DatagramSocket localSocket = null;
    public InetAddress remoteIPAddress = null;
    public int remotePort = 4444;
    public int localPort = 55555;
    String remoteIPName = "192.168.1.100";
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

    private void startService() {
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
        send("UDPServiceSever send: ");
    }



    public void onDestroy() {
        Log.d(TAG,"udpserver connection is closed");
        stopSelf();
        UDPThreadRunning = false;
    }

    //get local ip for smooth user
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
    private void sendUDPServerOrder(CarControl control) {

        Gson gson = new Gson();
        String json = gson.toJson(control);
        Intent intent = new Intent("UDPserver");
        intent.putExtra("order", json);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void run() {
        control = new CarControl();
        // TODO Auto-generated method stub
        Log.d(TAG, "start receiving thread");
        byte[] receiveData = new byte[1024];
        UDPThreadRunning = true;
        while (UDPThreadRunning.booleanValue()) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                localSocket.receive(receivePacket);
                String sentence = new String(receiveData, 0, receivePacket.getLength());
                Log.d(TAG, "RECEIVED: " + sentence);

                //get UDPClient ip and port
                remoteIPAddress = receivePacket.getAddress();
                remotePort = receivePacket.getPort();

                //receive gson data from UDPClient
                Gson gson = new GsonBuilder().create();
                CarControl command = gson.fromJson(sentence, CarControl.class);

                //control speed and turn rotation angle
                sendUDPServerOrder(command);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    //send data back to UDPClient
    public void send(String data) {
        Log.d(TAG, "sending:" + data);
        byte[] sendData = data.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, remoteIPAddress, remotePort);
        try {
            localSocket.send(sendPacket);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
