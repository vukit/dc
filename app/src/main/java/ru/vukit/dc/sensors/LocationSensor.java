package ru.vukit.dc.sensors;

import android.location.Location;
import android.location.LocationListener;

class LocationSensor extends DCSensor implements LocationListener {

    private final Double[] values;

    LocationSensor(Integer id, Integer code, String name, String settings, byte format) {
        super(id, code, name, settings, format);
        this.values = new Double[valuesLength];
    }

    @Override
    public String getValue(int i) {
        return String.valueOf(values[i]);
    }

    @Override
    public void onLocationChanged(Location location) {
        values[0] = location.getLatitude();
        values[1] = location.getLongitude();
        values[2] = location.getAltitude();
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

}
