
You may adjust some value to accommendate your own OpenCV setting

// download the repositories from "https://github.com/kanglei1130/SelfDriving";
// import this project with android studio,then 2 files need to be adjusted;

// first: open app/build.gradle
//        find "task ndkBuild(){commandLine....}", change your own path after "commandLine";

// second: open app/src/main/jni/Android.mk
//        find "OPENCVROOT:= /home/kang/Desktop/OpenCV-android-sdk_3"  then change it to your sdk path

// run and success.
