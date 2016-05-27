/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.R.integer;
import android.content.Context;
import android.graphics.PointF;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.navigation.R;

/**
 * This class manages custom-defined point-of-interest such as:
 * buddy, device owner, cdp(custom-defined-poi)
 */

public class CustomPoiManager
{	
	static final String LOG_TAG = "CustomPoiManager";
	static final int DEVICE_OWNER_POI_ID = -1;
	static final String CDP_FILE_EXT = ".cdp";
	static final String CDP_XML_ELEMENT_NAME ="CustomPoi";
	static final String CDPS_XML_ROOT_ELEMENT="ReconCustomPois";
	static final String CDP_ATTR_LAT = "lat";
	static final String CDP_ATTR_LNG = "lng";
	static final String CDP_ATTR_NAME = "name";
	static final String CDP_ATTR_ID = "id";
	static final String CDP_ATTR_TYPE = "type";
	static final String CDP_ATTR_CREATEDON = "createdOn";

	public class CustomPoi
	{
		public String mName=null;			//the name 
		public long mId;					//the id
		public double mLatitude=0;			//the latitude
		public double mLongitude=0;			//the longitude 
		public PoInterest mPoi;				//the rendering object submit to the map if necessary
		public int  mPoiType;				//the custom defined poi type: PoInterest.POI_TYPE_BUDDY, PoInterest.POI_TYPE_OWNER, or PoInterest.POI_TYPE_CDP
		public Date mCreatedOn;				//the time that the CustomPoi is created on
		
		public CustomPoi( double lat, double lng, String name, long id, int cPoiType )
		{
			mName = name;
			mId = id;
			mPoiType = cPoiType;
			mLatitude = lat;
			mLongitude = lng;
			PointF location = Util.mapLatLngToLocal(lat, lng);
			mPoi = new PoInterest( location.x, location.y, name, cPoiType );
			mCreatedOn = new Date();
			
		}
		
		public CustomPoi( double lat, double lng, String name, long id, int cPoiType, Date createdOn )
		{
			mName = name;
			mId = id;
			mPoiType = cPoiType;
			mLatitude = lat;
			mLongitude = lng;
			PointF location = Util.mapLatLngToLocal(lat, lng);
			mPoi = new PoInterest( location.x, location.y, name, cPoiType );
			mCreatedOn = createdOn;
			
		}
		
		/**
		 * Construct a CustomPoi from an xmlNode
		 */
		public CustomPoi( Node xmlNode )
		{
			inflate( xmlNode );
		}
		
		public void updateLocation( double lat, double lng )
		{
			mLatitude = lat;
			mLongitude = lng;
			PointF location = Util.mapLatLngToLocal(lat, lng);
			mPoi.mPosition.set(location.x, location.y);
		}
		
		/**
		 * 
		 * Convert the CustomPoi to an Xml representation
		 */
		public String toXml( )
		{
			String xml = String.format("<%s id=\"%d\" name=\"%s\" type=\"%d\" createdOn=\"%s\" lat=\"%f\" lng=\"%f\" />\n", CDP_XML_ELEMENT_NAME, mId, mName, mPoiType, mCreatedOn.toString(), mLatitude, mLongitude);
			return xml;	
		}
		
		/**
		 * Inflate the CustomPoi from an Xml Node
		 */
		public void inflate( Node xmlNode )
		{
       		if( xmlNode.getNodeType() == Node.ELEMENT_NODE && xmlNode.getNodeName().compareToIgnoreCase(CDP_XML_ELEMENT_NAME) == 0 )
    		{
        		NamedNodeMap attrs = xmlNode.getAttributes();
        		
        		//id
        		Node idAttr = attrs.getNamedItem(CDP_ATTR_ID);
        		mId = Long.parseLong( idAttr.getNodeValue() );
        		
    			//name
    			Node nameAttr = attrs.getNamedItem(CDP_ATTR_NAME);
    			mName =  nameAttr.getNodeValue();
    			
    			//lat
    			Node latAttr = attrs.getNamedItem(CDP_ATTR_LAT);
    			mLatitude =  Double.parseDouble( latAttr.getNodeValue() );

    			//lng
    			Node lngAttr = attrs.getNamedItem(CDP_ATTR_LNG);
    			mLongitude =  Double.parseDouble( lngAttr.getNodeValue() );
    			
    			//type
    			Node typeAttr = attrs.getNamedItem(CDP_ATTR_TYPE);
    			mPoiType =  Integer.parseInt( typeAttr.getNodeValue() );
    			
    			PointF location = Util.mapLatLngToLocal(mLatitude, mLongitude);
    			mPoi = new PoInterest( location.x, location.y, mName, mPoiType );
    			
    			Node timeAttr = attrs.getNamedItem( CDP_ATTR_CREATEDON );
    			if( timeAttr != null )
    			{
    				try
    				{
    					mCreatedOn = DateFormat.getTimeInstance().parse( timeAttr.getNodeValue() );
    				}
    				catch( ParseException e )
    				{
    					mCreatedOn = new Date();
    				}
    			}

    		}
		}
		
	};
	
