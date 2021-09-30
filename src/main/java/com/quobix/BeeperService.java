package com.quobix;

import com.vmware.bifrost.bus.MessagebusService;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class BeeperService {

    private MessagebusService bus;
    String davePhoneName;
    String michellePhoneName;
    String michelleId;
    String michellePass;
    String daveId;
    String davePass;
    String serviceUri;

    private String testPayload;

    public String getDavePhoneName() {
        return davePhoneName;
    }

    public void setDavePhoneName(String davePhoneName) {
        this.davePhoneName = davePhoneName;
    }

    public String getMichellePhoneName() {
        return michellePhoneName;
    }

    public void setMichellePhoneName(String michellePhoneName) {
        this.michellePhoneName = michellePhoneName;
    }

    public String getMichelleId() {
        return michelleId;
    }

    public void setMichelleId(String michelleId) {
        this.michelleId = michelleId;
    }

    public String getMichellePass() {
        return michellePass;
    }

    public void setMichellePass(String michellePass) {
        this.michellePass = michellePass;
    }

    public String getDaveId() {
        return daveId;
    }

    public void setDaveId(String daveId) {
        this.daveId = daveId;
    }

    public String getDavePass() {
        return davePass;
    }

    public void setDavePass(String davePass) {
        this.davePass = davePass;
    }

    public BeeperService(MessagebusService bus) throws Exception {
        this.bus = bus;
        this.serviceUri = "http://192.168.2.1:8080/beep";
    }

    private void postBeepRequest(String request) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(serviceUri);
        httpPost.setHeader("Content-Type", "application/json");
        StringEntity entity = new StringEntity(request);
        httpPost.setEntity(entity);
        client.execute(httpPost);
        client.close();
    }

    public void findDavesPhone() throws Exception {
        System.out.println("Finding Daves Phone");
        String payload = "{\"apple_id\": \"" + this.daveId + "\",\"password\":\"" + this.davePass + "\", \"name\":\"" + this.getDavePhoneName() + "\",\"message\":\"Beep Beep!\"}";
        this.postBeepRequest(payload);
    }

    public void findMichellesPhone() throws Exception {
        System.out.println("Finding Michelles Phone");
        String payload = "{\"apple_id\":\"" + this.michelleId + "\",\"password\":\"" + this.michellePass + "\",\"name\":\"" + this.getMichellePhoneName() + "\",\"message\":\"Beep Beep!\"}";
        this.postBeepRequest(payload);
    }


}
