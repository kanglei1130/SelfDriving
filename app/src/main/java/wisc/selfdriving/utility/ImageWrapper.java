package wisc.selfdriving.utility;

/**
 * Created by lkang on 4/19/17.
 */

public class ImageWrapper {
    public static final String IMAGE = "image";
    public static final String CONTROL = "status";

    private String type_;
    private Object data_;
    private long time_;
    private String imgData_;

    public ImageWrapper(String type, Object data) {
        this.type_ = type;
        this.data_ = data;
        this.time_ = System.currentTimeMillis();
    }
    ////////////////////////////////////////////
    public ImageWrapper(String type, String data) {
        this.type_ = type;
        this.imgData_ = data;
        this.time_ = System.currentTimeMillis();
    }
    //////////////////////////////////////////
}
