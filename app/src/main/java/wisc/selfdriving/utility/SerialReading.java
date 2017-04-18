package wisc.selfdriving.utility;

/**
 * Created by wei on 4/18/17.
 */

//make obj for sending HallData
public class SerialReading {
    public double speed_;
    public int rotation_;

    public SerialReading(double speed, int rotation){
        this.speed_ = speed;
        this.rotation_ = rotation;
    }
}