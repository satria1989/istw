/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package id.co.itasoft.istw;

/**
 *
 * @author asani
 */
public class MyConfig {
   private String urlMinio;
   private String userName;
   private String password;
   private String bucket;

    @Override
    public String toString() {
        return "MyConfig{" + "urlMinio=" + urlMinio + ", userName=" + userName + ", password=" + password + ", bucket=" + bucket + '}';
    }

   
    public MyConfig(String urlMinio, String userName, String password, String bucket) {
        this.urlMinio = urlMinio;
        this.userName = userName;
        this.password = password;
        this.bucket = bucket;
    }

   
    public String getUrlMinio() {
        return urlMinio;
    }

    public void setUrlMinio(String urlMinio) {
        this.urlMinio = urlMinio;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
   
}
