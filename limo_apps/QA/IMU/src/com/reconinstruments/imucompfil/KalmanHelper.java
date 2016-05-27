package com.reconinstruments.imucompfil;

//Helper class to store all the Kalman filter's methods

public class KalmanHelper {

	public static double[][] obtain_Xi_from_quaternion(double q[]) {
		
		double Xi[][]=new double[4][3];
		Xi[0][0] = -q[1];
		Xi[0][1] = -q[2];
		Xi[0][2] = -q[3];
		Xi[1][0] = q[0];
		Xi[1][1] = -q[3];
		Xi[1][2] = q[2];
		Xi[2][0] = q[3];
		Xi[2][1] = q[0];
		Xi[2][2] = -q[1];
		Xi[3][0] = -q[2];
		Xi[3][1] = q[1];
		Xi[3][2] = q[0];
		
		return Xi;
	}

	public static double[][] obtain_omega_from_w(double w[]) {
		double omega[][]=new double[4][4];
		int i, j;

		omega[0][0] = 0.0;
		omega[0][1] = -w[0];
		omega[0][2] = -w[1];
		omega[0][3] = -w[2];
		omega[1][0] = w[0];
		omega[1][1] = 0.0;
		omega[1][2] = w[2];
		omega[1][3] = -w[1];
		omega[2][0] = w[1];
		omega[2][1] = -w[2];
		omega[2][2] = 0.0;
		omega[2][3] = w[0];
		omega[3][0] = w[2];
		omega[3][1] = w[1];
		omega[3][2] = -w[0];
		omega[3][3] = 0.0;

		for (i = 0; i < 4; i++)
			for (j = 0; j < 4; j++)
				omega[i][j] = 0.5 * omega[i][j];
		
		return omega;
	}

	public static double [][] get_RotationMatrix_from_Quaternions(double p[]) {
		double a[][]=new double [3][3];
		int i;
		double norm = 0.0;

		norm = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2] + p[3] * p[3]);
		for (i = 0; i < 4; i++)
			p[i] = p[i] / norm;

		a[0][0] = 2.0 * (Math.pow(p[0], 2.0) + Math.pow(p[1], 2.0) - 0.5);
//		a[0][0] = Math.pow(p[0], 2.0) + Math.pow(p[1], 2.0)-Math.pow(p[2], 2.0) - Math.pow(p[3], 2.0);
		a[1][0] = 2.0 * (p[1] * p[2] + p[0] * p[3]);
		a[2][0] = 2.0 * (p[1] * p[3] - p[0] * p[2]);

		a[0][1] = 2.0 * (p[1] * p[2] - p[0] * p[3]);
		a[1][1] = 2.0 * (Math.pow(p[0], 2.0) + Math.pow(p[2], 2.0) - 0.5);
//		a[1][1] = Math.pow(p[0], 2.0) - Math.pow(p[1], 2.0)+Math.pow(p[2], 2.0) - Math.pow(p[3], 2.0);
		a[2][1] = 2.0 * (p[2] * p[3] + p[0] * p[1]);

		a[0][2] = 2.0 * (p[1] * p[3] + p[0] * p[2]);
		a[1][2] = 2.0 * (p[2] * p[3] - p[0] * p[1]);
		a[2][2] = 2.0 * (Math.pow(p[0], 2.0) + Math.pow(p[3], 2.0) - 0.5);
