package com.quobix.model;


public class LEDCommand {

    private LEDCommandType type;
    private int channel;
    private boolean broadcast = false;

    public LEDCommand(LEDCommandType type, int channel, boolean broadcast) {
        this.type = type;
        this.channel = channel;
        this.broadcast = broadcast;
    }

    public LEDCommand(LEDCommandType type, int channel) {
        this(type, channel, false);
    }

    public LEDCommandType getType() {
        return type;
    }

    public int getChannel() {
        return channel;
    }

    public boolean isBroadcast() {
        return broadcast;
    }
}
