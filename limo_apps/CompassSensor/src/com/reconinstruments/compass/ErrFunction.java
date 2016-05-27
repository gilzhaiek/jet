package com.reconinstruments.compass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import android.util.Log;
 
public class ErrFunction implements MultivariateFunction {
	
	private static final String TAG = "ErrFunction";
	private ArrayList<Reading> readings = new ArrayList<Reading>();
	
	private double[] bias_op  = new double[3];
	private double[] scale_op = new double[3];
	private double[][] scale_matrix = new double[3][3];
	
	private CalibrationParameters calParam;
	
	public ErrFunction(){
		calParam = new CalibrationParameters();		
	}
	
	public void addPoints(Double xMag, Double yMag, Double zMag) {
		readings.add(new Reading(xMag.doubleValue(), yMag.doubleValue(), zMag.doubleValue()));
	}
	
	/**
	 * Reports the variance of the magnetic norm squared
	 * @param arg0 - offsets for x, y, z
	 */
	@Override
	public double value(double[] arg0) {
		double[] xOffset = new double[readings.size()];
		double[] yOffset = new double[readings.size()];
		double[] zOffset = new double[readings.size()];
		
		for(int i=0; i<readings.size(); i++) {
			xOffset[i] = readings.get(i).x;
			yOffset[i] = readings.get(i).y;
			zOffset[i] = readings.get(i).z;
		}
		
		// Calculate norms squared
		double[] sqnorms = new double[xOffset.length];
		for(int i=0; i<xOffset.length; i++) {
			// add offsets to each array
			xOffset[i] += arg0[0];
			yOffset[i] += arg0[1];
			zOffset[i] += arg0[2];
			
			sqnorms[i] = (xOffset[i] * xOffset[i]) 
					+ (yOffset[i] * yOffset[i]) 
					+ (zOffset[i] * zOffset[i]);
		}
		
		// Calculate Variance
		double mean = 0;
		for(double x : sqnorms)
			mean += x;
		mean = mean / sqnorms.length;
		
		double variance = 0;
		for(double norm : sqnorms)
			variance += Math.pow((norm - mean), 2);
		
		return variance;
	}
	
	/**
	 * Check for outliers version 2
	 * @param arg0 - offsets for x, y, z
	 * @param min - minimum accepted value
	 * @param max - maximum accepted value
	 * @return returns the percentage of values that were inside the range
	 */
	public double rangePercent(double min, double max) {
		double count=0;
		double inside=0;

		for(Reading r : readings) {
			count = count + 1;
			double normSquared = (r.x * r.x) + (r.y * r.y) + (r.z * r.z);
			if((normSquared > (min * min)) && (normSquared < (max * max)) )
			{
				inside = inside + 1;
			}
		}

		if((count == 0) || (inside == 0)) {
			Log.w(TAG,"rangePercent: count=" + count + " inside=" + inside);
			return 0;
		}
		else
			return (inside / count);
	}

	/**
	 * Check for outliers
	 * @param arg0 - offsets for x, y, z
	 * @param min - minimum accepted value
	 * @param max - maximum accepted value
	 * @return returns true if there is no norm greater than max or less than min
	 */
	public boolean range(double[] arg0, double min, double max) {
		for(Reading r : readings) {
			double normSquared = (r.x * r.x) + (r.y * r.y) + (r.z * r.z);
			
			if(normSquared < (min * min)) return false;
			if(normSquared > (max * max)) return false;
		}
		
		return true;
	}
	
	class Stats {
		public double min,max,avg;
		public Stats(double a, double b, double c) {
            min = a;
            max = b;
            avg = c;
		}
	}

	class Reading {
		public double x, y, z;
		public Reading(double xMag, double yMag, double zMag) {
			x = xMag;
			y = yMag;
			z = zMag;
		}
	}

