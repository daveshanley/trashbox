package com.quobix;

import com.vmware.bifrost.bus.MessagebusService;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.HttpURLConnection;
import java.net.URL;


public class BeeperService {

    private MessagebusService bus;
    URL url;
    HttpURLConnection connection;

    private String testPayload;

    public BeeperService(MessagebusService bus) throws Exception {
        this.bus = bus;

    }

    public void findDavesPhone() throws Exception {
        System.out.println("Finding Daves Phone");
        testPayload = "{\"apple_id\": \"pop\",\"password\":\"chop\", \"name\":\"DavePhoneX\",\"message\":\"Beep Beep!\"}";
        url = new URL("https://requestb.in/1jzdjqx1");

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://localhost:9443/beep");
        httpPost.setHeader("Content-Type", "application/json");
        StringEntity entity = new StringEntity(testPayload);
        httpPost.setEntity(entity);

        CloseableHttpResponse response = client.execute(httpPost);
        client.close();
    }


}
