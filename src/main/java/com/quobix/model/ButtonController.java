package com.quobix.model;

import com.phidget22.*;
import com.quobix.LEDController;
import com.vmware.bifrost.bus.MessagebusService;

import java.util.UUID;

class ButtonListener extends BusEnabledListener implements AttachListener, DigitalInputStateChangeListener {

    public ButtonListener(MessagebusService bus, int channel, UUID id) {
        super(bus, channel, id);
    }

    @Override
    public void onAttach(AttachEvent attachEvent) {
        DigitalInput input = (DigitalInput) attachEvent.getSource();
        try {

            System.out.println("button controller digi input channel " + input.getChannel() + " on device " + input.getDeviceSerialNumber() + " hub port " + input.getHubPort() + " attached with ID: " + this.id);


//            this.bus.listenStream("button-control",
//                    (Message msg) -> {
//                        LEDCommand command = (LEDCommand)msg.getPayload();
//                        boolean executeCommand = false;
//                        if(command.getChannel() == this.channel){
//                            executeCommand = true;
//                        } else if(command.isBroadcast()) {
//                            executeCommand = true;
//                        }
//                        if(executeCommand) {
//
//                            switch(command.getType()) {
//                                case ON:
//                                    phid.setState(true);
//                                    break;
//                                case OFF:
//                                    phid.setState(false);
//                                    break;
//                            }
//                        }
//                    }
//            );

            bus.sendResponse("button-ready", this.channel);
        } catch (PhidgetException ex) {
            System.out.println(ex.getDescription());
        }
    }

    @Override
    public void onStateChange(DigitalInputStateChangeEvent digitalInputStateChangeEvent) {
        if(digitalInputStateChangeEvent.getState()){
            this.bus.sendResponse("button-click-events", this.id);
        }
    }
}

public class ButtonController {

    private LEDController ledController;
    private boolean active = false;
    private MessagebusService bus;
    private DigitalInput input;
    private int channel;
    private int hub;
    private int serial;
    private UUID id;

    public ButtonController(MessagebusService bus, int hub, int channel, int serial, UUID id, LEDController ledController) throws Exception {
        this.bus = bus;
        this.input = new DigitalInput();
        this.hub = hub;
        this.channel = channel;
        this.serial = serial;
        this.id = id;
        this.ledController = ledController;
        this.registerListeners();
        this.initController();
    }

    private void registerListeners() throws Exception {
        ButtonListener listener = new ButtonListener(this.bus, this.channel, this.id);
        input.addAttachListener(listener);
        input.addStateChangeListener(listener);

    }

    public void initController() throws Exception {
        this.input.setDeviceSerialNumber(serial);
        //Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);
        //this.input.setIsRemote(true);
        this.input.setChannel(channel);
        this.input.setHubPort(hub);
    }

    public void connect() {
        String op = "error connecting on channel " + this.channel;
        try {
            this.input.open();
        } catch (Exception e) {
            System.out.println(op);
            e.printStackTrace();
        }
    }
}
