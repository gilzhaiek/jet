//package android.networking;
package android.supl;

//package com.android.server.supl;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Class that implements abstract SecureManagerBase class.
 * @author Dmitriy Kardakov <Dmitriy.Kardakov@teleca.com>
 * @version 1.0
 */
public class TrustSecureManager extends SecureManagerBase {
    private TrustManagerFactory kmf = null;
    
    /**
     * Default TrustSecureManager constructor.
     */
    public TrustSecureManager () {
        super();
    }
    
    @Override
    protected void 
    initManagerFactory(KeyStore ks, char[] passphrase)
                       throws GeneralSecurityException {
        kmf = TrustManagerFactory.getInstance(CERTIFICATE_TYPE_X509);
        kmf.init(ks);
    }
    
    /**
     * Returns all available trust managers.
     * @return trust managers array
     */
    public TrustManager [] getKetManagers() {
        return kmf.getTrustManagers();
    }
}
