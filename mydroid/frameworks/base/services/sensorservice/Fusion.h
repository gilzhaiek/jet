#ifndef _FUSION_H_
#define _FUSION_H_

#include <jni.h>
#include <math.h>

#include <Eigen/Cholesky>
#include <Eigen/Geometry>

#include <utils/Errors.h>
#include "quat.h"
#include "mat.h"
#include "vec.h"

#define SIGMA_G_SQUARE 1e-4
#define SIGMA_Q_SQUARE 1e-4
#define CA 0.3
#define GRAVITY 9.80665
#define MIN_MAG_FIELD 20
#define MAX_MAG_FIELD 80

#ifndef PI
#define PI 3.141592653
#endif // PI

using namespace Eigen;
using namespace android;

class Fusion {
public:
    Fusion();
    void init();
    void handleGyro(const vec3_t& w, float dT);
    status_t handleAcc(const vec3_t& a);
    status_t handleMag(const vec3_t& m);
    vec4_t getAttitude() const;
    vec3_t getBias() const;
    mat33_t getRotationMatrix() const;
    bool hasEstimate() const;


private:
    void calculateOrientation();

    bool mHasEstimate;
    bool mHasGyr;
    bool mHasAcc;
    bool mHasMag;

    double dT;

    Vector3f acc_sensor;
    Vector3f acc_sensor_temp;
    Vector3f gyro_sensor;
    Vector3f gyro_sensor_temp;
    Vector3f mag_sensor;
    Vector3f mag_sensor_temp;

    Vector3f ex_acc;
    Matrix3f R_z; 
    Vector3f z_prev;
    Matrix3f P_prev_z; 
    Vector3f z2_prev;
    Matrix3f P_prev_z2; 
    Matrix3f mRotationMatrix;
    
    Matrix3f mRotRollPitch;
    void calculateExternAccel();
    void calculateRollPitch();
};

#endif // _FUSION_H_