	/**
	 * Calculate the Average Magnetic Strength from data recorded in reading
	 * @param magOffsetX
	 * @param magOffsetY
	 * @param magOffsetZ
	 * @return the average Magnetic Strength
	 */
	public double GetAverageMagStrengh(double magOffsetX, double magOffsetY,	double magOffsetZ){
		int size = readings.size();
		double total = 0.0;
		for(Reading r : readings){
			double rx = r.x + magOffsetX;
			double ry = r.y + magOffsetY;
			double rz = r.z + magOffsetZ;
			total += (rx * rx) + (ry * ry) + (rz * rz);	
		}
		total = total /size;
		total = Math.sqrt(total);
		return total;
	}
    /**
     * Return statistics on readings taken during calibration
	 * @param magOffsetX
	 * @param magOffsetY
	 * @param magOffsetZ
     * @return {min,max}
     */
	public Stats GetMinMax(){
		int size = readings.size();
        double norm = 0.0;
        double max = 0.0;
        double total = 0.0;
        double min = 0.0;
        int count = 0;
        boolean initial = true;
		for(Reading r : readings){
			double rx = r.x;
			double ry = r.y;
			double rz = r.z;
			norm = (rx * rx) + (ry * ry) + (rz * rz);	
			total += (rx * rx) + (ry * ry) + (rz * rz);	
            if (initial == true) 
            {
                min = norm;
                initial = false;
            }
            if (norm < min)
            {
                min = norm;
            }
            if (norm > max)
            {
                max = norm;
            }
            count = count + 1;
		}
        double avg = total / (double)count;
        Stats ret = new Stats(min,max,avg);
//        Log.e(TAG,"in GetMinMax(): norm: " + norm + ", min: " + min + ", max: "  + max + ", count" + count + ", avg: " + avg); 

        return ret;
	}

	public double[] GetOffsets() {
		return bias_op;
	}

	public double[] GetScales() {
		return scale_op;
	}
	
	public double[][] GetScaleMatrix(){
		return scale_matrix;
	}
	
	
	private boolean continueIteration(RealMatrix M_k1_inv, RealMatrix b_k1)
	{
		double scaleThreshold,biasThreshold;
		scaleThreshold = 0.00001d;
		biasThreshold = 0.00001d;
		
		if (b_k1.getNorm() > biasThreshold){
//			Log.i(TAG, "b_k1 norm > biasThreshold ");
			return true;
			
		}
			
		M_k1_inv= M_k1_inv.subtract(MatrixUtils.createRealIdentityMatrix(3));
		if (M_k1_inv.getNorm() > scaleThreshold){
//			Log.i(TAG, "M_k1_inv norm > scaleThreshold ");
			return true;
		}
			

		// If both bias and scale changes are small, stop iteration.
		Log.i(TAG, "Stop iteration!");
		return false;
	}
	
	private void printMatrixLog(RealMatrix M_k){
		Log.i(TAG, "row1:  " + M_k.getEntry(0, 0) + "  " + M_k.getEntry(0, 1) + "  " + M_k.getEntry(0, 2));
		Log.i(TAG, "row2:  " + M_k.getEntry(1, 0) + "  " +M_k.getEntry(1, 1) + "  " + M_k.getEntry(1, 2));
		Log.i(TAG, "row3:  " + M_k.getEntry(2, 0)  + "  " + M_k.getEntry(2, 1) + "  " + M_k.getEntry(2, 2));
	}

