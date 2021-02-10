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
package dipto.security;

import Exceptions.InvalidCipherNumberException;
import Exceptions.InvalidInitializationVectorException;
import Exceptions.InvalidKeyTypeException;
import algos.CryptoProprieties;
import authenticity.SignatureGen;
import confidentiality.Encryption;
import dipto.Utility;
import dipto.business.network.beans.FileMessage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import dipto.business.network.beans.SecretMessage;
import dipto.business.network.beans.StringMessage;
import java.io.File;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anon
 */
public class Encryptor {
    public Encryptor(){}
    
    public SecretMessage encryptThenMAC(Serializable message, SecretKey cipher1_key, SecretKey cipher2_key, SecretKey cipher3_key, SecretKey hmac_key, int message_order) throws 
    UnsupportedEncodingException, 
    NoSuchAlgorithmException, 
    NoSuchProviderException, 
    NoSuchPaddingException, 
    InvalidAlgorithmParameterException, 
    InvalidKeyException,
    IllegalBlockSizeException, 
    BadPaddingException, 
    InvalidInitializationVectorException, 
    InvalidCipherNumberException,
    InvalidKeyTypeException,
    IOException{
        
        byte[] iv_parameter = ByteBuffer.allocate(16).putInt(message_order).array();
        
        ArrayList<String> sym_algs = new ArrayList<>();
        sym_algs.add(CryptoProprieties.RECURSIVE_SYM_ALG1);
        sym_algs.add(CryptoProprieties.RECURSIVE_SYM_ALG2);
        sym_algs.add(CryptoProprieties.RECURSIVE_SYM_ALG3);
        
        List<SecretKey> sym_keys = new ArrayList<>();
        sym_keys.add(cipher1_key);
        sym_keys.add(cipher2_key);
        sym_keys.add(cipher3_key);
        
        SealedObject sealed_obj = Encryption.sealedObjectMultipleEnc(3, message, sym_algs, sym_keys, iv_parameter);
        
        SecretMessage message_to_send = null;
        
        try {
            message_to_send = (SecretMessage)Class.forName(message.getClass().getName()).newInstance();
        } catch (ClassNotFoundException | InstantiationException | 
                IllegalAccessException ex) {
            Logger.getLogger(Encryptor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        /*if(message instanceof StringMessage)
            message_to_send = new StringMessage();
        else if(message instanceof FileMessage)
            message_to_send = new FileMessage();*/
        
        message_to_send.setEnc_payload(sealed_obj);
        message_to_send.setSignature(SignatureGen.symSign(Utility.objectToByteArray(sealed_obj), CryptoProprieties.HMAC_ALG, hmac_key));
        message_to_send.setIv_parameter(iv_parameter);
        
        return message_to_send;
    }
}
