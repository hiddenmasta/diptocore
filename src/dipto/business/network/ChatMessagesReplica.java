/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network;

import dipto.Utility;
import java.util.ArrayList;

/**
 *
 * @author anon
 */
public class ChatMessagesReplica {
    private final ArrayList<String> messages;
    
    public ChatMessagesReplica(){
        this.messages = new ArrayList<>();
    }
    
    public String AddMessage(byte[] message, byte[] unix_time){
        String messageId = Utility.GetBase64MessageID(message, unix_time);
        
        messages.add(messageId);
        
        return messageId;
    }
    
    public int RemoveMessage(byte[] message, byte[] unix_time){
        int index = messages.indexOf(Utility.GetBase64MessageID(message, unix_time));
        messages.remove(index);
        return index;
    }
    
    public int RemoveMessage(String messageID){
        int index = messages.indexOf(messageID);
        messages.remove(index);
        return index;
    }
 
}
