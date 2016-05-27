package android.supl.config;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.io.InputStreamReader;
import android.util.Log;

import java.lang.Object;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * SUPL configuration class.
 * This class contains methods for working with
 * configuration file.
 *
 * @author Alexander Karimov <Alexander.Karimov@teleca.com>
 * @author Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>
 * @version 1.0
 */
public class SuplConfig {
    /**
     * Default location of SUPL configuration file (SuplConfig.spl)
     */
    private static final String CONFIG = "SuplConfig.spl";
    private static final String TAG = "SuplConfig";
    private static final int HASH_SIZE = 30;

    private HashMap<String, String> mConfig;

    /**
     * Reads configuration parameters.
     */
    private void ReadConfig (BufferedReader d) {
        HashMap<String, String> config = new HashMap<String, String>(HASH_SIZE);
        String line = null;
        try {
            int tag_open = 0;
            int tag_close = 0;
            int index = 0;
            line = d.readLine();
            while (line != null) {
			
                tag_open = line.indexOf('<') + 1;
                tag_close = line.indexOf('>');
                index = tag_close + 1;
                if (line.startsWith("#") || index >= line.length()) {
                    line = d.readLine();
                    continue;
                }
                if (tag_open > 0 && tag_close > tag_open + 1) {
                    while (' ' == line.codePointAt(index++) && index < line.length());
                    if (index <= line.length()){
                        config.put(line.substring(tag_open, tag_close),line.substring(--index));
						
                    }
					
                }
				
                line = d.readLine();
            }
        }
        catch (IOException e) {
            Log.e(TAG, "Config file reading failure:" + e.getMessage());
            mConfig = null;
        }
        mConfig = config;
    }

    /**
     * Reads configuration parameters from file.
     * @param Path path to configuration file
     */
    public void readConfigPath (String Path) {
        //DataInputStream in = null;
        BufferedReader d = null;
        if (Path == null) {
            Path = CONFIG;
        }
        try {
            d = new BufferedReader(
                    new InputStreamReader(new FileInputStream(Path)));
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found" + e.getMessage());
            mConfig = null;
        }
        ReadConfig(d);
    }

    /**
     * Prints configuration parameters from HashMap to Android's log stream.
     * @param h HashMap that contains configuration parameters
     */
    public void PrintConfig () {
        HashMap<String, String> h = mConfig;
        if (h != null ) {
            Set<String> Keys = h.keySet();
            String Tag = null;
            Iterator<String> it  = Keys.iterator();
            while (it.hasNext()) {
                Tag = it.next();
                Log.d(TAG, "TAG - " + Tag + ", VAL - " + h.get(Tag));
            }
        }
    }

    /**
     * Reads configuration parameters from InputStream.
     * @param in InputStream
     */
    public void readConfigStream (InputStream in) {
        if (in == null) {
            mConfig = null;
            return;
        }
        ReadConfig(new BufferedReader(new InputStreamReader(in)));
    }

    /**
     * Returns SSL storage path.
     */
    public String getStorePath () {
        if (mConfig != null) {
            if (mConfig.get("StorePath") != null) {
                return (String) mConfig.get("StorePath");
            }
        }
        return null;
    }

    /**
     * Returns Auto_FQDN certificate path.
     */
    public String getAutoFqdnStorePath () {
        if (mConfig != null) {
            if (mConfig.get("AutoFqdnStorePath") != null) {
                return (String) mConfig.get("AutoFqdnStorePath");
            }
        }

        return null;
    }
    /**
     * Returns Implementation type (Socket/Engine).
     */
    public String getImplType () {
        if (mConfig != null) {
            if (mConfig.get("ImplType") != null) {
                return (String) mConfig.get("ImplType");
            }
        }
        return null;
    }

    /* Returns cryptographic protocol type.  */
    public String getProtocolType()
    {
        if (mConfig != null) {
            if (mConfig.get("ProtocolType") != null) {
                return (String) mConfig.get("ProtocolType");
            }
        }

        return null;
    }

