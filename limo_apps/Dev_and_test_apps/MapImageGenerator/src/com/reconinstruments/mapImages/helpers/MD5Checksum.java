package com.reconinstruments.mapImages.helpers;

import java.io.*;
import java.security.MessageDigest;

public class MD5Checksum {

   public static byte[] CreateChecksum(InputStream fileInputStream) throws Exception {
       byte[] buffer = new byte[1024];
       MessageDigest complete = MessageDigest.getInstance("MD5");
       int numRead;

       do {
           numRead = fileInputStream.read(buffer);
           if (numRead > 0) {
               complete.update(buffer, 0, numRead);
           }
       } while (numRead != -1);

       fileInputStream.close();
       return complete.digest();
   }
   
   public static String GetMD5Checksum(String filename) throws Exception {
	   return GetMD5Checksum(new FileInputStream(filename));
   }

   // see this How-to for a faster way to convert
   // a byte array to a HEX string
   public static String GetMD5Checksum(InputStream fileInputStream) throws Exception {
       byte[] b = CreateChecksum(fileInputStream);
       String result = "";

       for (int i=0; i < b.length; i++) {
           result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
       }
       return result;
   }
}