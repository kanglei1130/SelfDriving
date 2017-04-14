package wisc.selfdriving.service;

/**
 * Created by wei on 2/23/17.
 */

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wei on 2/23/17.
 */


public class SerialPortService extends Service {

    private final String TAG = "Serial Port Service";
    public final String ACTION_USB_PERMISSION = "wisc.selfdriving.arduinousb.USB_PERMISSION";
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    int rotationNumber = 0;
    double previousTime = 0.00;

    private final Binder binder_ = new SerialBinder();
    private AtomicBoolean isRunning_ = new AtomicBoolean(false);


    public class SerialBinder extends Binder {
        public SerialPortService getService() {
            return SerialPortService.this;
        }
        public String sendCommand(String cmd) {
            if (serialPort != null) {
                Log.d(TAG,cmd);
                serialPort.write(cmd.getBytes());
                return cmd;
            } else {
                Log.d(TAG, cmd + " serialPort is null");
                return cmd;
            }
        }
    }

    public void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        registerReceiver(broadcastReceiver, filter);
        Log.d(TAG,"registerReceiver");
    }



    //auto start
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return START_STICKY;
    }

    //auto desotry
    public void onDestroy() {
        Log.d(TAG, "stop service");
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        } else {
            Log.d(TAG,"broadcastReceiver is null");
        }

        if (serialPort != null) {
            serialPort.close();
        }
        isRunning_.set(false);
        stopSelf();
    }

    private void startService() {
        Log.d(TAG, "start service");
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        Log.d(TAG, String.valueOf(usbDevices.size()));
        if (!usbDevices.isEmpty()) {
            Log.d(TAG,"usbDevices.is NOT Empty");
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(device, pi);
                keep = false;
                if (!keep)
                    break;
            }
        } else {
            Log.e(TAG, "usb device list is empty");
        }
        isRunning_.set(true);
        registerReceiver();
    }


    String buffer = "";
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                for(int i = 0; i < data.length(); ++i) {
                    String tmp = data.substring(i, i + 1);
                    buffer += tmp;
                    while(buffer.contains("\n")) {
                        int newline = buffer.indexOf("\n");
                        String command = buffer.substring(0, newline);
                        buffer = buffer.substring(newline + 1);
                        Log.d(TAG,command);
                        detectRotation(command);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            //
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        Log.d(TAG,"serialPort is not null in onReceive ");
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            serialPort.setBaudRate(115200);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            Log.d(TAG, "Serial Connection Opened!\n");

                        } else {
                            Log.d(TAG, "PORT NOT OPEN");
                        }
                    } else {
                        Log.d(TAG, "PORT IS NULL");
                    }
                } else {
                    Log.d(TAG, "PERM NOT GRANTED");
                }
            }
        };
    };

    /**
     *
     * @param data
     * @return
     */
    private int detectRotation(String data){
        if (data.contains("rotation(1.0)")) {
            rotationNumber++;
            double speed = calculateSpeed(rotationNumber);
            Log.d(TAG, "No."+rotationNumber + " rotation detected");
            Log.d(TAG, "Speed of this rotation is " + String.valueOf(speed));
            sendHallData(speed,rotationNumber);
            return rotationNumber;
        } else {
            return rotationNumber;
        }
    }

    private double calculateSpeed(int rotation){;
        double currenttime= System.currentTimeMillis();
        double speed = 1000.00/(currenttime-previousTime);
        previousTime = currenttime;
        return speed;
    }

    public class SerialReading {
        public double speed_;
        public int rotation_;

        public SerialReading(double speed, int rotation){
            this.speed_ = speed;
            this.rotation_ = rotation;
        }
    }

    private void sendHallData(double speed, int rotation) {

        SerialReading obj = new SerialReading(speed, rotation);
        Gson gson = new Gson();
        String json = gson.toJson(obj);

        Intent intent = new Intent("SerialPort");
        intent.putExtra("speedAndRotation", json);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder_;
    }

}
