/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network;

/**
 *
 * @author anon
 */
public interface IDeleteMessageTimerElapsed {
    void handleDeleteMessageTimerElapsed(int lineNumber, String userID);
}