	/**
	 * Calculate the Offset and scale from data recorded in reading 
	 * @param avgMagStrengh the Magnetic Field Strength you want to scale for 
	 * @return success calibrating or not
	 */
	public boolean CalculateOffsetAndScale(double magFieldStrength) {
		Log.i(TAG,"Sample Size : " + readings.size());
		int data_size = readings.size();
		double fieldStrength;
		
		/*******************************************************
		 *  First pass - Hard-iron calibration
		 *  
		 *  Sensor model:
		 *  actual_mag = sensor_mag  - bias
		 *  
		 *******************************************************/
		
		double[][] magnetXf= new double[data_size][4];
		double[][] magnetYf= new double[1][data_size];
		double[]magnetSampleX= new double[data_size];
		double[]magnetSampleY= new double[data_size];
		double[]magnetSampleZ= new double[data_size];

		int counter=0;
		for(Reading r : readings){
			magnetSampleX[counter]=r.x;
			magnetSampleY[counter]=r.y;
			magnetSampleZ[counter]=r.z;
			counter++;
		}
		for (int i = 0; i < data_size; i++) {
			magnetXf[i][0] = magnetSampleX[i];
			magnetXf[i][1] = magnetSampleY[i];
			magnetXf[i][2] = magnetSampleZ[i];
			magnetXf[i][3] = 1;
			magnetYf[0][i] = Math.pow(magnetXf[i][0], 2)+Math.pow(magnetXf[i][1], 2)+Math.pow(magnetXf[i][2], 2);
		}

		//if inv(Xf'*Xf)*Xf*Yf
		RealMatrix Xfmatrix = MatrixUtils.createRealMatrix(magnetXf);
		RealMatrix Yfmatrix = MatrixUtils.createRealMatrix(magnetYf);
		RealMatrix Xfmatrix_multiply = Xfmatrix.preMultiply(Xfmatrix.transpose());
		Xfmatrix_multiply = new LUDecomposition(Xfmatrix_multiply).getSolver().getInverse();
		RealMatrix Xfmatrix_multiply_0 = Yfmatrix.transpose().preMultiply(Xfmatrix.transpose());
		Xfmatrix_multiply = Xfmatrix_multiply.multiply(Xfmatrix_multiply_0);

		double b1 = Xfmatrix_multiply.getEntry(0, 0);
		double b2 = Xfmatrix_multiply.getEntry(1, 0);
		double b3 = Xfmatrix_multiply.getEntry(2, 0);
		double b4 = Xfmatrix_multiply.getEntry(3, 0);
		

		double biasX, biasY, biasZ;
		biasX = b1/2;
		biasY = b2/2;
		biasZ = b3/2;
		fieldStrength = Math.sqrt(b4 + (biasX*biasX) + (biasY*biasY) + (biasZ*biasZ));
				
		if (Double.isNaN(biasX)||Double.isNaN(biasY)||Double.isNaN(biasZ)){
			Log.i(TAG, "Can't compute initial conditions. Recollect data.");
			return false;
		}

		Log.i(TAG, "==== Initial value for the optimizations ====");
		Log.i(TAG, "MagField strength: " + fieldStrength);
		Log.i(TAG, "Initial bias: [" + biasX + ", " + biasY + ", " + biasZ +"]");
		

		// bias_op is used for the 2nd part of the calibration, using iterations to find the least square
		bias_op[0] = biasX;
		bias_op[1] = biasY;
		bias_op[2] = biasZ;

		/*******************************************************
		 *  Second pass - Full hard-iron & soft-iron calibration
		 *  
		 *  Sensor model: sensor_mag = M_k * actual_mag + b_k
		 *  			  actual_mag = inv(M_k) * (sensor_mag - b_k)
		 *  			  actual_mag = inv(M_k) * sensor_mag + (- inv(M_k)*b_k)
		 *  
		 *  mul_A = inv(M_k)
		 *  conv_B = -inv(M_k)*b_k
		 *******************************************************/
		
		double h = magFieldStrength;
		double[][] matrixData = { {1d,0.01d,0.01d}, {0.01d,1d,0.01d},{0.01d,0.01d,1d}};
		RealMatrix M_k = MatrixUtils.createRealMatrix(matrixData);
		RealMatrix M_k_inv = new LUDecomposition(M_k).getSolver().getInverse();
		RealMatrix b_k = MatrixUtils.createRealMatrix(3,1);
		b_k.setColumn(0, bias_op);

		RealMatrix M_k1_inv = new LUDecomposition(M_k).getSolver().getInverse();

		matrixData = new double[][]{ {1d}, {1d},{1d}};
		RealMatrix b_k1 = MatrixUtils.createRealMatrix(matrixData);

		int iteration = 0;

		//Preallocate matrix
		RealMatrix hs = MatrixUtils.createRealMatrix(3,1); //sensor measurement
		RealMatrix h_k = MatrixUtils.createRealMatrix(3,1);
		RealMatrix E_k = MatrixUtils.createRealMatrix(data_size, 1);
		RealMatrix E_1 = MatrixUtils.createRealMatrix(1, 1);
		RealMatrix H = MatrixUtils.createRealMatrix(data_size, 9);
		double hk1,hk1sq,hk2,hk2sq,hk3,hk3sq;
		RealMatrix A = MatrixUtils.createRealMatrix(9, 1);
		RealMatrix Q_k = MatrixUtils.createRealMatrix(3,3);
		RealMatrix alpha_k = MatrixUtils.createRealMatrix(3,3);
		RealMatrix lambda_k = MatrixUtils.createRealMatrix(3,1);
		RealMatrix V = MatrixUtils.createRealMatrix(3,3);
		RealMatrix D = MatrixUtils.createRealMatrix(3,3);

		double [] sampleNorm = new double[data_size];
		
		// Main iteration loop
		Log.i(TAG, "======== Start Full Magnetometer Calibration ========");
		while (continueIteration(M_k1_inv,b_k1)){
			iteration++;
//			Log.i(TAG, "Iteration: "+iteration);
			for (int i=0;i<data_size;i++){
				// apply the transformation from (6)
				hs.setEntry(0,0, magnetSampleX[i]);
				hs.setEntry(1,0, magnetSampleY[i]);
				hs.setEntry(2,0, magnetSampleZ[i]);
				hs = hs.subtract(b_k);
				h_k = hs.preMultiply(M_k_inv);

				// calculate E from (5)
				E_1 = h_k.preMultiply(h_k.transpose());
				sampleNorm[i]=Math.sqrt(E_1.getEntry(0, 0));
				E_1=E_1.scalarAdd(-h*h);
				E_k.setEntry(i, 0, E_1.getEntry(0, 0));

				// construct the H matrix
				hk1=h_k.getEntry(0,0);
				hk2=h_k.getEntry(1,0);
				hk3=h_k.getEntry(2,0);
				hk1sq = hk1*hk1;
				hk2sq = hk2*hk2;
				hk3sq = hk3*hk3;

				H.setEntry(i, 0, hk1sq);
				H.setEntry(i, 1, hk2sq);
				H.setEntry(i, 2, hk3sq);
				H.setEntry(i, 3, -hk1*hk2);
				H.setEntry(i, 4, -hk1*hk3);
				H.setEntry(i, 5, -hk2*hk3);
				H.setEntry(i, 6, 2*hk1);
				H.setEntry(i, 7, 2*hk2);
				H.setEntry(i, 8, 2*hk3);

			}


			// from (13)
			RealMatrix H_multiply = H.preMultiply(H.transpose());
			H_multiply = new LUDecomposition(H_multiply).getSolver().getInverse();
			RealMatrix H_multiply_0 = E_k.preMultiply(H.transpose());
			A = H_multiply.multiply(H_multiply_0);


			Q_k.setEntry(0, 0, 1-A.getEntry(0, 0));
			Q_k.setEntry(1, 1, 1-A.getEntry(1, 0));
			Q_k.setEntry(2, 2, 1-A.getEntry(2, 0));

			Q_k.setEntry(0, 1, A.getEntry(3, 0)/2);
			Q_k.setEntry(1, 0, A.getEntry(3, 0)/2);

			Q_k.setEntry(0, 2, A.getEntry(4, 0)/2);
			Q_k.setEntry(2, 0, A.getEntry(4, 0)/2);

			Q_k.setEntry(1, 2, A.getEntry(5, 0)/2);
			Q_k.setEntry(2, 1, A.getEntry(5, 0)/2);

			alpha_k = Q_k.multiply(M_k);

			lambda_k = A.getSubMatrix(6, 8, 0, 0);

			// from (12)		
			RealMatrix alpha_inv = new LUDecomposition(alpha_k).getSolver().getInverse();
			b_k1 = alpha_inv.multiply(lambda_k);

			EigenDecomposition eig = new EigenDecomposition(Q_k,0.0001);

			D = eig.getD();
			V = eig.getV();

			D.setEntry(0, 0, Math.sqrt(D.getEntry(0, 0)));
			D.setEntry(1, 1, Math.sqrt(D.getEntry(1, 1)));
			D.setEntry(2, 2, Math.sqrt(D.getEntry(2, 2)));

			M_k1_inv = V.multiply(D);
			M_k1_inv = M_k1_inv.multiply(V.transpose());
			
			// update M and b from (4)
			M_k = M_k.multiply(new LUDecomposition(M_k1_inv).getSolver().getInverse());
			M_k_inv = new LUDecomposition(M_k).getSolver().getInverse();
			b_k = b_k.add(b_k1);
			

			if (iteration > 20) { 
				// if solution doesn't converge, return calibration is false;
				Log.i(TAG, "iteration >50. Default back to initial conditions");
				return false;
			}
		}
		b_k=b_k.preMultiply(M_k_inv);
		bias_op=b_k.getColumn(0).clone();
		bias_op[0]=-bias_op[0];bias_op[1]=-bias_op[1];bias_op[2]=-bias_op[2];

		Log.i(TAG, "======== Finished Full Magnetometer Calibration after "+ iteration+" iterations  ========");

		Log.i(TAG, "bias_opX: " + bias_op[0] + " bias_opY: " + bias_op[1] + " bias_opZ: " + bias_op[2]);
		Log.i(TAG, "scale: " + M_k_inv.getEntry(0, 0) + "  " + M_k_inv.getEntry(0, 1) + "  " + M_k_inv.getEntry(0, 2));
		Log.i(TAG, "scale: " + M_k_inv.getEntry(1, 0) + "  " +M_k_inv.getEntry(1, 1) + "  " + M_k_inv.getEntry(1, 2));
		Log.i(TAG, "scale: " + M_k_inv.getEntry(2, 0)  + "  " + M_k_inv.getEntry(2, 1) + "  " + M_k_inv.getEntry(2, 2));
		//				Log.i(TAG, "scale_opX: " + scale_op[0] + " scale_opY: " + scale_op[1] + " scale_opZ: " + scale_op[2]);
		calParam.iterations = iteration;
		StandardDeviation std = new StandardDeviation();
		calParam.std = (float) std.evaluate(sampleNorm);
		Log.i(TAG, "Final sample std: " + calParam.std );
		
		scale_op[0] =M_k_inv.getEntry(0, 0);
		scale_op[1] =M_k_inv.getEntry(1, 1);
		scale_op[2] =M_k_inv.getEntry(2, 2);
		
		scale_matrix = M_k_inv.getData();

		resetVariables();

		return true;
	}
	
