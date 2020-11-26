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
package dipto.business.network;

import java.io.File;
import java.util.ArrayList;

/**
 *
 * @author anon
 */
public class FilesEncapsulator {
    private ArrayList<File> files;
    
    public FilesEncapsulator(){
        files = new ArrayList<>();
    }
    
    public synchronized void addMessage(File file){
        files.add(file);
        notify();
    }
    
    public synchronized File getMessage() throws InterruptedException{
        while(files.isEmpty() == true)
            wait();
        
        return files.remove(files.size()-1);
    }
}
