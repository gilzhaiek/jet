
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


public class TileCompressor {
	
    // OSMBaseDataTypes needs to match code in OSMDataSource - TODO pull this out of OSMDataSource into separate class and link here
	public enum OSMBaseDataTypes {
		AREA_LAND,
		AREA_OCEAN,
		AREA_CITYTOWN,
		AREA_WOODS,
		AREA_WATER,
		AREA_FIELD,
		AREA_PARK,
		HIGHWAY_MOTORWAY,	// OSM uses highway as British term for transport path (Road, bike path) and Motorway is Freeway
		HIGHWAY_PRIMARY,	// main city through road
		HIGHWAY_SECONDARY,	// main city road
		HIGHWAY_TERTIARY,	// lesser main city road
		HIGHWAY_RESIDENTIAL,	// residential road
//		HIGHWAY_SERVICE, 		// back alleys etc
		POI_RESTAURANT, 	//Type from MD
		POI_STORE,
		POI_HOSPITAL,
		POI_WASHROOM,
		POI_DRINKINGWATER,
		LINE_NATIONAL_BORDER,
		
		POI_PARKING, 		//Type from MD
		POI_INFORMATION, 	//Type from MD
		
		LINE_SKIRUN_GREEN,	//Type from MD
		LINE_SKIRUN_BLUE,
		LINE_SKIRUN_BLACK,
		LINE_SKIRUN_DBLACK,
		LINE_SKIRUN_RED,
		LINE_CHAIRLIFT,
		LINE_ROADWAY,
		LINE_WALKWAY,       //Type from MD
		AREA_SKI,           //Type from MD
		LINE_WATERWAY
	}


	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		System.out.println("TileCompressor:");
		
		String srcDirStr = System.getProperty("user.dir") + "/src_tiles";
		String dstDirStr = System.getProperty("user.dir") + "/dst_tiles";
		
		if (args.length > 0) {
			srcDirStr = args[0];
		}
		if (args.length > 1) {
			dstDirStr = args[1];
		}
		System.out.println("Converting xml files in " + srcDirStr);
		System.out.println("      into rgz files at " + dstDirStr);
		
		File dstDir = new File(dstDirStr);
		if (!dstDir.exists()){
			boolean result = dstDir.mkdir();
			if (result)
				System.out.println("folder=" + dstDir +" is created.");
			else
				System.out.println("folder=" + dstDir +" can not be created.");
		}
		File srcDir = new File(srcDirStr);
		File[] listOfFiles = srcDir.listFiles();
		if (listOfFiles == null){
			System.out.println("source folder ( " + srcDirStr + " ) has not file."); 
			return;
		}
	    
		
		Arrays.sort( listOfFiles, new Comparator()
		{
		    public int compare(Object o1, Object o2) {
		    	return ((((File)o1).getName()).compareToIgnoreCase((((File)o2).getName())));
		    }

		});
		