	private MapView mMapView = null;
	private ShpMap mMap = null;			
	private ArrayList<CustomPoi> mPois = null;					//all other custom pois such as buddy, owner etc.
	private ArrayList<CustomPoi> mCdpPois = null;				//all CDP pois
	private Context mContext = null;
	
	/**
	 * Construct a buddy manager, pass the ShpMap
	 * to it as a a rendering target. A buddy might
	 * be submit to or removed from the ShpMap when
	 * its location changed 
	 */
	public CustomPoiManager( Context context, ShpMap map, MapView mapView )
	{
		mMap = map;
		mPois = new ArrayList<CustomPoi>( 64 );
		mCdpPois = new ArrayList<CustomPoi>( 64 );
		mContext = context;
		mMapView = mapView;
	}
	
	protected static String GetParcelFileName(int resortID)
	{
		return "cdp_" + Integer.toString(resortID) + CDP_FILE_EXT;
	}	
		
	/**
	 * update the location of a custom poi
	 */
	public void updatePoiLocation( long id, int poiType, double lat, double lng )
	{
		CustomPoi poi = findPoi( id, poiType );
		if( poi != null )
		{
			poi.updateLocation(lat, lng);
			if( mMap.isEmpty() == false )
			{
				boolean showInMap =   mMap.mPoInterests.get(poi.mPoiType).contains(poi.mPoi);
				boolean insideMap = mMap.mBBox.contains(poi.mPoi.mPosition.x, poi.mPoi.mPosition.y);
				
				//custom poi is inside the active resort area
				if( insideMap )
				{
					if( showInMap == false )
					{
						//added the poi to the map for rendering						
						mMap.addPOI(poi.mPoi);						
						Toast.makeText(mContext.getApplicationContext(), poi.mName + " " + mContext.getResources().getString(R.string.enter_into_resort) + " " + mMap.mResortName + " ...", Toast.LENGTH_SHORT);
					}
				}
				else
				{
					//the custom poi is being rendered in the map
					//fire a toast and remove it from the map
					if( showInMap )
					{
						mMap.removePOI(poi.mPoi);
						
						Toast.makeText(mContext.getApplicationContext(), poi.mName + " " + mContext.getResources().getString(R.string.leave_resort) + " " + mMap.mResortName + " ...", Toast.LENGTH_SHORT);
					}
				}
				
			}
		}
		else
		{
			Log.e(CustomPoiManager.LOG_TAG, "Try to update a custom poi that has not been created yet!" );
		}
	}

