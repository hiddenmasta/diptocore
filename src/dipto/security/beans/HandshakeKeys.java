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
package dipto.security.beans;

import java.io.Serializable;
import java.security.SignedObject;

/**
 *
 * @author anon
 */
public class HandshakeKeys implements Serializable{
    private final SignedObject half_cipher1_key;
    private final SignedObject half_cipher2_key;
    private final SignedObject half_cipher3_key;
    private final SignedObject half_hmac_key;

    public HandshakeKeys(SignedObject half_cipher1_key, SignedObject half_cipher2_key, SignedObject half_cipher3_key, SignedObject half_hmac_key) {
        this.half_cipher1_key = half_cipher1_key;
        this.half_cipher2_key = half_cipher2_key;
        this.half_cipher3_key = half_cipher3_key;
        this.half_hmac_key = half_hmac_key;
    }

    public SignedObject getHalf_cipher1_key() {
        return half_cipher1_key;
    }

    public SignedObject getHalf_cipher2_key() {
        return half_cipher2_key;
    }

    public SignedObject getHalf_cipher3_key() {
        return half_cipher3_key;
    }
    
        public SignedObject getHalf_hmac_key() {
        return half_hmac_key;
    }
}
