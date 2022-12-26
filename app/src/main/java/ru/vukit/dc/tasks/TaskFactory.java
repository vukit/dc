package ru.vukit.dc.tasks;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import ru.vukit.dc.sensors.DCSensor;
import ru.vukit.dc.servres.DCServer;

public class TaskFactory {

    @Nullable
    public static DCTask makeTask(String id, String uuid, String name, ArrayList<DCSensor> sensors, DCServer server, String schedule, byte format) {
        if (sensors.size() == 0 || server == null) {
            return null;
        }
        switch (schedule.split(":")[0]) {
            case "simple":
                return new DCTaskSimple(id, uuid, name, sensors, server, schedule, format);
            case "cron":
                return new DCTaskCron(id, uuid, name, sensors, server, schedule, format);
        }
        return null;
    }

}
