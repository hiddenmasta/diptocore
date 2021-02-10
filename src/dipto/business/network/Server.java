/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network;

import dipto.business.network.tcpip.ServerTCP;
import dipto.business.network.tcpip.ClientTCP;
import dipto.Utility;
import dipto.business.network.beans.CustomFile;
import dipto.business.network.beans.FileMessage;
import dipto.business.network.beans.PendingChatRequest;
import dipto.business.network.beans.SecretMessage;
import dipto.security.SecurityHandler;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import java.io.File;

/**
 *
 * @author anon
 */
public abstract class Server implements ListChangeListener<Object>, IMessageReceived, IPeerLeftConversation, IFileReceived, IDeleteMessageTimerElapsed{
    protected ObservableList<PendingChatRequest> pending_requests;
    private String hashed_user_id;
    private ObservableList<String> recipients;
    protected String local_name;
    protected String global_name;
    public List<Client> client_threads;
    protected IGUIListener GUIListener;
    private Thread thread_connection_checking;
    protected Thread thread_wait_for_request;
    protected final SecurityHandler security_handler;
    
    public Server(IGUIListener GUIListener){
        pending_requests = FXCollections.observableArrayList();
        recipients = FXCollections.observableArrayList();
        client_threads = new CopyOnWriteArrayList<>();
        this.GUIListener = GUIListener;
        pending_requests.addListener((ListChangeListener<? super PendingChatRequest>) this);
        recipients.addListener((ListChangeListener<? super String>) this);
        this.security_handler = new SecurityHandler();
    }
    
    protected abstract void setLocalName() throws IOException;
    protected abstract void initiateConnexionToRecipient(String plain_recipient);
    protected abstract Thread CreateWaitingThread();
    
    public String GetLastRecipient(){
        return recipients.get(recipients.size()-1);
    }
    
    public boolean IsThereRecipient(String recipient){
        return recipients.contains(recipient);
    }
    
    public void RemoveRecipient(String recipient){
        recipients.remove(recipient);
    }
    
    public void AddRecipient(String recipientID){
        recipients.add(recipientID);
    }
    
    public void AddClientChild(Client child){
        client_threads.add(child);
    }
    
    public void Start(){
        security_handler.generateKeys();
        checkInternetConnection();
        thread_wait_for_request.start();
    }
    
    public void stop(){
        thread_connection_checking.interrupt();
        
        try {
            thread_connection_checking.join();
        } catch (InterruptedException ex) {}
        
        thread_wait_for_request.interrupt();
        
        try {
            thread_wait_for_request.join();
        } catch (InterruptedException ex) {}
        
        client_threads.forEach((Client thread) -> {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(ServerTCP.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    
    public String getLocalIpAndPort()
    {
        return local_name;
    }
    
    public String getGlobalIpAndPort(){
        return global_name;
    }
    
    public String getObfuscatedID(){
        try {
            if(hashed_user_id == null) 
                hashed_user_id = Utility.GetUserID(local_name);
                
            return hashed_user_id;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | 
                NoSuchProviderException ex) {
            Logger.getLogger(ServerTCP.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    public SecurityHandler GetSecurityHandler(){
        return security_handler;
    }
    
    public void checkInternetConnection(){
        thread_connection_checking = new InternetCheckThread(GUIListener);
        thread_connection_checking.start();
    }

    public void sendMessage(String recipient, Serializable message, int timerBeforeDeletion){
        Optional<Client> thread_cible = client_threads.stream().filter(csmr -> csmr.hashed_recipient_id.equals(recipient)).findFirst();
        
        if(thread_cible.isPresent())
            thread_cible.get().addOutGoingMessage(message, timerBeforeDeletion);
    }
    
    @Override
    public void handleNewMessage(SecretMessage message) {
        GUIListener.handleNewMessage(message);
    }

    @Override
    public void handlePeerLeftConversation(String recipient_id) {
        GUIListener.handlePeerLeftConversation(recipient_id);
    }

    @Override
    public void handleNewFile(CustomFile file) {
        GUIListener.handleNewFile(file);
    }

    @Override
    public void handleDeleteMessageTimerElapsed(int lineNumber, String userID) {
        GUIListener.handleDeleteMessageTimerElapsed(lineNumber, userID);
    }
}
