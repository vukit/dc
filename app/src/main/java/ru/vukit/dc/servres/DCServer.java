package ru.vukit.dc.servres;

public abstract class DCServer {

    public static int TIMEOUT = 2000;
    public final static String STATUS_UNKNOWN = "unknown";
    final static String STATUS_OK = "ok";

    public String id;
    public String status = STATUS_UNKNOWN;

    abstract public void connect();

    abstract public void send(String data);

    abstract public void disconnect();

    void setStatus(String currentStatus) {
        status = currentStatus;
    }

}
