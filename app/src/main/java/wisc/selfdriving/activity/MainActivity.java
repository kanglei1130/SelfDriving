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
import wisc.selfdriving.utility.ImageWrapper;
import wisc.selfdriving.utility.SerialReading;

import static junit.framework.Assert.assertTrue;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";
    Button startButton,testButton;
    JavaCameraView javaCameraView;

    VideoWriter videoWriter = null;
    private ImageProcess imageProcess = null;
    Mat mRgba, mGray;

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
        //receive data from UDPService
        LocalBroadcastManager.getInstance(this).registerReceiver(orderMessageReceiver, new IntentFilter("UDPserver"));


        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA, Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.WAKE_LOCK
            }, 1001);
        }
    }

    public void setButtonOnClickListener() {
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setEnabled(false);
                startUDPService();
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(orderMessageReceiver);

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

        imageProcess = new ImageProcess(0.2, width, height);

        File videoDir = new File(Constants.kVideoFolder);
        if(!videoDir.exists()) {
            videoDir.mkdir();
        }
        videoWriter = new VideoWriter();
        String file = Constants.kVideoFolder.concat("test.avi");
        // Android OpenCV support MJPG only
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


    long sum = 0;
    int counter = 0;
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        /*
        if(mSerialPortConnection != null && counter%50==0) {
            long start = System.currentTimeMillis();
            int ret = mSerialPortConnection.sendCommandFunction("time(" + String.valueOf(System.currentTimeMillis()) + ")");
            long end = System.currentTimeMillis();

            if(ret != -1) {
                sum += (end - start);
                counter++;
                //Log.d(TAG, "It took " + (end - start) + "ms to process the image" + " average:" + sum / counter);
            }
        }
        counter++;
        */
        //38ms to process an image on average
        //Nexus 5x

        //long start = System.currentTimeMillis();
        int steering = OpencvNativeClass.convertGray(mRgba.getNativeObjAddr(), mGray.getNativeObjAddr());
        //long end = System.currentTimeMillis();
        //sum += (end - start);
        //counter++;
        //Log.d(TAG, "It took " + (end - start) + "ms to process the image" + " average:" + sum/counter);

        /*
        CarControl command = new CarControl();
        command.setRelativeControl();
        command.steering_ = steering;
        updateCarControl(command);
        */

        ////////////////////////////////////////////////////////
        //write video to frames and encode frame to Mat byte then to BufArray, finally to String

        videoWriter.write(mRgba);
        MatOfByte buf = imageProcess.getCompressedData(mRgba);
        byte[] bytes_ = buf.toArray();
        String imgBufString = ImageProcess.Base64Encode(bytes_);
        ImageWrapper imageWrapper = new ImageWrapper(ImageWrapper.IMAGE, imgBufString);

        if(mUDPConnection != null) {
            long start = System.currentTimeMillis();
            Gson gson = new Gson();
            mUDPConnection.sendData(gson.toJson(imageWrapper));
            long end = System.currentTimeMillis();
            Log.d(TAG, "It took " + (end - start) + "ms to process the image");
        }
        //////////////////////////////////////////////////
        return mGray;
    }



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


    //receive data from UDPService
    private BroadcastReceiver orderMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //get order from UDPService and send it to serialport
            String getOrder = intent.getStringExtra("order");
            Gson gson = new Gson();
            //Log.d(TAG,"getOrder: " + getOrder);
            CarControl controller = gson.fromJson(getOrder,CarControl.class);
            if (controller != null) {
                float throttle = (float)0.0;
                float steering = controller.steering_;
                if(controller.throttle_ > 0.5) {
                    throttle = (float) ((controller.throttle_-0.5) * 0.4 + 1.0);
                }
                mSerialPortConnection.sendCommandFunction("throttle(" + String.valueOf(throttle) + ")");
                mSerialPortConnection.sendCommandFunction("steering(" + String.valueOf(steering) + ")");
            }
            //send to UDP remote host that the command is successful
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
