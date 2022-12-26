package ru.vukit.dc;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.location.LocationManager;

import ru.vukit.dc.database.DBDriver;

class LocationSensorFragmentState {
    static final double notLatitude = 360.0;
    static final double notLongitude = 360.0;
    static final private double notAltitude = -1;

    double sensorLatitude = notLatitude;
    double sensorLongitude = notLongitude;
    double sensorAltitude = notAltitude;
    String id;
    String name;
    int code;
    int valuesLength;
    boolean enable;
    String oldName;
    boolean oldEnable;
    String locationProvider, oldLocationProvider;

    private static LocationSensorFragmentState INSTANCE = null;

    private LocationSensorFragmentState() {
    }

    public static synchronized LocationSensorFragmentState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LocationSensorFragmentState();
        }
        return (INSTANCE);
    }

    @SuppressLint("Range")
    void setupSensor(Cursor cursor) {
        id = cursor.getString(cursor.getColumnIndex(DBDriver.DataBaseContract.Sensors._ID));
        name = cursor.getString(cursor.getColumnIndex(DBDriver.DataBaseContract.Sensors.NAME));
        code = Integer.parseInt(cursor.getString(cursor.getColumnIndex(DBDriver.DataBaseContract.Sensors.CODE)));
        enable = cursor.getString(cursor.getColumnIndex(DBDriver.DataBaseContract.Sensors.ENABLE)).equals("1");
        oldName = name;
        oldEnable = enable;
        String[] settingsParts = cursor.getString(cursor.getColumnIndex(DBDriver.DataBaseContract.Sensors.SETTINGS)).split(":");
        locationProvider = LocationManager.GPS_PROVIDER;
        if (!settingsParts[0].isEmpty()) {
            locationProvider = settingsParts[0];
        }
        oldLocationProvider = locationProvider;
    }

}
