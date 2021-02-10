/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network.beans;

import java.io.Serializable;

/**
 *
 * @author anon
 */
public class CustomFile implements Serializable{
    private final String file_name;
    private final byte[] content;
    
    public CustomFile(String file_name, byte[] content){
        this.content = content;
        this.file_name = file_name;
    }
    
    public String getFile_name() {
        return file_name;
    }
    
    public byte[] GetContent(){
        return content;
    }
}
