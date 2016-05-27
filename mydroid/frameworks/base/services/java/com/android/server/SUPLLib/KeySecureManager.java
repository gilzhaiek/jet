//package android.networking;

package android.supl;



//package com.android.server.supl;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import android.util.Log;

/**
 * Class that implements abstract SecureManagerBase class.
 * @author Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>
 * @version 1.0
 */
public class KeySecureManager extends SecureManagerBase {
    private KeyManagerFactory kmf = null;
    
    /**
     * KeySecureManager default constructor.
     */
    public KeySecureManager () {
        super();
    }
    
    @Override
    protected void initManagerFactory(KeyStore ks, char[] passphrase) 
                                      throws GeneralSecurityException {
	Log.d("CNet","===>> Inside initManagerFactory <<===");
        kmf = KeyManagerFactory.getInstance(CERTIFICATE_TYPE_X509);
        kmf.init(ks, passphrase);
    }
    
    /**
     * Returns all available key managers.
     * @return key managers array
     */
    public KeyManager [] getKetManagers () {
        return kmf.getKeyManagers();
    }
}
