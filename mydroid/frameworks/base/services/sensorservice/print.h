/* A bunch of printing functions used by Wes to test Android NDK stuff */

#ifndef _WES_H_
#define _WES_H_

#include <android/log.h>
#include <Eigen/Dense>
#include <string>

#include "vec.h"
#include "mat.h"

using namespace Eigen;
using namespace std;
using namespace android;

static void printVector(string tag, VectorXd mat) {
    __android_log_print(ANDROID_LOG_DEBUG, "POSITION_FUSION", "%s | 0: %f 1: %f 2: %f 3: %f 4: %f 5: %f ",
        tag.c_str(), mat(0), mat(1), mat(2), mat(3), mat(4), mat(5));
}

static void printVector3(string tag, VectorXd mat) {
    __android_log_print(ANDROID_LOG_DEBUG, "POSITION_FUSION", "%s | 0: %f 1: %f 2: %f",
        tag.c_str(), mat(0), mat(1), mat(2));
}

static void printVector3f(string tag, Vector3f mat) {
    __android_log_print(ANDROID_LOG_DEBUG, "POSITION_FUSION", "%s | 0: %f 1: %f 2: %f",
        tag.c_str(), mat(0), mat(1), mat(2));
}

static void printMatrix(string tag, MatrixXd mat) {
	for (int i = 0; i < 6; i++) {
        printVector(tag.c_str(), mat.row(i));
    }
}

static void printMatrix3(string tag, Matrix3d mat) {
	for (int i = 0; i < 3; i++) {
        printVector3(tag.c_str(), mat.row(i));
    }
}

static void printMatrix3f(string tag, Matrix3f mat) {
	for (int i = 0; i < 3; i++) {
        printVector3f(tag.c_str(), mat.row(i));
    }
}

static void print(string msg) {
    __android_log_print(ANDROID_LOG_DEBUG, "POSITION_FUSION", "%s", msg.c_str()); 
}

static void printValue(string tag, double val) {
    __android_log_print(ANDROID_LOG_DEBUG, "POSITION_FUSION", "%s | %f", tag.c_str(), val); 
}

/** To Print Android's vec.h and mat.h **/

static void printAndroidVector3f(string tag, vec3_t mat) {
    __android_log_print(ANDROID_LOG_DEBUG, "POSITION_FUSION", "%s | 0: %f 1: %f 2: %f",
        tag.c_str(), mat[0], mat[1], mat[2]);
}
static void printAndroidMatrix3f(string tag, mat33_t mat) {
	for (int i = 0; i < 3; i++) {
        printAndroidVector3f(tag.c_str(), mat[i]);
    }
    print("====================================");
}

#endif
