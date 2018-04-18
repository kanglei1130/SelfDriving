package wisc.selfdriving.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.google.gson.Gson;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import wisc.selfdriving.OpencvNativeClass;
import wisc.selfdriving.R;
import wisc.selfdriving.service.SerialPortConnection;
import wisc.selfdriving.service.SerialPortService;
import wisc.selfdriving.utility.SerialReading;



public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";
    Button startButton,testButton;
    JavaCameraView javaCameraView;

    Mat mRgba, mGray;

    //ObjectDetector detector;
    File mCascadeFile_stop;
    File mCascadeFile_trafficlight;
    boolean isStart = false;
    long previousTimer = 0;

    public int rotation = 0;

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

    /**
     * 1. turn on the RC car
     * 2. open the app
     * 3. plug USB serial port (press okay to allow usb serial communication)
     * @param savedInstanceState
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.btnstart);
        testButton = (Button) findViewById(R.id.btntest);
        setButtonOnClickListener();

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

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("SerialPort"));


        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA, Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.WAKE_LOCK
            }, 1001);
        }
        //loadCascadexml();
    }

    public void setButtonOnClickListener() {
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setEnabled(false);
                //control the button activity
                startSerialService();
            }
        });
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSerialPortConnection != null) {
                    if(mSerialPortConnection.sendCommandFunction("throttle(1.0)") != 1) {
                        Log.e(TAG, "serial port send failed");
                    }
                } else {
                    Log.d(TAG, "mSerialPortConnection is null");
                }
            }
        });
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
    }

    @Override
    public void onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped");
        mRgba.release();
    }

    //load cascade when use java based detector
   /* public void loadCascadexml(){
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is_stop = getResources().openRawResource(R.raw.stop_sign);
            File cascadeDir_stop = getDir("cascade_stop", Context.MODE_PRIVATE);
            mCascadeFile_stop = new File(cascadeDir_stop, "cascade_stop.xml");
            FileOutputStream os_stop = new FileOutputStream(mCascadeFile_stop);
            Log.d(TAG, "left cascade" + cascadeDir_stop.getAbsolutePath());

            InputStream is_trafficlight = getResources().openRawResource(R.raw.traffic_light);
            File cascadeDir_trafficlight = getDir("cascade_trafficlight", Context.MODE_PRIVATE);
            mCascadeFile_trafficlight = new File(cascadeDir_trafficlight, "cascade_trafficlight.xml");
            FileOutputStream os_trafficlight = new FileOutputStream(mCascadeFile_trafficlight);

            byte[] buffer2 = new byte[80096];
            int bytesRead2;
            while ((bytesRead2 = is_stop.read(buffer2)) != -1) {
                os_stop.write(buffer2, 0, bytesRead2);
            }

            byte[] buffer1 = new byte[80096];
            int bytesRead1;
            while ((bytesRead1 = is_trafficlight.read(buffer1)) != -1) {
                os_trafficlight.write(buffer1, 0, bytesRead1);
            }

            is_stop.close();
            is_trafficlight.close();
            os_stop.close();
            os_trafficlight.close();

        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }
    }*/

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

       if(!isStart){
            Thread controlThread = new Thread(detectorThread);
            controlThread.start();
            isStart = true;
        }
        return mRgba;
    }

    Runnable detectorThread = new Runnable() {
        @Override
        public void run() {
            while (true) {
                //Todo reszie  the mRgba or lower down the fequecy of mRgba detector
                if(true) {
                    //detector = new ObjectDetector();//if use java Detector

                    final long startTime = System.currentTimeMillis();
                    int result = 0;
                    //control detect frequency
                    //if (startTime - previousTimer>500) {
                    Mat detectorMrgba = new Mat();
                    mRgba.copyTo(detectorMrgba);

                    //the xml file is in sdcard
                    result = OpencvNativeClass.detector(detectorMrgba.getNativeObjAddr(),"/sdcard/stop_sign.xml", "/sdcard/traffic_light.xml", "/sdcard/Pictures/left_turn_prototype.png", "/sdcard/Pictures/right_turn_prototype.png");//internal storage "/data/user/0/wisc.selfdriving/app_cascade_trafficlight"
                    //result = detector.detectObjects_CASCADE(detectorMrgba, mCascadeFile_stop, mCascadeFile_trafficlight);//use if detect by java class
                    /*if (result==0) {
                        mRgba.copyTo(detectorMrgba);
                        result = detector.detectObjects_MSE(detectorMrgba,mFile_leftturn,mFile_rightturn);
                    }*/
                    detectorMrgba.release();
                    //detector = null;

                    final long endTime = System.currentTimeMillis();
                    Log.d(TAG, "Total execution time: " + (endTime - startTime) + "ms");
                    previousTimer = endTime;

                    switch (result) {
                        case 1:
                            //Log.d(TAG,"Stop-sign");
                            if (mSerialPortConnection != null) {
                                mSerialPortConnection.sendCommandFunction("throttle(0.0)");
                            }
                            break;
                        case 2:
                            //Log.d(TAG,"Red-light");
                            if (mSerialPortConnection != null) {
                                mSerialPortConnection.sendCommandFunction("throttle(0.0)");
                            }
                            break;
                        case 3:
                            //Log.d(TAG,"Green-light");
                            if (mSerialPortConnection != null) {
                                mSerialPortConnection.sendCommandFunction("throttle(1.1)");
                            }
                            break;
                        case 4:
                            //Log.d(TAG,"Left-turn");
                            if (mSerialPortConnection != null) {
                                mSerialPortConnection.sendCommandFunction("steering(0.2)");
                            }
                            break;
                        case 5:
                            //Log.d(TAG,"Right-turn");
                            if (mSerialPortConnection != null) {
                                mSerialPortConnection.sendCommandFunction("steering(0.8)");
                            }
                            break;
                        default:
                            Log.d(TAG,"Nothing Found!");
                            break;
                    }
                }

            }
        }
    };


    //initial SerialPortConnection
    private static Intent mSerial = null;
    private static SerialPortConnection mSerialPortConnection = null;

    private void startSerialService() {
        Log.d(TAG, "start serial service");
        mSerial = new Intent(this, SerialPortService.class);
        mSerialPortConnection = new SerialPortConnection();
        bindService(mSerial, mSerialPortConnection, Context.BIND_AUTO_CREATE);
        startService(mSerial);
    }

    private void stopSerialService() {

        Log.d(TAG, "stop serial service");
        if(mSerial != null && mSerialPortConnection != null) {
            mSerialPortConnection.sendCommandFunction("throttle(0.0)");
            mSerialPortConnection.sendCommandFunction("steering(0.5)");

            unbindService(mSerialPortConnection);
            stopService(mSerial);
            mSerial = null;
            mSerialPortConnection = null;
        }
    }

    //receive data from USBSerial
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //get rotation and speed data from USBSerial
            String speedAndRotation = intent.getStringExtra("serialMessage");
            Gson gson = new Gson();
            SerialReading reading = gson.fromJson(speedAndRotation, SerialReading.class);

            //rotation status and send back to UDP service
            rotation = reading.rotation_;
            long time = reading.time_;
            long rrt = System.currentTimeMillis() - time;
            Log.d(TAG, "RRT:" + rrt);
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1001: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(TAG, "Got permission to use location");
                }
            }
        }
    }

}
