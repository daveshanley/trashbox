package com.quobix;

import com.phidget22.*;

public class TrashBox {
    public static void main(String[] args) throws Exception {

        com.phidget22.Log.enable(LogLevel.INFO, null);

        MainController controller = new MainController();
        synchronized (controller) {
            controller.wait();
        }
    }
}
