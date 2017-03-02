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

void publish_points(Mat& img, Points& points, const Vec3b& icolor) {
	//Point x => row   y => column
	for(int i = 0; i < points.size(); ++i) {
		Point point = points.at(i);
		img.at<Vec3b>(point.y, point.x) = icolor;
	}
}


int toGray(Mat src, Mat& gray)
{

    /*
    cvtColor(img, gray, CV_RGBA2GRAY);

    Canny(gray, gray, 50, 200, 3); // Apply canny edge
    Ptr<LineSegmentDetector> ls = createLineSegmentDetector(LSD_REFINE_STD);
    double start = double(getTickCount());
    vector<Vec4f> lines_std;
    // Detect the lines
    ls->detect(gray, lines_std);
    double duration_ms = (double(getTickCount()) - start) * 1000 / getTickFrequency();

    Mat drawnLines(gray);
    ls->drawSegments(drawnLines, lines_std);
    gray = drawnLines;
    if (gray.rows == img.rows && gray.cols == img.cols) {
        return 1;
    } else {
        return 0;
    }
    */
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

    gray = test;
    if (gray.rows == src.rows && gray.cols == src.cols) {
        return 1;
    } else {
        return 0;
    }
}


