package wisc.selfdriving.utility;

/**
 * Created by wei on 4/18/17.
 */

//make order as object in order to send from UDPService to main
public class CarControl {

    public int speed_;
    public int rotation_;

    public CarControl() {
        this.speed_ = 0;
        this.rotation_ = 5;
    }
}
