package com.quobix;

import com.phidget22.Net;
import com.phidget22.ServerType;
import com.quobix.model.ActivePhone;
import com.quobix.model.ButtonController;
import com.quobix.model.LEDCommand;
import com.quobix.model.LEDCommandType;
import com.vmware.bifrost.bus.MessagebusService;
import com.vmware.bifrost.bus.model.Message;

import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioSystem;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.*;

public class MainController {

    LCDController lcdController;
    LEDManager ledManagerA;
    LEDManager ledManagerB;

    LEDController redButtonLEDController;
    LEDController greenButtonLEDController;
    LEDController featureButtonLEDController;

    LEDController internalGreenLEDControllerA;
    LEDController internalGreenLEDControllerB;

    UUID redLedControllerId;
    UUID greenLedControllerId;
    UUID featureControllerId;
    UUID internalGreenControllerAId;
    UUID internalGreenControllerBId;

    ButtonController redButton;
    ButtonController greenButton;
    ButtonController featureButton;

    ScheduledExecutorService scheduledExecutorService;
    ExecutorService executor;


    File bleep1File;
    File bleep2File;
    File sirenAlarmFile;

    Clip bleep1Clip;
    Clip bleep2Clip;
    Clip alertSirenClip;

    BeeperService beeperService;
    ActivePhone activePhone;

    int managerCount = 0;

    private boolean lcdReady = false;
    boolean stateSwitch = false;

    ScheduledFuture lcdThread;

    int buttonCountReady = 0;
    int serial = 370813;

    private MessagebusService bus;
    private ScheduledFuture demoPatternOneFuture;
    private ScheduledFuture demoPatternTwoFuture;
    private ScheduledFuture demoPatternThreeFuture;
    private ScheduledFuture internalGreenLightFutureAOn;
    private ScheduledFuture internalGreenLightFutureAOff;
    private ScheduledFuture internalGreenLightFutureBOn;
    private ScheduledFuture internalGreenLightFutureBOff;


    private boolean demoRunning = false;
    private boolean trashAlarmSounding = false;
    private int featureButtonClickCount = 0;

    public MainController() throws Exception {

        //com.phidget22.Log.enable(LogLevel.DEBUG, null);
        Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);

        this.executor = Executors.newFixedThreadPool(10);

        bus = new MessagebusService();
        beeperService = new BeeperService(bus);

        beeperService.setDaveId("*");
        beeperService.setDavePhoneName("*");
        beeperService.setDavePass("*");
        beeperService.setMichelleId("*");
        beeperService.setMichellePass("*");
        beeperService.setMichellePhoneName("*");

        redLedControllerId = UUID.randomUUID();
        greenLedControllerId = UUID.randomUUID();
        featureControllerId = UUID.randomUUID();
        internalGreenControllerAId = UUID.randomUUID();
        internalGreenControllerBId = UUID.randomUUID();


        this.listenForLCDReady();
        this.listenForLEDReady();

        this.listenForButtonsActive();
        this.listenForButtonsReady();
        this.listenForButtonsClicked();
        this.listenForTrashControl();

        this.scheduledExecutorService = Executors.newScheduledThreadPool(3);


