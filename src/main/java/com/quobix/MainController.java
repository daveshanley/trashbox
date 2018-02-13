package com.quobix;

import com.vmware.bifrost.bus.MessagebusService;

public class MainController {

    LCDController controller;
    private boolean lcdReady = false;
    private MessagebusService bus;

    public MainController() throws Exception {
        bus = new MessagebusService();
        this.listenForLCDReady();
        controller = new LCDController(bus);
        controller.initScreen();

    }

    private void listenForLCDReady() {
        bus.listenStream("lcd-ready",
                (val) -> {
                    System.out.println("LCD is now ready");
                    controller.writeTextLine(0, 0, "Welcome to TrashBox.");
                }
        );
    }

}
