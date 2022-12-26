package ru.vukit.dc.sensors;

import java.util.Observable;
import java.util.Observer;

import ru.vukit.dc.database.DBDriver;

class TextSensor extends DCSensor implements Observer {

    private final String[] values;

    TextSensor(Integer id, Integer code, String name, String settings, byte format) {
        super(id, code, name, settings, format);
        this.values = new String[valuesLength];
        this.values[0] = DBDriver.getInstance().selectSensorValue(String.valueOf(id));
        if (sensorsFragmentState != null) {
            sensorsFragmentState.addObserver(this);
        }
    }

    @Override
    public String getValue(int i) {
        return '"' + values[i] + '"';
    }

    @Override
    public void update(Observable o, Object arg) {
        String observable_name = o.getClass().getCanonicalName();
        if (observable_name != null && observable_name.equals("ru.vukit.dc.SensorsFragmentState")) {
            SensorMessage sensorMessage = (SensorMessage) arg;
            if (sensorMessage.getType() == SensorMessage.TYPE_NEW_DATA && id == sensorMessage.getSensorId()) {
                values[0] = sensorMessage.getMessage();
            }
        }
    }
}
