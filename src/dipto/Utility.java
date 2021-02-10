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
package dipto;

import algos.CryptoProprieties;
import integrity.HashGen;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Base64;
import static java.util.UUID.randomUUID;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author anon
 */
public abstract class Utility {
    public static String GetUserID(String recipient) throws 
    UnsupportedEncodingException, 
    NoSuchAlgorithmException, 
    NoSuchProviderException{
        ArrayList<byte[]> bytes = new ArrayList<>();
        byte[] recipient_hash = null;

        bytes.add(recipient.getBytes("UTF-8"));
        recipient_hash = HashGen.getHash(bytes, CryptoProprieties.HASH_ALG);

        return recipient_hash.toString().substring(recipient_hash.toString().length()-8);
    }
    
    public static byte [] GetMessageID(byte[] message, byte [] time) 
    throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException, IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ArrayList<byte[]> hash_inputs = new ArrayList<>();
        hash_inputs.add(message);

        baos.write(HashGen.getHash(hash_inputs, CryptoProprieties.HASH_ALG));
        baos.write(time);
        
        return baos.toByteArray();
    }
    
    public static void CopyTextToClipboard(String text){
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = defaultToolkit.getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }
    
    public static String getIpFromId(String id){
        return (id.split("\\:"))[0];
    }
    
    public static int getPortFromId(String id){
        return Integer.parseInt( (id.split("\\:"))[1] );
    }
    
    public static byte [] getUnixTimeBytes(){
        int unix_time = (int)(System.currentTimeMillis() / 1000);
        
        return new byte[]{
                (byte) (unix_time >> 24),
                (byte) (unix_time >> 16),
                (byte) (unix_time >> 8),
                (byte) unix_time
            };
    }
    
    public static byte [] objectToByteArray(Object obj){
        byte[] output = null;
        
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos)){
            oos.writeObject(obj);
            output = baos.toByteArray();
        } catch (IOException ex) {
            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return output;
    }
    
    public static String WipeText(){
        return randomUUID().toString();
    }
 
    public static String GetBase64FromBytes(byte[] bytes){
        return new String(Base64.getEncoder().encode(bytes));
    }
    
    public static String GetBase64MessageID(byte[] message, byte [] time){
        try {
            return GetBase64FromBytes(GetMessageID(message, time));
        } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException ex) {
            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
    public static boolean checkRecipientIntegrity(String id) {
        String[] array1 = id.split("\\:");
        
        if(array1.length != 2)
            return false;
        
        String[] array2 = array1[0].split("\\.");
        
        if(array2.length != 4)
            return false;
        
        for (String string : array2) {
            if(!isNumeric(string))
                return false;
            
            int nb = Integer.parseInt(string);
            
            if( !(nb >= 0 && nb <= 255) )
                return false;
        }
        
        if(!isNumeric(array1[1]))
            return false;
        
        int nb = Integer.parseInt(array1[1]);
            
        return nb >= 50000;
    }
    
    public static boolean isNumeric(String str)
    {
        for (char c : str.toCharArray())
        {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }
    
    public static String GetWANIpAddress(){
        BufferedReader input_aws = null;
        BufferedReader input_ipify = null;
        
        String ip_aws = "No Internet Access";
        String ip_ipify = "No Internet Access";
        
        try {
            input_aws = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()));
            input_ipify = new BufferedReader(new InputStreamReader(new URL("https://api.ipify.org").openStream()));
            
            ip_aws = input_aws.readLine();
            ip_ipify = input_ipify.readLine();
            
            input_aws.close();
            input_ipify.close();
        } catch (IOException ex) {
            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if(input_aws != null)
                    input_aws.close();
                if(input_ipify != null)
                    input_ipify.close();
            } catch (IOException ex) {
                Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return !ip_aws.equals("No Internet Access") ? ip_aws : 
               (!ip_ipify.equals("No Internet Access") ? ip_ipify : "No Internet Access");
    }
}
