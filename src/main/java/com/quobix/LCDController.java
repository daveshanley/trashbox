package com.quobix;

import com.phidget22.*;
import com.vmware.bifrost.bus.MessagebusService;

public class LCDController {

    public LCD phidget;
    MessagebusService bus;

    public LCDController(MessagebusService bus) throws Exception {
        this.bus = bus;
        phidget = new LCD();
        this.registerListeners();
    }

    private void registerListeners() throws Exception {
        phidget.addAttachListener(new AttachListener() {
            public void onAttach(AttachEvent ae) {
                LCD phid = (LCD) ae.getSource();
                try {
                    if (ae.getSource().getDeviceID() == DeviceID.PN_1204) {
                        phid.setScreenSize(LCDScreenSize.DIMENSIONS_4X40);
                        phid.setBacklight(1.0);
                        phid.setContrast(0.5);
                        bus.sendResponse("lcd-ready", true);
                    }
                } catch (PhidgetException ex) {
                    System.out.println(ex.getDescription());
                }
            }
        });
    }

    public void initScreen() throws Exception {
        phidget.setDeviceSerialNumber(329585);
        phidget.setChannel(0);
        Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);
        phidget.setIsRemote(true);
        System.out.println("Connecting to LCD.");
        phidget.open(5000);
    }

    public void writeTextLine(int line, int startChar, String text) throws PhidgetException {
        phidget.writeText(LCDFont.DIMENSIONS_5X8, startChar, line, text);
        phidget.flush();
    }
}
