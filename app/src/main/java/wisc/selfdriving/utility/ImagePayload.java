package wisc.selfdriving.utility;

import org.opencv.core.MatOfByte;

/**
 * Created by lkang on 4/19/17.
 */


public class ImagePayload {
    MatOfByte buffer_;

    public ImagePayload(MatOfByte input) {
        buffer_ = input;
    }

}

