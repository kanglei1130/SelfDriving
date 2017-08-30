package wisc.selfdriving.utility;

/**
 * Created by wei on 8/11/17.
 */

/**
 * speed and steering are both direct from controller
 */
public class CarControl {

    public float throttle_;
    public float steering_;
    public long time_;

    public CarControl() {
        this.throttle_ = (float) 0.0;
        this.steering_ = (float) 0.0;
        this.time_ = System.currentTimeMillis();
    }
}
