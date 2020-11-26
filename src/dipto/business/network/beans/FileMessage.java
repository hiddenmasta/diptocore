/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network.beans;

/**
 *
 * @author anon
 */
public class FileMessage extends SecretMessage{
    private final String file_name;
    
    public FileMessage(String file_name, byte [] payload){
        this.file_name = file_name;
        this.payload = payload;
    }

    public String getFile_name() {
        return file_name;
    }

    public byte[] getPayload() {
        return payload;
    }
}
