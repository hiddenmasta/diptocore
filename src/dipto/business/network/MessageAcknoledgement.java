/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network;

import dipto.Utility;
import dipto.business.network.exceptions.TooMuchUnacknowledgedMessagesException;
import dipto.business.network.exceptions.WrongConfirmationMessageException;
import dipto.business.network.exceptions.WrongMessageOrderException;
import java.util.ArrayList;

/**
 *
 * @author anon
 */
public class MessageAcknoledgement {
    private int last_message_order;
    private final ArrayList<String> sent_messages;
    private final Integer max_size_sliding_window;
    
    public MessageAcknoledgement(){
        this.sent_messages = new ArrayList<>();
        this.last_message_order = 0;
        this.max_size_sliding_window = 5;
    }
    
    public void addSentMessage(byte[] message, byte[] unix_time) throws TooMuchUnacknowledgedMessagesException{
        if(sent_messages.size() == max_size_sliding_window){
            throw new TooMuchUnacknowledgedMessagesException();
        } else{
            sent_messages.add(Utility.GetBase64MessageID(message, unix_time));
        }
    }
    
    public void checkReceivedMessageOrder(int message_order) throws WrongMessageOrderException{
        if(message_order != ++last_message_order){
            throw new WrongMessageOrderException();
        }
    }
    
    public void checkConfirmationMessageExists(byte[] message_id) throws WrongConfirmationMessageException{
        if(!sent_messages.remove(Utility.GetBase64FromBytes(message_id)))
            throw new WrongConfirmationMessageException();
    }
}
