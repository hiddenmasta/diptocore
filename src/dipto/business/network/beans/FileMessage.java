/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network.beans;

import dipto.Utility;
import dipto.business.network.Client;
import dipto.security.SecurityHandler;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anon
 */
public class FileMessage extends SecretMessage{
    private CustomFile file;
    
    public FileMessage(){}
    
    public FileMessage(CustomFile file){
        this.file = file;
    }

    public CustomFile GetFile() {
        return file;
    }

    @Override
    public Serializable GetEncryptedMessage(Client client, int secondsBeforeDeletion, byte[] buffer) {
        SecurityHandler security_handler = client.GetServer().GetSecurityHandler();
        
        SecretMessage secret_message = security_handler.PrepareEncryption(this, client.GetRecipient(), client.GetNbMessagesSent());
        secret_message.setUnix_time(Utility.getUnixTimeBytes());
        
        client.GetChatReplica().AddMessage(buffer, secret_message.getUnix_time());

        return secret_message;
    }

    @Override
    public byte[] GetPlainBytes() {
        return file.GetContent();
    }

    @Override
    public byte[] HandleMessage(Client client, SecretMessage enc_obj) {
        FileMessage message = (FileMessage)enc_obj;
        
        byte[] received_messaged_id = null;
        
        try {
            received_messaged_id = Utility.GetMessageID(message.GetPlainBytes(), unix_time);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException ex) {
            Logger.getLogger(FileMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        client.GetServer().handleNewFile(message.GetFile());
        
        return received_messaged_id;
    }


}
