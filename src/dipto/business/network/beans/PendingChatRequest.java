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
package dipto.business.network.beans;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author anon
 */
public class PendingChatRequest {
    private final String recipient;
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;

    public PendingChatRequest(String recipient, ObjectOutputStream oos, ObjectInputStream ois) {
        this.recipient = recipient;
        this.oos = oos;
        this.ois = ois;
    }

    public String getRecipient() {
        return recipient;
    }

    public ObjectOutputStream getOos() {
        return oos;
    }

    public ObjectInputStream getOis() {
        return ois;
    }
}
