package wisc.selfdriving.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Button;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.io.File;

import wisc.selfdriving.OpencvNativeClass;
import wisc.selfdriving.R;
import wisc.selfdriving.imageprocess.ImageProcess;
import wisc.selfdriving.service.SerialPortConnection;
import wisc.selfdriving.service.SerialPortService;
import wisc.selfdriving.service.UDPService;
import wisc.selfdriving.service.UDPServiceConnection;
import wisc.selfdriving.utility.CarControl;
import wisc.selfdriving.utility.Constants;
import wisc.selfdriving.utility.SerialReading;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";
    Button startButton,testButton;
    JavaCameraView javaCameraView;

    VideoWriter videoWriter = null;
    private ImageProcess imageProcess = null;
    Mat mRgba, mGray;

    public int rotation = 0;
    int numToRotate = 100;

    static {
        System.loadLibrary("MyOpencvLibs");
    }

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    javaCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.btnstart);
        testButton = (Button) findViewById(R.id.btntest);

        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(View.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.MODEL.equals("Nexus 5X")){
            //Nexus 5X's screen is reversed, ridiculous! the image sensor does not fit in corrent orientation
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        setUiEnabled(false);
    }

    public void onClickStart(View view) {

        startUDPService();
        //control the button activity
        setUiEnabled(true);

        startSerialService();
        //receive data from SerialPort
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("SerialPort"));
        //receive data from UDPService
        LocalBroadcastManager.getInstance(this).registerReceiver(orderMessageReceiver, new IntentFilter("UDPserver"));
    }

    //control the button activity
    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        testButton.setEnabled(bool);
    }

    //test the currency of HallData, press Test, the car will go forward for 5
    public void onClickTest(View view) {
        Log.d(TAG,"test begin");
        if (mSerialPortConnection != null) {
            mSerialPortConnection.sendCommandFunction("throttle(1.0)");
            if(mSerialPortConnection.sendCommandFunction("throttle(1.0)")==1){
            }
        } else {
            Log.d(TAG, "mSerialPortConnection is null");
        }
    }

    protected void onPause() {
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        stopSerialService();
        stopUDPService();
    }

    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC4);

        imageProcess = new ImageProcess(1.0, width, height);

        File videoDir = new File(Constants.kVideoFolder);
        if(!videoDir.exists()) {
            videoDir.mkdir();
        }
        videoWriter = new VideoWriter();
        String file = Constants.kVideoFolder.concat("test.avi");
        boolean openVW = videoWriter.open(file, VideoWriter.fourcc('M','J','P','G'), 10.0, new Size(width, height));
        if(openVW == false) {
            Log.e(TAG, "open video file failed");
        }
    }

    @Override
    public void onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped");
        mRgba.release();
        videoWriter.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        long start = System.currentTimeMillis();
        OpencvNativeClass.convertGray(mRgba.getNativeObjAddr(), mGray.getNativeObjAddr());
        long end = System.currentTimeMillis();
        //Log.d(TAG, "It took " + (end - start) + "ms to process the image");

        videoWriter.write(mRgba);

        //get encoded image and send to server
        MatOfByte buf = imageProcess.getCompressedData(mRgba);

        return mGray;
    }

    //initial SerialPortConnection
    private static Intent mSerial = null;
    private static SerialPortConnection mSerialPortConnection = null;

    private void startSerialService() {
        mSerial = new Intent(this, SerialPortService.class);
        mSerialPortConnection = new SerialPortConnection();
        bindService(mSerial, mSerialPortConnection, Context.BIND_AUTO_CREATE);
        startService(mSerial);
    }

    private void stopSerialService() {
        if(mSerial != null && mSerialPortConnection != null) {
            unbindService(mSerialPortConnection);
            stopService(mSerial);
            mSerial = null;
            mSerialPortConnection = null;
        }
    }

    //initial UDPConnetion
    private static Intent mUDP = null;
    private static UDPServiceConnection mUDPConnection = null;

    private void startUDPService() {
        Log.d(TAG, "startUDPService");
        mUDP = new Intent(this, UDPService.class);
        mUDPConnection = new UDPServiceConnection();
        bindService(mUDP, mUDPConnection, Context.BIND_AUTO_CREATE);
        startService(mUDP);
    }

    private void stopUDPService() {
        if(mUDP != null && mUDPConnection != null) {
            unbindService(mUDPConnection);
            stopService(mUDP);
            mUDP = null;
            mUDPConnection = null;
        }
    }

    //receive data from USBSerial
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //get rotation and speed data from USBSerial
            String speedAndRotation = intent.getStringExtra("speedAndRotation");
            Gson gson = new GsonBuilder().create();
            SerialReading reading = gson.fromJson(speedAndRotation, SerialReading.class);

            //rotation status and send back to UDP service
            rotation = reading.rotation_;
            mUDPConnection.sendData(String.valueOf(rotation));

            //used for test, go straight forward for numToRotate rotations
            /*
            if (rotation>numToRotate-1){
                Log.d(TAG, "rotation over " + numToRotate);
                if (mSerialPortConnection != null) {
                    mSerialPortConnection.sendCommandFunction("throttle(0.0)");
                    Log.d(TAG, "shold stop");
                } else {
                    Log.d(TAG, "mSerialPortConnection is null");
                }
            }
            */
        }
    };

    //receive data from UDPService
    private BroadcastReceiver orderMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //get order from UDPService
            String getOrder = intent.getStringExtra("order");
            Gson gson = new Gson();
            CarControl control = gson.fromJson(getOrder, CarControl.class);

            //detect order from UDPService and move as order

            double throttle = 0.0;
            if(control.speed_ != 0) {
                throttle = (double) control.speed_ * 0.02 + 1.0;
            }

            double steering = (double)control.rotation_ / 10.0;
            mSerialPortConnection.sendCommandFunction("throttle(" + String.valueOf(throttle) + ")");
            mSerialPortConnection.sendCommandFunction("steering(" + String.valueOf(steering) + ")");

        }
    };

}
