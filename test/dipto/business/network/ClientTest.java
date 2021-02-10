/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network;

import dipto.business.network.tcpip.ClientTCP;
import java.io.Serializable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author anon
 */
public class ClientTest {
    
    public ClientTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of respondToChatRequest method, of class Client.
     */
    @Test
    public void testRespondToChatRequest() {
        System.out.println("respondToChatRequest");
        boolean positive_response = false;
        ClientTCP instance = null;
        instance.respondToChatRequest(positive_response);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of addOutGoingMessage method, of class Client.
     */
    @Test
    public void testAddOutGoingMessage() {
        System.out.println("addOutGoingMessage");
        Serializable message = null;
        int[] timerBeforeDeletion = null;
        ClientTCP instance = null;
        instance.addOutGoingMessage(message, timerBeforeDeletion);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of initiateChatRequest method, of class Client.
     */
    @Test
    public void testInitiateChatRequest() {
        System.out.println("initiateChatRequest");
        ClientTCP instance = null;
        instance.initiateChatRequest();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of run method, of class Client.
     */
    @Test
    public void testRun() {
        System.out.println("run");
        ClientTCP instance = null;
        instance.run();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of terminate method, of class Client.
     */
    @Test
    public void testTerminate() {
        System.out.println("terminate");
        ClientTCP instance = null;
        instance.terminate();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
