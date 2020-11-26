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
package dipto.business.network;

import Exceptions.InvalidCipherNumberException;
import Exceptions.InvalidInitializationVectorException;
import Exceptions.InvalidKeyTypeException;
import algos.CryptoProprieties;
import confidentiality.Decryption;
import confidentiality.Encryption;
import dipto.security.Decryptor;
import dipto.security.Encryptor;
import dipto.security.HandshakeKeys;
import dipto.security.exceptions.InvalidSignatureException;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import keygenerator.KeyGen;
import authenticity.SignatureGen;
import dipto.Utility;
import static dipto.business.network.Server.*;
import dipto.business.network.beans.ConfirmationMessage;
import dipto.business.network.beans.DiscardedMessage;
import dipto.business.network.beans.EncryptionChangeMessage;
import dipto.business.network.beans.FileMessage;
import dipto.business.network.beans.SecretMessage;
import dipto.business.network.exceptions.TooMuchUnacknowledgedMessagesException;
import dipto.business.network.exceptions.WrongConfirmationMessageException;
import dipto.business.network.exceptions.WrongMessageOrderException;
import dipto.security.KeyStorage;
import java.io.File;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


/**
 *
 * @author anon
 */
public class Client extends Thread{ 
    private Thread sending_message_thread;
    private final String force_encryption_change_message;
    private Timer encryption_change_timer;
    private boolean active_encryption_change;
    private final Decryptor decryptor;
    private final Encryptor encryptor;
    private final Server server;
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;
    private boolean isStopped;
    private final MessagesEncapsulator messages_container;
    private final FilesEncapsulator files_container;
    private final MessageAcknoledgement sliding_window;
    private final ChatMessagesReplica chat_replica;
    int nb_messages_sent;
    public final String hashed_recipient_id;
    
    public Client(String recipient, Server server, ObjectOutputStream oos, ObjectInputStream ois){
        this.decryptor = new Decryptor();
        this.encryptor = new Encryptor();
        this.server = server;
        this.hashed_recipient_id = recipient;
        this.oos = oos;
        this.ois = ois;
        this.messages_container = new MessagesEncapsulator();
        this.files_container = new FilesEncapsulator();
        this.force_encryption_change_message = "{'force_encryption'}";
        this.sliding_window = new MessageAcknoledgement();
        this.chat_replica = new ChatMessagesReplica();
    }

