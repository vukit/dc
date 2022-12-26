package ru.vukit.dc.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import ru.vukit.dc.R;
import ru.vukit.dc.StartApplication;

public class SensorFactory {

    public static final int SENSOR_TYPE_LOCATION = 500;
    public static final int SENSOR_TYPE_TEXT = 1024;
    public static final int SENSOR_TYPE_MOSAIC = 1025;
    public static final int SENSOR_TYPE_XY = 1026;

    public static final SparseArray<String[]> parameters = new SparseArray<>();

    static {
        Context context = StartApplication.getInstance().getApplicationContext();
        parameters.put(SENSOR_TYPE_TEXT, new String[]{"TextSensor", "1", "Text", "", ""});
        parameters.put(SENSOR_TYPE_MOSAIC, new String[]{"MosaicSensor", "1", "Mosaic", "", ""});
        parameters.put(SENSOR_TYPE_XY, new String[]{"XYSensor", "2", "XY sensor", "", "", "Y", "X"});
        parameters.put(SENSOR_TYPE_LOCATION, new String[]{"LocationSensor", "3", "Location", "deg", "deg", "m", context.getString(R.string.latitude), context.getString(R.string.longitude), context.getString(R.string.altitude)});
        parameters.put(Sensor.TYPE_ACCELEROMETER, new String[]{"HardWareSensor", "3", "Accelerometer", "m/s^2", "m/s^2", "m/s^2", "X", "Y", "Z"});
        parameters.put(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED, new String[]{"HardWareSensor", "6", "Accelerometer uncalibrated", "m/s^2", "m/s^2", "m/s^2", "m/s^2", "m/s^2", "m/s^2", "X", "Y", "Z", "Xc", "Yc", "Zc"});
        parameters.put(Sensor.TYPE_AMBIENT_TEMPERATURE, new String[]{"HardWareSensor", "1", "Ambient temperature", "degC", "Temperature"});
        parameters.put(Sensor.TYPE_GAME_ROTATION_VECTOR, new String[]{"HardWareSensor", "3", "Game rotation vector", "", "", "", "X", "Y", "Z"});
        parameters.put(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, new String[]{"HardWareSensor", "3", "Geomagnetic rotation vector", "", "", "", "X", "Y", "Z"});
        parameters.put(Sensor.TYPE_GRAVITY, new String[]{"HardWareSensor", "3", "Gravity", "m/s^2", "m/s^2", "m/s^2", "X", "Y", "Z"});
        parameters.put(Sensor.TYPE_GYROSCOPE, new String[]{"HardWareSensor", "3", "Gyroscope", "rad/s", "rad/s", "rad/s", "X", "Y", "Z"});
        parameters.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, new String[]{"HardWareSensor", "6", "Gyroscope uncalibrated", "rad/s", "rad/s", "rad/s", "rad/s", "rad/s", "rad/s", "X", "Y", "Z", "Xd", "Yd", "Zd"});
        parameters.put(Sensor.TYPE_HEART_BEAT, new String[]{"HardWareSensor", "1", "Heart beat", "", "Heart beat"});
        parameters.put(Sensor.TYPE_HEART_RATE, new String[]{"HardWareSensor", "1", "Heart rate", "bpm", "Heart rate"});
        parameters.put(Sensor.TYPE_LIGHT, new String[]{"HardWareSensor", "1", "Light", "lux", "Light"});
        parameters.put(Sensor.TYPE_LINEAR_ACCELERATION, new String[]{"HardWareSensor", "3", "Linear acceleration", "m/s^2", "m/s^2", "m/s^2", "X", "Y", "Z"});
        parameters.put(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT, new String[]{"HardWareSensor", "1", "Low latency off-body detect", "", "Value 1"});
        parameters.put(Sensor.TYPE_MAGNETIC_FIELD, new String[]{"HardWareSensor", "3", "Magnetic field", "uT", "uT", "uT", "X", "Y", "Z"});
        parameters.put(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, new String[]{"HardWareSensor", "6", "Magnetic field uncalibrated", "uT", "uT", "uT", "uT", "uT", "uT", "X", "Y", "Z", "Xb", "Yb", "Zb"});
        parameters.put(Sensor.TYPE_MOTION_DETECT, new String[]{"HardWareSensor", "1", "Motion detect", "", "Value 1"});
        parameters.put(Sensor.TYPE_POSE_6DOF, new String[]{"HardWareSensor", "15", "POSE 6DOF", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "Value 1", "Value 2", "Value 3", "Value 4", "Value 5", "Value 6", "Value 7", "Value 8", "Value 9", "Value 10", "Value 11", "Value 12", "Value 13", "Value 14", "Value 15"});
        parameters.put(Sensor.TYPE_PRESSURE, new String[]{"HardWareSensor", "1", "Pressure", "hPa", "Pressure"});
        parameters.put(Sensor.TYPE_PROXIMITY, new String[]{"HardWareSensor", "1", "Proximity", "cm", "Proximity"});
        parameters.put(Sensor.TYPE_RELATIVE_HUMIDITY, new String[]{"HardWareSensor", "1", "Relative humidity", "%", "Humidity"});
        parameters.put(Sensor.TYPE_ROTATION_VECTOR, new String[]{"HardWareSensor", "5", "Rotation vector", "", "", "", "", "rad", "X", "Y", "Z", "W", "EHA"});
        parameters.put(Sensor.TYPE_SIGNIFICANT_MOTION, new String[]{"HardWareSensor", "1", "Significant motion", "", "Value 1"});
        parameters.put(Sensor.TYPE_STATIONARY_DETECT, new String[]{"HardWareSensor", "1", "Stationary detect", "", "Value 1"});
        parameters.put(Sensor.TYPE_STEP_COUNTER, new String[]{"HardWareSensor", "1", "Step counter", "", "Value 1"});
        parameters.put(Sensor.TYPE_STEP_DETECTOR, new String[]{"HardWareSensor", "1", "Step detector", "", "Value 1"});
    }

    @Nullable
    public static DCSensor makeSensor(Integer id, Integer code, String name, String settings, byte format) {
        if (parameters.get(code) != null) {
            switch (parameters.get(code)[0]) {
                case "HardWareSensor":
                    return new HardWareSensor(id, code, name, settings, format);
                case "LocationSensor":
                    return new LocationSensor(id, code, name, settings, format);
                case "TextSensor":
                    return new TextSensor(id, code, name, settings, format);
                case "MosaicSensor":
                    return new MosaicSensor(id, code, name, settings, format);
                case "XYSensor":
                    return new XYSensor(id, code, name, settings, format);
            }
        }
        return null;
    }

}