		for (int i = 0; i < listOfFiles.length; i++) {
	    	File inputFilePath = listOfFiles[i];
	    	String filePath = inputFilePath.getAbsolutePath();
	        String extension = filePath.substring( (filePath.lastIndexOf(".") + 1), filePath.length());

	       /* if (i > 2)
	        	continue;*/
	        
	    	if (inputFilePath.isFile() && extension.equalsIgnoreCase("xml")) {
		        String filePrefix = filePath.substring( (filePath.lastIndexOf("/") + 1),  (filePath.lastIndexOf(".")));
	    		ArrayList<OSMDataRecord> records;
	    		ArrayList<OSMDataRecord> testRecords;
				try {
					records = loadTile(inputFilePath);
					//System.out.println("---" + filePrefix + ", read record.size=" + records.size());
					File zippedCmpOutFilePath = new File(dstDirStr + "/" + filePrefix + ".rgz");
		    		int numRecCompressed = compressTile(records, zippedCmpOutFilePath);
		    		
		    		float compressionRatio;
		    		if(zippedCmpOutFilePath.length() > 0) {
		    			compressionRatio = (float)(inputFilePath.length())/(float)(zippedCmpOutFilePath.length());
		    		}
		    		else {
		    			compressionRatio = 1.f;
		    		}
		    		compressionRatio = (int)(compressionRatio *10.f)/10.f;
		    		
		    		if (records.size() != numRecCompressed){
		    			System.out.println(" " + inputFilePath.getName() + " compressed from " + String.format("%6.2f", (float)inputFilePath.length()/1000.f) + "k to " +  String.format("%6.2f", (float)zippedCmpOutFilePath.length()/1000.f) + "k (ratio " + String.format("%4.1f",compressionRatio) + ":1.0) -- written to " + zippedCmpOutFilePath.getName() + " | " + numRecCompressed + " records");
					
		    			System.out.println(" ERROR, input_records=" +  records.size() +", output_records=" + numRecCompressed);
		    		}
					
		    		testRecords = uncompressTile(zippedCmpOutFilePath);
					for(OSMDataRecord record : testRecords) {
						//System.out.println("test: " + record.mOSMType + ", " + record.mName);
					}
				} 
				catch (XmlPullParserException e) {
					e.printStackTrace();
					System.out.println("Error compressing tile " + inputFilePath.getName() + ": " + e.getMessage());
					
				} 
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
	    	}
	    }
		System.out.println("finished. " + listOfFiles.length + " files processed.");
	}
	
     private static int compressTile(ArrayList<OSMDataRecord> records, File filePath) throws IOException {		// data out (encoding)

		// pack object - first calc required buffer size to hold all record data
		int numBytesNeededForRecords = 2;	// start with short for number of records
		for(OSMDataRecord record : records) {
			numBytesNeededForRecords += 1 + record.PackingSize();		// +1 for object type identifier
		}

		// create buffer
		ByteBuffer packedTileData = ByteBuffer.allocate(numBytesNeededForRecords);
		byte[] packedTileDataArray = new byte[numBytesNeededForRecords];
		packedTileData.wrap(packedTileDataArray);
		
		// pack record data into buffer
		packedTileData.putShort((short)records.size());	// first record how many records are present
		int recCnt = 0;
		for(OSMDataRecord record : records) {
			recCnt++;
			packedTileData.put((byte) record.mOSMType.ordinal());			// add object type identifier 
			record.PackIntoBuf(packedTileData);
		}
		
		FileOutputStream output = new FileOutputStream(filePath);
//		output.write(packedTileData.array());
	
		GZIPOutputStream zipoutput = new GZIPOutputStream(new BufferedOutputStream(output));
		zipoutput.write(packedTileData.array());
		zipoutput.close();
		
		return recCnt;
   }

     private static ArrayList<OSMDataRecord> uncompressTile(File cmpFilePath) throws IOException {		// data out (encoding)
     	ArrayList<OSMDataRecord> records = new ArrayList<OSMDataRecord>();

        int data = 0;
     	byte[] fileData = null;
     	FileInputStream inputFile = new FileInputStream(cmpFilePath);
     	GZIPInputStream zipStream = new GZIPInputStream(inputFile);  // casting fileinputstream into ZipInputStream
     	
        ByteArrayOutputStream baOutput = new ByteArrayOutputStream();
        while( ( data = zipStream.read() ) != - 1 )   {
        	baOutput.write( data );
        }
        fileData = baOutput.toByteArray();
        baOutput.close();

     	zipStream.close(); 
     	inputFile.close();
     	
    	
		ByteBuffer packedTileData = ByteBuffer.wrap(fileData);
		int numRecords = (int)packedTileData.getShort();	// get number of records in this compressed file
		
		for(int ii = 0; ii < numRecords; ii++) {		// TODO this will currently blow up if there were any exceptions thrown (bad data) during tile compression
			int nextTypeOrdinal = (int)packedTileData.get();
			TileCompressor.OSMBaseDataTypes objBaseType = TileCompressor.OSMBaseDataTypes.values()[nextTypeOrdinal];// get new object type identifier
			//System.out.println("Uncompressing record #" + (ii+1) + " of " + numRecords + " (" + packedTileData.position() + " of "+ packedTileData.capacity() + "):  type=" + objBaseType );

			OSMDataRecord currentRecord = null;
			if(objBaseType != null) {
				// create new record with empty point data
				switch(objBaseType) {
					case POI_RESTAURANT:
//					case POI_STORE:
//					case POI_HOSPITAL: 
					case POI_WASHROOM: 
					case POI_DRINKINGWATER: 
					case POI_PARKING:
					case POI_INFORMATION: 
					{
						currentRecord = new OSMPOIRecord(objBaseType, "", new PointXY(0.f,0.f), false);
						if(currentRecord != null) {
							currentRecord.UnpackFromBuf(packedTileData);
							records.add(currentRecord);
						}
						break;
					}
					case HIGHWAY_MOTORWAY: 
					case HIGHWAY_PRIMARY: 
					case HIGHWAY_SECONDARY: 
					case HIGHWAY_TERTIARY: 
					case HIGHWAY_RESIDENTIAL: 
					case LINE_NATIONAL_BORDER: 
					
					case LINE_SKIRUN_GREEN:
					case LINE_SKIRUN_BLUE:
					case LINE_SKIRUN_BLACK:
					case LINE_SKIRUN_DBLACK:
					case LINE_SKIRUN_RED:
					case LINE_CHAIRLIFT:
					case LINE_ROADWAY:
					case LINE_WALKWAY:
					case LINE_WATERWAY:
					{
						OSMTrailRecord currentTrailRecord = new OSMTrailRecord(objBaseType, "", new ArrayList<PointXY>(), 0, false, false);
						if(currentTrailRecord != null) {
							currentTrailRecord.UnpackFromBuf(packedTileData);
							records.add(currentTrailRecord);
						}
						break;
					}
					case AREA_LAND: 
					case AREA_OCEAN: 
					case AREA_CITYTOWN: 
					case AREA_WOODS: 
					case AREA_WATER: 
					case AREA_FIELD: 
					case AREA_PARK: 
					case AREA_SKI:
					{
						OSMAreaRecord currentAreaRecord = new OSMAreaRecord(objBaseType, "", new ArrayList<PointXY>(), false);
						if(currentAreaRecord != null) {
							currentAreaRecord.UnpackFromBuf(packedTileData);
							records.add(currentAreaRecord);
						}
						break;
					}
				}
			}

		}

     	return records;
 	}
 	
	
    private static ArrayList<OSMDataRecord> loadTile(File filepath) throws XmlPullParserException {
		
		ArrayList<OSMDataRecord> records = new ArrayList<OSMDataRecord>();
		
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();

		records.clear();
		boolean foundPointInTileRegion = false;
			
		int recordCount = 0;
		try {
			InputStream is;
			BufferedReader br;

			br = new BufferedReader(new FileReader(filepath));
			Date startTime = new Date();

		    // auto-detect the encoding from the stream
			parser.setInput(br);

			recordCount = 0;
			boolean profileFound = false;
			boolean done = false;
			int eventType = parser.getEventType();   // get and process event
			OSMDataRecord currentRecord = null;
			
			int tileID = 0;
			
			while (eventType != XmlPullParser.END_DOCUMENT && !done){
				String name = null;

				name = parser.getName();
				if(name == null) name = "null";
//				Log.e(TAG, "eventType:"+eventType + "-"+ name);

				switch (eventType){
					case XmlPullParser.START_DOCUMENT: {
						name = parser.getName();
						break;
					}
	
					case XmlPullParser.END_TAG: {
						name = parser.getName();
						if(name.equalsIgnoreCase("rmo")){ 
							currentRecord = null;
							if(!foundPointInTileRegion && currentRecord != null) {
								foundPointInTileRegion = false;
							}
						}
						break;
					}

					case XmlPullParser.START_TAG: { 
						name = parser.getName();
						if(name != null) {
							if(name.equalsIgnoreCase("rim")){ 
								String tileIDStr = parser.getAttributeValue(0);
								tileID = Integer.parseInt(tileIDStr);
								String ver = parser.getAttributeValue(1);
							} 
							if(name.equalsIgnoreCase("rmo")){ 
								String curObjName = "not defined";
								String curObjAtt = "not defined";
								String curObjRoadType = "not defined";
								int numAttributes = parser.getAttributeCount();

								String curObjID = parser.getAttributeValue(0);
								String curObjType = parser.getAttributeValue(1);
								String attrName;
								
								if(numAttributes > 2) {
									attrName = parser.getAttributeName(2);
									if(attrName.equalsIgnoreCase("name")) {
										curObjName = parser.getAttributeValue(2);
									}
									if(attrName.equalsIgnoreCase("highway")) {
										curObjRoadType = parser.getAttributeValue(2);
									}
								}
								if(numAttributes > 3) {
									attrName = parser.getAttributeName(3);
									if(attrName.equalsIgnoreCase("name")) {
										curObjName = parser.getAttributeValue(3);
									}
									if(attrName.equalsIgnoreCase("highway")) {
										curObjRoadType = parser.getAttributeValue(3);
									}
								}
								 
								OSMBaseDataTypes objBaseType = null;
								if(curObjType.equalsIgnoreCase("highway-residential road"))	objBaseType = OSMBaseDataTypes.HIGHWAY_RESIDENTIAL;
								if(curObjType.equalsIgnoreCase("highway-motorway"))			objBaseType = OSMBaseDataTypes.HIGHWAY_MOTORWAY;
								if(curObjType.equalsIgnoreCase("highway-primary"))			objBaseType = OSMBaseDataTypes.HIGHWAY_PRIMARY;
								if(curObjType.equalsIgnoreCase("highway-secondary")) 		objBaseType = OSMBaseDataTypes.HIGHWAY_SECONDARY;
								if(curObjType.equalsIgnoreCase("highway-tertiary"))			objBaseType = OSMBaseDataTypes.HIGHWAY_TERTIARY;
								if(curObjType.equalsIgnoreCase("nationalborder"))			objBaseType = OSMBaseDataTypes.LINE_NATIONAL_BORDER;
								
								if(curObjType.equalsIgnoreCase("land"))	 			objBaseType = OSMBaseDataTypes.AREA_LAND;
								if(curObjType.equalsIgnoreCase("ocean"))	 		objBaseType = OSMBaseDataTypes.AREA_OCEAN;
								if(curObjType.equalsIgnoreCase("citytown"))	 		objBaseType = OSMBaseDataTypes.AREA_CITYTOWN;
								if(curObjType.equalsIgnoreCase("wood"))	 			objBaseType = OSMBaseDataTypes.AREA_WOODS;
								if(curObjType.equalsIgnoreCase("water"))	 		objBaseType = OSMBaseDataTypes.AREA_WATER;
								if(curObjType.equalsIgnoreCase("field"))	 		objBaseType = OSMBaseDataTypes.AREA_FIELD;
								if(curObjType.equalsIgnoreCase("park"))	 			objBaseType = OSMBaseDataTypes.AREA_PARK;
								
								if(curObjType.equalsIgnoreCase("washroom"))	        objBaseType = OSMBaseDataTypes.POI_WASHROOM;
								if(curObjType.equalsIgnoreCase("drinkingwater"))	objBaseType = OSMBaseDataTypes.POI_DRINKINGWATER;
								
								
								if(curObjType.equalsIgnoreCase("restaurant"))	objBaseType = OSMBaseDataTypes.POI_RESTAURANT;
								if(curObjType.equalsIgnoreCase("park"))	objBaseType = OSMBaseDataTypes.POI_PARKING;
								if(curObjType.equalsIgnoreCase("info"))	objBaseType = OSMBaseDataTypes.POI_INFORMATION;
								

								if(curObjType.equalsIgnoreCase("skiTrail-Green"))	objBaseType = OSMBaseDataTypes.LINE_SKIRUN_GREEN;
								if(curObjType.equalsIgnoreCase("skiTrail-Blue"))	objBaseType = OSMBaseDataTypes.LINE_SKIRUN_BLUE;
								if(curObjType.equalsIgnoreCase("skiTrail-Black"))	objBaseType = OSMBaseDataTypes.LINE_SKIRUN_BLACK;
								if(curObjType.equalsIgnoreCase("skiTrail-DBlack"))	objBaseType = OSMBaseDataTypes.LINE_SKIRUN_DBLACK;
								if(curObjType.equalsIgnoreCase("skiTrial-Red"))	objBaseType = OSMBaseDataTypes.LINE_SKIRUN_RED;
								if(curObjType.equalsIgnoreCase("chairLift"))	objBaseType = OSMBaseDataTypes.LINE_CHAIRLIFT;
								if(curObjType.equalsIgnoreCase("road"))	objBaseType = OSMBaseDataTypes.LINE_ROADWAY;
								if(curObjType.equalsIgnoreCase("walk"))	objBaseType = OSMBaseDataTypes.LINE_WALKWAY;
								if(curObjType.equalsIgnoreCase("skiResort")) objBaseType = OSMBaseDataTypes.AREA_SKI;
								if(curObjType.equalsIgnoreCase("waterway"))	objBaseType = OSMBaseDataTypes.LINE_WATERWAY;
								
								
								
								if(objBaseType != null) {
									recordCount ++;
									// create new record with empty point data
									currentRecord = null;
									switch(objBaseType) {
										case POI_RESTAURANT:
										case POI_STORE: 
										case POI_HOSPITAL: 
										case POI_WASHROOM: 
										case POI_DRINKINGWATER: 
										case POI_PARKING:
										case POI_INFORMATION:
										{
											currentRecord = new OSMPOIRecord(objBaseType, curObjName, new PointXY(0.f,0.f), false);
											break;
										}
										case HIGHWAY_MOTORWAY: 
										case HIGHWAY_PRIMARY: 
										case HIGHWAY_SECONDARY: 
										case HIGHWAY_TERTIARY: 
										case HIGHWAY_RESIDENTIAL: 
										case LINE_NATIONAL_BORDER: 
										
										case LINE_SKIRUN_GREEN:
										case LINE_SKIRUN_BLUE:
										case LINE_SKIRUN_BLACK:
										case LINE_SKIRUN_DBLACK:
										case LINE_SKIRUN_RED:
										case LINE_CHAIRLIFT:
										case LINE_ROADWAY:
										case LINE_WALKWAY:
										case LINE_WATERWAY:
										{
											currentRecord = new OSMTrailRecord(objBaseType, curObjName, new ArrayList<PointXY>(), 0, false, false);
											break;
										}
										case AREA_LAND: 
										case AREA_OCEAN: 
										case AREA_CITYTOWN: 
										case AREA_WOODS: 
										case AREA_WATER: 
										case AREA_FIELD: 
										case AREA_PARK: 
										case AREA_SKI:
										{
											currentRecord = new OSMAreaRecord(objBaseType, curObjName, new ArrayList<PointXY>(), false);
											break;
										}
									}
									if(currentRecord != null) {
										records.add(currentRecord);
									}
									
									foundPointInTileRegion = false;
								}
							} 

							if(name.equalsIgnoreCase("point")){ 
								if(currentRecord != null) {
									String latString = parser.getAttributeValue(0);
									String longString = parser.getAttributeValue(1);

									double latitude = Double.valueOf(latString);
									double longitude = Double.valueOf(longString);
									
									Integer objTileIndex =	GetTileIndex(longitude, latitude); 
									if(objTileIndex == tileID) {
										foundPointInTileRegion = true;	// for error reporting

									}
									
									if(latitude >= -90. && latitude <= 90. && longitude >= -180. && longitude <= 180.) {
										if(currentRecord instanceof OSMPOIRecord) {
											((OSMPOIRecord)currentRecord).SetLocation((float)longitude, (float)latitude);
										}
										else {
											if(currentRecord instanceof OSMAreaRecord) {
												((OSMAreaRecord)currentRecord).AddPoint((float)longitude, (float)latitude);
											}
											else {
												((OSMTrailRecord)currentRecord).AddPoint((float)longitude, (float)latitude);
											}
										}
									}
								}
							} 

						}
					}
				} // end switch
			    eventType = parser.next();   // get and process event

			}	// while end
//			Log.i(TAG, "Loaded " + recordCount + " records from tile " + tileIndex);
			Date endTime = new Date();
	
		}
		catch (Exception e) {
		}

		return records;
	}
    
    //=========================
    // needs to match code in GeoTile - TODO pull this out of GeoTile into separte class and link here
    
	private final static float EQUITORIALCIRCUMFRENCE = 40075017.0f;	// taken from wikipedia/Earth
	private final static float MERIDIONALCIRCUMFRENCE = 40007860.0f;	// taken from wikipedia/Earth
