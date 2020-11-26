/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network.beans;

import java.io.Serializable;

/**
 *
 * @author anon
 */
public class ChatRequest implements Serializable {
    private final String sender;
    
    public ChatRequest(String sender){
        this.sender = sender;
    }
    
    public String getSender(){
        return sender;
    }
}
