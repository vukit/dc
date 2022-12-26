package ru.vukit.dc.tasks;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import ru.vukit.dc.sensors.DCSensor;
import ru.vukit.dc.servres.DCServer;

class DCTaskSimple extends DCTask {

    private Integer period;
    private Timer timer;

    DCTaskSimple(String id, String uuid, String name, ArrayList<DCSensor> sensors, DCServer server, String schedule, byte format) {
        super(id, uuid, name, sensors, server, format);
        try {
            period = Integer.parseInt(schedule.split(":")[1]);
            if (period <= 0 || period >= 60000) {
                period = null;
                status = "invalid sampling period";
            }
        } catch (NumberFormatException ex) {
            period = null;
            status = "invalid sampling period";
        }
    }

    @Override
    public void run() {
        if (period != null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    sendData();
                }
            }, 0, period);
        }
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

}
