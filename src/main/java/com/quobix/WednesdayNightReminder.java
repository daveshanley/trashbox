package com.quobix;


import com.vmware.bifrost.bus.MessagebusService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public class WednesdayNightReminder implements Runnable {

    private boolean running;
    private MessagebusService bus;

    public WednesdayNightReminder(MessagebusService bus) {
        running = false;
        this.bus = bus;
    }

    @Override
    public void run() {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        if (date.getDayOfWeek().equals(DayOfWeek.WEDNESDAY)) {
            if (time.getHour() >= 21) {
                if (!running) {
                    running = true;
                    this.bus.sendResponse("trash-control-start", true);
                }
            }
        }

        if (running) {
            if (time.getHour() >= 23 && time.getMinute() >= 59 && time.getSecond() >= 59) {
                this.running = false;
                this.bus.sendResponse("trash-control-stop", true);
            }
        }
    }
}