	/**
	 * search for a buddy by its id. Return null if not existed
	 */
	public CustomPoi findPoi( long id, int poiType )
	{
		ArrayList<CustomPoi> pois;
		
		if( poiType == PoInterest.POI_TYPE_CDP )
		{
			pois = mCdpPois;
		}
		else
		{
			pois = mPois;
		}
		for( CustomPoi poi : pois )
		{
			if( poi.mId == id && poi.mPoiType == poiType )
			{
				return poi;
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * Added a custom defined poi to the CustomPoiManager
	 */
	public void addPoi( CustomPoi poi )
	{
		CustomPoi existed = findPoi( poi.mId, poi.mPoiType );
		
		if( existed == null )
		{
			if( poi.mPoiType == PoInterest.POI_TYPE_CDP )
			{
				mCdpPois.add( poi );
			}
			else
			{
				mPois.add( poi );
			}
		}
		else
		{
			Log.e(CustomPoiManager.LOG_TAG, "Custom POI " + poi.mName + " has alreaduy been added to the manager");
		}
	}
	

	/**
	 * Remove a custom defined poi from CustomPoiManager
	 */
	public boolean removePoi( CustomPoi poi )
	{
		ArrayList<CustomPoi> pois = null;
		if( poi.mPoiType == PoInterest.POI_TYPE_CDP )
		{
			pois = mCdpPois;
		}
		else
		{
			pois = mPois;
		}
		
		int idx = -1;
		for( CustomPoi test : pois )
		{
			++idx;
			if( test == poi  )
			{
				pois.remove(idx);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Remove a custom defined poi from CustomPoiManager
	 */
	public boolean removePoi( PoInterest poi )
	{
		ArrayList<CustomPoi> pois = null;
		if( poi.getType() == PoInterest.POI_TYPE_CDP )
		{
			pois = mCdpPois;
		}
		else
		{
			pois = mPois;
		}
		
		int idx = -1;
		for( CustomPoi test : pois )
		{
			++idx;
			if( test.mPoi == poi  )
			{
				pois.remove(idx);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * One or more custom poi has updated. Do
	 * whatever need to be done
	 */
	public void postCustomPoiUpdated( )
	{
		if( mMapView != null )
		{
			//refresh the mapview
			mMapView.invalidate();
		}
	}
	
	
	/**
	 * Debug utiltiy function for adding testing buddy to the CustomPoiManager
	 * 
	 */
	public void addDebugBuddies()
	{
		if( mMap.isEmpty() )
			return;
		
		for( int i = 0; i < 5; ++i )
		{
			CustomPoi poi = new CustomPoi( 0.f, 0.f, "Buddy"+i, i, PoInterest.POI_TYPE_BUDDY );
			this.addPoi(poi);
		}
		
		//update the position
		for( int i = 0; i < 5; ++i )
		{
			float width = mMap.mBBox.right - mMap.mBBox.left;
			float height = mMap.mBBox.bottom - mMap.mBBox.top;
			
			float y = mMap.mBBox.top + (float)(height*Math.random());
			float x = mMap.mBBox.left + (float)(width*Math.random());
			
			PointF localPos = new PointF( x, y );
			Util.mapLocalToLatLng(localPos);
			this.updatePoiLocation(i, PoInterest.POI_TYPE_BUDDY, localPos.y, localPos.x);
		}
	}
	
	public String GetAvailablePinName(String baseName)
	{
		int largestID = 0;
		String retName = baseName;
		for( CustomPoi poi : mCdpPois )
		{
			if( poi.mPoiType == PoInterest.POI_TYPE_CDP )
			{
				if(poi.mName.length() > baseName.length())
				{
					if(poi.mName.startsWith(baseName)) // Beginning
					{
						try
						{
							int id = Integer.parseInt(poi.mName.substring(baseName.length()));
							if(id > largestID)
							{
								largestID = id;
							}
						}
						catch (NumberFormatException e) {}
					}
				}
			}
		}
		
		return baseName + (largestID+1);
	}
	
	/**
	 * Load Custom-defined-Poi for a specific resort site
	 */
	public void loadCDPs(Context context, int resortId )
	{
		String fileName = GetParcelFileName(resortId);		
	
		File file = new File(context.getFilesDir(),  fileName );
		
		//make sure that the file exists and is readable
		if( file.exists() && file.canRead() )
		{
			try
			{
				FileInputStream inputStream = context.openFileInput(fileName);
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder( );
				Document doc = docBuilder.parse( inputStream );
				
		        NodeList nodes = doc.getElementsByTagName( CDPS_XML_ROOT_ELEMENT);
		        Node rootNode = nodes.item(0);
		        
		        Node cdpNode = rootNode.getFirstChild();
		        
		        while( cdpNode != null )
		        {
		        	//only processing on CDP nodes
		        	if( cdpNode.getNodeType() == Node.ELEMENT_NODE && cdpNode.getNodeName().compareToIgnoreCase( CDP_XML_ELEMENT_NAME )==0)
		        	{		        		
	        			CustomPoi cPoi = new CustomPoi( cdpNode );
	        			
	        			addPoi( cPoi );
	        			mMap.addPOI( cPoi.mPoi );	        			
	        		}
        		
		        	cdpNode = cdpNode.getNextSibling();
		        }

		        //close the file input stream
		        inputStream.close();
		        
			}
			catch(FileNotFoundException e)
			{
				//do nothing here, since we`ve already checked if the file exists or not
			}
			catch(ParserConfigurationException e)
			{
				e.printStackTrace();
			}
			catch(SAXException e)
			{
				//parsing errors for the CDP
				e.printStackTrace();
				Log.e( LOG_TAG, "Errors in paring the " + fileName + " for loding custom-defined-poi" );
			}
			catch( IOException e)
			{
				e.printStackTrace();
			}
		}	
	}
	
	/**
	 * Reset the CustomPoiManager:
	 * todo: remove all Cdp poi from the list
	 */
	public void reset( )
	{
		mCdpPois.clear();
	}
	
	/**
	 * Utility function
	 * Serialize all CDP's to files that stored at EXTERNAL_STORAGE/mapdata/
	 */
	public void saveCDPs(Context context,  int resortId )
	{
		String fileName = GetParcelFileName(resortId);
		
		File file = new File( fileName );
		
		//if there is no CDP, skip the saving
		if( mCdpPois == null )
		{
			return;
		}
		
		try
		{
			FileOutputStream outputStream = context.openFileOutput( fileName, Context.MODE_PRIVATE);
			
			//root element
			String str = String.format("<%s>\n", CDPS_XML_ROOT_ELEMENT);			
			outputStream.write(str.getBytes());
			
			//individual CPD elements
			for( CustomPoi poi : mCdpPois )
			{
				if( poi.mPoiType == PoInterest.POI_TYPE_CDP )
				{
					str = poi.toXml();
					outputStream.write(str.getBytes());		
				}
			}
			
			//close the root element
			str = String.format("</%s>", CDPS_XML_ROOT_ELEMENT );
			outputStream.write(str.getBytes());
			
			//now all CDP is serialized, let's close the file
			outputStream.close();			
			
		}
		catch( IOException e)
		{
			e.printStackTrace();
		}	
	}

	/**
	 * Generate an unique ID for a custom POI
	 * This function call java.util.UUID to 
	 * create a UUID first then get the timeStamp from the UUID
	 * as a long for the ID
	 */
	static public long generateID()
	{
		UUID newId = UUID.randomUUID();
		return newId.getMostSignificantBits();
	}
}
