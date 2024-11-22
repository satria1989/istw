/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package id.co.itasoft.istw;

/**
 *
 * @author User
 */
public class MasterImageSignatureData {
    byte[] bytes;
    String fileName;

    public byte[] getBytes() {
        return bytes;
    }

    public String getFileName() {
        return fileName;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
}
