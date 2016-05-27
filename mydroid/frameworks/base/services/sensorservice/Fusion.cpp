#include "Fusion.h"
#include "print.h"

using namespace Eigen;

Vector3f googleToEigenVec3f(const vec3_t& w) {
    Vector3f vec;
    vec << w[0], w[1], w[2];
    return vec;
}

mat33_t eigenToGoogleMat3f(Matrix3f mat) {
    mat33_t newMat;

    for (int i = 0; i < 3; i++)
        for (int j = 0; j < 3; j++)
            newMat[j][i] = mat(i, j);

    return newMat;
}

vec3_t eigenToGoogleVec3f(Vector3f vec) {
    vec3_t newVec;

    for (int i = 0; i < 3; i++) {
        newVec[i] = vec(i);
    }
    
    return newVec;
}

/* public */
Fusion::Fusion() { 
    init();
}

void Fusion::init() {
    /* Roll & Pitch */
    z_prev << 0, 0, 1;
    ex_acc = Vector3f::Zero();
    R_z = Matrix3f::Zero(); 
    P_prev_z = Matrix3f::Identity(); 
    mRotRollPitch = Matrix3f::Identity();

    /* Yaw */
    z2_prev << 0, 0, 1;
    P_prev_z2 = Matrix3f::Identity();

    /* Control */
    mHasGyr = false;
    mHasAcc = false;
    mHasMag = false;
    mHasEstimate = false;
}

void Fusion::handleGyro(const vec3_t& w, float dT) {
    if (dT > 0.1) // TODO check for long duration properly
        dT = 0.1;

    this->dT = dT;

    gyro_sensor = googleToEigenVec3f(w);
//    gyro_sensor[0]=-gyro_sensor_temp[2];
//    gyro_sensor[1]=-gyro_sensor_temp[0];
//    gyro_sensor[2]=gyro_sensor_temp[1];

    mHasGyr = true;

    if (mHasGyr && mHasAcc && mHasMag)
    {
        mHasEstimate = true;
        calculateOrientation();
    }
}

status_t Fusion::handleAcc(const vec3_t& a) {
	acc_sensor = googleToEigenVec3f(a);
//    acc_sensor[0]=-acc_sensor_temp[2];
//    acc_sensor[1]=-acc_sensor_temp[0];
//    acc_sensor[2]=acc_sensor_temp[1];
    
    mHasAcc = true;

    return NO_ERROR;
}

status_t Fusion::handleMag(const vec3_t& m) {
	mag_sensor = googleToEigenVec3f(m);
//    mag_sensor[0]=-mag_sensor_temp[2]; //inverted z-axis is corrected in sensors.conf
//    mag_sensor[1]=-mag_sensor_temp[0];
//    mag_sensor[2]=mag_sensor_temp[1];
    
    mHasMag = true;

    return NO_ERROR;
}

vec4_t Fusion::getAttitude() const {
    return matrixToQuat(getRotationMatrix());
}

// TODO what's bias?
vec3_t Fusion::getBias() const {
    vec3_t dummy;
    dummy = 0;
    return dummy;
}

mat33_t Fusion::getRotationMatrix() const {
    return eigenToGoogleMat3f(mRotationMatrix);
}

bool Fusion::hasEstimate() const {
    return mHasEstimate;
}

