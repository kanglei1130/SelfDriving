import java.util.ArrayList;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import static org.opencv.core.Core.absdiff;
import static org.opencv.core.Core.sumElems;
import static org.opencv.core.CvType.CV_32F;

public class ObjectDetector {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static String stopsign_address = "C:\\Users\\HAORAN\\Desktop\\stop_sign_prototype.png";
    public static String redlight_address = "C:\\Users\\HAORAN\\Desktop\\red_light_prototype.png";
    public static String greenlight_address = "C:\\Users\\HAORAN\\Desktop\\green_light_prototype.png";
    public static String leftturn_address = "C:\\Users\\HAORAN\\Desktop\\left_turn_prototype.png";
    public static String rightturn_address = "C:\\Users\\HAORAN\\Desktop\\right_turn_prototype.png";
    public static int stopsign_THRESHOLD = 7000;
    public static int redlight_THRESHOLD = 5000;
    public static int greenlight_THRESHOLD = 5500;
    public static int leftturn_THRESHOLD = 5000;
    public static int rightturn_THRESHOLD = 5000;

    /* Computes Mean Square Error between two n-d matrices. */
    /* Lower value means more similar. */
    public static double meanSquareError(Mat img1, Mat img2) {
        Mat s1 = new Mat();
        absdiff(img1, img2, s1);   // |img1 - img2|
        s1.convertTo(s1, CV_32F);  // cannot make a square on 8 bits
        s1 = s1.mul(s1);           // |img1 - img2|^2
        Scalar s = sumElems(s1);        // sum elements per channel
        double sse = s.val[0] + s.val[1] + s.val[2];  // sum channels
        double mse = sse / (double)(img1.channels() * img1.total());
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
    public static int detectObjects(String obj_address) {
        // set thresholds
        ArrayList<Integer> thresholds = new ArrayList<Integer>();
        thresholds.add(stopsign_THRESHOLD);
        thresholds.add(redlight_THRESHOLD);
        thresholds.add(greenlight_THRESHOLD);
        thresholds.add(leftturn_THRESHOLD);
        thresholds.add(rightturn_THRESHOLD);

        // model addresses
        ArrayList<String> addresses = new ArrayList<String>();
        addresses.add(stopsign_address);
        addresses.add(redlight_address);
        addresses.add(greenlight_address);
        addresses.add(leftturn_address);
        addresses.add(rightturn_address);

        System.out.println("Read image from: " + obj_address);
        Mat targetImage = Imgcodecs.imread(obj_address, Imgcodecs.IMREAD_COLOR);

        // resize the image
        int width = 500;
        int height = width * targetImage.rows() / targetImage.cols();
        Imgproc.resize(targetImage, targetImage, new Size(width, height));
        System.out.println("Analyzing ... ...");

        for (int i = 0; i < addresses.size(); i++) {
            // read prototype image
            Mat prototypeImg = Imgcodecs.imread(addresses.get(i), Imgcodecs.IMREAD_COLOR);
            System.out.println("Loading prototype image: " + addresses.get(i));

            int minMSE = Integer.MAX_VALUE;
            Mat tmpImg = prototypeImg.clone();
            double ratio = (double)tmpImg.rows() / tmpImg.cols();
            Mat window;
            for (double wsize = tmpImg.cols(); wsize > 15;) {
                if (tmpImg.rows() < 18 || tmpImg.cols() < 18)
                    break;
                if (tmpImg.rows() > 400 || tmpImg.cols() > 400) {
                    wsize /= 1.5;
                    Imgproc.resize(tmpImg, tmpImg, new Size(wsize, wsize * ratio));
                    continue;
                }

                for (int y = 0; y < targetImage.rows(); y += 7) {
                    for (int x = 0; x < targetImage.cols(); x += 7) {
                        if (x + tmpImg.cols() >= targetImage.cols() || y + tmpImg.rows() >= targetImage.rows())
                            continue;
                        Rect R = new Rect(x, y, tmpImg.cols(), tmpImg.rows()); // create a rectangle
                        window = new Mat(targetImage, R);           // crop the region of interest using above rectangle
                        double tempSim = meanSquareError(tmpImg, window);
                        if (tempSim < minMSE) {
                            minMSE = (int)(tempSim);
                        }
                    }
                }
                wsize /= 1.5;
                Imgproc.resize(tmpImg, tmpImg, new Size(wsize, wsize * ratio));
            }
            if (minMSE < thresholds.get(i)) {
                return i + 1;
            }
        }
        return 0;
    }

    public static void main(String [] args) {
        System.out.println("\n\nOpenCV - Object Detection");
        System.out.println("Welcome to OpenCV " + Core.VERSION + ", lib is: " + Core.NATIVE_LIBRARY_NAME);
        System.out.println("Method: Mean Square Error Method");
        ObjectDetector detector = new ObjectDetector();
        System.out.println("Stop-sign Prototype Address: " + detector.stopsign_address);
        System.out.println("Red-light Prototype Address: " + detector.redlight_address);
        System.out.println("Green-light Prototype Address: " + detector.greenlight_address);
        System.out.println("Left-turn Prototype Address: " + detector.leftturn_address);
        System.out.println("Right-turn Prototype Address: " + detector.rightturn_address);

        final long startTime = System.currentTimeMillis();
        // int result = detector.detectObjects("C:\\Users\\HAORAN\\Desktop\\stopsign.jpg");
        // int result = detector.detectObjects("C:\\Users\\HAORAN\\Desktop\\rightturn.png");
        // int result = detector.detectObjects("C:\\Users\\HAORAN\\Desktop\\leftturn.png");
        // int result = detector.detectObjects("C:\\Users\\HAORAN\\Desktop\\redlight.png");
        int result = detector.detectObjects("C:\\Users\\HAORAN\\Desktop\\greenlight.png");

        final long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");

        switch (result) {
            case 1:
                System.out.println("Stop-sign");
                break;
            case 2:
                System.out.println("Red-light");
                break;
            case 3:
                System.out.println("Green-light");
                break;
            case 4:
                System.out.println("Left-turn");
                break;
            case 5:
                System.out.println("Right-turn");
                ;
                break;
            default:
                System.out.println("Nothing Found!");
                ;
                break;
        }
    }
}