//	private final static double TILE_HEIGHT_IN_DEGREES = 0.05;	// in degree latitude
//	private final static double TILE_HEIGHT_IN_METERS = 5556.6;  // calculated from TILE_HEIGHT_IN_DEGREES/360. * MERIDIONALCIRCUMFRENCE - defined as a const to avoid computational differences with Tile construction code 
//	private final static double TILE_WIDTH_IN_METERS = 5556.6;	// in meters as tiles are square in distance
//	private final static double TILE_WIDTH_IN_DEGREES_AT_EQUATOR = 0.04991621;  // calculated from TILE_WIDTH_IN_METERS/EQUITORIALCIRCUMFRENCE * 360.;	// in degree longitude
	private final static double TILE_HEIGHT_IN_DEGREES = 0.025;	// in degree latitude
	private final static double TILE_HEIGHT_IN_METERS = 2778.3;  // calculated from TILE_HEIGHT_IN_DEGREES/360. * MERIDIONALCIRCUMFRENCE - defined as a const to avoid computational differences with Tile construction code 
	private final static double TILE_WIDTH_IN_METERS = 2778.3;	// in meters as tiles are square in distance
	private final static double TILE_WIDTH_IN_DEGREES_AT_EQUATOR = 0.024958105;  // calculated from TILE_WIDTH_IN_METERS/EQUITORIALCIRCUMFRENCE * 360.;	// in degree longitude
	private final static int NUMBER_TILES_PER_HEMISPHERE = (int) (90.0/TILE_HEIGHT_IN_DEGREES + 1e-10);  

    
	public static int FloorWithJavaFix(double value) {	// *sometimes* JAVA does not compute division results properly and changes values like 5.0 to 4.999999999 (annoying)
		return (int) Math.floor(value + 1e-10);				// this dramatically effects boundary calculation, so this function was written to address that
	}
	
	public static Integer GetTileIndex(double longitude, double latitude) { // x=long, y=lat
		int longIndex;
		int latIndex;
		double dlat = TILE_HEIGHT_IN_DEGREES;

//		if(latitude == 90.0)  latitude -= 0.00001;	// 
		double latRatio = latitude/dlat;		// can have JAVA rounding issue with Flooring
//		Log.d(TAG, "latitude:" + latitude + ", " + "dlat: " + dlat + " -> LatRatio: " + latRatio);
		int baseLatIndex = FloorWithJavaFix(latRatio);
		double dlong = TILE_WIDTH_IN_DEGREES_AT_EQUATOR / Math.cos(Math.toRadians(baseLatIndex*dlat)) ;
		latIndex = baseLatIndex + NUMBER_TILES_PER_HEMISPHERE;
		
//		Log.d(TAG, "tile width at baseLat:" + baseLatIndex + ", " + "baseLat: " + (baseLatIndex*dlat) + ", " + dlong);
		double positiveLongitude = (longitude+360.f) % 360.f;
		longIndex = FloorWithJavaFix(positiveLongitude/dlong);

		return CombineTileSubIndices(latIndex, longIndex );
	}

	public static Integer CombineTileSubIndices(int latIndex, int longIndex) {
		return latIndex * 10000 + longIndex;
	}

}
