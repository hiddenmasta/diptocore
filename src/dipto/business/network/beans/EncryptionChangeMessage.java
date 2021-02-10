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
public class EncryptionChangeMessage extends PlainMessage{
    public EncryptionChangeMessage(){
    }

    @Override
    public Serializable GetMessage() {
        return new EncryptionChangeMessage();
    }

    @Override
    public void HandleMessage(Client client) {
        client.PrepareForEncryptionChange();
    }
}
