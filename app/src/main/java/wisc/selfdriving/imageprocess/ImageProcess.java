package wisc.selfdriving.imageprocess;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Created by lkang on 3/10/17.
 */

public class ImageProcess {

    private double compressRatio_ = 1.0;
    private int originalWidth = 0;
    private int originalHeight = 0;
    private int newWidth = 0;
    private int newHeight = 0;

    private static final String TAG = "ImageProcess";


    public ImageProcess(double ratio, int width, int height) {
        this.assignRatio(ratio);
        this.originalWidth = width;
        this.originalHeight = height;
        this.newWidth = (int) (this.originalWidth * this.compressRatio_);
        this.newHeight = (int) (this.originalHeight * this.compressRatio_);
    }

    private void assignRatio(double ratio) {
        if(ratio >= 1.0) {
            this.compressRatio_ = 1.0;
        } else if(ratio <= 0.1) {
            this.compressRatio_ = 0.1;
        } else {
            this.compressRatio_ = ratio;
        }
    }

    public void updateRatio(double ratio) {
        this.assignRatio(ratio);
        this.newWidth = (int) (this.originalWidth * this.compressRatio_);
        this.newHeight = (int) (this.originalHeight * this.compressRatio_);
    }

    public MatOfByte getCompressedData(Mat input) {
        if(this.compressRatio_ != 1.0) {
            Size size = new Size(this.newWidth, this.newHeight);
            Imgproc.resize(input, input, size);
        }
        MatOfByte buf = new MatOfByte();
        Imgcodecs.imencode(".jpg", input, buf);
        return buf;
    }
}
