/*Dipto, a reasonably secure end-to-end desktop chat app built by the paranoid, for the paranoid
Copyright (C) 2018 Hiddenmaster

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.*/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import keygenerator.KeyGen;
import keygenerator.KeysAndCertificate;
import org.bouncycastle.operator.OperatorCreationException;

/**
 *
 * @author anon
 */
public class KeyStorage {
    private KeyStore keystore;
    
    public KeyStorage() throws 
    KeyStoreException, 
    IOException, 
    NoSuchAlgorithmException, 
    CertificateException, 
    InvalidKeyException, 
    SignatureException
    {
        /* Creation of the session keystore */
        keystore = KeyStore.getInstance("JCEKS");
        keystore.load(null, "".toCharArray());
        
        try {
            KeysAndCertificate keys_cert = KeyGen.generateKeysAndCertificate();
            keystore.setKeyEntry("session", keys_cert.getPrk(), "".toCharArray(), new X509Certificate[]{keys_cert.getCert()});
        } catch (OperatorCreationException | NoSuchProviderException ex) {
            Logger.getLogger(KeyStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public synchronized SecretKey getSecretKey(String alias) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException{
        if(keystore.isKeyEntry(alias))
            return (SecretKey)keystore.getKey(alias, "".toCharArray());
        else
            return null;
    }
    
    public synchronized X509Certificate getOwnCertificate() throws KeyStoreException{
        return (X509Certificate) keystore.getCertificate("session");
    }
    
    public synchronized PrivateKey getOwnPrivateKey() throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException{
        return (PrivateKey)keystore.getKey("session", "".toCharArray());
    }
    
    public synchronized void saveKey(String alias, SecretKey key) throws KeyStoreException{
        if(!keystore.isKeyEntry(alias))
            keystore.setKeyEntry(alias, key, "".toCharArray(), null);
    }
    
    public synchronized void removeKey(String alias) throws KeyStoreException{
        if(keystore.isKeyEntry(alias))
            keystore.deleteEntry(alias);
    }
    
    
}
