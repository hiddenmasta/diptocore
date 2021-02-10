/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.security.beans;

import java.security.KeyPair;

/**
 *
 * @author anon
 */
public class TemporaryKeysForDH {
    private final KeyPair half_hmac_key;
    private final KeyPair half_cipher1_key;
    private final KeyPair half_cipher2_key;
    private final KeyPair half_cipher3_key;
    
    public TemporaryKeysForDH(KeyPair half_hmac_key, 
                         KeyPair half_cipher1_key,
                         KeyPair half_cipher2_key,
                         KeyPair half_cipher3_key){
        this.half_hmac_key = half_hmac_key;
        this.half_cipher1_key = half_cipher1_key;
        this.half_cipher2_key = half_cipher2_key;
        this.half_cipher3_key = half_cipher3_key;
    }

    public KeyPair getHalf_hmac_key() {
        return half_hmac_key;
    }

    public KeyPair getHalf_cipher1_key() {
        return half_cipher1_key;
    }

    public KeyPair getHalf_cipher2_key() {
        return half_cipher2_key;
    }

    public KeyPair getHalf_cipher3_key() {
        return half_cipher3_key;
    }
}
