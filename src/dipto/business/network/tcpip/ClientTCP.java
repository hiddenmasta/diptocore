/*Dipto, a reasonably secure end-to-end desktop chat app built by the paranoid, for the paranoid
Copyright (C) 2018 Hiddenmaster

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.*/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network.tcpip;

import dipto.security.exceptions.InvalidSignatureException;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import dipto.Utility;
import dipto.business.network.Client;
import dipto.business.network.Server;
import dipto.business.network.beans.ConfirmationMessage;
import dipto.business.network.beans.PlainMessage;
import dipto.business.network.beans.SecretMessage;
import dipto.business.network.exceptions.WrongMessageOrderException;
import java.nio.ByteBuffer;


/**
 *
 * @author anon
 */
public class ClientTCP extends Client{ 
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;

    public ClientTCP(String recipient, Server server, ObjectOutputStream oos, ObjectInputStream ois){
        super(server, recipient);
        this.oos = oos;
        this.ois = ois;
    }
    
    private void HandlePossibleTermination(){
        if(!is_being_interrupted){
            PeerLeft(hashed_recipient_id); 
            this.interrupt();
        }
    }

    @Override
    public void run(){
        while(!is_receiving_thread_stopped){
            Object encrypted_obj;
            
            try {
                encrypted_obj = Read();
                
                if(encrypted_obj instanceof PlainMessage){
                    ((PlainMessage)encrypted_obj).HandleMessage(this);
                }else if(encrypted_obj instanceof SecretMessage){
                    server.GetSecurityHandler().VerifyMessageSignature((SecretMessage)encrypted_obj, hashed_recipient_id);
                    
                    int message_order = ByteBuffer.wrap(((SecretMessage)encrypted_obj).getIv_parameter()).getInt();
                    sliding_window.checkReceivedMessageOrder(message_order);
                    
                    Object unencrypted_obj = server.GetSecurityHandler().PrepareDecryption((SecretMessage)encrypted_obj, hashed_recipient_id);
                    
                    byte[] received_messaged_id = ((SecretMessage)encrypted_obj).HandleMessage(this, (SecretMessage)unencrypted_obj);
                
                    messages_container.addMessage(new ConfirmationMessage(received_messaged_id), 0);
                }
            } catch (InvalidSignatureException | WrongMessageOrderException ex) {
                Logger.getLogger(ClientTCP.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            encrypted_obj = Utility.WipeText();
        }
    }
    
    @Override
    public void interrupt(){
        try {
            is_being_interrupted = true;
            is_receiving_thread_stopped = true;
            
            if(oos != null)
                oos.close();
            if(ois != null)
                ois.close();
            
            Close();
        } catch (IOException ex) {
            Logger.getLogger(ClientTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public Object Read(){
        try {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            if(ex instanceof EOFException || ex instanceof SocketException){
                HandlePossibleTermination();
            }else
                Logger.getLogger(ClientTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    @Override
    public void Write(Object obj){
        try {
            if(obj instanceof String)
                oos.writeUTF((String)obj);
            else
                oos.writeObject(obj); 
            
            oos.flush();
        } catch (IOException ex) {
            if(ex instanceof EOFException || ex instanceof SocketException){
                HandlePossibleTermination();
            }else
                Logger.getLogger(ClientTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}


