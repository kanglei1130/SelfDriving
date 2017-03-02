package wisc.selfdriving;

/**
 * Created by lkang on 1/25/17.
 */

public class OpencvNativeClass {
    public native static int convertGray(long matAddrRgba, long matAddrGray);
    public native static double getSteeringAngle();
    public native static double getAcceleration();
}
