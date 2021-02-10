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

import dipto.business.network.beans.PendingChatRequest;
import dipto.Utility;
import dipto.business.network.IGUIListener;
import dipto.business.network.Server;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.NoSuchProviderException;
import java.util.List;

/**
 *
 * @author anon
 */
public class ServerTCP extends Server{
    private int port_ecoute;
    private ServerSocket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    
    public ServerTCP(IGUIListener message_listener)
    {
        super(message_listener);
        port_ecoute = 50000;
    }
    
    public void Start(){
        thread_wait_for_request = CreateWaitingThread();
        super.Start();
    }

    // TO MODIFY
    @Override
    public void initiateConnexionToRecipient(String plain_recipient){
        Server this_server = this;
        
        new Thread(){
            @Override
            public void run(){
            try {
                Socket socket_client = new Socket(Utility.getIpFromId(plain_recipient), Utility.getPortFromId(plain_recipient));

                ClientTCP client_thread = new ClientTCP(Utility.GetUserID(plain_recipient), 
                                                          this_server, 
                                                          new ObjectOutputStream(socket_client.getOutputStream()), 
                                                          new ObjectInputStream(socket_client.getInputStream()));
                client_thread.initiateChatRequest();
            } catch (IOException | NoSuchAlgorithmException | NoSuchProviderException ex) {
                Logger.getLogger(ServerTCP.class.getName()).log(Level.SEVERE, null, ex);
            }}
        }.start();
    }
    
    @Override
    protected Thread CreateWaitingThread(){
        return new Thread(){
            @Override
            public void run(){
                Socket socket_client;
                boolean bind_error = false;
                
                do
                {
                    try {
                        socket = new ServerSocket(port_ecoute);
                        setLocalName();

                        if(bind_error)
                            bind_error = false;
                    } catch (IOException ex){
                        if(ex instanceof BindException){
                            port_ecoute++;
                            bind_error = true;
                        }
                    }
                }while(bind_error);

                try{
                    while(!Thread.currentThread().isInterrupted()){
                        socket_client = socket.accept();

                        oos = new ObjectOutputStream(socket_client.getOutputStream());
                        ois = new ObjectInputStream(socket_client.getInputStream());

                       pending_requests.add(new PendingChatRequest(ois.readUTF(), oos, ois));
                    }
                } catch(IOException ex){
                    if(ex instanceof SocketException == false)
                        Logger.getLogger(ServerTCP.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
            
            @Override
            public void interrupt(){
                try {
                    if(ois != null)
                        ois.close();
                    if(oos != null)
                        oos.close();
                    if(socket != null)
                        socket.close();
                } catch (IOException ex) {
                    Logger.getLogger(ServerTCP.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
    }
    
    @Override
    protected void setLocalName() throws IOException
    {
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            
            local_name = socket.getLocalAddress().getHostAddress() + ":" + port_ecoute;
        } catch (SocketException | UnknownHostException ex) {
            Logger.getLogger(ServerTCP.class.getName()).log(Level.SEVERE, null, ex);
            local_name = InetAddress.getLocalHost().getHostAddress() + ":" + port_ecoute;
        }
    }
    
    @Override
    /* New chat request has been added */
    public void onChanged(Change<? extends Object> pendingRequest) {
        if(pendingRequest.next()){
            if(pendingRequest.wasAdded()){
                List<?>  added_items = pendingRequest.getAddedSubList();
                
                if(added_items.get(0) instanceof String)
                    GUIListener.handleNewRecipient((String) added_items.get(0));
                    
                else
                {
                    PendingChatRequest pending_request = (PendingChatRequest) added_items.get(0);
                    //System.out.println("The user " + pending_request.getRecipient() + " would like to start a conversation\nType 'y' to accept, 'N' to refuse: ");

                    String input = "y";
                    /*try(BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
                        input = br.readLine();
                    } catch (IOException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }*/

                    ClientTCP thread = new ClientTCP(pending_request.getRecipient(), this, pending_request.getOos(), pending_request.getOis());

                    if(input.equals("y"))
                        thread.respondToChatRequest(true);
                    else if(input.equals("N"))
                        thread.respondToChatRequest(false);

                    pending_requests.remove(pending_request);
                }
            }
        }
    }
}
