
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Scanner;
import java.util.TreeMap;


public class s3TileAnalyser {
	
//	public enum OSMBaseDataTypes {
//	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
//		System.out.println("s3TileAnalyser:");
		
		String cusRegionDef = null;
		String cusRegionName = null;
		RectXY cusRegionBoundary = null;
		boolean printPrompts = true;
		String[] components;
		if(args.length > 0) {
			// take first arg as input
			cusRegionDef = args[0];
			cusRegionDef = cusRegionDef.substring(1, cusRegionDef.length() -1);
			components = cusRegionDef.split(";");
			if(components.length != 5) {
				System.out.println("Error: bad argument " + cusRegionDef);
				return;
			}
			try {
				cusRegionName = components[0];
				cusRegionBoundary = new RectXY(Float.parseFloat(components[1]),Float.parseFloat(components[4]), Float.parseFloat(components[3]), Float.parseFloat(components[2]));
				printPrompts = false;
			}
			catch (Exception e) {
				System.out.println("Error: bad argument " + cusRegionDef);
				return;
			}
		}
		
		ArrayList<RegionData> mRegionData = new ArrayList<RegionData>();
		TreeMap<Integer, S3Tile> S3Tiles = new TreeMap<Integer, S3Tile>();	// hold all existing tiles
		// input files
		String regionsFileStr = System.getProperty("user.dir") + "/inputGeoRegions.txt";
		String s3TilesFileStr = System.getProperty("user.dir") + "/existingtiles.txt";
		
		// output files
	    String strDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
	    String csvPathStr = System.getProperty("user.dir") + "/output/analysis/";
	    File csvPath = new File(csvPathStr);
	    if(!csvPath.exists()) {
	    	csvPath.mkdirs();
	    }

	    String matlabTileIndicesFileStr = System.getProperty("user.dir") + "/plotting/existingTiles.out";	// for matlab-based tile visualization, ie plotting on map
		String regionFilePrefix = System.getProperty("user.dir") + "/output/regionDetails/";
		String outFileStr = csvPathStr + "regionalTileAnalysis_" + strDate + ".csv";
		String extraTilesFileStr = csvPathStr + "extraTiles_" + strDate + ".csv";
		String oldFormatTilesFileStr = csvPathStr + "oldFormatS3Tiles_" + strDate + ".csv";

		if(printPrompts) {
			System.out.println("Analysing S3 contents... ");
		}
		int tilesWithOldFormat = 0;
		
		File s3File = new File(s3TilesFileStr);
		File regionsFile = new File(regionsFileStr);
		if (!s3File.exists() || !regionsFile.exists()){
			System.out.println("Aborting analysis... missing " + s3TilesFileStr + " or " + regionsFileStr);
			return;
		}

