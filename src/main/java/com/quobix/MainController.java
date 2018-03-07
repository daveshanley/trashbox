package com.quobix;

import com.phidget22.LogLevel;
import com.phidget22.Net;
import com.phidget22.ServerType;
import com.quobix.model.ButtonController;
import com.quobix.model.LEDCommand;
import com.quobix.model.LEDCommandType;
import com.vmware.bifrost.bus.MessagebusService;
import com.vmware.bifrost.bus.model.Message;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController {

    LCDController lcdController;
    LEDManager ledManagerA;
    LEDManager ledManagerB;

    LEDController redButtonLEDController;
    LEDController greenButtonLEDController;

    UUID redLedControllerId;
    UUID greenLedControllerId;


    ButtonController redButton;
    ButtonController greenButton;

    ScheduledExecutorService scheduledeExecutor;
    ExecutorService executor;


    File bleep1File;
    File bleep2File;
    AudioStream bleep1Stream;
    AudioStream bleep2Stream;

    BeeperService beeperService;

    int managerCount = 0;

    private boolean lcdReady = false;
    boolean stateSwitch = false;

    int buttonCountReady = 0;
    int serial = 370813;

    private MessagebusService bus;

    public MainController() throws Exception {
        //com.phidget22.Log.enable(LogLevel.DEBUG, null);


        Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

        this.executor = Executors.newFixedThreadPool(10);

        bus = new MessagebusService();
        beeperService = new BeeperService(bus);

        redLedControllerId = UUID.randomUUID();
        greenLedControllerId = UUID.randomUUID();

        this.listenForLCDReady();
        this.listenForLEDReady();

        this.listenForButtonsActive();
        this.listenForButtonsReady();
        this.listenForButtonsClicked();

        this.scheduledeExecutor = Executors.newScheduledThreadPool(32);


        Runnable task = () -> {

            try {
                redButtonLEDController = new LEDController(this.bus, serial, 31, 1, redLedControllerId);
                greenButtonLEDController = new LEDController(this.bus, serial, 30, 1, greenLedControllerId);

                System.out.println("Creating red button controller");
                redButton = new ButtonController(bus, 3, 0, 370813, redLedControllerId, redButtonLEDController);


                System.out.println("Connecting red button controller");
                redButton.connect();

                System.out.println("Creating green button controller");
                greenButton = new ButtonController(bus, 3, 1, 370813, greenLedControllerId, greenButtonLEDController);


                System.out.println("Connecting green button controller");
                greenButton.connect();

                redButtonLEDController.connect();
                greenButtonLEDController.connect();


                System.out.println("red id: " + this.redLedControllerId);
                System.out.println("green id: " + this.greenLedControllerId);

            } catch (Exception e) {
                e.printStackTrace();
            }

        };


        this.executor.submit(task);


    }

    private void listenForButtonsClicked() {
        this.bus.listenStream("button-click-events",
                (Message msg) -> {
                    this.executor.submit(
                            () -> {
                                UUID id = (UUID) msg.getPayload();
                                if(id.equals(this.redLedControllerId)) {
                                    this.playBleep1();
                                    try {
                                        beeperService.findDavesPhone();
                                    } catch (Exception e){
                                        e.printStackTrace();
                                    }

                                }
                                if(id.equals(this.greenLedControllerId)) {
                                    this.playBleep2();
                                }
                            }
                    );
                }
        );
    }

    private void listenForButtonsActive() {
        this.bus.listenStream("buttons-active",
                (Message msg) -> {
                    this.executor.submit(
                            () -> {
                                System.out.println("Creating led manager");
                                ledManagerA = new LEDManager(bus, 0, 370813, 32);
                                ledManagerB = new LEDManager(bus, 1, 370813, 15);
                            }
                    );
                }
        );
    }

    private void listenForButtonsReady() {
        this.bus.listenStream("button-ready",
                (Message msg) -> {
                    this.buttonCountReady++;
                    if (this.buttonCountReady == 2) {
                        this.bus.sendResponse("buttons-active", true);
                    }
                }
        );
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
                    //this.scheduledeExecutor.scheduleAtFixedRate(task, 3000, 2000, TimeUnit.MILLISECONDS);
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

    private void resetAudio() {
        try {



            bleep1File = new File("sfx/bleep1.wav");
            bleep2File = new File("sfx/bleep2.wav");
            bleep1Stream = new AudioStream(new FileInputStream(bleep1File));
            bleep2Stream = new AudioStream(new FileInputStream(bleep2File));
        } catch (Exception exp) {
            exp.printStackTrace();
        }

    }

    private void playBleep1() {
        try {


            AudioPlayer.player.stop(bleep1Stream);

            this.resetAudio();
            AudioPlayer.player.start(bleep1Stream);
            Runtime.getRuntime().exec("aplay sfx/bleep1.wav");
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private void playBleep2() {
        try {
            AudioPlayer.player.stop(bleep2Stream);
            this.resetAudio();
            AudioPlayer.player.start(bleep2Stream);
            Runtime.getRuntime().exec("aplay sfx/bleep2.wav");
        } catch (Exception e) {
           // e.printStackTrace();
        }
    }


}
