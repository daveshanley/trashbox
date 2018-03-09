package com.quobix;

import com.phidget22.*;
import com.quobix.model.BusEnabledListener;
import com.quobix.model.LEDCommand;
import com.vmware.bifrost.bus.MessagebusService;
import com.vmware.bifrost.bus.model.Message;

import java.util.UUID;

class LEDListener extends BusEnabledListener implements AttachListener {

    private boolean isButton;

    public LEDListener(MessagebusService bus, int channel, UUID id, boolean isButton) {
        super(bus, channel, id);
        this.isButton = isButton;
    }

    @Override
    public void onAttach(AttachEvent attachEvent) {
        DigitalOutput phid = (DigitalOutput) attachEvent.getSource();
        try {
            if (phid.getDeviceClass() != DeviceClass.VINT) {
                System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " attached");
            } else {
                System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " hub port " + phid.getHubPort() + " attached with ID: " + this.id);
            }

            this.bus.listenStream("led-control",
                    (Message msg) -> {
                        LEDCommand command = (LEDCommand) msg.getPayload();
                        boolean executeCommand = false;
                        if (command.getChannel() == this.channel) {
                            executeCommand = true;
                        } else if (command.isBroadcast()) {
                            if (!command.isIgnoreButton()) {
                                executeCommand = true;
                            } else if (!this.isButton) {
                                executeCommand = true;
                            }

                        }
                        if (executeCommand) {
                            this.handleState(phid, command);
                        }
                    }
            );

            if (this.isButton) {
                this.bus.listenStream("led-control-button",
                        (Message msg) -> {
                            LEDCommand command = (LEDCommand) msg.getPayload();
                            this.handleState(phid, command);
                        }
                );
            }

            bus.sendResponse("led-ready-announce-" + this.id.toString(), phid.getChannel());
        } catch (PhidgetException ex) {
            System.out.println(ex.getDescription());
        }
    }

    private void handleState(DigitalOutput output, LEDCommand command) throws Exception{
        switch (command.getType()) {
            case ON:
                output.setState(true);
                break;
            case OFF:
                output.setState(false);
                break;
        }
    }
}

public class LEDController {

    MessagebusService bus;
    DigitalOutput phidget;
    int serial;
    int channel;
    int hubPort;
    boolean isButton = false;
    UUID id;

    public LEDController(MessagebusService bus, int serial, int channel, int hubPort, UUID id, boolean isButton) throws Exception {
        phidget = new DigitalOutput();
        this.bus = bus;
        this.serial = serial;
        this.channel = channel;
        this.hubPort = hubPort;
        this.isButton = isButton;
        this.id = id;
        this.registerListeners();
        this.initController();
    }

    private void registerListeners() throws Exception {
        phidget.addAttachListener(new LEDListener(this.bus, this.channel, this.id, this.isButton));
    }

    public void initController() throws Exception {
        this.phidget.setDeviceSerialNumber(serial);
        //Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);
        //this.lcdOne.setIsRemote(true);
        this.phidget.setChannel(channel);
        this.phidget.setHubPort(hubPort);
    }

    public void connect() {
        String op = "error connecting on channel " + this.channel;
        try {
            this.phidget.open();
        } catch (Exception e) {
            System.out.println(op);
            e.printStackTrace();
        }

    }

    public void on() throws Exception {
        phidget.setState(true);
    }

    public void off() throws Exception {
        //lcdOne.setChannel(channel);
        phidget.setState(false);
    }

    public void close() throws Exception {
        // lcdOne.close();
    }

}
