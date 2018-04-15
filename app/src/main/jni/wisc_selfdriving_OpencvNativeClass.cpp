//
// Created by lkang on 2/8/17.
//

#include <wisc_selfdriving_OpencvNativeClass.h>
//
// Created by lkang on 1/25/17.
//

JNIEXPORT jdouble JNICALL Java_wisc_selfdriving_OpencvNativeClass_getSteeringAngle(JNIEnv *, jclass)
{
    return 0.0;
}
JNIEXPORT jdouble JNICALL Java_wisc_selfdriving_OpencvNativeClass_getAcceleration(JNIEnv *, jclass)
{
    return 0.0;
}


JNIEXPORT jint JNICALL Java_wisc_selfdriving_OpencvNativeClass_convertGray(JNIEnv *, jclass, jlong addrRgba, jlong addrGray)
{
    Mat& mRgb = *(Mat*)addrRgba;
    Mat& mGray = *(Mat*)addrGray;

    int conv;
    jint retVal;
    conv = toGray(mRgb, mGray);

    retVal = (jint)conv;
    return retVal;
}

string jstring2string(JNIEnv *env, jstring jStr) {
    if (!jStr)
        return "";

    const jclass stringClass = env->GetObjectClass(jStr);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string((char *)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}

JNIEXPORT jint JNICALL Java_wisc_selfdriving_OpencvNativeClass_detector(JNIEnv *env, jclass, jlong ptr, jstring stopsign, jstring traffic, jstring leftturn, jstring rightturn)
{
    Mat* mRgb = (Mat*) ptr;
    string stop_sign = jstring2string(env, stopsign);
    string traffic_light = jstring2string(env, traffic);
    string left_turn = jstring2string(env, leftturn);
    string right_turn = jstring2string(env, rightturn);

    if ((*mRgb).empty()) {
      LOGD("empty");
      return (jint)-1;
    }
    int result = detectObjects_CASCADE(*mRgb, stop_sign, traffic_light);
    if (result == 1)
        LOGD("Stop-sign");
    else
        LOGD("NIL");
    if (result != 0)
        return (jint) result;
    else
      result = detectObjects_MSE(left_turn, right_turn, *mRgb);
    return (jint)result;
}

void publish_points(Mat& img, Points& points, const Vec3b& icolor) {
	//Point x => row   y => column
	for(int i = 0; i < points.size(); ++i) {
		Point point = points.at(i);
		img.at<Vec3b>(point.y, point.x) = icolor;
	}
}


int toGray(Mat src, Mat& gray)
{
    cvtColor( src, gray, COLOR_BGR2GRAY );
    Canny(gray, gray, 200, 400);

    LaneMarkerDetector detector(src);

    Mat temp = Mat::zeros(src.rows, src.cols, src.type());
    detector.laneMarkerDetector(gray, src, temp);


    Point center(src.cols/2, src.rows*4/5);
    temp.at<Vec3b>(center.y, center.x) = kLaneRed;


	Mat test = Mat::zeros(src.rows, src.cols, src.type());
    cvtColor(gray, test, COLOR_GRAY2BGR);

    Points left = detector.getLeftLane(center);
	Points right = detector.getRightLane(center);
    publish_points(test, left, kLaneRed);
    publish_points(test, right, kLaneRed);

    	//Point
    Points cline = detector.getDirectionLine();
    publish_points(test, cline, kLaneWhite);


	int leftsum = 0;
	int rightsum = 0;
	for(int i = 0; i < cline.size() && i < 20; ++i) {
		Point point = cline.at(i);
		if(point.x < center.x) {
			leftsum++;
		} else {
			rightsum++;
		}
	}
    int steering = 0;
   	int sum = leftsum + rightsum;
   	if(sum > 6) {
   		double diff = double(leftsum - rightsum)/sum;
   		if(diff > 0.3) {
   			steering = -1;
   		} else if(diff < -0.3) {
   			steering = 1;
   		} else {

   		}
   	}
   	gray = test;
   	return steering;
}

/* Return an integer:
     - 0 for nothing found,
     - 1 for stop-sign,
     - 2 for red light,
     - 3 for green light
*/
int detectObjects_CASCADE(Mat mat, string stopsign_xml, string trafficlight_xml) {
    CascadeClassifier stopSignDetector, trafficLightDetector;
    //stopSignDetector.load(stopsign_xml);
    trafficLightDetector.load(trafficlight_xml);
    if(!stopSignDetector.load(stopsign_xml)) {
        LOGD("Error loading cascade of stop sign");
    }

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

double meanSquareError(const Mat &img1, const Mat &img2) {
    Mat s1;
    if (img1.rows == img2.rows)
        LOGD("fuck");
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