//		a[2][2] = Math.pow(p[0], 2.0) - Math.pow(p[1], 2.0)-Math.pow(p[2], 2.0) + Math.pow(p[3], 2.0);

		
		return a;
	}

	public static double[]get_zyx_EulerAngles_from_Orientation(double a[][]) {
		/*
		 * 1st rotation : phi about x axis 2nd rotation : theta about y axis 3rd
		 * rotation : psi about z axis
		 */
		double EulerAngles[] = new double [3];
		double phi, theta, psi;

		phi = Math.atan2(a[2][1], a[2][2]);
		theta = Math.atan2(-a[2][0], Math.sqrt(a[2][1] * a[2][1] + a[2][2] * a[2][2]));
		psi = Math.atan2(a[1][0], a[0][0]);

		EulerAngles[0] = phi;
		EulerAngles[1] = theta;
		EulerAngles[2] = psi;
		
		return EulerAngles;
	}

	public static double dotpro(double b[], double c[]) {
		int i;
		double temp, a;
		temp = 0.0;
		a = 0.0;
		for (i = 0; i < 3; i++) {
			temp = b[i] * c[i];
			a = a + temp;
		}
		return (a);
	}

	public static double[] ats(double a[][], double s[]) {
		double sp[]=new double [3];
		int i, j;
		for (i = 0; i < 3; i++) {
			sp[i] = 0.0;
			for (j = 0; j < 3; j++)
				sp[i] = sp[i] + a[j][i] * s[j];
		}
		return sp;
	}

	public static double [] wcs(double w[], double s[]) {
		double sd[] = new double [3];
		sd[0] = -w[2] * s[1] + w[1] * s[2];
		sd[1] = w[2] * s[0] - w[0] * s[2];
		sd[2] = -w[1] * s[0] + w[0] * s[1];
		
		return sd;
	}

	public static double[] matmulvec(double a[][], double b[], int m, int n) {
		double c[]=new double[m];
		int i, j;

		for (i = 0; i < m; i++) {
			c[i] = 0.0;
			for (j = 0; j < n; j++)
				c[i] = c[i] + (a[i][j] * b[j]);
		}
		
		return c;
	}
	
	public static double[][] matmulmat(double a[][],double b[][],int m,int n,int r)
	{	double c[][]=new double[m][r];
		int i,j,k;

		for(i=0;i<m;i++){
			for(j=0;j<r;j++){ 
				c[i][j] = 0.0;
//				for(k=0;k<n;k++) c[i][j] = c[i][j] + (a[i][k]*b[j][k]);
				for(k=0;k<n;k++) c[i][j] = c[i][j] + (a[i][k]*b[k][j]);
			}
		}
		return c;
	}
	
	public static double[][] matmulmattrans(double a[][],double b[][],int m,int n,int r)
	{	double c[][]=new double [m][r];
		int i,j,k;

		for(i=0;i<m;i++){
			for(j=0;j<r;j++){ 
				c[i][j] = 0.0;
				for(k=0;k<n;k++) c[i][j] = c[i][j]+ (a[i][k]*b[j][k]);
			}
		}
		
		return c;
	}
	
	public static double vector_norm(double vector[], int n)
	{
		double norm;
		int i;

		norm = 0.0;
		
	   for(i=0;i<n;i++) norm = norm + (vector[i]*vector[i]);
	   norm = Math.sqrt(norm);

	   return norm;
	}
	
	public static double[][] inverse4by4(double m[][])
	{    
		double inverse[][] = new double [4][4];
		
	    int i,j;
		double value;
		double m00,m01,m02,m03; double m10,m11,m12,m13;	double m20,m21,m22,m23;	double m30,m31,m32,m33;


		m00 = m[0][0]; m01 = m[0][1]; m02 = m[0][2]; m03 = m[0][3];
		m10 = m[1][0]; m11 = m[1][1]; m12 = m[1][2]; m13 = m[1][3];
		m20 = m[2][0]; m21 = m[2][1]; m22 = m[2][2]; m23 = m[2][3];
		m30 = m[3][0]; m31 = m[3][1]; m32 = m[3][2]; m33 = m[3][3];
		
		value = 
	      m03 * m12 * m21 * m30-m02 * m13 * m21 * m30-m03 * m11 * m22 * m30+m01 * m13 * m22 * m30+
	      m02 * m11 * m23 * m30-m01 * m12 * m23 * m30-m03 * m12 * m20 * m31+m02 * m13 * m20 * m31+
	      m03 * m10 * m22 * m31-m00 * m13 * m22 * m31-m02 * m10 * m23 * m31+m00 * m12 * m23 * m31+
	      m03 * m11 * m20 * m32-m01 * m13 * m20 * m32-m03 * m10 * m21 * m32+m00 * m13 * m21 * m32+
	      m01 * m10 * m23 * m32-m00 * m11 * m23 * m32-m02 * m11 * m20 * m33+m01 * m12 * m20 * m33+
	      m02 * m10 * m21 * m33-m00 * m12 * m21 * m33-m01 * m10 * m22 * m33+m00 * m11 * m22 * m33;

		if( Math.abs(value) < 0.00000000001){
			//printf("In 4by4 inverting procedure, the determinant is too small: det=%f  \n",value);
			//exit(1);
		}

		inverse[0][0] = m12*m23*m31 - m13*m22*m31 + m13*m21*m32 - m11*m23*m32 - m12*m21*m33 + m11*m22*m33;
	    inverse[0][1] = m03*m22*m31 - m02*m23*m31 - m03*m21*m32 + m01*m23*m32 + m02*m21*m33 - m01*m22*m33;
	    inverse[0][2] = m02*m13*m31 - m03*m12*m31 + m03*m11*m32 - m01*m13*m32 - m02*m11*m33 + m01*m12*m33;  
	    inverse[0][3] = m03*m12*m21 - m02*m13*m21 - m03*m11*m22 + m01*m13*m22 + m02*m11*m23 - m01*m12*m23;  
	    inverse[1][0] = m13*m22*m30 - m12*m23*m30 - m13*m20*m32 + m10*m23*m32 + m12*m20*m33 - m10*m22*m33;  
	    inverse[1][1] = m02*m23*m30 - m03*m22*m30 + m03*m20*m32 - m00*m23*m32 - m02*m20*m33 + m00*m22*m33;  
	    inverse[1][2] = m03*m12*m30 - m02*m13*m30 - m03*m10*m32 + m00*m13*m32 + m02*m10*m33 - m00*m12*m33;  
	    inverse[1][3] = m02*m13*m20 - m03*m12*m20 + m03*m10*m22 - m00*m13*m22 - m02*m10*m23 + m00*m12*m23; 
	    inverse[2][0] = m11*m23*m30 - m13*m21*m30 + m13*m20*m31 - m10*m23*m31 - m11*m20*m33 + m10*m21*m33;  
	    inverse[2][1] = m03*m21*m30 - m01*m23*m30 - m03*m20*m31 + m00*m23*m31 + m01*m20*m33 - m00*m21*m33;  
	    inverse[2][2] = m01*m13*m30 - m03*m11*m30 + m03*m10*m31 - m00*m13*m31 - m01*m10*m33 + m00*m11*m33;  
	    inverse[2][3] = m03*m11*m20 - m01*m13*m20 - m03*m10*m21 + m00*m13*m21 + m01*m10*m23 - m00*m11*m23;  
	    inverse[3][0] = m12*m21*m30 - m11*m22*m30 - m12*m20*m31 + m10*m22*m31 + m11*m20*m32 - m10*m21*m32;  
	    inverse[3][1] = m01*m22*m30 - m02*m21*m30 + m02*m20*m31 - m00*m22*m31 - m01*m20*m32 + m00*m21*m32; 
	    inverse[3][2] = m02*m11*m30 - m01*m12*m30 - m02*m10*m31 + m00*m12*m31 + m01*m10*m32 - m00*m11*m32; 
	    inverse[3][3] = m01*m12*m20 - m02*m11*m20 + m02*m10*m21 - m00*m12*m21 - m01*m10*m22 + m00*m11*m22;  

		for(i=0;i<4;i++){
			for(j=0;j<4;j++) inverse[i][j] = inverse[i][j]/value;
		}
		
		return inverse;
	}
	
	//Not used
	public static void get_EulerParameters_from_OrientationMatrix(double a[][], double e[])
	{
		int i;
		double traceA,normalize;

		traceA = a[0][0] + a[1][1] + a[2][2];

	    e[0] = Math.sqrt((traceA+1.0)/4.0);
	    
		if(e[0] <= 0.0){
			//printf("Euler Parameter e0 is zeor\n");
			//exit(1);
		}
		e[1] = (a[2][1] - a[1][2])/e[0]/4.0;
	    e[2] = (a[0][2] - a[2][0])/e[0]/4.0;
	    e[3] = (a[1][0] - a[0][1])/e[0]/4.0;

		normalize = Math.sqrt( e[0]*e[0] + e[1]*e[1] + e[2]*e[2] + e[3]*e[3] );
		for(i=0;i<4;i++) e[i] = e[i]/normalize;

	}
	
	public static double[] deltaq_from_deltapi(double q[], double deltapi[])
	{	double deltaq[] = new double[4];
		int i;

		deltaq[0] = -q[1]*deltapi[0] - q[2]*deltapi[1] - q[3]*deltapi[2];
		deltaq[1] =  q[0]*deltapi[0] + q[3]*deltapi[1] - q[2]*deltapi[2];
		deltaq[2] = -q[3]*deltapi[0] + q[0]*deltapi[1] + q[1]*deltapi[2];
		deltaq[3] =  q[2]*deltapi[0] - q[1]*deltapi[1] + q[0]*deltapi[2];

		for(i=0;i<4;i++) deltaq[i] = 0.5*deltaq[i];
		
		return deltaq;
	}
	
	//not used
	public static void G_from_EulerParameter(double e[], double G[][])
	{
	  G[0][0] = -e[1];  G[0][1] =  e[0];  G[0][2] =  e[3];  G[0][3] = -e[2];
	  G[1][0] = -e[2];  G[1][1] = -e[3];  G[1][2] =  e[0];  G[1][3] =  e[1];
	  G[2][0] = -e[3];  G[2][1] =  e[2];  G[2][2] = -e[1];  G[2][3] =  e[0];
	}
	
	public static double [] asp(double a[][],double sp[])
	{	double s[]=new double [3];
	   s[0] = a[0][0]*sp[0] + a[0][1]*sp[1] + a[0][2]*sp[2];
	   s[1] = a[1][0]*sp[0] + a[1][1]*sp[1] + a[1][2]*sp[2];
	   s[2] = a[2][0]*sp[0] + a[2][1]*sp[1] + a[2][2]*sp[2];
	   
	   return s;
	}


	public static double [][]transposeMatrix(double a[][])
	{	double aatrans[][]=new double [3][3];
		aatrans[0][0] = a[0][0];	aatrans[0][1] = a[1][0];  	aatrans[0][2] = a[2][0];  
		aatrans[1][0] = a[0][1];  	aatrans[1][1] = a[1][1];	aatrans[1][2] = a[2][1];  
		aatrans[2][0] = a[0][2];  	aatrans[2][1] = a[1][2]; 	aatrans[2][2] = a[2][2];  
		
		return aatrans;
	}
	
	public static double [][]aatrans(double a[])
	{	double aatrans[][]=new double [3][3];
		aatrans[0][0] = a[0]*a[0];  aatrans[0][1] = a[0]*a[1];  aatrans[0][2] = a[0]*a[2];  
		aatrans[1][0] = a[1]*a[0];  aatrans[1][1] = a[1]*a[1];  aatrans[1][2] = a[1]*a[2];  
		aatrans[2][0] = a[2]*a[0];  aatrans[2][1] = a[2]*a[1];  aatrans[2][2] = a[2]*a[2];  
		return aatrans;
	}

	
	//Not used
	public static void mattransmulvec(double a[][],double b[],double c[],int m,int n)
	{
		int i,j;

		for(i=0;i<n;i++){
	        c[i] =0.0;
			for(j=0;j<m;j++) c[i] = c[i]+(a[j][i]*b[j]);
		}
	}


	public static double[][] inverse3by3(double m[][])
	{    double inverse[][]=new double[3][3];
		double a,b,c,d,e,f,g,h,i;
		double det;

		a = m[0][0];
		b = m[0][1];
		c = m[0][2];
		d = m[1][0];
		e = m[1][1];
		f = m[1][2];
		g = m[2][0];
		h = m[2][1];
		i = m[2][2];

		det = a*(e*i-f*h) - b*(d*i-f*g) + c*(d*h-e*g);

		inverse[0][0] = (e*i-f*h)/det;
		inverse[0][1] = (c*h-b*i)/det;
		inverse[0][2] = (b*f-c*e)/det;
		inverse[1][0] = (f*g-d*i)/det;
		inverse[1][1] = (a*i-c*g)/det;
		inverse[1][2] = (c*d-a*f)/det;
		inverse[2][0] = (d*h-e*g)/det;
		inverse[2][1] = (b*g-a*h)/det;
		inverse[2][2] = (a*e-b*d)/det;
		
		return inverse;
	}
}
