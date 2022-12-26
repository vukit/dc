package ru.vukit.dc.sensors;

import java.util.Observable;
import java.util.Observer;

import ru.vukit.dc.database.DBDriver;

class XYSensor extends DCSensor implements Observer {

    private final String[] values;

    XYSensor(Integer id, Integer code, String name, String settings, byte format) {
        super(id, code, name, settings, format);
        this.values = new String[valuesLength];
        String dbValues = DBDriver.getInstance().selectSensorValue(String.valueOf(id));
        for (int k = 0; k < valuesLength; k++) {
            try {
                this.values[k] = dbValues.split(",")[k];
            } catch (IndexOutOfBoundsException ex) {
                this.values[k] = "NaN";
            }
        }
        try {
            if (!settings.split(":")[5].equals("1")) {
                valuesLength = 1;
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            valuesLength = 1;
        }
        if (sensorsFragmentState != null) {
            sensorsFragmentState.addObserver(this);
        }
    }

    @Override
    public String getValue(int i) {
        return values[i];
    }

    @Override
    public void update(Observable o, Object arg) {
        String observable_name = o.getClass().getCanonicalName();
        if (observable_name != null && observable_name.equals("ru.vukit.dc.SensorsFragmentState")) {
            SensorMessage sensorMessage = (SensorMessage) arg;
            if (sensorMessage.getType() == SensorMessage.TYPE_NEW_DATA && id == sensorMessage.getSensorId()) {
                String settings = sensorMessage.getSettings();
                valuesLength = Integer.parseInt(settings.split(":")[1]);
                String messageValues = sensorMessage.getMessage();
                for (int k = 0; k < valuesLength; k++) {
                    try {
                        this.values[k] = messageValues.split(",")[k];
                    } catch (IndexOutOfBoundsException ex) {
                        this.values[k] = "";
                    }
                }
            }
        }
    }

}
