#include <iostream>
#include <ctime>
#include <opencv2/core/core.hpp>
#include <iostream>
#include <vector>
#include <opencv2/opencv.hpp>
#include <opencv2/video.hpp>
#include <opencv2/videoio.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/highgui.hpp>

using namespace cv;
using namespace std;

#define leftturn_THRESHOLD 5000
#define rightturn_THRESHOLD 5000

/* Return an integer:
     - 0 for nothing found,
     - 1 for stop-sign,
     - 2 for red light,
     - 3 for green light
*/
int detectObjects_CASCADE(Mat mat, string stopsign_xml, string trafficlight_xml) {
    CascadeClassifier stopSignDetector, trafficLightDetector;
    stopSignDetector.load(stopsign_xml);
    trafficLightDetector.load(trafficlight_xml);

    Mat detectorMrgba;
    mat.copyTo(detectorMrgba);
    vector<Rect> targetVectors;
    Vec3b rgb;

    stopSignDetector.detectMultiScale(detectorMrgba, targetVectors);
    if (targetVectors.size() > 0)
        return 1;
    else
        cout << "No stop sign found!" << endl;

    trafficLightDetector.detectMultiScale(mat, targetVectors);
    for(size_t i = 0 ; i < targetVectors.size() ; ++i) {
        Rect rect = targetVectors[i];
        for (int x = rect.x; x+3 < rect.x+rect.width; x += 3) {
            for (int y = rect.y; y+3 < rect.y+rect.height; y+= 3) {
                rgb = mat.at<Vec3b>(x, y);
                // if (rgb == NULL)
                //    continue;
                if (rgb[0] - rgb[1] > 40)
                    return 2;
                else if (rgb[1] - rgb[0] > 40)
                    return 3;
            }
        }
    }
    return 0; // nothing find
}

static double meanSquareError(const Mat &img1, const Mat &img2) {
    Mat s1;
    absdiff(img1, img2, s1);   // |img1 - img2|
    s1.convertTo(s1, CV_32F);  // cannot make a square on 8 bits
    s1 = s1.mul(s1);           // |img1 - img2|^2
    Scalar s = sum(s1);        // sum elements per channel
    double sse = s.val[0] + s.val[1] + s.val[2];  // sum channels
    double mse = sse / (double)(img1.channels() * img1.total());
    return mse;
}

/* Return an integer:
     - 4 for left-turn-sign,
     - 5 for right-turn-sign
*/
int detectObjects_MSE(string left_prototype, string right_prototype, Mat mat) {
    // resize the image
    int width = 500;
    int height = width * mat.rows / mat.cols;
    resize(mat, mat, Size(width, height));

    Mat left_prototypeImg = imread(left_prototype);
    Mat right_prototypeImg = imread(right_prototype);

    int left_minMSE = INT_MAX;
    int right_minMSE = INT_MAX;

    Mat left_tmpImg, right_tmpImg;
    left_prototypeImg.copyTo(left_tmpImg);
    right_prototypeImg.copyTo(right_tmpImg);

    double left_ratio = (double)left_tmpImg.rows / left_tmpImg.cols;
    double right_ratio = (double)right_tmpImg.rows / right_tmpImg.cols;

    Mat window;
    double wsize = left_tmpImg.cols < right_tmpImg.cols ? left_tmpImg.cols : right_tmpImg.cols;
    while (wsize > 20) {
        if (left_tmpImg.rows < 18 || left_tmpImg.cols < 18)
            break;
        if (left_tmpImg.rows > 400 || left_tmpImg.cols > 400) {
            wsize /= 1.5;
            resize(left_tmpImg, left_tmpImg, Size(wsize, wsize * left_ratio));
            continue;
        }
        if (right_tmpImg.rows > 400 || right_tmpImg.cols > 400) {
            wsize /= 1.5;
            resize(right_tmpImg, right_tmpImg, Size(wsize, wsize * right_ratio));
            continue;
        }

        for (int y = 0; y < mat.rows; y += 8) {
            for (int x = 0; x < mat.cols; x += 8) {
                if (x + left_tmpImg.cols >= mat.cols || y + left_tmpImg.rows >= mat.rows || x + right_tmpImg.cols >= mat.cols || y + right_tmpImg.rows >= mat.rows)
                    continue;
                Rect R1(x, y, left_tmpImg.cols, left_tmpImg.rows); // create a rectangle
                Rect R2(x, y, right_tmpImg.cols, right_tmpImg.rows); // create a rectangle
                window = mat(R1);           // crop the region of interest using above rectangle
                double left_tempSim = meanSquareError(left_tmpImg, window);
                window.release();
                window = mat(R2);           // crop the region of interest using above rectangle
                double right_tempSim = meanSquareError(right_tmpImg, window);
                window.release();

                if (left_tempSim < left_minMSE) {
                    left_minMSE = (int)(left_tempSim);
                }
                if (right_tempSim < right_minMSE) {
                    right_minMSE = (int)(right_tempSim);
                }
            }
        }
        wsize /= 1.5;
        resize(left_tmpImg, left_tmpImg, Size(wsize, wsize * left_ratio));
        resize(right_tmpImg, right_tmpImg, Size(wsize, wsize * right_ratio));
    }
    mat.release();

    if (left_minMSE < leftturn_THRESHOLD) {
        return 4;
    } else if (right_minMSE < rightturn_THRESHOLD) {
        return 5;
    }
    return 0;
}
