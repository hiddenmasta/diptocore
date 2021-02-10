/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network;

import dipto.business.network.beans.CustomFile;
import dipto.business.network.beans.EncryptionChangeMessage;
import dipto.business.network.beans.FileMessage;
import dipto.business.network.beans.PlainMessage;
import dipto.business.network.beans.SecretMessage;
import dipto.business.network.beans.StringMessage;
import dipto.business.network.exceptions.TooMuchUnacknowledgedMessagesException;
import dipto.business.network.tcpip.ClientTCP;
import dipto.security.SecurityHandler;
import static dipto.security.SecurityHandler.force_encryption_change_message;
import dipto.security.beans.HandshakeKeys;
import dipto.security.exceptions.InvalidSignatureException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anon
 */
public abstract class Client extends Thread{
    protected Thread sending_message_thread;
    protected final Server server;
    protected boolean isStopped;
    protected final MessagesEncapsulator messages_container;
    protected final MessageAcknowledgement sliding_window;
    protected final ConversationMirror chat_replica;
    protected int nb_messages_sent;
    protected boolean initiated_encryption_change;
    protected Timer encryption_change_timer;
    protected boolean is_receiving_thread_stopped;
    protected boolean is_being_interrupted;
    private boolean peer_already_left;
    
    public final String hashed_recipient_id;
    
    public Client(Server server, String recipient){
        this.messages_container = new MessagesEncapsulator();
        this.sliding_window = new MessageAcknowledgement();
        this.chat_replica = new ConversationMirror();
        this.server = server;
        this.hashed_recipient_id = recipient;
        this.is_receiving_thread_stopped = false;
        this.is_being_interrupted = false;
        this.peer_already_left = false;
    }
    
    public int GetNbMessagesSent(){
        return nb_messages_sent;
    }
    
    public Server GetServer(){
        return server;
    }
    
    public String GetRecipient(){
        return hashed_recipient_id;
    }
    
    public ConversationMirror GetChatReplica(){
        return chat_replica;
    }
    
    public MessageAcknowledgement GetAcknowledgmentHandler(){
        return sliding_window;
    }
    
    //public abstract void respondToChatRequest(boolean positive_response);
    //public abstract void initiateChatRequest(); 
    //protected abstract void waitForOutgoingMessages();
    protected abstract Object Read();
    protected abstract void Write(Object obj);
    
    public void setTimerBeforeMessageDeletion(int seconds, String messageID){
        if(seconds <= 0)
            return;
        
        new Timer(){}.schedule(new TimerTask(){
            @Override
            public void run(){
                server.handleDeleteMessageTimerElapsed(chat_replica.RemoveMessage(messageID), hashed_recipient_id);
            }
        }, seconds * 1000);
    }
    