		try {
			FileReader input = new FileReader(s3TilesFileStr);
			BufferedReader bufRead = new BufferedReader(input);
			String myLine = null;


			FileWriter oldoutput = new FileWriter(oldFormatTilesFileStr);
			BufferedWriter oldFormatOutWrite = new BufferedWriter(oldoutput);
			String fileString = "Index,Left,Right,Top,Bottom\n";
			oldFormatOutWrite.write(fileString);

			FileWriter mOutput = new FileWriter(matlabTileIndicesFileStr);
			BufferedWriter matlabBufWrite = new BufferedWriter(mOutput);

			// parse s3 tile index file into treemap, generating plotting info as we go
			long cnt=0;
			while ( (myLine = bufRead.readLine()) != null) {    
				String[] s3LScomponent = myLine.split("\\s");
				String tileFileName = s3LScomponent[s3LScomponent.length-1];
				int location = tileFileName.lastIndexOf(".");
				if(location == 9) {		// only process new format files
					String indexStr = tileFileName.substring(0,location);
					String typeStr = tileFileName.substring(location+1);
					if(cnt >= 410782) {
						cnt +=0;
					}
					if(typeStr.equalsIgnoreCase("rgz")) {
						Integer curIndex = Integer.valueOf(indexStr);		// todo trap for non numberic indexStr
						if(curIndex != null) {
							GeoRegion tileRegion = GeoTile.GetGeoRegionFromTileIndex(curIndex);

							int latIndex = (int)((curIndex / 100000)+ 0.1); // 0.1 handles JAVA round down error
							int longIndex = curIndex % 100000;

							// break up lat, long indices to simplify tile plotting using matlab/octave - see plottiles.m
							fileString = "" + String.format("%04d", latIndex) + "," + String.format("%05d", longIndex) + "\n";
							matlabBufWrite.write(fileString);

							S3Tiles.put(curIndex, new S3Tile(curIndex, tileRegion.mBoundingBox));
						}
					}
				}	
				else {
					if(location == 8) {
						String indexStr = tileFileName.substring(0,location);
						String typeStr = tileFileName.substring(location+1);
						if(typeStr.equalsIgnoreCase("rgz")) {
							Integer curIndex = Integer.valueOf(indexStr);		// todo trap for non numberic indexStr
							GeoRegion tileRegion = GeoTile.GetGeoRegionFromTileIndex(curIndex);
							fileString = "" + curIndex + "," + tileRegion.mBoundingBox.left  + "," + tileRegion.mBoundingBox.right + "," + tileRegion.mBoundingBox.top  + ","+ tileRegion.mBoundingBox.bottom + "\n";
							oldFormatOutWrite.write(fileString);
//							System.out.println("Old format tile: " + tileFileName);
							tilesWithOldFormat ++;
						}
					}
					else {
						if(printPrompts) {
							System.out.println("non-numeric rgz file on s3: " + tileFileName);
						}
					}
				}
			

				if(printPrompts && (cnt % 10000) == 0) {		// print out what's happening
					System.out.println("" + cnt);
				}
				cnt++;
			}

			bufRead.close();
			// at end, loop all regions, reporting all regions stats and non-region - also save this to file
			matlabBufWrite.close();
			oldFormatOutWrite.close();

			
			int numExistingTiles = 0;
			if(cusRegionDef != null) { // valid custom region supplied as argument
				GeoRegion custRegion = new GeoRegion();
				custRegion.MakeUsingBoundingBox(cusRegionBoundary.left,cusRegionBoundary.top,cusRegionBoundary.right,cusRegionBoundary.bottom );
				ArrayList<Integer> requiredTiles = GeoTile.GetTileListForGeoRegion(custRegion, null);

				fileString = "TileIndex, OnS3, Left, Right, Top, Bottom";
				System.out.println(fileString);

				for(Integer index : requiredTiles) {
					S3Tile s3Tile = S3Tiles.get(index);
					if(s3Tile != null) { // tile on S3
						fileString = "" + index + ", 1," + String.format("%10.3f",s3Tile.mBounds.left) + "," + String.format("%10.3f",s3Tile.mBounds.right) + "," + String.format("%10.3f",s3Tile.mBounds.top)  + ","+ String.format("%10.3f",s3Tile.mBounds.bottom) ;
						System.out.println(fileString);
						s3Tile.mUsed = true;
						numExistingTiles++;
					}
					else {
						GeoRegion tileRegion = GeoTile.GetGeoRegionFromTileIndex(index);
						fileString = "" + index + ", 0," + String.format("%10.3f",tileRegion.mBoundingBox.left) + "," + String.format("%10.3f",tileRegion.mBoundingBox.right) + "," + String.format("%10.3f",tileRegion.mBoundingBox.top)  + ","+ String.format("%10.3f",tileRegion.mBoundingBox.bottom) ;
						System.out.println(fileString);
					}
				}
				
				int numRequiredTiles = requiredTiles.size();
				if(numRequiredTiles > 0) {
					float percent = (float)numExistingTiles / (float)numRequiredTiles * 100.f;
					fileString = "\n" + cusRegionName + "," + percent + "% of tiles on S3,(" + numExistingTiles + "/" + numRequiredTiles + ")";
					System.out.println(fileString);
				}
				else {
					System.out.println("Region " + cusRegionName + " - error: 0 required tiles, what's going on??? ");
				}
				
			}
			else {
				// process regions one at a time

				input = new FileReader(regionsFileStr);
				bufRead = new BufferedReader(input);
				myLine = null;


				FileWriter output = new FileWriter(outFileStr);
				BufferedWriter bufWrite = new BufferedWriter(output);
				fileString = "Region,Percent Complete,#Existing Tiles,#Required Tiles\n";
				bufWrite.write(fileString);

				// read region file into array of region objects (stringname, georegion, treemap of enclosed tiles, treemap of existing tileindicies)
				while ( (myLine = bufRead.readLine()) != null) {    
					if(myLine.contains("[")) {
						if(!myLine.contains("\"") && !myLine.contains("\'")) {	// ignore region header lines  -- in current file Australia title line is different !!!


							myLine.replaceAll("#","");	// uncomment commented lines
							components = myLine.split(";");
							components[0] = (components[0].substring(1, components[0].length() -2)).replaceAll("\\W","").replaceAll("\\s","");	// clean up components, could do this more efficiently !
							components[1] = components[1].replaceAll("\\s","");
							components[2] = components[2].replaceAll("\\s","");
							components[3] = components[3].replaceAll("\\s","");
							components[4] = (components[4].substring(0, components[4].length() -1)).replaceAll("\\s","");
							RegionData newRegion = new RegionData(components);

							System.out.println("loading region " + newRegion.regionName + " with " + newRegion.numRequiredTiles + " tiles");

							String folderPathStr = regionFilePrefix + components[0] ;
							File folderPath = new File(folderPathStr);
							if(!folderPath.exists()) {
								folderPath.mkdirs();
							}

							FileWriter regionoutput = new FileWriter(folderPathStr+ "/"+newRegion.regionName + "_completedTiles_" + strDate + ".csv");
							BufferedWriter regionOutWrite = new BufferedWriter(regionoutput);
							fileString = "TileIndex, OnS3, Left, Right, Top, Bottom\n";
							regionOutWrite.write(fileString);

							for(Integer index : newRegion.requiredTiles) {
								S3Tile s3Tile = S3Tiles.get(index);
								if(s3Tile != null) { // tile on S3
									fileString = "" + index + ", 1," + String.format("%10.3f",s3Tile.mBounds.left) + "," + String.format("%10.3f",s3Tile.mBounds.right) + "," + String.format("%10.3f",s3Tile.mBounds.top)  + ","+ String.format("%10.3f",s3Tile.mBounds.bottom) + "\n";
									regionOutWrite.write(fileString);
									s3Tile.mUsed = true;
									numExistingTiles++;
								}
								else {
									GeoRegion tileRegion = GeoTile.GetGeoRegionFromTileIndex(index);
									fileString = "" + index + ", 0," + String.format("%10.3f",tileRegion.mBoundingBox.left) + "," + String.format("%10.3f",tileRegion.mBoundingBox.right) + "," + String.format("%10.3f",tileRegion.mBoundingBox.top)  + ","+ String.format("%10.3f",tileRegion.mBoundingBox.bottom) + "\n";
									regionOutWrite.write(fileString);
								}
							}

							regionOutWrite.close();

							int numRequiredTiles = newRegion.requiredTiles.size();
							if(numRequiredTiles > 0) {
								float percent = (float)numExistingTiles / (float)numRequiredTiles * 100.f;
								fileString = "" + newRegion.regionName + "," + percent + "," + numExistingTiles + "," + numRequiredTiles + "\n";
								bufWrite.write(fileString);
								System.out.println("Region " + newRegion.regionName + "  " + percent + "% complete (" + numExistingTiles + "/" + numRequiredTiles + ")");
							}
							else {
								System.out.println("Region " + newRegion.regionName + " - error: 0 required tiles ??? ");
							}


						}
					}
				}
				bufRead.close();
				bufWrite.close();

				FileWriter extraoutput = new FileWriter(extraTilesFileStr);
				BufferedWriter extraOutWrite = new BufferedWriter(extraoutput);
				fileString = "Index,Left,Right,Top,Bottom\n";
				extraOutWrite.write(fileString);

				cnt = 0;
				for(Object tile :S3Tiles.values().toArray()) {
					S3Tile s3Tile = (S3Tile)tile;
					if(!s3Tile.mUsed) {
						Integer curIndex = s3Tile.mIndex;		// todo trap for non numberic indexStr
						GeoRegion tileRegion = GeoTile.GetGeoRegionFromTileIndex(curIndex);
						fileString = "" + curIndex + "," + tileRegion.mBoundingBox.left  + "," + tileRegion.mBoundingBox.right + "," + tileRegion.mBoundingBox.top  + ","+ tileRegion.mBoundingBox.bottom + "\n";
						extraOutWrite.write(fileString);
						cnt ++;
					}
				}
				extraOutWrite.close();

				if(cnt > 0) {
					System.out.println("\nWARNING !!  " + cnt + " additional tiles found on S3 that do not belong to any of the defined regions - See /cvs_output/extraTiles<data>.cvs");
				}
				if(tilesWithOldFormat > 0) {
					System.out.println("\nWARNING !!  " + tilesWithOldFormat + " tiles found on S3 that have old file format (8 digits) - See /cvs_output/oldFormatTiles<data>.cvs");
				}	
			}

		}
		catch (IOException e) {
			System.out.println("IOException...:" + e.getMessage());
		}
		catch (Exception e) {
			System.out.println("IOException...:" + e.getMessage());
		}
	}
	

}
