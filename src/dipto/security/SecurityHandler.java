/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.security;

import Exceptions.InvalidCipherNumberException;
import Exceptions.InvalidInitializationVectorException;
import Exceptions.InvalidKeyTypeException;
import algos.CryptoProprieties;
import authenticity.SignatureGen;
import confidentiality.Decryption;
import confidentiality.Encryption;
import dipto.Utility;
import dipto.business.network.Client;
import dipto.business.network.beans.SecretMessage;
import dipto.business.network.tcpip.ClientTCP;
import dipto.business.network.tcpip.ServerTCP;
import static dipto.security.KeyStorage.CIPHER1_KEY;
import static dipto.security.KeyStorage.CIPHER2_KEY;
import static dipto.security.KeyStorage.CIPHER3_KEY;
import static dipto.security.KeyStorage.HMAC_KEY;
import dipto.security.beans.HandshakeKeys;
import dipto.security.beans.TemporaryKeysForDH;
import dipto.security.exceptions.InvalidSignatureException;
import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import keygenerator.KeyGen;

/**
 *
 * @author anon
 */
public class SecurityHandler {
    private final Decryptor decryptor;
    private final Encryptor encryptor;
    private TemporaryKeysForDH keypairKeys;
    private KeyStorage key_storage;
    
    public static final String force_encryption_change_message = "{'force_encryption'}";
    
    public SecurityHandler(){
        this.decryptor = new Decryptor();
        this.encryptor = new Encryptor();
    }
    
