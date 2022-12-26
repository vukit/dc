package ru.vukit.dc.tasks;

import java.util.ArrayList;

import ru.vukit.dc.sensors.DCSensor;
import ru.vukit.dc.servres.DCServer;

abstract public class DCTask {

    public final static int TIMEOUT = 1000;
    public final static String STATUS_UNKNOWN = "unknown";
    private final static String STATUS_OK = "running";

    public final String id;
    private final ArrayList<DCSensor> sensors;
    private final DCServer server;
    private final StringBuilder data;
    private final int dataHeaderLength;
    public String status = STATUS_OK;

    DCTask(String id, String uuid, String name, ArrayList<DCSensor> sensors, DCServer server, byte format) {
        this.id = id;
        this.sensors = sensors;
        this.server = server;
        data = new StringBuilder();
        switch (format) {
            case 0: // Не включать UUID и название задачи
                data.append("{");
                dataHeaderLength = data.length();
                break;
            case 1: // Не включать UUID
                data.append("{\"task\":\"");
                data.append(name);
                data.append("\",");
                dataHeaderLength = data.length();
                break;
            case 2: // Не включать название задачи
                data.append("{\"uuid\":\"");
                data.append(uuid);
                data.append("\",");
                dataHeaderLength = data.length();
                break;
            case 3: // Включать UUID и название задачи
            default:
                data.append("{\"uuid\":\"");
                data.append(uuid);
                data.append("\",");
                data.append("\"task\":\"");
                data.append(name);
                data.append("\",");
                dataHeaderLength = data.length();
                break;
        }
    }

    abstract public void run();

    abstract public void stop();

    void sendData() {
        data.setLength(dataHeaderLength);
        data.append("\"timestamp\":");
        data.append(System.currentTimeMillis());
        data.append(",");
        data.append("\"sensors\":[");
        for (DCSensor sensor : sensors) {
            data.append(sensor.getData());
            data.append(",");
        }
        data.setLength(data.length() - 1);
        data.append("]}");
        server.send(data.toString());
        status = STATUS_OK;
    }

}
