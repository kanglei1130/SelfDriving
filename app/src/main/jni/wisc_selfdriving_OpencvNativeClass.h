//
// Created by lkang on 2/8/17.
//

#ifndef SELFDRIVING_WISC_SELFDRIVING_OPENCVNATIVECLASS_H
#define SELFDRIVING_WISC_SELFDRIVING_OPENCVNATIVECLASS_H

#include <jni.h>
#include <stdio.h>
#include <opencv2/opencv.hpp>

#include "lane_marker_detector.h"

using namespace cv;
using namespace std;

extern "C" {
#endif
/*
 * Class:     wisc_ndkopencvtest1_OpencvNativeClass
 * Method:    convertGray
 * Signature: (JJ)I
 */

int toGray(Mat img, Mat& gray);
void publish_points(Mat& img, Points& points, const Vec3b& icolor);


JNIEXPORT jint JNICALL Java_wisc_selfdriving_OpencvNativeClass_convertGray
        (JNIEnv *, jclass, jlong, jlong);
JNIEXPORT jdouble JNICALL Java_wisc_selfdriving_OpencvNativeClass_getSteeringAngle
        (JNIEnv *, jclass);
JNIEXPORT jdouble JNICALL Java_wisc_selfdriving_OpencvNativeClass_getAcceleration
        (JNIEnv *, jclass);

#ifdef __cplusplus
}

#endif //SELFDRIVING_WISC_SELFDRIVING_OPENCVNATIVECLASS_H
