package com.reconinstruments.mapImages.dal;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.util.Log;

import com.reconinstruments.mapImages.helpers.DataDecoder;

public class ZippedRecordDAL {
	protected static final String TAG = "ZippedRecordDAL";
	
	DataDecoder mDecoder = null;
	
	public ZippedRecordDAL() {
		mDecoder = new DataDecoder();
	}
	
	public ByteBuffer CreateByteBuffer(ZipFile zippedRecords, String zippedAssetFileName, String alternativZippedAssetFileName) throws Exception {
		ZipEntry zippedAsset = zippedRecords.getEntry(zippedAssetFileName);
		if(zippedAsset == null) {
			if(alternativZippedAssetFileName != null) {
				zippedAsset = zippedRecords.getEntry(alternativZippedAssetFileName);
			}
			
			if(zippedAsset == null) {
				Log.e(TAG,"Bad File - "+zippedAssetFileName);
				throw new IllegalArgumentException("zippedAsset "+zippedAssetFileName+" is null");
			}
		}
		
		InputStream inputStream = zippedRecords.getInputStream(zippedAsset);
		int len = (int)zippedAsset.getSize();
		byte[] content = new byte[len];
		int offset = 0;
		while( len > 0 )
		{
			int actualRead = inputStream.read( content, offset, len );
			offset += actualRead;
			len -= actualRead;
		}
		
		byte[] decodedContent = mDecoder.Decode(content);
		ByteBuffer outputByteBuffer = ByteBuffer.wrap( decodedContent );
		inputStream.close();
		
		return outputByteBuffer;
	}
}
