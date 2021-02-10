/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network.beans;

import dipto.business.network.Client;
import dipto.business.network.exceptions.WrongConfirmationMessageException;
import dipto.business.network.tcpip.ClientTCP;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anon
 */
public class ConfirmationMessage extends PlainMessage{
    private final byte [] message_ack;
    
    public ConfirmationMessage(byte [] message_ack){
        this.message_ack = message_ack;
    }

    public byte[] getMessageID() {
        return message_ack;
    }

    @Override
    public Serializable GetMessage() {
        return new ConfirmationMessage(message_ack);
    }

    @Override
    public void HandleMessage(Client client) {
        try {
            client.GetAcknowledgmentHandler().checkConfirmationMessageExists(message_ack);
        } catch (WrongConfirmationMessageException ex) {
            Logger.getLogger(ClientTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
