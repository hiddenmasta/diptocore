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
package dipto.business.network;

import dipto.business.network.beans.PendingChatRequest;
import dipto.Utility;
import dipto.business.network.beans.FileMessage;
import dipto.business.network.beans.SecretMessage;
import dipto.business.network.exceptions.GlobalIpsDontMatchException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import dipto.security.KeyStorage;
import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.security.NoSuchProviderException;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author anon
 */
public class Server implements ListChangeListener<Object>, IMessageReceived, IPeerLeftConversation, IFileReceived, IDeleteMessageTimerElapsed{
    private Server this_client;
    private IGUIListener client_listener;
    private Thread thread_connectionChecking;
    private int port_ecoute_initial;
    private int port_ecoute;
    private ObservableList<PendingChatRequest> pending_requests;
    private ServerSocket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String hashed_user_id;
    private KeyStorage key_storage;
    private ObservableList<String> recipients;
    
    public String local_name;
    public String global_name;
    
    public List<Client> client_threads;
    public boolean isStopped;
    
    public static final String CIPHER1_KEY = "CIPHER_KEY";
    public static final String CIPHER2_KEY = "CIPHER2_KEY";
    public static final String CIPHER3_KEY = "CIPHER3_KEY";
    public static final String HMAC_KEY = "HMAC_KEY";

    public Server(IGUIListener message_listener)
    {
        pending_requests = FXCollections.observableArrayList();
        recipients = FXCollections.observableArrayList();
        client_threads = new CopyOnWriteArrayList<>();
        this.client_listener = message_listener;
        this_client = this;
        isStopped = false;
        pending_requests.addListener((ListChangeListener<? super PendingChatRequest>) this);
        recipients.addListener((ListChangeListener<? super String>) this);
        
        port_ecoute = port_ecoute_initial = 50000;
    }
    
    
    public String GetLastRecipient(){
        return recipients.get(recipients.size()-1);
    }
    
    public boolean IsThereRecipient(String recipient){
        return recipients.contains(recipient);
    }
    
    public void RemoveRecipient(String recipient){
        recipients.remove(recipient);
    }
    
    public KeyStorage GetKeyStore(){
        return key_storage;
    }
    
    public void AddRecipient(String recipientID){
        recipients.add(recipientID);
    }
    
    public void AddClientChild(Client child){
        client_threads.add(child);
    }
    
    public void generateKeys(){
        new Thread(){
            @Override
            public void run(){
                try {
                    key_storage = new KeyStorage();
                    System.out.println("fini");
                } catch (KeyStoreException | IOException |
                        NoSuchAlgorithmException | CertificateException |
                        InvalidKeyException | SignatureException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
    
    public String getObfuscatedID(){
        try {
            if(hashed_user_id == null) 
                hashed_user_id = Utility.GetUserID(local_name);
                
            return hashed_user_id;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | 
                NoSuchProviderException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }

    public void waitForIncomingRequests(){
        new Thread(){
            @Override
            public void run(){
                Socket socket_client;
                boolean bind_error = false;

                try{

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

                    while(!isStopped){
                        socket_client = socket.accept();

                        oos = new ObjectOutputStream(socket_client.getOutputStream());
                        ois = new ObjectInputStream(socket_client.getInputStream());

                        pending_requests.add(new PendingChatRequest(ois.readUTF(), oos, ois));
                    }
                } catch(IOException ex){
                    if(ex instanceof SocketException == false)
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
        }.start();
        
        checkInternetConnection();
    }
    
    private void setLocalName() throws 
    IOException
    {
        local_name = InetAddress.getLocalHost().getHostAddress() + ":" + port_ecoute;
    }
    
    public String getLocalIpAndPort()
    {
        return local_name;
    }
    
    public String getGlobalIpAndPort(){
        return global_name;
    }
    
    public void checkInternetConnection()
    {
        thread_connectionChecking = new Thread(){
            @Override
            public void run(){
                Timer timer = new Timer();
                
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        boolean isConnected = false;

                        try {
                            BufferedReader input_aws = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()));
                            BufferedReader input_ipify = new BufferedReader(new InputStreamReader(new URL("https://api.ipify.org").openStream()));
                            
                            String ip_aws = input_aws.readLine();
                            String ip_ipify = input_ipify.readLine();
                            
                            input_aws.close();
                            input_ipify.close();

                            if(!ip_aws.equals(ip_ipify))
                                throw new GlobalIpsDontMatchException();
                            
                            isConnected = true;

                            global_name = ip_aws + ":" + port_ecoute_initial;

                        } catch (GlobalIpsDontMatchException ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        } catch(IOException ex){
                            isConnected = false;
                        } finally{
                            client_listener.handleConnectionStateChanged(isConnected);
                        }
                    }
                }, 0, 1000 * 30);
            }
        };
        
        thread_connectionChecking.start();
    }

     public void initiateConnexionToRecipient(String plain_recipient){
        new Thread(){
            @Override
            public void run(){
            try {
                Socket socket_client = new Socket(Utility.getIpFromId(plain_recipient), Utility.getPortFromId(plain_recipient));

                Client client_thread = new Client(Utility.GetUserID(plain_recipient), 
                                                          this_client, 
                                                          new ObjectOutputStream(socket_client.getOutputStream()), 
                                                          new ObjectInputStream(socket_client.getInputStream()));
                client_thread.initiateChatRequest();
            } catch (IOException | NoSuchAlgorithmException | NoSuchProviderException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }}
        }.start();
    }

    public void sendMessage(String recipient, Serializable message, int timerBeforeDeletion){
        Optional<Client> thread_cible = client_threads.stream().filter(csmr -> csmr.hashed_recipient_id.equals(recipient)).findFirst();
        thread_cible.get().addOutGoingMessage(message, timerBeforeDeletion);
    }

    public void stop(){
        thread_connectionChecking.interrupt();
        
        try {
            thread_connectionChecking.join();
        } catch (InterruptedException ex) {}
        
        client_threads.forEach(thread -> {
            thread.terminate();
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        isStopped = true;
        
        try {
            if(ois != null)
                ois.close();
            if(oos != null)
                oos.close();
            if(socket != null)
                socket.close();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    /* New chat request has been added */
    public void onChanged(Change<? extends Object> pendingRequest) {
        if(pendingRequest.next()){
            if(pendingRequest.wasAdded()){
                List<?>  added_items = pendingRequest.getAddedSubList();
                
                if(added_items.get(0) instanceof String)
                    client_listener.handleNewRecipient((String) added_items.get(0));
                    
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

                    Client thread = new Client(pending_request.getRecipient(), this, pending_request.getOos(), pending_request.getOis());

                    if(input.equals("y"))
                        thread.respondToChatRequest(true);
                    else if(input.equals("N"))
                        thread.respondToChatRequest(false);

                    pending_requests.remove(pending_request);
                }
            }
        }
    }

    @Override
    public void handleNewMessage(SecretMessage message) {
        client_listener.handleNewMessage(message);
    }

    @Override
    public void handlePeerLeftConversation(String recipient_id) {
        client_listener.handlePeerLeftConversation(recipient_id);
    }

    @Override
    public void handleNewFile(FileMessage file) {
        client_listener.handleFileReceived(file);
    }

    @Override
    public void handleDeleteMessageTimerElapsed(int lineNumber, String userID) {
        client_listener.handleDeleteMessageTimerElapsed(lineNumber, userID);
    }
}
