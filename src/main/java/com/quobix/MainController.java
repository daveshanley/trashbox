package com.quobix;

import com.phidget22.LogLevel;
import com.phidget22.Net;
import com.phidget22.ServerType;
import com.quobix.model.ActivePhone;
import com.quobix.model.ButtonController;
import com.quobix.model.LEDCommand;
import com.quobix.model.LEDCommandType;
import com.vmware.bifrost.bus.MessagebusService;
import com.vmware.bifrost.bus.model.Message;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;
import java.util.concurrent.*;

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
    ActivePhone activePhone;

    int managerCount = 0;

    private boolean lcdReady = false;
    boolean stateSwitch = false;

    ScheduledFuture lcdThread;

    int buttonCountReady = 0;
    int serial = 370813;

    private MessagebusService bus;

    public MainController() throws Exception {
        //com.phidget22.Log.enable(LogLevel.DEBUG, null);


        //Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

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
        this.listenForTrashControl();

        this.scheduledeExecutor = Executors.newScheduledThreadPool(3);


        Runnable task = () -> {

            try {
                redButtonLEDController = new LEDController(this.bus, serial, 31, 1, redLedControllerId, true);
                greenButtonLEDController = new LEDController(this.bus, serial, 30, 1, greenLedControllerId, true);

                redButton = new ButtonController(bus, 3, 0, 370813, redLedControllerId, redButtonLEDController);
                redButton.connect();

                greenButton = new ButtonController(bus, 3, 1, 370813, greenLedControllerId, greenButtonLEDController);
                greenButton.connect();

                redButtonLEDController.connect();
                greenButtonLEDController.connect();

            } catch (Exception e) {
                e.printStackTrace();
            }

        };


        this.executor.submit(task);


    }
    private void switchActivePhone() throws Exception {
        switch(this.activePhone) {

            case Dave:
                this.activePhone = ActivePhone.Michelle;
                lcdController.writeMichellePhoneSelected();
                break;

            case Michelle:
                this.activePhone = ActivePhone.Dave;
                lcdController.writeDavePhoneSelected();
                break;
        }
        lcdController.setMaxBrightness();
        this.lcdThread.cancel(true);
        this.startLcdTimer();
        this.lightUpButtons();
    }

    private void triggerFindPhone() throws Exception {
        switch(this.activePhone) {

            case Dave:
                beeperService.findDavesPhone();
                break;

            case Michelle:
                beeperService.findMichellesPhone();
                break;
        }
    }

    private void dimLCDAndRestoreDefaultMessage() {
        this.executor.submit(
                () -> {
                    try {
                        this.lcdController.setNoBrightness();
                        this.lcdController.writeTrashboxWelcome();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                    this.disableButtonLights();
                }
        );
    }


    private void lightUpButtons() {
        this.executor.submit(
                () -> {
                    this.bus.sendResponse("led-control-button",
                            new LEDCommand(LEDCommandType.ON, 0, true, true));
                }
        );
    }

    private void disableButtonLights() {
        this.executor.submit(
                () -> {
                    this.bus.sendResponse("led-control-button",
                            new LEDCommand(LEDCommandType.OFF, 0, true, true));
                }
        );
    }

    private void listenForTrashControl() {
        this.bus.listenStream("trash-control-start",
                (Message msg) -> {
                    this.executor.submit(
                            () -> {
                                this.bus.sendResponse("led-control",
                                        new LEDCommand(LEDCommandType.ON, 0, true, true));
                            }
                    );
                }
        );

        this.bus.listenStream("trash-control-stop",
                (Message msg) -> {
                    this.executor.submit(
                            () -> {
                                this.bus.sendResponse("led-control",
                                        new LEDCommand(LEDCommandType.OFF, 0, true, true));
                            }
                    );
                }
        );

    }

    private void listenForButtonsClicked() {
        this.bus.listenStream("button-click-events",
                (Message msg) -> {
                    this.executor.submit(
                            () -> {
                                UUID id = (UUID) msg.getPayload();
                                if(id.equals(this.redLedControllerId)) {
                                    try {
                                        this.playBleep1();
                                        this.triggerFindPhone();
                                    } catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                                if(id.equals(this.greenLedControllerId)) {
                                    try {
                                        this.playBleep2();
                                        this.switchActivePhone();
                                    } catch (Exception e){
                                        e.printStackTrace();
                                    }
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
                    this.lcdController = new LCDController(this.bus);
                    this.lcdController.initScreen();
                    this.appReady();
                }
        );
    }

    private void startLcdTimer() {
        lcdThread = scheduledeExecutor.scheduleWithFixedDelay(
                () -> {
                    this.dimLCDAndRestoreDefaultMessage();
                },
                10,
                10,
                TimeUnit.SECONDS);
    }

    private void appReady() {
        scheduledeExecutor.scheduleAtFixedRate(new WednesdayNightReminder(bus), 3000, 1000, TimeUnit.MILLISECONDS);
        startLcdTimer();
        lightUpButtons();

    }

    private void listenForLCDReady() {
        bus.listenStream("lcd-ready",
                (val) -> {
                    System.out.println("LCD is now ready");
                    lcdController.setMaxBrightness();
                    lcdController.writeTrashboxWelcome();
                    this.activePhone = ActivePhone.Dave;

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

    private void playBleep2() {
        try {

            AudioPlayer.player.stop(bleep1Stream);
            this.resetAudio();
            AudioPlayer.player.start(bleep1Stream);
            Runtime.getRuntime().exec("aplay sfx/bleep1.wav");
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private void playBleep1() {
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
