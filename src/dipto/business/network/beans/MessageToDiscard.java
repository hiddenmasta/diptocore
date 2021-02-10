/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network.beans;

import dipto.business.network.Client;
import java.io.Serializable;

/**
 *
 * @author anon
 */
public class MessageToDiscard extends PlainMessage{
    public MessageToDiscard(){
    }

    @Override
    public Serializable GetMessage() {
        return new MessageToDiscard();
    }

    @Override
    public void HandleMessage(Client client) {}
}
