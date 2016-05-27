package com.reconinstruments.applauncher.transcend;
/** 
 * This module is mock data provider.Separatre files should be stored in
 * the mass storage or any other place that you deem suitable. These files 
 * 
 * An altitude file, Pressure file, Temperature file and locatoin file. 
 * The data for each file shoule be one data point per line. The application. 
 */
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

public class ReconMockDataProvider {

	//Altitude
	private FileInputStream afs;
	private DataInputStream ain;
	private BufferedReader abr;
	
		
	//Pressure
	private FileInputStream pfs;
	private DataInputStream pin;
	private BufferedReader pbr;
	
	//Temperature
	private FileInputStream tfs;
	private DataInputStream tin;
	private BufferedReader tbr;
	
	//speed
	private FileInputStream lfs;
	private DataInputStream lin;
	private BufferedReader lbr;
	
	
	public ReconMockDataProvider(File altFile, File pressureFile, File temperatureFile, File speedFile){ 
	
	try{

		afs = new FileInputStream(altFile);
		ain = new DataInputStream(afs);
		abr = new BufferedReader(new InputStreamReader(ain));
		
		pfs = new FileInputStream(pressureFile);
		pin = new DataInputStream(pfs);
		pbr = new BufferedReader(new InputStreamReader(pin));
		
		tfs = new FileInputStream(temperatureFile);
		tin = new DataInputStream(tfs);
		tbr = new BufferedReader(new InputStreamReader(tin));
		
		lfs = new FileInputStream(speedFile);
		lin = new DataInputStream(lfs);
		lbr = new BufferedReader(new InputStreamReader(lin));
	}catch (Exception e){
	}
		
		
	}
	public float getPressureAlt(){
		float f = -10000;
		try{
			String strLine;
			strLine = abr.readLine();
			if (strLine != null){
				f = Float.valueOf(strLine.trim()).floatValue();
			}
			
		} catch (Exception e){
			
		}
		return f;
	}
	
	public float getPressure(){
		float f = -10000;
		try{
			String strLine;
			strLine = pbr.readLine();
			if (strLine != null){
				f = Float.valueOf(strLine.trim()).floatValue();
			}
			
		} catch (Exception e){
			
		}
		return f;
	}
	public float getTemperature(){
		float f = -10000;
		try{
			String strLine;
			strLine = tbr.readLine();
			if (strLine != null){
				f = Float.valueOf(strLine.trim()).floatValue();
			}
			
		} catch (Exception e){
			
		}
		return f;
	}
	public float getSpeed(){
		float f = -10000;
		try{
			String strLine;
			strLine = lbr.readLine();
			if (strLine != null){
				f = Float.valueOf(strLine.trim()).floatValue();
			}
			
		} catch (Exception e){
			
		}
		return f;
		
	}
	

}
