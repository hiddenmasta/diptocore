/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network.beans;

import dipto.Utility;
import dipto.business.network.Client;
import dipto.security.SecurityHandler;
import static dipto.security.SecurityHandler.force_encryption_change_message;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anon
 */
public class StringMessage extends SecretMessage{
    private String txt;
    
    public StringMessage(){}
    
    public StringMessage(String txt){
        this.txt = txt;
    }
    
    public String GetText(){
        return txt;
    }
    
    @Override
    public byte[] GetPlainBytes(){
        try {
            return txt.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(StringMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    @Override
    public Serializable GetEncryptedMessage(Client client, int secondsBeforeDeletion, byte[] buffer){
        if(txt.equals(force_encryption_change_message))
            return new EncryptionChangeMessage();
        
        SecurityHandler security_handler = client.GetServer().GetSecurityHandler();
        
        SecretMessage secret_message = security_handler.PrepareEncryption(this, client.GetRecipient(), client.GetNbMessagesSent());
        secret_message.setUnix_time(Utility.getUnixTimeBytes());
        
        String messageID = client.GetChatReplica().AddMessage(buffer, secret_message.getUnix_time());

        if( secondsBeforeDeletion > 0){
            client.setTimerBeforeMessageDeletion(secondsBeforeDeletion, messageID);
            secret_message.setSecondsBeforeDeletion(secondsBeforeDeletion);
        }

        return secret_message;
    }

    @Override
    public byte[] HandleMessage(Client client, SecretMessage enc_obj) {
        StringMessage message = (StringMessage)enc_obj;
        String plain_message = message.GetText();
        
        byte[] received_messaged_id = null;
        
        try {                    
            received_messaged_id = Utility.GetMessageID(message.GetPlainBytes(), unix_time);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException ex) {
            Logger.getLogger(StringMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.plain_message = plain_message;
        this.sender = client.hashed_recipient_id;

        client.GetServer().handleNewMessage(this);
        String messageID = client.GetChatReplica().AddMessage(message.GetPlainBytes(), unix_time);

        if(secondsBeforeDeletion > 0)
            client.setTimerBeforeMessageDeletion(secondsBeforeDeletion, messageID);
        
        return received_messaged_id;
    }
}
