package ru.vukit.dc.sensors;

public class SensorMessage {

    public static final int TYPE_NEW_DATA = 1;

    private final int sensorId;
    private final int type;
    private final String message;
    private final String settings;

    public SensorMessage(int sensorId, int type, String message, String settings) {
        this.sensorId = sensorId;
        this.type = type;
        this.message = message;
        this.settings = settings;
    }

    int getSensorId() {
        return sensorId;
    }

    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getSettings() {
        return settings;
    }

}
