//
// Created by lkang on 2/8/17.
//

#ifndef SELFDRIVING_WISC_SELFDRIVING_OPENCVNATIVECLASS_H
#define SELFDRIVING_WISC_SELFDRIVING_OPENCVNATIVECLASS_H

#include <jni.h>
#include <stdio.h>
#include <opencv2/opencv.hpp>

#include "lane_marker_detector.h"

#include <iostream>
#include <opencv2/core/core.hpp>
#include <vector>
#include <opencv2/video.hpp>
#include <opencv2/videoio.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/highgui.hpp>
#include <string>

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include <android/log.h>
#define LOG_TAG "DetectorCPP"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

using namespace cv;
using namespace std;

#define leftturn_THRESHOLD 5000
#define rightturn_THRESHOLD 5000

extern "C" {
#endif
/*
 * Class:     wisc_ndkopencvtest1_OpencvNativeClass
 * Method:    convertGray
 * Signature: (JJ)I
 */

int toGray(Mat img, Mat& gray);
void publish_points(Mat& img, Points& points, const Vec3b& icolor);

int detectObjects_CASCADE(Mat mat, string stopsign_xml, string trafficlight_xml);
double meanSquareError(const Mat &img1, const Mat &img2);
int detectObjects_MSE(string left_prototype, string right_prototype, Mat mat);
string jstring2string(JNIEnv *env, jstring jStr);


JNIEXPORT jint JNICALL Java_wisc_selfdriving_OpencvNativeClass_convertGray
        (JNIEnv *, jclass, jlong, jlong);
JNIEXPORT jdouble JNICALL Java_wisc_selfdriving_OpencvNativeClass_getSteeringAngle
        (JNIEnv *, jclass);
JNIEXPORT jdouble JNICALL Java_wisc_selfdriving_OpencvNativeClass_getAcceleration
        (JNIEnv *, jclass);
JNIEXPORT jint JNICALL Java_wisc_selfdriving_OpencvNativeClass_detector
        (JNIEnv *env, jclass, jlong ptr, jstring stopsign, jstring traffic, jstring leftturn, jstring rightturn);

#ifdef __cplusplus
}

#endif //SELFDRIVING_WISC_SELFDRIVING_OPENCVNATIVECLASS_H
