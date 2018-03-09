package com.quobix.model;


public class LEDCommand {

    private LEDCommandType type;
    private int channel;
    private boolean broadcast = false;
    private boolean ignoreButton = false;

    public LEDCommand(LEDCommandType type, int channel, boolean broadcast, boolean ignoreButton) {
        this.type = type;
        this.channel = channel;
        this.broadcast = broadcast;
        this.ignoreButton = ignoreButton;
    }

    public LEDCommand(LEDCommandType type, int channel) {
        this(type, channel, false, false);
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

    public boolean isIgnoreButton() {
        return ignoreButton;
    }
}
