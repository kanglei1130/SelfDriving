package wisc.selfdriving.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;

import wisc.selfdriving.utility.CarControl;

public class UDPService extends Service implements Runnable {

    private static final String TAG = "UDPService";
    //UDPServer udpserver = new UDPServer();
    private final Binder binder_ = new UDPService.UDPBinder();
    public String ip = "";
    public String order = "";
    public String rotation = "0";





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
        getIpAddress();
        Log.d(TAG,ip);
        return START_STICKY;
    }

    private void startService() {
        Log.d(TAG,"Start UDP server");
        try {
            serverSocket = new DatagramSocket(serverPort);
            clientIPAddress = InetAddress.getByName(IPName);
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


    private void sendUDPServerOrder(CarControl control) {

        Gson gson = new Gson();
        String json = gson.toJson(control);
        Log.d(TAG, "sending" + json);

        Intent intent = new Intent("UDPserver");
        intent.putExtra("order", json);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        this.send(json);
    }

    ///////////////////////////////////////////////////////////////////////////////////////


    public DatagramSocket serverSocket = null;
    public InetAddress clientIPAddress = null;
    public int clientPort = 4444;
    public int serverPort = 55555;
    String IPName = "192.168.1.102";
    private Boolean UDPThreadRunning = null;


    CarControl control;

    public void run() {
        control = new CarControl();
        // TODO Auto-generated method stub
        Log.d(TAG, "start receiving thread");
        byte[] receiveData = new byte[1024];
        UDPThreadRunning = true;
        while (UDPThreadRunning.booleanValue()) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                serverSocket.receive(receivePacket);
                String sentence = new String(receiveData, 0, receivePacket.getLength());
                Log.d(TAG, "RECEIVED: " + sentence);

                clientIPAddress = receivePacket.getAddress();
                clientPort = receivePacket.getPort();
                Log.d(TAG, clientIPAddress.toString());
                Log.d(TAG, String.valueOf(clientPort));

                Gson gson = new GsonBuilder().create();
                CarControl command = gson.fromJson(sentence, CarControl.class);

                //control forward, stop and backward
                control.speed_ += command.speed_;
                control.rotation_ += command.rotation_;

                control.speed_ = Math.max(control.speed_, 0);
                control.speed_ = Math.min(control.speed_, 10);


                control.rotation_ = Math.max(control.rotation_, 0);
                control.rotation_ = Math.min(control.rotation_, 10);



                sendUDPServerOrder(control);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public void send(String data) {
        Log.d(TAG, data);
        byte[] sendData = data.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIPAddress, clientPort);
        try {
            serverSocket.send(sendPacket);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
