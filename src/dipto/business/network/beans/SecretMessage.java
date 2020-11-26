/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dipto.business.network.beans;

import java.io.Serializable;
import javax.crypto.SealedObject;

/**
 *
 * @author anon
 */
public class SecretMessage implements Serializable{
    protected byte[] payload;
    
    private String sender;
    private SealedObject enc_payload;
    private byte[] signature;
    private byte[] iv_parameter;
    private String plain_message;
    private byte [] unix_time;
    private int secondsBeforeDeletion;
    
    public SecretMessage(){
    }

    public SecretMessage(String sender, SealedObject plain_message, byte [] unix_time){
        super();
        this.sender = sender;
        this.enc_payload = plain_message;
        this.unix_time = unix_time;
    }

    public int getSecondsBeforeDeletion() {
        return secondsBeforeDeletion;
    }

    public void setSecondsBeforeDeletion(int secondsBeforeDeletion) {
        this.secondsBeforeDeletion = secondsBeforeDeletion;
    }

    public String getSender() {
        return sender;
    }

    public byte[] getPayload() {
        return payload;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getIv_parameter() {
        return iv_parameter;
    }
    
    public String getPlainMessage(){
        return plain_message;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public void setIv_parameter(byte[] iv_parameter) {
        this.iv_parameter = iv_parameter;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setPlain_message(String plain_message) {
        this.plain_message = plain_message;
    }

    public void setUnix_time(byte[] unix_time) {
        this.unix_time = unix_time;
    }

    public SealedObject getEnc_payload() {
        return enc_payload;
    }

    public void setEnc_payload(SealedObject enc_payload) {
        this.enc_payload = enc_payload;
    }

    public byte[] getUnix_time() {
        return unix_time;
    }
}
