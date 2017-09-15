package wisc.selfdriving.imageprocess;

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

    public static String Base64Encode(byte[] in){
        StringBuilder out = new StringBuilder((in.length * 4) / 3);
        int b;
        for (int i = 0; i < in.length; i += 3){
            b = (in[i] & 0xFC) >> 2;
            out.append(CODES.charAt(b));
            b = (in[i] & 0x03) << 4;
            if (i + 1 < in.length){
                b |= (in[i + 1] & 0xF0) >> 4;
                out.append(CODES.charAt(b));
                b = (in[i + 1] & 0x0F) << 2;
                if (i + 2 < in.length){
                    b |= (in[i + 2] & 0xC0) >> 6;
                    out.append(CODES.charAt(b));
                    b = in[i + 2] & 0x3F;
                    out.append(CODES.charAt(b));
                }else{
                    out.append(CODES.charAt(b));
                    out.append('=');
                }
            }else{
                out.append(CODES.charAt(b));
                out.append("==");
            }
        }
        return out.toString();
    }

    //CODE sample for 64base coding
    private static final String CODES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
}