	private void resetVariables() {
		readings.clear();
	}
	
	private boolean isNegative(double number) {
		return number<=0;
	}
	
	public boolean writeReadingssToCSV(){

		File mRoot = android.os.Environment.getExternalStorageDirectory();
		File mDir = new File(mRoot.getAbsolutePath() + "/calibration_data");
		File mFile = new File(mDir, "Readings_"+calParam.id+".csv");
		try {
			if(!mDir.exists())
				mDir.mkdir();
			if(!mFile.exists())
				mFile.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		BufferedWriter bufferedWriter = null;
		try {
			if(!mFile.exists()){
				mFile.createNewFile();
			}
			FileWriter fw;

			fw = new FileWriter(mFile);
			bufferedWriter = new BufferedWriter(fw);
			String line = " ";
			for(Reading r : readings){
				line = String.format("%.5f, %.5f, %.5f\n", (float) r.x,(float)r.y,(float)r.z);
				bufferedWriter.write(line);
			}

			Log.d(TAG,"Written to Readings.csv file in ");
		} catch (IOException e) {
			Log.d(TAG, "Could not write Readings.csv");
			e.printStackTrace();
			return false;
		}finally {
			try {
				if (bufferedWriter != null){
					bufferedWriter.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return true;
	}
	
	
	public boolean writeCalParamToCSV(){
		//TODO: Move this to CalibrationParameters class

		File mRoot = android.os.Environment.getExternalStorageDirectory();
		File mDir = new File(mRoot.getAbsolutePath() + "/calibration_data");
		File mFile = new File(mDir, "calparam_"+calParam.id+".txt");
		
		try {
			if(!mDir.exists())
				mDir.mkdir();
			if(!mFile.exists())
				mFile.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		BufferedWriter bufferedWriter = null;
		try {
			if(!mFile.exists()){
				mFile.createNewFile();
			}
			FileWriter fw;

			fw = new FileWriter(mFile);
			bufferedWriter = new BufferedWriter(fw);
			String line = " ";

			line = String.format("Std of calibrated data = %.6f \n", calParam.std);
			bufferedWriter.write(line);
			line = String.format("Number of iterations = %d \n", calParam.iterations);
			bufferedWriter.write(line);
			line = "\n\n============ Results ===========\n";
			line = String.format("mul_A1 = %.6f, %.6f, %.6f\n", (float)scale_matrix[0][0],  (float)scale_matrix[0][1], (float)scale_matrix[0][2]);
			bufferedWriter.write(line);
			line = String.format("mul_A2 = %.6f, %.6f, %.6f\n", (float)scale_matrix[1][0],  (float)scale_matrix[1][1], (float)scale_matrix[1][2]);
			bufferedWriter.write(line);
			line = String.format("mul_A3 = %.6f, %.6f, %.6f\n", (float)scale_matrix[2][0], (float) scale_matrix[2][1], (float)scale_matrix[2][2]);
			bufferedWriter.write(line);
			line = String.format("conv_B = %.6f, %.6f, %.6f\n", (float)bias_op[0],  (float)bias_op[1], (float)bias_op[2]);
			bufferedWriter.write(line);
			

			Log.d(TAG,"Written to calparam.csv file in ");
		} catch (IOException e) {
			Log.d(TAG, "Could not write calparam.csv");
			e.printStackTrace();
			return false;
		}finally {
			try {
				if (bufferedWriter != null){
					bufferedWriter.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}


		return true;
	}

}
