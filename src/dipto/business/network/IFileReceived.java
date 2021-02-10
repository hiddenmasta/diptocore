/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network;

import dipto.business.network.beans.CustomFile;

/**
 *
 * @author anon
 */
public interface IFileReceived {
    void handleNewFile(CustomFile file);
}
