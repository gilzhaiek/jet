/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

/**
 * @author Hongzhi Wang
 * this class perform the AES decryption
 * of a given buffer of binary data
 */
public class DataDecoder
{
	public static boolean Initialized = false;   
	private static Cipher mCipher = null;
	//a temporary key for encrypt/decrypt map data for field testing only
	static final String ENCRYPT_KEY = "hwang-8309-0310=";
	static final String CIPHER_TRANSFORMATION = "AES";
	static final String DECODER_ERROR_CHANNEL = "Decryption Error";
	
	public class InvalidMapDataException extends java.lang.Exception
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public InvalidMapDataException()
		{
			super();
		}
	}
	
	public DataDecoder()
	{
		Initialized = (mCipher != null);
	}
	
	public void Initialize(){
		if(Initialized)
			return;
		
		//create the encryption cipher first
		byte[] rawKey = getKey().getBytes();
		SecretKeySpec keySpec = new SecretKeySpec( rawKey, CIPHER_TRANSFORMATION );
		
		try
		{
			mCipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			mCipher.init(Cipher.DECRYPT_MODE, keySpec);
		}
		catch( NoSuchAlgorithmException noAlgorithmException )
		{
			Log.e(DECODER_ERROR_CHANNEL, CIPHER_TRANSFORMATION + " is not supported");
			Log.e(DECODER_ERROR_CHANNEL, noAlgorithmException.getMessage());
			
			//has trouble to initialize the cipher, so just set it as NULL;
			mCipher = null;
		}
		catch( NoSuchPaddingException  notSupportedPaddingException )
		{
			Log.e(DECODER_ERROR_CHANNEL,  "not supported padding for " +CIPHER_TRANSFORMATION);
			Log.e(DECODER_ERROR_CHANNEL, notSupportedPaddingException.getMessage());
			
			//has trouble to initialize the cipher, so just set it as NULL;
			mCipher = null;

		}
		catch( InvalidKeyException invalidKeyException )
		{
			Log.e(DECODER_ERROR_CHANNEL, "invalid decryption key");
			Log.e(DECODER_ERROR_CHANNEL, invalidKeyException.getMessage());
			
			//has trouble to initialize the cipher, so just set it as NULL;
			mCipher = null;
		}
		finally
		{
			Initialized = true;
		}
	}
		
	
	private String getKey()
	{
		//TODO: compose a private key from the serializ number of the host device
		return ENCRYPT_KEY;
	}
	
	/** 
	 * Decode the passed-in source content
	 * using the mCipher. If mCipher is null(not supported cipher)
	 * or the source content is not encrypted or encrypted with wrong key
	 * then just return null
	 * @throws Exception 
	 */
	public byte[] Decode( byte[] sourceContent ) throws Exception
	{
		if(!Initialized) {
			Initialize();
		}
		if( mCipher == null )
		{
			throw new Exception ("Couldn't Initialize the Decoder - Wrong Key");
		}
		else
		{
			byte[] decoded = null;
			
			try
			{
				decoded = mCipher.doFinal( sourceContent );
				return decoded;
			}
			catch( IllegalBlockSizeException illegalBlock )
			{
				Log.e( DECODER_ERROR_CHANNEL, "Illegal block size of the source data to be decoded");
				Log.e( DECODER_ERROR_CHANNEL, illegalBlock.getMessage() );
				
				//the source data might be not encoded or encode with the wrong key
				throw new InvalidMapDataException();
			}
			catch( BadPaddingException badPadding )
			{
				Log.e( DECODER_ERROR_CHANNEL, "Bad padding of the source data to be decoded");
				Log.e( DECODER_ERROR_CHANNEL, badPadding.getMessage() );

				//the source data might be not encoded or encode with the wrong key
				throw new InvalidMapDataException();
			}
			catch( IllegalStateException illegalState )
			{
				//should really capture this exception in our case, since the cipher was initialize
				//with the decrypt mode. But lets try to catch it 
				Log.e( DECODER_ERROR_CHANNEL, "Decoder was not initialized with the decoding mode");
				Log.e( DECODER_ERROR_CHANNEL, illegalState.getMessage() );

				//the source data might be not encoded or encoded with the wrong key
				throw new InvalidMapDataException();
				
			}
		}
	}
}