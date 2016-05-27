package com.reconinstruments.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Log;


/**
 * 
 * <code>FileUtils</code> reads the file from SD Card and converts it as a String object.
 *
 */
public class FileUtils {
    
    public static final String TAG  = "FileUtils";
    
    public static String getStringFromSDCard(File externalDir, String relativePath) throws Exception {
        File fl = new File(externalDir, relativePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        fin.close();        
        return ret;
    }    
    
    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
          sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
	/**
	 * Simple Method for copying file
	 * @param src
	 * @param dst
	 * @throws IOException
	 */
	public static boolean copy(File src, File dst) {
		InputStream in;
		OutputStream out;
		try {
			if (!dst.exists())
				dst.createNewFile();

			in = new FileInputStream(src);
			out = new FileOutputStream(dst);

			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getLocalizedMessage());
			return false;
		} catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage());
			return false;
		}

		return true;
	}
	

    /** 
     * generate an md5 checksum from a byte array 
     * offset allows md5 to be calculated ignoring the beginning of the data
     *
     * @param array
     * @param offset
     * @return md5String
     *
     **/
    public static String md5(byte[] array,int offset) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            //digest.update(array);
            digest.update(array, offset, array.length-offset);
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(String.format("%02x", messageDigest[i]&0xff));
            String md5 = hexString.toString();
            return md5;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /** 
     * generate an md5 checksum from a byte array 
     * offset allows md5 to be calculated ignoring the beginning of the data
     * 
     * @return md5String
     **/
    public static String md5(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            FileInputStream f = new FileInputStream(file);

            byte[] buffer = new byte[8192];
            int len = 0;
            while (-1 != (len = f.read(buffer))) {
                digest.update(buffer,0,len);
            }

            byte[] md5hash = digest.digest();

            //converting byte array to Hexadecimal String 
            StringBuilder sb = new StringBuilder(2 * md5hash.length);
            
            for(byte b : md5hash){
                sb.append(String.format("%02x", b&0xff));
            }
            
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /* Create directory if it doesn't exist, if file exists in it's place delete and make directory 
     * return true if directory already existed or was created */
    public static boolean forceCreateDirectory(File directory) {
        if (!directory.exists()) {
            return directory.mkdirs();
        } 
        else if (!directory.isDirectory()) {
            Log.e(TAG, directory.getPath() + " exists, but is not a directory. Deleting and creating directory");
            if (!directory.delete()) {
                Log.e(TAG, "Could not delete file: "+directory.getPath());
                return false;
            }
            return directory.mkdirs();
        }

        return true;
    }
    
    public static int chmod(File file, String permissions) throws IOException, InterruptedException {
        String[] strExec = new String[] {
                "/system/bin/sh", "-c","/system/bin/chmod "+permissions+" "+file 
        };

        Process p = Runtime.getRuntime().exec(strExec);
        return p.waitFor();
    }

    public static boolean isFileAndExists(File file) {
        return (file!=null && file.isFile() && file.exists());
    }
}
