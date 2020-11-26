/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network;

import dipto.business.network.beans.FileMessage;
import dipto.business.network.beans.SecretMessage;

/**
 *
 * @author anon
 */
public interface IGUIListener {
    void handleNewMessage(SecretMessage message);
    void handleConnectionStateChanged(boolean is_connected);
    void handleNewRecipient(String recipient_id);
    void handlePeerLeftConversation(String recipient_id);
    void handleFileReceived(FileMessage file);
    void handleDeleteMessageTimerElapsed(int lineNumber, String userID);
}
