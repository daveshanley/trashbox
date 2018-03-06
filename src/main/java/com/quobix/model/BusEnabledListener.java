package com.quobix.model;

import com.vmware.bifrost.bus.MessagebusService;

import java.util.UUID;

public abstract class BusEnabledListener {

    protected MessagebusService bus;
    protected int channel;
    protected UUID id;

    public BusEnabledListener(MessagebusService bus, int channel, UUID id) {
        this.bus = bus;
        this.channel = channel;
        this.id = id;
    }

}