        Runnable task = () -> {

            try {
                redButtonLEDController = new LEDController(this.bus, serial, 31, 1, redLedControllerId, true);
                greenButtonLEDController = new LEDController(this.bus, serial, 30, 1, greenLedControllerId, true);
                featureButtonLEDController = new LEDController(this.bus, serial, 20, 1, featureControllerId, true);
                internalGreenLEDControllerA = new LEDController(this.bus, serial, 16, 1, internalGreenControllerAId, true);
                internalGreenLEDControllerB = new LEDController(this.bus, serial, 17, 1, internalGreenControllerBId, true);


                redButton = new ButtonController(bus, 3, 0, 370813, redLedControllerId, redButtonLEDController);
                redButton.connect();

                greenButton = new ButtonController(bus, 3, 1, 370813, greenLedControllerId, greenButtonLEDController);
                greenButton.connect();

                featureButton = new ButtonController(bus, 3, 2, 370813, featureControllerId, featureButtonLEDController);
                featureButton.connect();

                redButtonLEDController.connect();
                greenButtonLEDController.connect();
                featureButtonLEDController.connect();
                internalGreenLEDControllerA.connect();
                internalGreenLEDControllerB.connect();



            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        this.executor.submit(task);
    }

    private void switchActivePhone() throws Exception {
        System.out.println("switching active phone");
        switch (this.activePhone) {

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
        System.out.println("triggering phone. ");

        switch (this.activePhone) {

            case Dave:
                System.out.println("triggering daves phone. ");
                beeperService.findDavesPhone();
                break;

            case Michelle:
                System.out.println("triggering michelles phone. ");
                //beeperService.findMichellesPhone();
                beeperService.findDavesPhone();
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

                    // turn feature light back on
                    try {
                        featureButtonLEDController.on();
                    } catch (Exception exp) {
                        // don't care
                    }
                }
        );
    }

    private void listenForTrashControl() {
        this.bus.listenStream("trash-control-start",
                (Message msg) -> {
                    this.executor.submit(
                            () -> {

                                this.trashAlarmSounding = true;

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

                                this.trashAlarmSounding = false;

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
                                if (id.equals(this.redLedControllerId)) {
                                    System.out.println("red button clicked, lets go!");
                                    if (demoRunning) {
                                        System.out.println("stopping demo for red button");
                                        stopDemoLightCycle();
                                    }
                                    if (trashAlarmSounding) {
                                        System.out.println("stopping trash alarm for red button");
                                        stopTrashAlarm();
                                    }
                                    try {
                                        System.out.println("playing beep for red button");
                                        this.playBleep1();
                                        System.out.println("beep played");
                                        this.flashTrashBasic();
                                        System.out.println("trash flashed");
                                        this.triggerFindPhone();
                                        System.out.println("red button actions finished");
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (id.equals(this.greenLedControllerId)) {
                                    System.out.println("green button clicked");
                                    if (demoRunning) {
                                        stopDemoLightCycle();
                                    }
                                    if (trashAlarmSounding) {
                                        stopTrashAlarm();
                                    }
                                    try {
                                        this.playBleep2();
                                        this.switchActivePhone();
                                        //this.bus.sendResponse("trash-control-start", true);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (id.equals(this.featureControllerId)) {
                                    System.out.println("feature button clicked");
                                    this.featureButtonClicked();
                                }
                            }
                    );
                }
        );
    }

    private void featureButtonClicked() {

        if (this.demoRunning) {
            System.out.println("Demo Cycle Off.....");
            stopDemoLightCycle();
            return;

        }
        if (this.trashAlarmSounding) {
            System.out.println("Stopping Trash Alarm");
            stopTrashAlarm();
            // stop trash alarm
            return;
        }

        // pretend its trash day.
        soundTrashAlarm();

    }

    private void stopTrashAlarm() {
        executor.execute(this.flashOff);

        // kick demo back on after 5 seconds.
        this.scheduledExecutorService.schedule(() -> {
            startDemoLightCycle();
        }, 5, TimeUnit.SECONDS);
        this.trashAlarmSounding = false;
    }

    private void soundTrashAlarm() {
        System.out.println("Starting Trash Alarm");
        this.trashAlarmSounding = true;
        playTrashAlarm();
        scheduledExecutorService.schedule(this.flashOn, 300, TimeUnit.MILLISECONDS);


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
                    if (this.buttonCountReady == 3) {
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
        lcdThread = scheduledExecutorService.scheduleWithFixedDelay(
                () -> {
                    this.dimLCDAndRestoreDefaultMessage();
                },
                10,
                10,
                TimeUnit.SECONDS);
    }

    private void appReady() {
        scheduledExecutorService.scheduleAtFixedRate(new WednesdayNightReminder(bus), 3000, 1000, TimeUnit.MILLISECONDS);
        startLcdTimer();
        lightUpButtons();
        System.out.println("App is ready!");
        try {
            featureButtonLEDController.on();
        } catch (Exception exp) {
            // don't care.
        }
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
            sirenAlarmFile = new File("sfx/alert.wav");

            var b1Stream = AudioSystem.getAudioInputStream(bleep1File);
            bleep1Clip = AudioSystem.getClip();
            bleep1Clip.open(b1Stream);

            var b2Stream = AudioSystem.getAudioInputStream(bleep2File);
            bleep2Clip = AudioSystem.getClip();
            bleep2Clip.open(b2Stream);

            var sirenStream = AudioSystem.getAudioInputStream(sirenAlarmFile);
            alertSirenClip = AudioSystem.getClip();
            alertSirenClip.open(sirenStream);

        } catch (Exception exp) {
            exp.printStackTrace();
        }

    }

    private void playBleep2() {
        // when running on hardware
        try {
            System.out.println("playing bleep 2");
            Runtime.getRuntime().exec("aplay sfx/bleep1.wav");
        } catch (Exception e) {
            //e.printStackTrace();
        }

        // when running on local dev
        try {
            bleep1Clip.stop();
            bleep1Clip.close();
            this.resetAudio();
            bleep1Clip.start();
        } catch (Exception e) {
            // e.printStackTrace();
        }


    }

    private void playBleep1() {

        // when running on hardware
        try {
            System.out.println("playing bleep 1");
            Runtime.getRuntime().exec("aplay sfx/bleep2.wav");
        } catch (Exception e) {
            //e.printStackTrace();
        }

         //when running on local dev
        try {
            bleep2Clip.stop();
            bleep2Clip.close();
            this.resetAudio();
            bleep2Clip.start();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private void playTrashAlarm() {

        // when running on hardware
        try {
            System.out.println("playing alert siren");
            Runtime.getRuntime().exec("aplay sfx/alert.wav");
        } catch (Exception e) {
            //e.printStackTrace();
        }

        // when running on local dev.
        try {
            alertSirenClip.stop();
            alertSirenClip.close();
            this.resetAudio();
            alertSirenClip.start();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    /* flash sequences */
    private void flashTrashBasic() {
        executor.execute(this.flashOn);
        scheduledExecutorService.schedule(this.flashOff, 1, TimeUnit.SECONDS);
    }

    private Runnable flashOn = () -> {
        this.bus.sendResponse("trash-control-start", true);
    };

    private Runnable flashOff = () -> {
        this.bus.sendResponse("trash-control-stop", true);
    };

    private void flashTrashBlinkPatternOne() {
        executor.execute(this.flashOn);
        scheduledExecutorService.schedule(this.flashOff, 100, TimeUnit.MILLISECONDS);
        scheduledExecutorService.schedule(this.flashOn, 300, TimeUnit.MILLISECONDS);
        scheduledExecutorService.schedule(this.flashOff, 600, TimeUnit.MILLISECONDS);
        scheduledExecutorService.schedule(this.flashOn, 900, TimeUnit.MILLISECONDS);
        scheduledExecutorService.schedule(this.flashOff, 1200, TimeUnit.MILLISECONDS);
        // off completely, don't get hung.
        scheduledExecutorService.schedule(this.flashOff, 1800, TimeUnit.MILLISECONDS);
    }

    private void flashTrashBlinkPatternTwo() {
        ScheduledFuture blinkOnFuture, blinkOffFuture;
        blinkOnFuture = scheduledExecutorService.scheduleWithFixedDelay(this.flashOn, 100, 800, TimeUnit.MILLISECONDS);
        blinkOffFuture = scheduledExecutorService.scheduleWithFixedDelay(this.flashOff, 400, 800, TimeUnit.MILLISECONDS);

        Runnable stopCompletely = () -> {
            blinkOffFuture.cancel(false);
            blinkOnFuture.cancel(false);
            this.scheduledExecutorService.schedule(this.flashOff, 1500, TimeUnit.MILLISECONDS);
        };
        this.scheduledExecutorService.schedule(stopCompletely, 5, TimeUnit.SECONDS);
    }

    private void flashTrashBlinkPatternThree() {
        executor.execute(this.flashOn);
        scheduledExecutorService.schedule(this.flashOff, 2, TimeUnit.SECONDS);
        scheduledExecutorService.schedule(this.flashOn, 4, TimeUnit.SECONDS);
        scheduledExecutorService.schedule(this.flashOff, 6, TimeUnit.SECONDS);
        scheduledExecutorService.schedule(this.flashOn, 8, TimeUnit.SECONDS);
        scheduledExecutorService.schedule(this.flashOff, 10, TimeUnit.SECONDS);
        scheduledExecutorService.schedule(this.flashOn, 12, TimeUnit.SECONDS);
        scheduledExecutorService.schedule(this.flashOff, 14, TimeUnit.SECONDS);
        // off completely, don't get hung.
        scheduledExecutorService.schedule(this.flashOff, 15, TimeUnit.SECONDS);
    }


    private void startDemoLightCycle() {
        this.demoRunning = true;
        System.out.println("Demo running!");
        demoPatternOneFuture = scheduledExecutorService.scheduleAtFixedRate(this::flashTrashBlinkPatternOne, 0, 30, TimeUnit.SECONDS);
        demoPatternTwoFuture = scheduledExecutorService.scheduleAtFixedRate(this::flashTrashBlinkPatternTwo, 20, 30, TimeUnit.SECONDS);
        demoPatternThreeFuture = scheduledExecutorService.scheduleAtFixedRate(this::flashTrashBlinkPatternThree, 30, 30, TimeUnit.SECONDS);

        try {

            internalGreenLightFutureAOn = scheduledExecutorService.scheduleAtFixedRate(internalGreenLEDControllerA::on, 0, 10, TimeUnit.SECONDS);
            internalGreenLightFutureAOff = scheduledExecutorService.scheduleAtFixedRate(internalGreenLEDControllerA::off, 9, 10, TimeUnit.SECONDS);
            internalGreenLightFutureBOn = scheduledExecutorService.scheduleAtFixedRate(internalGreenLEDControllerB::on, 4, 8, TimeUnit.SECONDS);
            internalGreenLightFutureBOff = scheduledExecutorService.scheduleAtFixedRate(internalGreenLEDControllerB::off, 11,  8, TimeUnit.SECONDS);
            //scheduledExecutorService.scheduleAtFixedRate(internalGreenLEDControllerB::fadeDown, 15, 20, TimeUnit.SECONDS);


        } catch (Exception exp) {
            exp.printStackTrace();
            // don't care;
        }

    }

    private void stopDemoLightCycle() {
        System.out.println("Demo stopping");
        if (demoPatternOneFuture != null && demoPatternTwoFuture != null && demoPatternThreeFuture != null) {

            demoPatternOneFuture.cancel(true);
            demoPatternTwoFuture.cancel(true);
            demoPatternThreeFuture.cancel(true);

            internalGreenLightFutureAOn.cancel(true);
            internalGreenLightFutureAOff.cancel(true);
            internalGreenLightFutureBOn.cancel(true);
            internalGreenLightFutureBOff.cancel( true);

            scheduledExecutorService.schedule(flashOff, 300, TimeUnit.MILLISECONDS);
        }
        demoRunning = false;
    }

}
