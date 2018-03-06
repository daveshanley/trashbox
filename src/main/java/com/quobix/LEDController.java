package com.quobix;

import com.phidget22.*;
import com.quobix.model.BusEnabledListener;
import com.quobix.model.LEDCommand;
import com.vmware.bifrost.bus.MessagebusService;
import com.vmware.bifrost.bus.model.Message;

import java.util.UUID;

class LEDListener extends BusEnabledListener implements AttachListener {

    public LEDListener(MessagebusService bus, int channel, UUID id) {
        super(bus, channel, id);
    }

    @Override
    public void onAttach(AttachEvent attachEvent) {
        DigitalOutput phid = (DigitalOutput) attachEvent.getSource();
        try {
            if(phid.getDeviceClass() != DeviceClass.VINT){
                System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " attached");
            }
            else{
                System.out.println("channel " + phid.getChannel() + " on device " + phid.getDeviceSerialNumber() + " hub port " + phid.getHubPort() + " attached with ID: " + this.id);
            }

            this.bus.listenStream("led-control",
                    (Message msg) -> {
                        LEDCommand command = (LEDCommand)msg.getPayload();
                        boolean executeCommand = false;
                        if(command.getChannel() == this.channel){
                            executeCommand = true;
                        } else if(command.isBroadcast()) {
                            executeCommand = true;
                        }
                        if(executeCommand) {

                            switch(command.getType()) {
                                case ON:
                                    phid.setState(true);
                                    break;
                                case OFF:
                                    phid.setState(false);
                                    break;
                            }
                        }
                    }
            );

            bus.sendResponse("led-ready-announce-" + this.id.toString(), phid.getChannel());
        } catch (PhidgetException ex) {
            System.out.println(ex.getDescription());
        }
    }
}

public class LEDController {

    MessagebusService bus;
    DigitalOutput phidget;
    int serial;
    int channel;
    int hubPort;
    UUID id;

    public LEDController(MessagebusService bus, int serial, int channel, int hubPort, UUID id) throws Exception {
      phidget = new DigitalOutput();
      this.bus = bus;
      this.serial = serial;
      this.channel = channel;
      this.hubPort = hubPort;
      this.id = id;
      this.registerListeners();
      this.initController();
    }

    private void registerListeners() throws Exception {
        phidget.addAttachListener(new LEDListener(this.bus, this.channel, this.id));
    }

    public void initController() throws Exception {
        this.phidget.setDeviceSerialNumber(serial);
        //Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);
        //this.phidget.setIsRemote(true);
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
        //phidget.setChannel(channel);
        phidget.setState(false);
    }

    public void close() throws Exception {
       // phidget.close();
    }

}
