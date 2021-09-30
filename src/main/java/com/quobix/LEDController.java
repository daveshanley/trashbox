package com.quobix;

import com.phidget22.*;
import com.quobix.model.BusEnabledListener;
import com.quobix.model.LEDCommand;
import com.vmware.bifrost.bus.MessagebusService;
import com.vmware.bifrost.bus.model.Message;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class LEDListener extends BusEnabledListener implements AttachListener {

    private boolean isButton;
    private DigitalOutput phid;
    ExecutorService executor;

    public LEDListener(MessagebusService bus, int channel, UUID id, boolean isButton) {
        super(bus, channel, id);
        this.isButton = isButton;
        executor = Executors.newFixedThreadPool(5);
    }

    @Override
    public void onAttach(AttachEvent attachEvent) {
        phid = (DigitalOutput) attachEvent.getSource();
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
                            // fire in a thread.
                            executor.execute(() -> {
                                this.handleState(phid, command);
                            });

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

    private void switchOn() throws Exception {
        try {
            phid.setState(true);
        } catch (Exception exp) {
            phid.setState(false);
        }
    }

    private void switchOff() throws Exception {
        try {
            phid.setState(false);
        } catch (Exception exp) {
            phid.setState(false);
        }
    }

    private void handleState(DigitalOutput output, LEDCommand command) {

        try {

            switch (command.getType()) {
                case ON:
                    this.switchOn();
                    break;
                case OFF:
                    this.switchOff();
                    break;
            }

        } catch (Exception exp) {

            System.out.println("Warning, setting state timed out, resetting controller");
            try {
                this.switchOff();
            } catch (Exception expOff) {
                // I don't care.
            }
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
        this.phidget.setIsRemote(true);
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

    public void on() {
        try {
            phidget.setState(true);
        } catch (Exception exp) {
            // don't care.
        }
    }

    public void off() {
        //lcdOne.setChannel(channel);
        try {
            phidget.setState(false);
        } catch (Exception exp) {
            // don't care;
        }
    }

    public void close() throws Exception {
        // lcdOne.close();
    }

    public void fadeDown() {

        try {

            double cycleValue = 0.99;
            int decimalPlaces = 2;
            on();
            while (cycleValue > 0) {

                cycleValue -= 0.01;
                BigDecimal cycleCalc = new BigDecimal(cycleValue);
                cycleCalc = cycleCalc.setScale(decimalPlaces, BigDecimal.ROUND_DOWN);
                cycleValue = cycleCalc.doubleValue();

                phidget.setDutyCycle(cycleValue);

            }
            off();
        } catch (Exception exp) {
            // don't care.
        }
    }

    public void fadeUp(){

        try {

            double cycleValue = 0.0;
            int decimalPlaces = 2;
            phidget.setDutyCycle(cycleValue);
            on();
            while (cycleValue > 0) {

                cycleValue += 0.01;
                BigDecimal cycleCalc = new BigDecimal(cycleValue);
                cycleCalc = cycleCalc.setScale(decimalPlaces, BigDecimal.ROUND_DOWN);
                cycleValue = cycleCalc.doubleValue();
                phidget.setDutyCycle(cycleValue);
            }

        } catch (Exception exp) {
            // really don't care.
        }
    }

}