    protected void encryptionChangeCycle(){
        initiated_encryption_change = true;
        encryption_change_timer = new Timer();
        encryption_change_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                addOutGoingMessage(new EncryptionChangeMessage());
            }
        }, 60 * 1000, 60 * 1000);
    }
    
    protected void addOutGoingMessage(Serializable message, int... timerBeforeDeletion){
        Serializable new_obj = null;
        
        if(message instanceof String)
            new_obj = new StringMessage((String)message);
        else if(message instanceof File){
            File file = (File)message;
            
            try {
                new_obj = new FileMessage(new CustomFile(file.getName(), Files.readAllBytes(file.toPath())));
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
            
        else
            new_obj = message;
        
        messages_container.addMessage(new_obj, timerBeforeDeletion.length == 1 ? timerBeforeDeletion[0] : 0);
    }
    
    protected void PeerLeft(String recipient_id){
        if(!peer_already_left){
            server.handlePeerLeftConversation(hashed_recipient_id); 
            peer_already_left = true;
        }
    }
    
    public void respondToChatRequest(boolean positive_response) {
        Write(new Boolean(true));

        if(!positive_response)
            interrupt();
        else{
            StartHandShake(false);
            start();
            waitForOutgoingMessages(this);
            server.AddRecipient(hashed_recipient_id);
            server.AddClientChild(this);
        }
    }
    
    public void initiateChatRequest(){
        Write(server.getObfuscatedID());
        
        if(Read().equals(true)){
            StartHandShake(true);
            start();
            waitForOutgoingMessages(this);
            server.AddRecipient(hashed_recipient_id);
            server.AddClientChild(this);
            //encryptionChangeCycle();
        }

        else
            interrupt();
    }
    
    protected void StartHandShake(Boolean isActive){
        X509Certificate recipient_certificat;
        HandshakeKeys recipient_keys;
        SecurityHandler security_handler = server.GetSecurityHandler();
        
        if(isActive){
            Write(security_handler.GetCertificate());
            recipient_certificat = (X509Certificate)Read();
            //recipient_certificat.checkValidity();
        }else{
            recipient_certificat = (X509Certificate)Read();
            //recipient_certificat.checkValidity();
            Write(security_handler.GetCertificate());
        }

        if(isActive){
            Write(security_handler.GetHandshakeKeys(recipient_certificat));
            recipient_keys = (HandshakeKeys)Read();
        }else{
            recipient_keys = (HandshakeKeys)Read();
            Write(security_handler.GetHandshakeKeys(recipient_certificat));
        }
        
        try{
            security_handler.VerifyDHKeysSignature(recipient_keys, recipient_certificat);
            security_handler.createSessionKeys(recipient_keys, hashed_recipient_id);
        }catch(InvalidSignatureException ex){
            Logger.getLogger(ClientTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    protected void waitForOutgoingMessages(Client client){
        sending_message_thread = new Thread(() -> {
            while(!is_receiving_thread_stopped){
                ArrayList<Serializable> message;

                try {
                    message = messages_container.getLastPendingMessage();

                    Object obj_to_send = null;

                    if(message.get(0) instanceof PlainMessage)
                        obj_to_send = ((PlainMessage)message.get(0)).GetMessage();
                    else if(message.get(0) instanceof SecretMessage){
                        SecretMessage secret_message = (SecretMessage)message.get(0);
                        byte[] plain_bytes = secret_message.GetPlainBytes();
                        ++nb_messages_sent;
                        obj_to_send = secret_message.GetEncryptedMessage(client,
                                                                         message.size() == 2 ? (int)message.get(1) : 0,
                                                                         plain_bytes);

                        sliding_window.addSentMessage(plain_bytes, ((SecretMessage)obj_to_send).getUnix_time());
                    }

                    Write(obj_to_send);
                } catch (TooMuchUnacknowledgedMessagesException |
                        InterruptedException ex) {    
                    Logger.getLogger(ClientTCP.class.getName()).log(Level.SEVERE, null, ex);
                }
            }});
        
        sending_message_thread.start();
        messages_container.PeriodicallyCheckMessages();
    }
    
    public void PrepareForEncryptionChange(){
        if(initiated_encryption_change){
            messages_container.PauseMessageChecking();
            StartHandShake(true);
            messages_container.UnpauseMessageChecking();
        }else{
            addOutGoingMessage(force_encryption_change_message);
            messages_container.PauseMessageChecking();
            StartHandShake(false);
            messages_container.UnpauseMessageChecking();
        }
    }
    
    public void Close(){
        if(server.IsThereRecipient(hashed_recipient_id))
            server.RemoveRecipient(hashed_recipient_id);

        server.client_threads.remove(this);

        server.GetSecurityHandler().RemoveSessionKeysFromKeyStore(hashed_recipient_id);

        if(encryption_change_timer != null)
            encryption_change_timer.cancel();

        sending_message_thread.interrupt();

        try {
            sending_message_thread.join();
        } catch (InterruptedException ex) {}

        messages_container.StopMessageChecking();
    }
}
