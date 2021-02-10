/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network;

import dipto.Utility;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author anon
 */
public class InternetCheckThread extends Thread{
    private final IGUIListener listener;
    
    public InternetCheckThread(IGUIListener listener){
        this.listener = listener;
    }
    
    @Override
    public void run(){
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                listener.handleConnectionStateChange(!Utility.GetWANIpAddress().equals("No Internet Access"));
            }
        }, 0, 1000 * 30);
    }
}
