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
public abstract class PlainMessage implements Serializable{
    public abstract Serializable GetMessage();
    public abstract void HandleMessage(Client client);
}
