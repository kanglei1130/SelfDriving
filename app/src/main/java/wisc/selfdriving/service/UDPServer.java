package wisc.selfdriving.service;

/**
 * Created by wei on 4/13/17.
 */

import android.nfc.Tag;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class UDPServer implements Runnable {
    public DatagramSocket serverSocket = null;
    public InetAddress clientIPAddress = null;
    public int clientPort = 4444;
    public int serverPort = 55555;
    String IPName = "192.168.1.102";
    private static final String TAG = "UDPServer";

    public UDPServer() {
        try {
            serverSocket = new DatagramSocket(serverPort);
            //serverSocket.setReuseAddress(true);
            clientIPAddress = InetAddress.getByName(IPName);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void send(String data) {
        Log.d(TAG,"send " + data);
        byte[] sendData = data.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIPAddress, clientPort);
        try {
            serverSocket.send(sendPacket);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void run() {
        // TODO Auto-generated method stub
        byte[] receiveData = new byte[1024];
        while(true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                serverSocket.receive(receivePacket);
                String sentence = new String(receiveData,0,receivePacket.getLength());
                Log.d(TAG, "RECEIVED: " + sentence);
                clientIPAddress = receivePacket.getAddress();
                clientPort = receivePacket.getPort();
                Log.d(TAG, clientIPAddress.toString());
                Log.d(TAG, String.valueOf(clientPort));

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
