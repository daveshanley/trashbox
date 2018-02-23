package com.quobix;

import com.quobix.model.LEDCommand;
import com.quobix.model.LEDCommandType;
import com.vmware.bifrost.bus.MessagebusService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController {

    LCDController lcdController;
    LEDManager ledManagerA;
    LEDManager ledManagerB;
    ScheduledExecutorService scheduledeExecutor;

    int managerCount = 0;

    private boolean lcdReady = false;
    boolean stateSwitch = false;


    private MessagebusService bus;

    public MainController() throws Exception {
        bus = new MessagebusService();
        this.listenForLCDReady();
        this.listenForLEDReady();

        this.scheduledeExecutor = Executors.newScheduledThreadPool(32);

        ledManagerA = new LEDManager(bus, 0, 370813, 32);
        ledManagerB = new LEDManager(bus, 1, 370813, 15);
    }

    private void listenForLEDReady() {
        bus.listenStream("ledmanager-ready",
                (val) -> {
                    this.managerCount++;
                    if (this.managerCount < 2)
                        return;

                    System.out.println("We're all ready, waiting 5 seconds then turning it all on.");
                    Thread.sleep(5000);

                    this.bus.sendResponse("led-control",
                            new LEDCommand(LEDCommandType.ON, 0, true));


                    this.stateSwitch = true;
                    Runnable task = () -> {
                        if (this.stateSwitch) {
                            System.out.println("Turning Off.");
                            this.bus.sendResponse("led-control", new LEDCommand(LEDCommandType.OFF, 0, true));
                            this.stateSwitch = false;
                        } else {
                            System.out.println("Turning On.");
                            this.bus.sendResponse("led-control", new LEDCommand(LEDCommandType.ON, 0, true));
                            this.stateSwitch = true;
                        }

                    };

                    this.scheduledeExecutor.scheduleAtFixedRate(task, 3000, 2000, TimeUnit.MILLISECONDS);


                }
        );
    }

    private void listenForLCDReady() {
        bus.listenStream("lcd-ready",
                (val) -> {
                    System.out.println("LCD is now ready");
                    lcdController.writeTextLine(0, 0, "#####");
                    lcdController.writeTextLine(1, 2, "#");
                    lcdController.writeTextLine(2, 2, "#");
                    lcdController.writeTextLine(3, 2, "#");


                    lcdController.writeTextLine(0, 6, "####");
                    lcdController.writeTextLine(1, 6, "#  #");
                    lcdController.writeTextLine(2, 6, "###");
                    lcdController.writeTextLine(3, 6, "#  #");

                    lcdController.writeTextLine(0, 12, "#");
                    lcdController.writeTextLine(1, 12, "#");
                    lcdController.writeTextLine(2, 12, "#");
                    lcdController.writeTextLine(3, 12, "#");

                }
        );
    }

}
