//package android.networking;

package android.supl;

//package com.android.server.supl;

import java.io.*;
import java.security.*;
import android.util.Log;

/**
 * Abstract class that contains methods for
 * working with SSL certificates storage.
 * @author Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>
 * @version 1.0
 */
public abstract class SecureManagerBase extends Object {
    /** Called when the activity is first created. */
    private String storePath = null ;
    private static final String DEFAULT_PASSWORD = "123456";
    public static final String CERTIFICATE_TYPE_X509 = "X509";
    public static final String SSL_STORAGE_TYPE_BKS = "BKS";
    private KeyStore ksKeysAndCert = null;
    
    /**
     * SecureManagerBase default constructor.
     */
    public SecureManagerBase() {
    }
    
    /**
     * Initilizes/loads SSL keys & certificates from storage.
     * @param Path - path to SSL storage
     * @param password - password for SSL storage
     */
    public void Init(String Path, String password) throws 
                                                   GeneralSecurityException,
                                                   IOException {
        char[] passphrase;
	Log.d("CNet","===>>> Inside SecureManagerBase Init <<===");
        if (Path != null) {
            storePath = Path;
	    Log.d("CNet","===>>> SMB: Path is NOT NULL <<===");
        }
        
        if (password == null) {
	    Log.d("CNet","===>>> SMB: Password is NULL <<===");
            passphrase = DEFAULT_PASSWORD.toCharArray();
        }
        else {
	    Log.d("CNet","===>>> SMB: Password is NOT NULL <<===");
            passphrase = password.toCharArray();
        }
        
        Log.d("CNet","===>>> SMB: Getting KeyStore Instance <<==="); 
        ksKeysAndCert = KeyStore.getInstance(SSL_STORAGE_TYPE_BKS);
        Log.d("CNet","===>>> SMB: Loading Instance <<==="); 
        ksKeysAndCert.load(new FileInputStream(storePath), passphrase);
        Log.d("CNet","===>>> SMB: Calling initManagerFactory <<==="); 
        initManagerFactory(ksKeysAndCert, passphrase) ;
    }
    
    protected abstract void 
    initManagerFactory(KeyStore ks, char[] passphrase)
                       throws GeneralSecurityException;
    
}
