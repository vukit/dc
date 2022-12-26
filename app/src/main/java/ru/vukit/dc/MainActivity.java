package ru.vukit.dc;

import static ru.vukit.dc.SettingsFragment.KEY_FIRST_RUN;
import static ru.vukit.dc.SettingsFragment.KEY_SERVER_TIMEOUT;
import static ru.vukit.dc.SettingsFragment.KEY_STARTUP_SCREEN;
import static ru.vukit.dc.SettingsFragment.KEY_UUID;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.maps.MapView;
import com.google.android.material.navigation.NavigationView;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.vukit.dc.database.DBDriver;
import ru.vukit.dc.sensors.SensorFactory;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, MainActivityInterface {

    final MainActivityState state = MainActivityState.getInstance();
    SharedPreferences sharedPreferences;
    NavigationView navigationView;
    private static final int PERMISSIONS_REQUEST_INTERNET = 1;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2;
    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setupPreferences();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getHeaderView(0).findViewById(R.id.drawer_header).setOnClickListener(v -> {
            selectAction(getString(R.string.action_about));
            drawer.closeDrawer(GravityCompat.START);
        });
        if (!state.isFirstStart) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                Context context = StartApplication.getInstance().getApplicationContext();
                Intent mainServiceIntent = new Intent(context, MainService.class);
                mainServiceIntent.putExtra("launchInitiator", MainService.applicationInitiator);
                context.stopService(mainServiceIntent);
                context.startService(mainServiceIntent);
            });
            Thread mapViewInit = new Thread(() -> {
                try {
                    MapView mv = new MapView(getApplicationContext());
                    mv.onCreate(null);
                    mv.onPause();
                    mv.onDestroy();
                } catch (Exception ignored) {
                }
            });
            mapViewInit.setContextClassLoader(getClass().getClassLoader());
            mapViewInit.start();
            state.isFirstStart = true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        state.connectController(this);
        String selectedAction = state.fragmentStack.peek();
        if (selectedAction == null) {
            selectAction(sharedPreferences.getString(KEY_STARTUP_SCREEN, getString(R.string.action_about)));
        } else {
            selectAction(selectedAction);
        }
        if (!state.isCheckedPermission) {
            state.permissionInternet = (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED);
            if (!state.permissionInternet)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, PERMISSIONS_REQUEST_INTERNET);
            state.permissionAccessFineLocation = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            if (!state.permissionAccessFineLocation)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            state.permissionAccessCoarseLocation = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            if (!state.permissionAccessCoarseLocation)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            state.isCheckedPermission = true;
        }
    }

    @Override
    protected void onStop() {
        state.disconnectController();
        super.onStop();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_INTERNET:
                state.permissionInternet = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                state.permissionAccessFineLocation = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
            case PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION:
                state.permissionAccessCoarseLocation = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (state.fragmentStack.poll() != null) {
                if (!state.fragmentStack.isEmpty()) {
                    selectAction(state.fragmentStack.pop());
                } else {
                    super.onBackPressed();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        selectAction(item.getTitle().toString());
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void selectAction(String selectedAction) {
        if (!selectedAction.equals(state.fragmentStack.peek())) {
            if (selectedAction.equals(getString(R.string.action_settings))) {
                addFragment(new SettingsFragment());
                navigationView.setCheckedItem(R.id.action_settings);
            } else if (selectedAction.equals(getString(R.string.action_sensors))) {
                addFragment(new SensorsFragment());
                navigationView.setCheckedItem(R.id.action_sensors);
            } else if (selectedAction.equals(getString(R.string.action_servers))) {
                addFragment(new ServersFragment());
                navigationView.setCheckedItem(R.id.action_servers);
            } else if (selectedAction.equals(getString(R.string.action_tasks))) {
                addFragment(new TasksFragment());
                navigationView.setCheckedItem(R.id.action_tasks);
            } else if (selectedAction.equals(getString(R.string.action_about))) {
                addFragment(new About());
                navigationView.setCheckedItem(R.id.action_about);
            } else if (selectedAction.equals(getString(R.string.action_show_location_sensor))) {
                addFragment(new LocationSensorFragment());
            } else if (selectedAction.equals(getString(R.string.action_show_hardware_sensor))) {
                addFragment(new HardwareSensorFragment());
            } else if (selectedAction.equals(getString(R.string.action_show_text_sensor))) {
                addFragment(new TextSensorFragment());
            } else if (selectedAction.equals(getString(R.string.action_show_mosaic_sensor))) {
                addFragment(new MosaicSensorFragment());
            } else if (selectedAction.equals(getString(R.string.action_show_xy_sensor))) {
                addFragment(new XYSensorFragment());
            }
            if (state.fragmentStack.isEmpty()) {
                state.fragmentStack.push(selectedAction);
            } else {
                if (!selectedAction.equals(state.fragmentStack.peek())) {
                    state.fragmentStack.push(selectedAction);
                }
            }
        }
    }

    private void addFragment(Fragment new_fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_container, new_fragment);
        fragmentTransaction.commit();
    }

    void setupPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (sharedPreferences.getBoolean(KEY_FIRST_RUN, true)) {
            editor.putString(KEY_SERVER_TIMEOUT, getString(R.string.msec2000));
            editor.putBoolean(KEY_FIRST_RUN, false);
            editor.putString(KEY_STARTUP_SCREEN, getString(R.string.action_sensors));
            editor.putString(KEY_UUID, UUID.randomUUID().toString().toUpperCase());
            editor.apply();
            SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (mSensorManager != null) {
                List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
                if (deviceSensors != null) {
                    for (Sensor sensor : deviceSensors) {
                        if (SensorFactory.parameters.get(sensor.getType()) != null && !DBDriver.getInstance().isPresentSensorByCode(sensor.getType())) {
                            DBDriver.getInstance().createSensor(String.valueOf(sensor.getType()), SensorFactory.parameters.get(sensor.getType())[2], "");
                        }
                    }
                }
            }
            DBDriver.getInstance().createSensor(String.valueOf(SensorFactory.SENSOR_TYPE_LOCATION), SensorFactory.parameters.get(SensorFactory.SENSOR_TYPE_LOCATION)[2], LocationManager.GPS_PROVIDER);
        }
    }

}
