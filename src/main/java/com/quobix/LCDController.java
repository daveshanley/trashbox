package com.quobix;

import com.phidget22.*;
import com.vmware.bifrost.bus.MessagebusService;
import com.vmware.bifrost.bus.model.Message;

public class LCDController {

    public LCD lcdOne;
    public LCD lcdTwo;

    MessagebusService bus;
    int count = 0;

    public LCDController(MessagebusService bus) throws Exception {
        this.bus = bus;
        lcdOne = new LCD();

        this.registerListeners();
    }

    private void registerListeners() throws Exception {
        AttachListener listener = new AttachListener() {
            public void onAttach(AttachEvent ae) {
                System.out.println("LCD-attached!");
                LCD phid = (LCD) ae.getSource();
                try {
                    if (ae.getSource().getDeviceID() == DeviceID.PN_1204) {
                        phid.setScreenSize(LCDScreenSize.DIMENSIONS_4X40);
                        phid.setBacklight(1.0);
                        phid.setContrast(0.5);
                        //phid.setCursorBlink(true);
                        bus.sendResponse("lcd-ready", true);

                    }
                } catch (PhidgetException ex) {
                    System.out.println(ex.getDescription());
                }
            }
        };

        lcdOne.addAttachListener(listener);
    }

    public void initScreen() throws Exception {
        lcdOne.setDeviceSerialNumber(329585);
        lcdOne.setChannel(0);
        lcdOne.setIsRemote(true);



        System.out.println("Connecting to LCD.");

        lcdOne.open(5000);



    }

    public void writeTextLine(int line, int startChar, String text) throws PhidgetException {
        lcdOne.writeText(LCDFont.DIMENSIONS_5X8, startChar, line, text);
        lcdOne.flush();
    }

    public void writeTrashboxWelcome() throws Exception {
        lcdOne.clear();
        this.writeTextLine(0, 0, "Welcome To TrashBox");
    }

    public void writeDavePhoneSelected() throws Exception {
        lcdOne.clear();
        lcdOne.setBacklight(1.0);
        this.writeTextLine(0, 0, "Dave's Phone Selected");
        this.writeTextLine(2, 0, "Push the red button to find it!");
    }

    public void writeMichellePhoneSelected() throws Exception {
        lcdOne.clear();
        lcdOne.setBacklight(1.0);
        this.writeTextLine(0, 0, "Michelle's Phone Selected");
        this.writeTextLine(2, 0, "Push the red button to find it!");
    }

    public void setMaxBrightness() throws Exception {
        lcdOne.setBacklight(1.0);
        lcdOne.setContrast(0.7);
    }

    public void setNoBrightness() throws Exception {
        lcdOne.setBacklight(0);
        lcdOne.setContrast(1);

    }
}
