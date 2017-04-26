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