/* private */
void Fusion::calculateOrientation() {
//    printVector3f("gyro_sensor", gyro_sensor);
//    printVector3f("mag_sensor", mag_sensor);
//    printVector3f("acc_sensor", acc_sensor);
//    printValue("dT", dT);

    Matrix3f omega_w;
    Matrix3f Phi_w;
    Matrix3f z_skew;
    Matrix3f Q_z;
    Matrix3f P_priori_z;

    omega_w << 
        0, -gyro_sensor(2), gyro_sensor(1),
        gyro_sensor(2), 0, -gyro_sensor(0),
        -gyro_sensor(1), gyro_sensor(0), 0;

    // printMatrix3f("omega_w", omega_w);

    Phi_w = Matrix3f::Identity() - dT * omega_w;

    // printMatrix3f("Phi_w", Phi_w);

    Vector3f z_priori;
    z_priori = Phi_w * z_prev;
    z_priori /= z_priori.norm();

    z_skew << 
        0, -z_prev(2), z_prev(1),
        z_prev(2), 0, -z_prev(0),
        -z_prev(1), z_prev(0), 0;

    // printMatrix3f("z_skew", z_skew);

    Q_z = -dT*dT * z_skew * SIGMA_G_SQUARE * z_skew;

    // printMatrix3f("Q_z", Q_z);
    P_priori_z = Phi_w * P_prev_z * Phi_w + Q_z;

    /* Measurement Update */
    Vector3f z_measured;
    Matrix3f H_z;
    Matrix3f K_z;
    float sigma_acc;

    z_measured = acc_sensor - CA * ex_acc;
    H_z = GRAVITY * Matrix3f::Identity();

    // printMatrix3f("H_z", H_z);
    // printMatrix3f("P_priori_z", P_priori_z);

    sigma_acc = (1/3) * CA * CA * ex_acc.norm() * ex_acc.norm();
    // printValue("sigma_acc", sigma_acc);
    // printValue("SIGMA_Q_SQUARE", SIGMA_Q_SQUARE);
    R_z = (sigma_acc + SIGMA_Q_SQUARE) * Matrix3f::Identity();
    
    // printMatrix3f("R_z", R_z);

    K_z = (P_priori_z * H_z) * (H_z * P_priori_z * H_z + R_z).inverse();

    // printMatrix3f("K_z", K_z);

    z_prev = z_priori + K_z * (z_measured - H_z.transpose() * z_priori);
    z_prev /= z_prev.norm();
    P_prev_z = (Matrix3f::Identity() - K_z * H_z) * P_priori_z;
    ex_acc = acc_sensor - GRAVITY * z_prev;
    // printVector3f("ex_acc", ex_acc);

    /* Roll & Pitch */
    Matrix3f Rot_roll;
    Matrix3f Rot_pitch;

    float roll_rad = atan2(z_prev(1), z_prev(2));
    float pitch_rad = atan2(-z_prev(0), z_prev(1)/sin(roll_rad));
    float roll = roll_rad * 180 / PI;
    float pitch = pitch_rad * 180 / PI;

    Rot_pitch << 
        cos(pitch_rad), 0, sin(pitch_rad),
        0, 1, 0,
        -sin(pitch_rad), 0, cos(pitch_rad);

    Rot_roll <<
        1, 0, 0,
        0, cos(roll_rad), -sin(roll_rad),
        0, sin(roll_rad), cos(roll_rad);

    mRotRollPitch = Rot_pitch * Rot_roll; 

    /* Yaw */ 
    Vector3f z2_priori;
    z2_priori = Phi_w * z2_prev;
    z2_priori = z2_priori/z2_priori.norm();

    Matrix3f z2_skew; 
    z2_skew << 
        0, -z2_prev(2), z2_prev(1),
        z2_prev(2), 0, -z2_prev(0),
        -z2_prev(1), z2_prev(0), 0;

    // printMatrix3f("z2_skew", z2_skew);

    Matrix3f Q_z2; 
    Q_z2 = -dT * dT * z2_skew * SIGMA_G_SQUARE * z2_skew;

    // printMatrix3f("Q_z2", Q_z2);

    Matrix3f P_priori_z2; 
    P_priori_z2 = Phi_w * P_prev_z2 * Phi_w + Q_z2;

    // printMatrix3f("P_priori_z2", P_priori_z2);

    float cr = cos(roll_rad);
    float sr = sin(roll_rad);
    float cp = cos(pitch_rad);
    float sp = sin(pitch_rad);

    if (mag_sensor.sum() != 0) {
        // Measurement Update
        Vector3f mag_roll_pitch = mRotRollPitch * mag_sensor; 
        // printVector3f("mag_roll_pitch", mag_roll_pitch);
        // printVector3f("mag_sensor", mag_sensor);

        Vector2f mag_field; 
        mag_field << 0, 1;

        Vector2f mag_h_nf;
        mag_h_nf << mag_field(0), mag_field(1);
        // angle between magnetic field vector and true north
        float theta_north = atan2(mag_h_nf(1), mag_h_nf(0)); // theta is measured wrt East direction 
        float theta_north_deg = theta_north * 180 / PI;

        // printVector3f("mag_sensor", mag_sensor);
        
        Vector2f mag_h;
        mag_h << mag_roll_pitch(0), mag_roll_pitch(1);

        // printVector3f("mag_roll_pitch", mag_roll_pitch);
        float theta_mag = atan2(mag_h(1),mag_h(0)); 
        
        double yaw_measured_cos = (mag_h_nf.transpose() * mag_h); // this step is stable
        yaw_measured_cos /= (mag_h_nf.norm() * mag_h.norm()); // this step makes it crazy

        // printValue("mag_h.norm()", mag_h.norm());

        double yaw_measured_rad = acos(yaw_measured_cos);

        // printValue("before", yaw_measured_rad);
        // printVector3f("mag_h", mag_h);

        // Correct Measured Yaw to be Between 0 - 2*PI

        if (mag_h(0) > 0 && mag_h(1) > 0) {
            if (theta_mag < theta_north) {
                yaw_measured_rad = -yaw_measured_rad;
            }
        }

        if (mag_h(0) < 0 && mag_h(1) < 0) {
            if (abs(theta_mag) + abs(theta_north) < PI ) {
                yaw_measured_rad = -yaw_measured_rad;
            }
        }

        if (mag_h(0) > 0 && mag_h(1) < 0) {
            yaw_measured_rad = -yaw_measured_rad;
        }

        float cy = cos(yaw_measured_rad); 
        float sy = -sin(yaw_measured_rad); // if you dont multiply by -1 it will give you the negative angle

        Vector3f z2_measured;
        z2_measured << cy*cp, (cy*sp*sr)-(sy*cr), (cy*sp*cy)+(sy*sr);
        z2_measured  = z2_measured / z2_measured.norm();

        float sigma_mag;

        if (mag_sensor.norm() > MIN_MAG_FIELD && mag_sensor.norm() < MAX_MAG_FIELD) {
            sigma_mag = 6e-6;
        } else {
            sigma_mag = 1e8;
        }

        Matrix3f H_z2 = Matrix3f::Identity();
        Matrix3f R_z2;
        R_z2  = sigma_mag * Matrix3f::Identity();
        
        Matrix3f K_z2;
        K_z2 = (P_priori_z2 * H_z2) * (H_z2 * P_priori_z2 * H_z2.transpose() + R_z2).inverse();
        // printMatrix3f("K_z2", K_z2);

        z2_prev = z2_priori + K_z2 * (z2_measured - H_z2 * z2_priori);

        // printVector3f("z2_priori", z2_priori);
        // printMatrix3f("P_priori_z2", P_priori_z2);

        z2_prev = z2_prev / z2_prev.norm();
        P_prev_z2 = (Matrix3f::Identity() - K_z2 * H_z2) * P_priori_z2;
    } 
   
    float cy_est = z2_prev(0) / cp;
    float sy_est = -cr * z2_prev(1) + sr * z2_prev(2);
    float yaw_rad = atan2(sy_est, cy_est);
    float yaw = yaw_rad * 180/PI;

    Matrix3f Rot_yaw; 
    Rot_yaw << 
        cos(yaw_rad), -sin(yaw_rad), 0,
        sin(yaw_rad), cos(yaw_rad), 0,
        0, 0, 1;

    mRotationMatrix = Rot_yaw * mRotRollPitch;

    if (isnan(mRotationMatrix.sum())) {
        init();
        print("nan");
    }

//    printMatrix3f("mRotationMatrix", mRotationMatrix);

/*
    float* orientation = new float[3];
    orientation[0] = yaw;
    orientation[1] = pitch;
    orientation[2] = roll;

    printValue("yaw", yaw);
    printValue("pitch", pitch);
    printValue("roll", roll);
  */  
    
    // From Android frameworks, it is ZXY rotation matrix
//    values[0] = (float)Math.atan2(R[1], R[4]);    //YAW
//    values[1] = (float)Math.asin(-R[7]);          //PITCH
//    values[2] = (float)Math.atan2(-R[6], R[8]);   //ROLL

    //Construct rotation matrix according to Android framework
    
    float android_yaw = yaw_rad;
    float android_pitch = roll_rad;
    float android_roll = -pitch_rad;
    
    mRotationMatrix(0,0) = cos(android_yaw)*cos(android_roll)-sin(android_yaw)*sin(android_pitch)*sin(android_roll);        //0
    mRotationMatrix(0,1) = -cos(android_pitch)*sin(android_yaw);                                                        //1
    mRotationMatrix(0,2) = cos(android_yaw)*sin(android_roll)+cos(android_roll)*sin(android_yaw)*sin(android_pitch);                    //2
    mRotationMatrix(1,0) = cos(android_roll)*sin(android_yaw)+cos(android_yaw)*sin(android_pitch)*sin(android_roll);                    //3
    mRotationMatrix(1,1) = cos(android_yaw)*cos(android_pitch);                                                         //4
    mRotationMatrix(1,2) = sin(android_yaw)*sin(android_roll)-cos(android_yaw)*cos(android_roll)*sin(android_pitch);    //5
    mRotationMatrix(2,0) = -cos(android_pitch)*sin(android_roll);                                                       //6
    mRotationMatrix(2,1) = sin(android_pitch);                                                                          //7
    mRotationMatrix(2,2) = cos(android_pitch)*cos(android_roll);                                                        //8
    
    
    Matrix3f matrixTemp=mRotationMatrix.transpose();
    mRotationMatrix = matrixTemp;
    
//    printValue("yaw", android_yaw*180/PI);
//    printValue("pitch", android_pitch*180/PI);
//    printValue("roll", android_roll*180/PI);
    
//    Vector3f orientation;
//    orientation << android_yaw*180/PI, android_pitch*180/PI,android_roll*180/PI;
//    printVector3f("ORIENTATION", orientation);
    
    
}

