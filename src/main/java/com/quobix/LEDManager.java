package com.quobix;

import com.quobix.model.LEDCommand;
import com.quobix.model.LEDCommandType;
import com.vmware.bifrost.bus.MessagebusService;
import com.vmware.bifrost.bus.model.Message;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LEDManager {

    LEDController[] controllers;
    MessagebusService bus;
    int hubPort;
    int hubSerial;
    ExecutorService executor;
    ScheduledExecutorService scheduledeExecutor;
    int ledCount = 0;
    int ledActiveCount = 0;

    boolean stateSwitch = false;
    UUID id;


    public LEDManager(MessagebusService bus, int hubPort, int hubSerial, int ledCount) {
        this.executor = Executors.newFixedThreadPool(10);
        this.scheduledeExecutor = Executors.newScheduledThreadPool(10);
        this.bus = bus;
        this.hubPort = hubPort;
        this.hubSerial = hubSerial;
        this.ledCount = ledCount;
        this.id = UUID.randomUUID();
        try {
            this.listenForReady();
            this.createControllers();
        }catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    private void listenForReady() {
        this.bus.listenStream("led-ready-announce-" + this.id.toString(),
                (Message msg) -> {
                    ledActiveCount++;
                    if (ledActiveCount >= ledCount) {
                        System.out.println("ledCount Matched");

                        System.out.println("sending led-manager ready");
                        this.bus.sendResponse("ledmanager-ready",
                                new LEDCommand(LEDCommandType.ON, 0, true));

                    }
                }
        );
    }

    private void createControllers() throws Exception {

        controllers = new LEDController[ledCount];
        for (int i = 0; i < ledCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    controllers[index] = new LEDController(bus, hubSerial, index, hubPort, this.id);
                    controllers[index].connect();
                } catch (Exception exp) {
                    exp.printStackTrace();
                    System.out.println("something went wrong");
                }
            });
        }
    }
}
