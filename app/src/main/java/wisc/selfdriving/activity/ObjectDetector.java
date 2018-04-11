package wisc.selfdriving.activity;

import android.content.Context;
import android.nfc.Tag;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import wisc.selfdriving.R;

import static org.opencv.core.Core.absdiff;
import static org.opencv.core.Core.randu;
import static org.opencv.core.Core.sumElems;
import static org.opencv.core.Core.trace;
import static org.opencv.core.CvType.CV_32F;

public class ObjectDetector {

    private static String TAG = "ObjectDetector";

    public static int leftturn_THRESHOLD = 5000;
    public static int rightturn_THRESHOLD = 5000;


    /* Computes Mean Square Error between two n-d matrices. */
    /* Lower value means more similar. */
    public static double meanSquareError(Mat img1, Mat img2) {
        Mat s1 = Mat.zeros(img1.rows(), img1.cols(), CV_32F);
        if (img1.cols() != img2.cols() || img1.rows() != img2.rows())
            Imgproc.resize(img2, img2, new Size(img1.cols(), img1.rows()));
        Core.absdiff(img1, img2, s1);   // |img1 - img2|
        s1.convertTo(s1, CV_32F);  // cannot make a square on 8 bits
        s1 = s1.mul(s1);           // |img1 - img2|^2
        Scalar s = sumElems(s1);        // sum elements per channel
        double sse = s.val[0] + s.val[1] + s.val[2];  // sum channels
        double mse = sse / (double)(img1.channels() * img1.total());
        s1.release();
        System.gc();
        System.runFinalization();
        return mse;
    }

    /* Detect stop sign with simple method by comparing MSE */
    /* Return an integer:
         - 0 for nothing found,
         - 1 for stop-sign,
         - 2 for red light,
         - 3 for green light,
         - 4 for left-turn-sign,
         - 5 for right-turn-sign
    */
    public static int detectObjects_MSE(Mat mat,File left,File right) {
        // resize the image
        int width = 500;
        int height = width * mat.rows() / mat.cols();
        Imgproc.resize(mat, mat, new Size(width, height));

        Mat left_prototypeImg = Imgcodecs.imread("/sdcard/Pictures/left_turn_prototype.png", Imgcodecs.IMREAD_COLOR);
        Mat right_prototypeImg = Imgcodecs.imread("/sdcard/Pictures/right_turn_prototype.png", Imgcodecs.IMREAD_COLOR);

        int left_minMSE = Integer.MAX_VALUE;
        int right_minMSE = Integer.MAX_VALUE;
        Mat left_tmpImg = new Mat();
        Mat right_tmpImg = new Mat();
        left_prototypeImg.copyTo(left_tmpImg);
        right_prototypeImg.copyTo(right_tmpImg);
        double left_ratio = (double)left_tmpImg.rows() / left_tmpImg.cols();
        double right_ratio = (double)right_tmpImg.rows() / right_tmpImg.cols();
        Mat window;
        double wsize = left_tmpImg.cols() < right_tmpImg.cols() ? left_tmpImg.cols() : right_tmpImg.cols();
        while (wsize > 20) {
            if (left_tmpImg.rows() < 18 || left_tmpImg.cols() < 18)
                break;
            if (left_tmpImg.rows() > 400 || left_tmpImg.cols() > 400) {
                wsize /= 1.5;
                Imgproc.resize(left_tmpImg, left_tmpImg, new Size(wsize, wsize * left_ratio));
                continue;
            }
            if (right_tmpImg.rows() > 400 || right_tmpImg.cols() > 400) {
                wsize /= 1.5;
                Imgproc.resize(right_tmpImg, right_tmpImg, new Size(wsize, wsize * right_ratio));
                continue;
            }

            for (int y = 0; y < mat.rows(); y += 8) {
                for (int x = 0; x < mat.cols(); x += 8) {
                    if (x + left_tmpImg.cols() >= mat.cols() || y + left_tmpImg.rows() >= mat.rows() || x + right_tmpImg.cols() >= mat.cols() || y + right_tmpImg.rows() >= mat.rows())
                        continue;
                    Rect R1 = new Rect(x, y, left_tmpImg.cols(), left_tmpImg.rows()); // create a rectangle
                    Rect R2 = new Rect(x, y, right_tmpImg.cols(), right_tmpImg.rows()); // create a rectangle
                    window = new Mat(mat, R1);           // crop the region of interest using above rectangle
                    double left_tempSim = meanSquareError(left_tmpImg, window);
                    window.release();
                    System.gc();
                    System.runFinalization();
                    window = new Mat(mat, R2);           // crop the region of interest using above rectangle
                    double right_tempSim = meanSquareError(right_tmpImg, window);
                    window.release();
                    System.gc();
                    System.runFinalization();

                    if (left_tempSim < left_minMSE) {
                        left_minMSE = (int)(left_tempSim);
                    }
                    if (right_tempSim < right_minMSE) {
                        right_minMSE = (int)(right_tempSim);
                    }
                }
            }
            wsize /= 1.5;
            Imgproc.resize(left_tmpImg, left_tmpImg, new Size(wsize, wsize * left_ratio));
            Imgproc.resize(right_tmpImg, right_tmpImg, new Size(wsize, wsize * right_ratio));
        }
        mat.release();
        System.gc();
        System.runFinalization();

        if (left_minMSE < leftturn_THRESHOLD) {
            return 4;
        } else if (right_minMSE < rightturn_THRESHOLD) {
            return 5;
        }
        return 0;
    }

    public static int detectObjects_CASCADE(Mat mat,File stopsign_xml,File trafficlight_xml) {
        CascadeClassifier stopSignDetector = new CascadeClassifier(stopsign_xml.getAbsolutePath());

        Mat detectorMrgba = new Mat();
        mat.copyTo(detectorMrgba);
        MatOfRect targetVectors = new MatOfRect();
        double[] rgb;
        stopSignDetector.detectMultiScale(detectorMrgba, targetVectors);
        if (targetVectors.toArray().length > 0)
            return 1;
        else
            System.out.println("No stop sign found!");

        CascadeClassifier redLightDetector = new CascadeClassifier(trafficlight_xml.getAbsolutePath());
        targetVectors = new MatOfRect();
        redLightDetector.detectMultiScale(mat, targetVectors);
        for (Rect rect : targetVectors.toArray()) {
            for (int x = rect.x; x+3 < rect.x+rect.width; x += 3) {
                for (int y = rect.y; y+3 < rect.y+rect.height; y+= 3) {
                    rgb = mat.get(x, y);
                    if (rgb == null)
                        continue;
                    if (rgb[0] - rgb[1] > 40)
                        return 2;
                    else if (rgb[1] - rgb[0] > 40)
                        return 3;
                }
            }
        }

        mat.release();
        detectorMrgba.release();
        targetVectors.release();
        stopSignDetector = null;
        System.gc();
        System.runFinalization();
        return 0; // nothing find
    }

}
