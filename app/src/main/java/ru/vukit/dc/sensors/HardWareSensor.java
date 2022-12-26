package ru.vukit.dc.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

class HardWareSensor extends DCSensor implements SensorEventListener {

    private final Float[] values;

    HardWareSensor(Integer id, Integer code, String name, String settings, byte format) {
        super(id, code, name, settings, format);
        this.values = new Float[valuesLength];
    }

    @Override
    public String getValue(int i) {
        return String.valueOf(values[i]);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        for (int k = 0; k < valuesLength; k++) {
            values[k] = sensorEvent.values[k];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

}
