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

import dipto.business.network.beans.DiscardedMessage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anon
 */
public class MessagesEncapsulator {
    private final ArrayList<ArrayList<Serializable>> messages;
    private Thread discardedMessageThread;
    private Timer timer;
    
    public MessagesEncapsulator(){
        messages = new ArrayList<>();
    }
    
    public void addMessage(Serializable message, int timerBeforeDeletion){
        ArrayList<Serializable> temp = new ArrayList<>();
        temp.add(message);
        
        if(timerBeforeDeletion > 0)
            temp.add(new Integer(timerBeforeDeletion));

        messages.add(temp);
        NotifySelf();
    }
    
    public synchronized ArrayList<Serializable> getLastPendingMessage() throws InterruptedException{
        while(messages.isEmpty() == true)
            wait();
            
        return messages.remove(messages.size()-1);    
    }
    
    public void PeriodicallyCheckMessages(){
        timer = new Timer();
        
        discardedMessageThread = new Thread(){
            @Override
            public void run(){
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if(messages.isEmpty() == true)
                            addMessage(new DiscardedMessage(), 0);                        
                        
                        NotifySelf();
                    }
                }, 0, 2 * 1000);
            }
        };
        
        discardedMessageThread.start();
    }
    
    public synchronized void PauseMessageChecking(){
        try {
            timer.wait();
        } catch (InterruptedException ex) {
            Logger.getLogger(MessagesEncapsulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public synchronized void UnpauseMessageChecking(){
        timer.notify();
    }
    
    public void StopMessageChecking(){
        timer.cancel();
        discardedMessageThread.interrupt();
        
        try {
            discardedMessageThread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(MessagesEncapsulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private synchronized void NotifySelf(){
        notify();
    }
}
