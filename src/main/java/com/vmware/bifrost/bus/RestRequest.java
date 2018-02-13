package com.vmware.bifrost.bus;

import java.net.URI;

/*
 * Copyright(c) VMware Inc. 2018
 */
public class RestRequest<PayloadT> {

    public static enum Type {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE
    }

    private Type type;
    private URI URI;
    private PayloadT payload;

    public RestRequest(Type type, URI URI, PayloadT payload) {
        this.type = type;
        this.URI = URI;
        this.payload = payload;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public java.net.URI getURI() {
        return URI;
    }

    public void setURI(java.net.URI URI) {
        this.URI = URI;
    }

    public PayloadT getPayload() {
        return payload;
    }

    public void setPayload(PayloadT payload) {
        this.payload = payload;
    }
}