    public void RemoveSessionKeysFromKeyStore(String hashed_recipient_id){
        try {
            key_storage.removeKey(hashed_recipient_id+CIPHER1_KEY);
            key_storage.removeKey(hashed_recipient_id+CIPHER2_KEY);
            key_storage.removeKey(hashed_recipient_id+CIPHER3_KEY);
            key_storage.removeKey(hashed_recipient_id+HMAC_KEY);
        } catch (KeyStoreException ex) {
            Logger.getLogger(ClientTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public KeyStorage GetKeyStore(){
        return key_storage;
    }
            
    public void saveSessionKeys(String hashed_recipient_id,
                                   SecretKey sk_cipher1, 
                                   SecretKey sk_cipher2, 
                                   SecretKey sk_cipher3, 
                                   SecretKey sk_hmac){
        try {
            RemoveSessionKeysFromKeyStore(hashed_recipient_id);
            
            key_storage.saveKey(hashed_recipient_id+CIPHER1_KEY, sk_cipher1);
            key_storage.saveKey(hashed_recipient_id+CIPHER2_KEY, sk_cipher2);
            key_storage.saveKey(hashed_recipient_id+CIPHER3_KEY, sk_cipher3);
            key_storage.saveKey(hashed_recipient_id+HMAC_KEY, sk_hmac);
        } catch (KeyStoreException ex) {
            Logger.getLogger(ClientTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void createSessionKeys(HandshakeKeys recipient_keys, String recipient_id){
        try {
            SealedObject recipient_sealed_half_cipher1_key = (SealedObject)recipient_keys.getHalf_cipher1_key().getObject();
            SealedObject recipient_sealed_half_cipher2_key = (SealedObject)recipient_keys.getHalf_cipher2_key().getObject();
            SealedObject recipient_sealed_half_cipher3_key = (SealedObject)recipient_keys.getHalf_cipher3_key().getObject();
            SealedObject recipient_sealed_half_hmac_key = (SealedObject)recipient_keys.getHalf_hmac_key().getObject();
            
            PublicKey recipient_half_cipher1_key = (PublicKey)Decryption.sealedObjectDec(recipient_sealed_half_cipher1_key, key_storage.getOwnPrivateKey());
            PublicKey recipient_half_cipher2_key = (PublicKey)Decryption.sealedObjectDec(recipient_sealed_half_cipher2_key, key_storage.getOwnPrivateKey());
            PublicKey recipient_half_cipher3_key = (PublicKey)Decryption.sealedObjectDec(recipient_sealed_half_cipher3_key, key_storage.getOwnPrivateKey());
            PublicKey recipient_half_hmac_key = (PublicKey)Decryption.sealedObjectDec(recipient_sealed_half_hmac_key , key_storage.getOwnPrivateKey());
            
            SecretKey sk_cipher1 = KeyGen.generateSharedDHKey(keypairKeys.getHalf_cipher1_key(), recipient_half_cipher1_key, CryptoProprieties.RECURSIVE_SYM_KEY_ALG1);
            SecretKey sk_cipher2 = KeyGen.generateSharedDHKey(keypairKeys.getHalf_cipher2_key(), recipient_half_cipher2_key, CryptoProprieties.RECURSIVE_SYM_KEY_ALG2);
            SecretKey sk_cipher3 = KeyGen.generateSharedDHKey(keypairKeys.getHalf_cipher3_key(), recipient_half_cipher3_key, CryptoProprieties.RECURSIVE_SYM_KEY_ALG3);
            SecretKey sk_hmac = KeyGen.generateSharedDHKey(keypairKeys.getHalf_hmac_key(), recipient_half_hmac_key, CryptoProprieties.RECURSIVE_SYM_KEY_ALG3);
            
            saveSessionKeys(recipient_id, sk_cipher1, sk_cipher2, sk_cipher3, sk_hmac);
        } catch (IOException | ClassNotFoundException | 
                NoSuchAlgorithmException | InvalidKeyException | 
                NoSuchProviderException | KeyStoreException | 
                UnrecoverableKeyException ex) {
            Logger.getLogger(ClientTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public TemporaryKeysForDH CreateTemporaryKeys(){
        try {
            KeyPair half_hmac_key = KeyGen.generateOwnDHKey();
            KeyPair half_cipher1_key = KeyGen.generateOwnDHKey();
            KeyPair half_cipher2_key = KeyGen.generateOwnDHKey();
            KeyPair half_cipher3_key = KeyGen.generateOwnDHKey();
            
            return new TemporaryKeysForDH(half_hmac_key, half_cipher1_key, half_cipher2_key, half_cipher3_key);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | 
                InvalidAlgorithmParameterException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    public X509Certificate GetCertificate(){
        try {
            return key_storage.getOwnCertificate();
        } catch (KeyStoreException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
    
    public HandshakeKeys GetHandshakeKeys(X509Certificate recipient_certificat){
        try {
            keypairKeys = CreateTemporaryKeys();
            
            KeyPair half_hmac_key = keypairKeys.getHalf_hmac_key();
            KeyPair half_cipher1_key = keypairKeys.getHalf_cipher1_key();
            KeyPair half_cipher2_key = keypairKeys.getHalf_cipher2_key();
            KeyPair half_cipher3_key = keypairKeys.getHalf_cipher3_key();
            
            SealedObject sealed_half_cipher1_key = Encryption.sealedObjectEnc(half_cipher1_key.getPublic(),
                    CryptoProprieties.ASYM_ALG,
                    recipient_certificat.getPublicKey(),
                    null);
            SealedObject sealed_half_cipher2_key = Encryption.sealedObjectEnc(half_cipher2_key.getPublic(),
                    CryptoProprieties.ASYM_ALG,
                    recipient_certificat.getPublicKey(),
                    null);
            SealedObject sealed_half_cipher3_key = Encryption.sealedObjectEnc(half_cipher3_key.getPublic(),
                    CryptoProprieties.ASYM_ALG,
                    recipient_certificat.getPublicKey(),
                    null);
            
            SealedObject sealed_half_hmac_key = Encryption.sealedObjectEnc(half_hmac_key.getPublic(),
                    CryptoProprieties.ASYM_ALG,
                    recipient_certificat.getPublicKey(),
                    null);
            
            SignedObject signed_half_cipher1_key = SignatureGen.signObject(sealed_half_cipher1_key,
                    key_storage.getOwnPrivateKey(),
                    CryptoProprieties.SIGNATURE_ALG);
            SignedObject signed_half_cipher2_key = SignatureGen.signObject(sealed_half_cipher2_key,
                    key_storage.getOwnPrivateKey(),
                    CryptoProprieties.SIGNATURE_ALG);
            SignedObject signed_half_cipher3_key = SignatureGen.signObject(sealed_half_cipher3_key,
                    key_storage.getOwnPrivateKey(),
                    CryptoProprieties.SIGNATURE_ALG);
            
            SignedObject signed_half_hmac_key = SignatureGen.signObject(sealed_half_hmac_key,
                    key_storage.getOwnPrivateKey(),    
                    CryptoProprieties.SIGNATURE_ALG);
            
            return new HandshakeKeys(signed_half_cipher1_key, 
                                              signed_half_cipher2_key, 
                                              signed_half_cipher3_key,
                                              signed_half_hmac_key);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | 
                InvalidAlgorithmParameterException | InvalidKeyTypeException | 
                NoSuchPaddingException | InvalidKeyException | 
                InvalidInitializationVectorException | IOException | 
                IllegalBlockSizeException | SignatureException | 
                KeyStoreException | UnrecoverableKeyException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    public void VerifyDHKeysSignature(HandshakeKeys recipient_keys, X509Certificate recipient_certificat) throws InvalidSignatureException{
        try {
            if( !SignatureGen.verifyObjectSign(recipient_keys.getHalf_cipher1_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG) ||
                    !SignatureGen.verifyObjectSign(recipient_keys.getHalf_cipher2_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG) ||
                    !SignatureGen.verifyObjectSign(recipient_keys.getHalf_cipher3_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG) ||
                    !SignatureGen.verifyObjectSign(recipient_keys.getHalf_hmac_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG))
                throw new InvalidSignatureException();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | 
                InvalidKeyException | SignatureException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void VerifyMessageSignature(SecretMessage message, String recipient_id) throws InvalidSignatureException{
        try {
            SecretKey hmac_key = key_storage.getSecretKey(recipient_id+HMAC_KEY);

            byte[] payload = Utility.objectToByteArray(message.getEnc_payload());
            byte[] rcv_HMAC = message.getSignature();
            
            if(!SignatureGen.verifySymSign(payload, rcv_HMAC, CryptoProprieties.HMAC_ALG, hmac_key))
                throw new InvalidSignatureException();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | 
                InvalidKeyException | KeyStoreException | 
                UnrecoverableKeyException ex) {
            Logger.getLogger(SecurityHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void generateKeys(){
        new Thread(){
            @Override
            public void run(){
                try {
                    key_storage = new KeyStorage();
                    key_storage.PrepareSessionKeys();
                    System.out.println("fini");
                } catch (KeyStoreException | IOException |
                        NoSuchAlgorithmException | CertificateException |
                        InvalidKeyException | SignatureException ex) {
                    Logger.getLogger(ServerTCP.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
    
    public SecretMessage PrepareEncryption(Serializable obj, String recipient_id, int IVparameter){
        try {
            SecretKey cipher1_key = key_storage.getSecretKey(recipient_id+CIPHER1_KEY);
            SecretKey cipher2_key = key_storage.getSecretKey(recipient_id+CIPHER2_KEY);
            SecretKey cipher3_key = key_storage.getSecretKey(recipient_id+CIPHER3_KEY);
            SecretKey hmac_key = key_storage.getSecretKey(recipient_id+HMAC_KEY);
            
            return encryptor.encryptThenMAC(obj, cipher1_key, cipher2_key, cipher3_key, hmac_key, IVparameter);
        } catch (KeyStoreException | NoSuchAlgorithmException | 
                UnrecoverableKeyException | NoSuchProviderException | 
                NoSuchPaddingException | InvalidAlgorithmParameterException | 
                InvalidKeyException | IllegalBlockSizeException | 
                BadPaddingException | InvalidInitializationVectorException | 
                InvalidCipherNumberException | InvalidKeyTypeException | 
                IOException ex) {
            Logger.getLogger(SecurityHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    public Object PrepareDecryption(SecretMessage obj, String recipient_id){
        try {
            SecretKey cipher1_key = key_storage.getSecretKey(recipient_id+CIPHER1_KEY);
            SecretKey cipher2_key = key_storage.getSecretKey(recipient_id+CIPHER2_KEY);
            SecretKey cipher3_key = key_storage.getSecretKey(recipient_id+CIPHER3_KEY);
            
            return decryptor.decrypt(obj, cipher1_key, cipher2_key, cipher3_key);
        } catch (KeyStoreException | NoSuchAlgorithmException | 
                UnrecoverableKeyException | NoSuchProviderException | 
                NoSuchPaddingException | InvalidAlgorithmParameterException | 
                InvalidKeyException | IllegalBlockSizeException | 
                BadPaddingException | InvalidInitializationVectorException | 
                InvalidCipherNumberException | IOException |
                ClassNotFoundException ex) {
            Logger.getLogger(SecurityHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
}