    public void respondToChatRequest(boolean positive_response) {
        try {
            oos.writeBoolean(positive_response);
            oos.flush();    

            if(!positive_response)
                terminate();
            else{
                startPassiveHandshake();
                start();
                waitForOutgoingMessages();
                server.AddRecipient(hashed_recipient_id);
                server.AddClientChild(this);
            }
                
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addOutGoingMessage(Serializable message, int... timerBeforeDeletion){
        messages_container.addMessage(message, timerBeforeDeletion.length == 1 ? timerBeforeDeletion[0] : 0);
    }
    
    public void initiateChatRequest(){
        try {
            oos.writeUTF(server.getObfuscatedID());
            oos.flush();
            
            if(ois.readBoolean()){
                startActiveHandshake();
                start();
                waitForOutgoingMessages();
                server.AddRecipient(hashed_recipient_id);
                server.AddClientChild(this);
                encryptionChangeCycle();
            }
            
            else
                terminate();
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run(){
        while(!isStopped){
            Object obj;
            
            try {
                obj = ois.readObject();
                
                if(obj instanceof SecretMessage){
                    KeyStorage keyStore = server.GetKeyStore();
                    SecretKey cipher1_key = keyStore.getSecretKey(hashed_recipient_id+CIPHER1_KEY);
                    SecretKey cipher2_key = keyStore.getSecretKey(hashed_recipient_id+CIPHER2_KEY);
                    SecretKey cipher3_key = keyStore.getSecretKey(hashed_recipient_id+CIPHER3_KEY);
                    SecretKey hmac_key = keyStore.getSecretKey(hashed_recipient_id+HMAC_KEY);
                    
                    byte[] payload = Utility.objectToByteArray(((SecretMessage)obj).getEnc_payload());
                    byte[] rcv_HMAC = ((SecretMessage)obj).getSignature();
                    
                    if(!SignatureGen.verifySymSign(payload, rcv_HMAC, CryptoProprieties.HMAC_ALG, hmac_key))
                        throw new InvalidSignatureException();
                    
                    int message_order = ByteBuffer.wrap(((SecretMessage)obj).getIv_parameter()).getInt();
                    sliding_window.checkReceivedMessageOrder(message_order);
                    
                    Serializable message = decryptor.decrypt((SecretMessage)obj, cipher1_key, cipher2_key, cipher3_key);
                    
                    byte[] received_messaged_id = null;
                    
                    if(message instanceof String){
                        String plain_message = (String)message;
                        received_messaged_id = Utility.GetMessageID(plain_message.getBytes("UTF-8"), ((SecretMessage)obj).getUnix_time());                    
                        ((SecretMessage)obj).setPlain_message(plain_message);
                        ((SecretMessage)obj).setSender(hashed_recipient_id);

                        server.handleNewMessage((SecretMessage)obj);
                        String messageID = chat_replica.AddMessage(plain_message.getBytes("UTF-8"), ((SecretMessage)obj).getUnix_time());
                        
                        if(((SecretMessage)obj).getSecondsBeforeDeletion() > 0)
                            setTimerBeforeMessageDeletion(((SecretMessage)obj).getSecondsBeforeDeletion(), messageID);

                        plain_message = Utility.WipeText();                          
                    }else if(message instanceof File){
                        File file = (File)message;
                        received_messaged_id = Utility.GetMessageID(Files.readAllBytes(file.toPath()), ((SecretMessage)obj).getUnix_time());
                        server.handleNewFile((FileMessage)obj);
                    }
                    
                    oos.writeObject(new ConfirmationMessage(received_messaged_id));
                    oos.flush();
                } else if(obj instanceof EncryptionChangeMessage){
                    if(active_encryption_change){
                        messages_container.PauseMessageChecking();
                        startActiveHandshake();
                        messages_container.UnpauseMessageChecking();
                        active_encryption_change = false;
                    }
                    
                    else{
                        addOutGoingMessage(force_encryption_change_message);
                        messages_container.PauseMessageChecking();
                        startPassiveHandshake();
                        messages_container.UnpauseMessageChecking();
                        System.out.println("fin new handshake");
                    }
                } else if(obj instanceof ConfirmationMessage){
                    try {
                            sliding_window.checkConfirmationMessageExists(((ConfirmationMessage)obj).getMessageID());
                    } catch (WrongConfirmationMessageException ex) {
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (ClassNotFoundException | KeyStoreException | 
                    NoSuchAlgorithmException | UnrecoverableKeyException | 
                    NoSuchProviderException | InvalidKeyException | 
                    NoSuchPaddingException | 
                    InvalidAlgorithmParameterException | IllegalBlockSizeException | 
                    BadPaddingException | InvalidInitializationVectorException | 
                    InvalidCipherNumberException | 
                    InvalidSignatureException | WrongMessageOrderException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }catch(IOException ex){
                /* If the recipient left the conversation */
                if(ex instanceof EOFException || ex instanceof SocketException){
                    terminate();
                    server.handlePeerLeftConversation(hashed_recipient_id);                
                }

                else
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            obj = Utility.WipeText();
        }
    }
    
    private void setTimerBeforeMessageDeletion(int seconds, String messageID){
        if(seconds <= 0)
            return;
        
        new Thread(){
            @Override
            public void run(){
                new Timer(){}.schedule(new TimerTask(){
                    @Override
                    public void run(){
                        server.handleDeleteMessageTimerElapsed(chat_replica.RemoveMessage(messageID), hashed_recipient_id);
                    }
                }, seconds * 1000);
            }
            
        }.start();
    }

    private void waitForOutgoingMessages(){
        sending_message_thread = new Thread(){
            @Override
            public void run(){
                while(!isStopped){
                    ArrayList<Serializable> message;
                    
                    try {
                        message = messages_container.getLastPendingMessage();
                        Serializable objToSend;
                        
                        if(message.get(0).equals(force_encryption_change_message))
                            objToSend = new EncryptionChangeMessage();
                        else if(!(message.get(0) instanceof DiscardedMessage)){
                            KeyStorage keyStore = server.GetKeyStore();
                            SecretKey cipher1_key = keyStore.getSecretKey(hashed_recipient_id+CIPHER1_KEY);
                            SecretKey cipher2_key = keyStore.getSecretKey(hashed_recipient_id+CIPHER2_KEY);
                            SecretKey cipher3_key = keyStore.getSecretKey(hashed_recipient_id+CIPHER3_KEY);
                            SecretKey hmac_key = keyStore.getSecretKey(hashed_recipient_id+HMAC_KEY);
                            
                            SecretMessage secret_message = encryptor.encryptThenMAC(message.get(0), cipher1_key, cipher2_key, cipher3_key, hmac_key, ++nb_messages_sent);
                            secret_message.setUnix_time(Utility.getUnixTimeBytes());
                            
                            objToSend = secret_message;
                            byte[] messageBytes = null;
                            
                            if(message.get(0) instanceof String){
                                messageBytes = ((String) message.get(0)).getBytes("UTF-8");
                                
                                String messageID = chat_replica.AddMessage(messageBytes, secret_message.getUnix_time());
                                
                                if( message.size() == 2){
                                    setTimerBeforeMessageDeletion((int)message.get(1), messageID);
                                    secret_message.setSecondsBeforeDeletion((int)message.get(1));
                                }
                            }
                            else if(message.get(0) instanceof File){
                                File file = (File)message.get(0);
                                messageBytes = Files.readAllBytes(file.toPath());
                            }
                                
                            sliding_window.addSentMessage(messageBytes, secret_message.getUnix_time());
                        }
                        
                        oos.writeObject(objToSend); 
                        oos.flush();
                    } catch (IOException | KeyStoreException | 
                            NoSuchAlgorithmException | UnrecoverableKeyException | 
                            NoSuchProviderException | NoSuchPaddingException | 
                            InvalidAlgorithmParameterException | InvalidKeyException | 
                            IllegalBlockSizeException | BadPaddingException | 
                            InvalidInitializationVectorException | InvalidCipherNumberException | 
                            InvalidKeyTypeException | TooMuchUnacknowledgedMessagesException ex) {    
                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {}
                    
                    //message = Utility.WipeText();
                }
            }
        };
                
        sending_message_thread.start();
        messages_container.PeriodicallyCheckMessages();
    }
    
    private void encryptionChangeCycle(){
        encryption_change_timer = new Timer();

        encryption_change_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                active_encryption_change = true;
                addOutGoingMessage(force_encryption_change_message);
            }
        }, 60 * 1000, 60 * 1000);
    }

    public void terminate(){
        try {
            isStopped = true;
            
            if(oos != null)
                oos.close();
            if(ois != null)
                ois.close();

            if(server.IsThereRecipient(hashed_recipient_id))
                server.RemoveRecipient(hashed_recipient_id);

            server.client_threads.remove(this);
            
            cleanKeyStore();
            
            if(encryption_change_timer != null)
                encryption_change_timer.cancel();
            
            sending_message_thread.interrupt();
            
            try {
                sending_message_thread.join();
            } catch (InterruptedException ex) {}
            
            messages_container.StopMessageChecking();
            
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void sendCertificate(){
        try {
            KeyStorage keyStore = server.GetKeyStore();
            X509Certificate certificat = keyStore.getOwnCertificate();
            
            oos.writeObject(certificat);
            oos.flush();        
        } catch (KeyStoreException | IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void saveSessionKeys(SecretKey sk_cipher1, SecretKey sk_cipher2, SecretKey sk_cipher3, SecretKey sk_hmac){
        try {
            KeyStorage keyStore = server.GetKeyStore();
            
            keyStore.saveKey(hashed_recipient_id+CIPHER1_KEY, sk_cipher1);
            keyStore.saveKey(hashed_recipient_id+CIPHER2_KEY, sk_cipher2);
            keyStore.saveKey(hashed_recipient_id+CIPHER3_KEY, sk_cipher3);
            keyStore.saveKey(hashed_recipient_id+HMAC_KEY, sk_hmac);
        } catch (KeyStoreException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void createAndSaveSessionKeys(HandshakeKeys recipient_keys, KeyPair half_hmac_key, KeyPair half_cipher1_key, KeyPair half_cipher2_key, KeyPair half_cipher3_key){
        try {
            KeyStorage keyStore = server.GetKeyStore();
            
            SealedObject recipient_sealed_half_cipher1_key = (SealedObject)recipient_keys.getHalf_cipher1_key().getObject();
            SealedObject recipient_sealed_half_cipher2_key = (SealedObject)recipient_keys.getHalf_cipher2_key().getObject();
            SealedObject recipient_sealed_half_cipher3_key = (SealedObject)recipient_keys.getHalf_cipher3_key().getObject();
            SealedObject recipient_sealed_half_hmac_key = (SealedObject)recipient_keys.getHalf_hmac_key().getObject();
            
            PublicKey recipient_half_cipher1_key = (PublicKey)Decryption.sealedObjectDec(recipient_sealed_half_cipher1_key, keyStore.getOwnPrivateKey());
            PublicKey recipient_half_cipher2_key = (PublicKey)Decryption.sealedObjectDec(recipient_sealed_half_cipher2_key, keyStore.getOwnPrivateKey());
            PublicKey recipient_half_cipher3_key = (PublicKey)Decryption.sealedObjectDec(recipient_sealed_half_cipher3_key, keyStore.getOwnPrivateKey());
            PublicKey recipient_half_hmac_key = (PublicKey)Decryption.sealedObjectDec(recipient_sealed_half_hmac_key , keyStore.getOwnPrivateKey());
            
            SecretKey sk_cipher1 = KeyGen.generateSharedDHKey(half_cipher1_key, recipient_half_cipher1_key, CryptoProprieties.RECURSIVE_SYM_KEY_ALG1);
            SecretKey sk_cipher2 = KeyGen.generateSharedDHKey(half_cipher2_key, recipient_half_cipher2_key, CryptoProprieties.RECURSIVE_SYM_KEY_ALG2);
            SecretKey sk_cipher3 = KeyGen.generateSharedDHKey(half_cipher3_key, recipient_half_cipher3_key, CryptoProprieties.RECURSIVE_SYM_KEY_ALG3);
            SecretKey sk_hmac = KeyGen.generateSharedDHKey(half_hmac_key, recipient_half_hmac_key, CryptoProprieties.RECURSIVE_SYM_KEY_ALG3);
            
            keyStore.saveKey(hashed_recipient_id+CIPHER1_KEY, sk_cipher1);
            keyStore.saveKey(hashed_recipient_id+CIPHER2_KEY, sk_cipher2);
            keyStore.saveKey(hashed_recipient_id+CIPHER3_KEY, sk_cipher3);
            keyStore.saveKey(hashed_recipient_id+HMAC_KEY, sk_hmac);
        } catch (IOException | ClassNotFoundException | 
                NoSuchAlgorithmException | InvalidKeyException | 
                NoSuchProviderException | KeyStoreException | 
                UnrecoverableKeyException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startActiveHandshake()
    {
        try {
            KeyStorage keyStore = server.GetKeyStore();
            
            sendCertificate();
            
            X509Certificate recipient_certificat = (X509Certificate)ois.readObject();
            recipient_certificat.checkValidity();
            
            KeyPair half_hmac_key = KeyGen.generateOwnDHKey();
            KeyPair half_cipher1_key = KeyGen.generateOwnDHKey();
            KeyPair half_cipher2_key = KeyGen.generateOwnDHKey();
            KeyPair half_cipher3_key = KeyGen.generateOwnDHKey();
            
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
                                                              keyStore.getOwnPrivateKey(),
                                                              CryptoProprieties.SIGNATURE_ALG);
            SignedObject signed_half_cipher2_key = SignatureGen.signObject(sealed_half_cipher2_key,
                                                              keyStore.getOwnPrivateKey(),
                                                              CryptoProprieties.SIGNATURE_ALG);
            SignedObject signed_half_cipher3_key = SignatureGen.signObject(sealed_half_cipher3_key,
                                                              keyStore.getOwnPrivateKey(),
                                                              CryptoProprieties.SIGNATURE_ALG);
            
            SignedObject signed_half_hmac_key = SignatureGen.signObject(sealed_half_hmac_key,
                                                              keyStore.getOwnPrivateKey(),
                                                              CryptoProprieties.SIGNATURE_ALG);
            
            oos.writeObject(new HandshakeKeys(signed_half_cipher1_key, 
                                              signed_half_cipher2_key, 
                                              signed_half_cipher3_key,
                                              signed_half_hmac_key));
            oos.flush();
            
            HandshakeKeys recipient_keys = (HandshakeKeys)ois.readObject();
            
            if( !SignatureGen.verifyObjectSign(recipient_keys.getHalf_cipher1_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG) ||
                !SignatureGen.verifyObjectSign(recipient_keys.getHalf_cipher2_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG) ||
                !SignatureGen.verifyObjectSign(recipient_keys.getHalf_cipher3_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG) ||
                !SignatureGen.verifyObjectSign(recipient_keys.getHalf_hmac_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG))
                throw new InvalidSignatureException();
            
            cleanKeyStore();
            createAndSaveSessionKeys(recipient_keys, half_hmac_key, half_cipher1_key, half_cipher2_key, half_cipher3_key);
            
        } catch (IOException ex){
                /* If the recipient left the conversation */
                if(ex instanceof EOFException || ex instanceof SocketException){
                    terminate();
                    server.handlePeerLeftConversation(hashed_recipient_id);  
                }
                
                else
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch(NoSuchAlgorithmException | NoSuchProviderException | 
                InvalidAlgorithmParameterException | ClassNotFoundException | 
                InvalidKeyException | SignatureException | 
                NoSuchPaddingException | InvalidInitializationVectorException | 
                IllegalBlockSizeException | InvalidSignatureException | 
                InvalidKeyTypeException | CertificateExpiredException | 
                CertificateNotYetValidException | KeyStoreException | UnrecoverableKeyException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private X509Certificate readCertificate() throws CertificateExpiredException, IOException, ClassNotFoundException, CertificateNotYetValidException{
        X509Certificate recipient_certificat = (X509Certificate)ois.readObject();
        recipient_certificat.checkValidity();
            
        return recipient_certificat;
    }
    
    private void startPassiveHandshake()
    {
        try {
            KeyStorage keyStore = server.GetKeyStore();
            
            X509Certificate recipient_certificat = readCertificate();
            sendCertificate();

            KeyPair half_hmac_key = KeyGen.generateOwnDHKey();
            KeyPair half_cipher1_key = KeyGen.generateOwnDHKey();
            KeyPair half_cipher2_key = KeyGen.generateOwnDHKey();
            KeyPair half_cipher3_key = KeyGen.generateOwnDHKey();

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
                                                              keyStore.getOwnPrivateKey(),
                                                              CryptoProprieties.SIGNATURE_ALG);
            SignedObject signed_half_cipher2_key = SignatureGen.signObject(sealed_half_cipher2_key,
                                                              keyStore.getOwnPrivateKey(),
                                                              CryptoProprieties.SIGNATURE_ALG);
            SignedObject signed_half_cipher3_key = SignatureGen.signObject(sealed_half_cipher3_key,
                                                              keyStore.getOwnPrivateKey(),
                                                              CryptoProprieties.SIGNATURE_ALG);
            
            SignedObject signed_half_hmac_key = SignatureGen.signObject(sealed_half_hmac_key,
                                                              keyStore.getOwnPrivateKey(),
                                                              CryptoProprieties.SIGNATURE_ALG);

            HandshakeKeys recipient_keys = (HandshakeKeys)ois.readObject();

            if( !SignatureGen.verifyObjectSign(recipient_keys.getHalf_cipher1_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG) ||
                !SignatureGen.verifyObjectSign(recipient_keys.getHalf_cipher2_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG) ||
                !SignatureGen.verifyObjectSign(recipient_keys.getHalf_cipher3_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG) ||
                !SignatureGen.verifyObjectSign(recipient_keys.getHalf_hmac_key(), recipient_certificat.getPublicKey(), CryptoProprieties.SIGNATURE_ALG))
                throw new InvalidSignatureException();

            oos.writeObject(new HandshakeKeys(signed_half_cipher1_key, 
                                              signed_half_cipher2_key, 
                                              signed_half_cipher3_key, 
                                              signed_half_hmac_key));
            oos.flush();
            
            cleanKeyStore();
            createAndSaveSessionKeys(recipient_keys, half_hmac_key, half_cipher1_key, half_cipher2_key, half_cipher3_key);

        } catch (IOException ex){
            /* If the recipient left the conversation */
            if(ex instanceof EOFException || ex instanceof SocketException){
                terminate();
                server.handlePeerLeftConversation(hashed_recipient_id);  
            }  
            
            else
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch(InvalidKeyTypeException | NoSuchAlgorithmException | 
                NoSuchProviderException | NoSuchPaddingException | 
                InvalidKeyException | InvalidInitializationVectorException | 
                InvalidAlgorithmParameterException | IllegalBlockSizeException | 
                SignatureException | KeyStoreException | 
                UnrecoverableKeyException | ClassNotFoundException | 
                InvalidSignatureException | CertificateExpiredException | 
                CertificateNotYetValidException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void cleanKeyStore(){
        try {
            KeyStorage keyStore = server.GetKeyStore();
            
            keyStore.removeKey(hashed_recipient_id+CIPHER1_KEY);
            keyStore.removeKey(hashed_recipient_id+CIPHER2_KEY);
            keyStore.removeKey(hashed_recipient_id+CIPHER3_KEY);
            keyStore.removeKey(hashed_recipient_id+HMAC_KEY);
        } catch (KeyStoreException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
