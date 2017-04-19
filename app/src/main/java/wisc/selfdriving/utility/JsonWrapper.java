package wisc.selfdriving.utility;

/**
 * Created by lkang on 4/19/17.
 */

public class JsonWrapper {
    public static final String IMAGE = "image";
    public static final String CONTROL = "control";

    private String type_;
    private Object data_;
    private long time_;

    public JsonWrapper(String type, Object data) {
        this.type_ = type;
        this.data_ = data;
        this.time_ = System.currentTimeMillis();
    }
}
