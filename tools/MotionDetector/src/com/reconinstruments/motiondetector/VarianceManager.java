package com.reconinstruments.motiondetector;

public class VarianceManager {

    public static final String TAG = "VarianceManager";

    //The minimum number of variances that must be held for algorithms
    private static final int HELD_VARIANCES = 6;

    //Thresholds for the Etienne algorithm
    private static final float STOP_VAR_THRESHOLD = (float) 0.85;
    private static final float MOVE_VAR_THRESHOLD = (float) 30.5;

    //Thresholds for Ahmed's algorithms
    private static final float STOP_ACCVAR_THRESHOLD = (float) 2;
    private static final float STOP_GYROVAR_THRESHOLD = (float) 0.027;
    private static final float WALK_ACCVAR_THRESHOLD = (float) 3;

    //Primitive held values
    private float[] mAccVariances;
    private float mGyroXVariance;
    private float mGyroYVariance;
    private boolean mState;

    //Constructor requires maximum of values to be recieved
    public VarianceManager() {
        this.mAccVariances = new float[HELD_VARIANCES];
    }

    //Return the variance calculated for the last 1 second period
    public float getVariance() { return mAccVariances[0]; }

    //The motion algorithm by Etienne THIS IS THE DETERMINATION FORMULA CURRENT DECIDED UPON
    public boolean etienneAlgorithm() {
        //Definitely Stopped
        if ((mAccVariances[0] < STOP_VAR_THRESHOLD) &&
            (mAccVariances[1] < STOP_VAR_THRESHOLD) &&
            (mAccVariances[2] < STOP_VAR_THRESHOLD) &&
            (mAccVariances[3] < STOP_VAR_THRESHOLD) &&
            (mAccVariances[4] < STOP_VAR_THRESHOLD) &&
            (mAccVariances[5] < STOP_VAR_THRESHOLD) ) {
            mState = false;
            return false;
        }
        //Definitely Moving
        else if ((mAccVariances[0] > MOVE_VAR_THRESHOLD) &&
                 (mAccVariances[1] > MOVE_VAR_THRESHOLD) ) {
            mState = true;
            return true;
        }
        //Probably moving
        else if ((mAccVariances[0] >= STOP_VAR_THRESHOLD) && (mAccVariances[0] <= MOVE_VAR_THRESHOLD) &&
                 (mAccVariances[1] >= STOP_VAR_THRESHOLD) && (mAccVariances[1] <= MOVE_VAR_THRESHOLD) &&
                 (mAccVariances[2] >= STOP_VAR_THRESHOLD) && (mAccVariances[2] <= MOVE_VAR_THRESHOLD)) {
            mState = true;
            return true;
        }
        else
            return mState;
    }

    //Ahmed's algorithm1
    public boolean ahmedAccAlgorithm() {
        return mAccVariances[0] > STOP_ACCVAR_THRESHOLD;
    }

    //Ahmed's algorithm2
    public boolean ahmedAccGyroAlgorithm() {
        if (mAccVariances[0] <= STOP_ACCVAR_THRESHOLD)
            return false;
        else if ((mAccVariances[0] > STOP_ACCVAR_THRESHOLD) && (mAccVariances[0] <= WALK_ACCVAR_THRESHOLD)) {
            if ((mGyroXVariance > STOP_GYROVAR_THRESHOLD) && (mGyroYVariance >= STOP_GYROVAR_THRESHOLD))
                return true;
            else
                return false;
        }
        else
            return true;
    }

    //Compute a new variance for the last second and add it to the queue of held variances
    public void addAccVariance(float[] rawAccelValues) {

        int highestArrValue = rawAccelValues.length - 1;

        for (int i = HELD_VARIANCES - 1; i > 0; i--)
            mAccVariances[i] = mAccVariances [i-1];

        float valsum=0;
        float squaresum=0;

        /* variance = SUM( x(i) - mean(x) )^2
         *               divided by N-1
         *
         * This simplifies to SUM( x(i)^2 ) - ((SUM( x(i) ) / N) * SUM( x(i) )
         *                              divided by N-1
         */
        for (int i = highestArrValue; i >= 0; i--){
            valsum += rawAccelValues[i];
            squaresum += rawAccelValues[i] * rawAccelValues[i];
        }

        mAccVariances[0] = (squaresum - ((valsum / (highestArrValue + 1) ) * valsum)) / highestArrValue;
    }

    //Compute new variance for Gyro X and Y for the last second and store it (Only one is ever held)
    public void addGyroVariance(float[][] rawGyroValues, int gyroTick) {
        float XValSum = 0;
        float XSquareSum = 0;
        float YValSum = 0;
        float YSquareSum = 0;

        for (int i = gyroTick - 1; i >= 0; i--){
            XValSum += rawGyroValues[i][0];
            YValSum += rawGyroValues[i][1];
            XSquareSum += rawGyroValues[i][0] * rawGyroValues[i][0];
            YSquareSum += rawGyroValues[i][1] * rawGyroValues[i][1];
        }

        mGyroXVariance = (XSquareSum - ((XValSum / gyroTick ) * XValSum)) / (gyroTick - 1);
        mGyroYVariance = (YSquareSum - ((YValSum / gyroTick ) * YValSum)) / (gyroTick - 1);
    }
}