package ru.vukit.dc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;

import ru.vukit.dc.database.DBDriver;
import ru.vukit.dc.sensors.SensorFactory;

@Keep
public class LocationSensorFragment extends Fragment implements LocationListener, OnMapReadyCallback {

    final LocationSensorFragmentState state = LocationSensorFragmentState.getInstance();
    MapView mapView;
    GoogleMap map;
    Marker marker;
    LocationManager mLocationManager;
    TextView tvValues;
    EditText name;
    CheckBox enable;
    Spinner provider;
    final HashMap<String, Integer> arrayLocationProvider = new HashMap<>() {{
        put(LocationManager.GPS_PROVIDER, 0);
        put(LocationManager.NETWORK_PROVIDER, 1);
        put(LocationManager.PASSIVE_PROVIDER, 2);
    }};

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.location_sensor, container, false);
        name = view.findViewById(R.id.location_sensor_name);
        name.setText(state.name);
        enable = view.findViewById(R.id.location_sensor_enable);
        enable.setChecked(state.enable);
        enable.setOnClickListener(view1 -> {
            if (enable.isChecked()) {
                stopLocationManager();
                startLocationManager();
                state.enable = true;
            } else {
                stopLocationManager();
                state.enable = false;
            }
        });
        provider = view.findViewById(R.id.location_provider);
        provider.setSelection(0);
        Integer locationProviderId = arrayLocationProvider.get(state.locationProvider);
        if (locationProviderId != null) {
            provider.setSelection(locationProviderId);
        }
        provider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                for (HashMap.Entry<String, Integer> entry : arrayLocationProvider.entrySet()) {
                    if (entry.getValue() == i) {
                        state.locationProvider = entry.getKey();
                        stopLocationManager();
                        startLocationManager();
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        state.valuesLength = Integer.parseInt(SensorFactory.parameters.get(state.code)[1]);
        CharSequence valueNamesInfo = "";
        SpannableString valueNameInfo;
        String newLines = "\n\n";
        for (int i = 0; i < state.valuesLength; i++) {
            if (i == state.valuesLength - 1) newLines = "\n";
            valueNameInfo = new SpannableString(SensorFactory.parameters.get(state.code)[i + 3 + state.valuesLength] + " (" + SensorFactory.parameters.get(state.code)[i + 3] + "): " + newLines);
            valueNamesInfo = TextUtils.concat(valueNamesInfo, valueNameInfo);
        }
        ((TextView) view.findViewById(R.id.location_sensor_values_name)).setText(valueNamesInfo);
        tvValues = view.findViewById(R.id.location_sensor_values);
        if (state.enable) {
            startLocationManager();
        }
        mapView = view.findViewById(R.id.location_sensor_map_view);
        mapView.onCreate(null);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getResources().getString(R.string.edit_sensor_dialog_title));
        }
        if (mapView != null) {
            mapView.onResume();
            mapView.getMapAsync(this);
        }
    }

    @Override
    public void onPause() {
        stopLocationManager();
        if (mapView != null) mapView.onPause();
        if ((!name.getText().toString().trim().isEmpty() && !name.getText().toString().trim().equals(state.oldName)) ||
                enable.isChecked() != state.oldEnable || !state.locationProvider.equals(state.oldLocationProvider)) {
            DBDriver.getInstance().updateSensor(state.id, name.getText().toString().trim(), state.locationProvider, "", (enable.isChecked() ? "1" : "0"), "1");
        }
        super.onPause();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setMapToolbarEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);
        uiSettings.setZoomControlsEnabled(false);
        LatLng center_point;
        if (!(state.sensorLatitude == LocationSensorFragmentState.notLatitude || state.sensorLongitude == LocationSensorFragmentState.notLongitude)) {
            center_point = new LatLng(state.sensorLatitude, state.sensorLongitude);
        } else {
            center_point = new LatLng(0.0, 0.0);
        }
        map.moveCamera(CameraUpdateFactory.newLatLng(center_point));
        marker = map.addMarker(new MarkerOptions().position(center_point));
        if (marker != null) {
            marker.setIcon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_myplaces));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        updateMap(location);
    }

    private void updateMap(Location location) {
        CharSequence values_info = "";
        SpannableString value_info;
        if (location != null) {
            state.sensorLatitude = location.getLatitude();
            state.sensorLongitude = location.getLongitude();
            state.sensorAltitude = location.getAltitude();
        }
        LatLng center_point;
        if (!(state.sensorLatitude == LocationSensorFragmentState.notLatitude || state.sensorLongitude == LocationSensorFragmentState.notLongitude)) {
            value_info = new SpannableString(state.sensorLatitude + "\n\n");
            values_info = TextUtils.concat(values_info, value_info);
            value_info = new SpannableString(state.sensorLongitude + "\n\n");
            values_info = TextUtils.concat(values_info, value_info);
            value_info = new SpannableString(state.sensorAltitude + "\n");
            values_info = TextUtils.concat(values_info, value_info);
            tvValues.setText(values_info);
            center_point = new LatLng(state.sensorLatitude, state.sensorLongitude);
        } else {
            center_point = new LatLng(0.0, 0.0);
        }
        if (map != null) {
            map.moveCamera(CameraUpdateFactory.newLatLng(center_point));
        }
        if (marker != null) {
            marker.setPosition(center_point);
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationManager() {
        Location location;
        if ((ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            mLocationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
            if (mLocationManager != null) {
                mLocationManager.requestLocationUpdates(state.locationProvider, 1000, 1, this);
                location = mLocationManager.getLastKnownLocation(state.locationProvider);
                updateMap(location);
            }
        }
    }

    private void stopLocationManager() {
        if (mLocationManager != null) mLocationManager.removeUpdates(this);
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

}
