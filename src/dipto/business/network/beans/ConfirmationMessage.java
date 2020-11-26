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
public class ConfirmationMessage implements Serializable{
    private final byte [] message_ack;
    
    public ConfirmationMessage(byte [] message_ack){
        this.message_ack = message_ack;
    }

    public byte[] getMessageID() {
        return message_ack;
    }
}
