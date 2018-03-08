package com.quobix;


public class WednesdayNightReminder implements Runnable {

    private int counter;

    public WednesdayNightReminder() {
        counter = 0;
    }

    @Override
    public void run() {
        System.out.println("checking the time..." + counter++);
    }
}
