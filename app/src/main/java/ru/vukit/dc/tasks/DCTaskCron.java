package ru.vukit.dc.tasks;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import it.sauronsoftware.cron4j.InvalidPatternException;
import it.sauronsoftware.cron4j.Scheduler;
import ru.vukit.dc.sensors.DCSensor;
import ru.vukit.dc.servres.DCServer;

class DCTaskCron extends DCTask {

    private final String cronString;
    private Scheduler scheduler;
    private Integer sampleAmount;
    private Integer samplePeriod;

    DCTaskCron(String id, String uuid, String name, ArrayList<DCSensor> sensors, DCServer server, String schedule, byte format) {
        super(id, uuid, name, sensors, server, format);
        String[] scheduleParts = schedule.split(":");
        cronString = scheduleParts[1];
        try {
            sampleAmount = Integer.parseInt(scheduleParts[2]);
            samplePeriod = Integer.parseInt(scheduleParts[3]);
            if (sampleAmount * samplePeriod <= 0 && sampleAmount * samplePeriod >= 60000) {
                sampleAmount = null;
                samplePeriod = null;
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
            sampleAmount = null;
            samplePeriod = null;
        }
    }

    @Override
    public void run() {
        scheduler = new Scheduler();
        try {
            scheduler.schedule(cronString, new Runnable() {
                Timer timer;
                int counter;

                @Override
                public void run() {
                    if (sampleAmount == null && samplePeriod == null) {
                        sendData();
                    } else if (sampleAmount != null && samplePeriod != null) {
                        counter = 0;
                        timer = new Timer();
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                sendData();
                                counter++;
                                if (counter == sampleAmount) {
                                    timer.cancel();
                                    timer = null;
                                }
                            }
                        }, 0, samplePeriod);
                    }
                }
            });
            scheduler.start();
        } catch (InvalidPatternException ex) {
            scheduler = null;
            status = "invalid cron pattern";
        }
    }

    @Override
    public void stop() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }

}
