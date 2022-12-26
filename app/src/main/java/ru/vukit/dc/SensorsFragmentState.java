package ru.vukit.dc;

import androidx.annotation.Keep;

import java.util.Observable;

import ru.vukit.dc.sensors.SensorMessage;

@Keep
@SuppressWarnings({"deprecation"})
public class SensorsFragmentState extends Observable {

    private static SensorsFragmentState INSTANCE = null;

    private SensorsFragmentState() {
    }

    public static synchronized SensorsFragmentState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SensorsFragmentState();
        }
        return (INSTANCE);
    }

    void notifySensors(SensorMessage data) {
        setChanged();
        notifyObservers(data);
    }

}
