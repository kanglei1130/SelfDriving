package wisc.selfdriving;

import android.content.res.AssetManager;
import org.opencv.core.Mat;


/**
 * Created by lkang on 1/25/17.
 */

public class OpencvNativeClass {
    public native static int convertGray(long matAddrRgba, long matAddrGray);
    public native static int detector(long ptr, String a, String b, String c, String d);

    public native static double getSteeringAngle();
    public native static double getAcceleration();
}
