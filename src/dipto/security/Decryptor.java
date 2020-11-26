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
import confidentiality.Decryption;
import dipto.business.network.beans.SecretMessage;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 *
 * @author anon
 */
public class Decryptor {
    public Decryptor(){}
    
    public Serializable decrypt(SecretMessage message, SecretKey cipher1_key, SecretKey cipher2_key, SecretKey cipher3_key) throws 
    NoSuchAlgorithmException, 
    NoSuchProviderException, 
    InvalidKeyException, 
    NoSuchPaddingException, 
    InvalidAlgorithmParameterException, 
    IllegalBlockSizeException, 
    IllegalBlockSizeException, 
    BadPaddingException, 
    InvalidInitializationVectorException, 
    UnsupportedEncodingException, 
    InvalidCipherNumberException, 
    IOException, 
    ClassNotFoundException 
    {
        ArrayList<SecretKey> sym_keys = new ArrayList<>();
        sym_keys.add(cipher1_key);
        sym_keys.add(cipher2_key);
        sym_keys.add(cipher3_key);
       
        //byte[] plain_bytes = Decryption.multipleSymDec(3, payload, sym_algs, sym_keys, message.getIv_parameter());
        
        return (String)Decryption.sealedObjectMultipleDec(message.getEnc_payload(), sym_keys);
    }
}
