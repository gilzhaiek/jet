package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer;

import java.util.Arrays;

public class GLHelper {
	
	public static double PI = 3.14159265359;
	public static double RAD2DEG = 180/PI;
	public static double DEG2RAD = PI/180;
	
	public static float[] normalize(float[] vec){
		float mag = magnitude(vec);
		if(!isZero(vec)){
			vec[0] /= mag;
			vec[1] /= mag;
			vec[2] /= mag;
			return vec;
		}
		else return vec;
	}
	
	public static float magnitude(float a, float b, float c){
		return (float)Math.sqrt(a*a + b*b + c*c);
	}
	
	public static float magnitude(float[] vec){
		return (float)Math.sqrt(vec[0]*vec[0] + vec[1]*vec[1] + vec[2]*vec[2]);
	}
	
	public static float dot(float a1, float b1, float c1, float a2, float b2, float c2){
		return a1*a2 + b1*b2 + c1*c2;
	}
	
	public static float dot(float[] a, float[] b ){
		return a[0]*b[0] + a[1] * b[1] + a[2] * b[2];
	}
	
	/**
	 * Takes two vectors, {@code a} and {@code b} and returns their cross product,
	 * {@code a x b}
	 * @param out - Output vector that will hold the result. Must not be null.
	 * @param a - First vector
	 * @param b - Second vector
	 */
	public static void cross(float[] out, float[] a, float[] b){
		if(null == out){
			try {
				throw new Exception("Output vector is null!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		//if a and b are equal, cross product is zero
		// so in that case, set to {1, 0, 0}
		if(!Arrays.equals(a, b)){
			out[0] = a[1]*b[2] - a[2]*b[1];
			out[1] = a[2]*b[0] - a[0]*b[2];
			out[2] = a[0]*b[1] - a[1]*b[0];
		}
		else {
			out[0] = 1f;
			out[1] = 0f;
			out[2] = 0f;
		}
	}
	
	public static float lerp(float v0, float v1, float t){
		return (1-t)*v0 + t*v1;
	}
	
	/**
	 * Clamps a value between {@code min} and {@code max}
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	public static float clamp(float value, float min, float max){
		if(value >= min && value <= max) return value;
		else if(value < min) return min;
		else return max;
	}
	
	/**
	 * Negates a vector
	 * @param vec
	 * @return the negative of {@code vec}
	 */
	public static float[] negative(float[] vec){
		vec[0] = -vec[0];
		vec[1] = -vec[1];
		vec[2] = -vec[2];
		return vec;
	}

	/**
	 * Checks that a vector is not zero
	 * @param vec
	 * @return
	 */
	public static boolean isZero(float[] vec){
		return (vec[0] == 0 && vec[1] == 0 && vec[2] == 0);
	}
	
	/**
	 * Calculates the inverse square root of X
	 * @param x
	 * @return
	 */
	public static float invSqrt(float x) {
	    float xhalf = 0.5f*x;
	    int i = Float.floatToIntBits(x);
	    i = 0x5f3759df - (i>>1);
	    x = Float.intBitsToFloat(i);
	    x = x*(1.5f - xhalf*x*x);
	    return x;
	}
}
