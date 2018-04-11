LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#opencv
OPENCVROOT:= /home/wei/OpenCV-android-sdk
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES := wisc_selfdriving_OpencvNativeClass.cpp lane_marker_detector.cpp
LOCAL_LDLIBS += -llog
LOCAL_MODULE := MyOpencvLibs

include $(BUILD_SHARED_LIBRARY)
