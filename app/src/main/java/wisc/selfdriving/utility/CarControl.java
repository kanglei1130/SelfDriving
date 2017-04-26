package wisc.selfdriving.utility;

/**
 * Created by wei on 4/18/17.
 */

//make order as object in order to send from UDPService to main
public class CarControl {

    /**
     * speed is from 0.0 to 10.0
     * steering is from 0.0  to 10.0, 5.0 by default
     */
    public double speed_;
    public double steering_;

    public CarControl() {
        this.speed_ = 0.0;
        this.steering_ = 5.0;
    }

    public void setRelativeControl() {
        this.steering_ = 0;
    }
}