    /**
     * Returns true if localhost connection is required.
     */
    public boolean isLocalhost () {
        if (mConfig != null) {
            if (mConfig.get("Localhost") != null) {
                if (mConfig.get("Localhost").equals("true")) {
                    return true;
                }
            }
        }
        return false;
    }

	public static String jfqdnToIP(String hostname)
    {     
    String host_IP = null;
             
     try
       {
       host_IP = InetAddress.getByName(hostname).getHostAddress();   
       }
     catch(UnknownHostException ex)
      {
       //system.out.println(ex);
       }      
       return host_IP;                 
   }

	
    /**
     * Returns SLP server IP address & port.
     */
    public String getSLPHost () {
       String temp;
	   String IP;
       String slp;
       String port;
	   
        Log.d(TAG,"getSLPHost");     
        if (mConfig != null)
         {                               
           if ( mConfig.get("slphost_fqdn") !=null  )
              {
                                                     
                       temp = mConfig.get("slphost_fqdn");
                       Log.d(TAG,"getSLPHost  slphost_fqdn : "+temp);
                       IP=(String)jfqdnToIP(temp);
                       //Log.d(TAG,"after jfqdnToIP + "+IP);
                       port= mConfig.get("port");
                       temp = IP.concat(":"); ;
                       slp=temp.concat(port); ;    
                       Log.d(TAG,"getSLPHost  IP+port: "+slp);
                       return (String)slp;                   
                }//end of slphost_fqdn not null
		  }	
         else    
         {
          // some dummmy value to pass if the field is empty?
          }
                   
	 return null;
    }


    public int getMcc () {
        String mcc = "-1";
        if (mConfig != null) {
            if (mConfig.get("mcc") != null) {
                mcc = (String) mConfig.get("mcc");
            }
        }

        return Integer.parseInt(mcc);
    }

    public int getMnc () {
        String mnc = "-1";
        if (mConfig != null) {
            if (mConfig.get("mnc") != null) {
                mnc = (String) mConfig.get("mnc");
            }
        }

        return Integer.parseInt(mnc);
    }

    public int getLac () {
        String lac = "-1";
        if (mConfig != null) {
            if (mConfig.get("lac") != null) {
                lac = (String) mConfig.get("lac");
            }
        }

        return Integer.parseInt(lac);
    }

    public int getCi () {
        String ci = "-1";
        if (mConfig != null) {
            if (mConfig.get("ci") != null) {
                ci = (String) mConfig.get("ci");
            }
        }

        return Integer.parseInt(ci);
    }

	 public int getCellType () {
        String citype = "1";
        if (mConfig != null) {
            if (mConfig.get("celltype") != null) {
                citype = (String) mConfig.get("celltype");
				Log.d(TAG,"getCellType  citype: "+citype);
            }else
            {
				Log.d(TAG,"celltype = NULL");
            }
        }else
    	{
			Log.d(TAG,"mConfig = null");
    	}

        return Integer.parseInt(citype);
    }
	public String getMsisdn () {
		String msisdn = "-1";
		if (mConfig != null) {
			if (mConfig.get("msisdn") != null){
				msisdn = (String) mConfig.get("msisdn");
			}
		}
		return msisdn;
	}

public int getHmcc () {
        String hmcc = "-1";
        if (mConfig != null) {
            if (mConfig.get("hmcc") != null) {
                hmcc = (String) mConfig.get("hmcc");
               Log.d(TAG,"getHmcc : frm Configfile: "+ hmcc);
            }
        }

        return Integer.parseInt(hmcc);
    }

    public int getHmnc () {
        String hmnc = "-1";
        if (mConfig != null) {
            if (mConfig.get("hmnc") != null) {
                hmnc = (String) mConfig.get("hmnc");
               Log.d(TAG,"getHmnc : frm Configfile: "+ hmnc);
            }
        }

        return Integer.parseInt(hmnc);
    }
}


