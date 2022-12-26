package ru.vukit.dc;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import ru.vukit.dc.database.DBDriver;
import ru.vukit.dc.sensors.DCSensor;
import ru.vukit.dc.sensors.SensorFactory;
import ru.vukit.dc.servres.DCServer;
import ru.vukit.dc.servres.ServerFactory;
import ru.vukit.dc.tasks.DCTask;
import ru.vukit.dc.tasks.TaskFactory;

import static ru.vukit.dc.SettingsFragment.KEY_BACKGROUND_SERVICE;
import static ru.vukit.dc.SettingsFragment.KEY_DATA_FORMAT;
import static ru.vukit.dc.SettingsFragment.KEY_SERVER_TIMEOUT;
import static ru.vukit.dc.SettingsFragment.KEY_UUID;

public class MainService extends Service {

    public static final int broadcastReceiverInitiator = 1;
    public static final int applicationInitiator = 2;

    SharedPreferences sharedPreferences;
    boolean backgroundService;
    NotificationManager notificationManager;
    final String CHANNEL_ID = "data_collector";
    Timer watchDogTasksTimer;
    Timer watchDogServersTimer;
    boolean isAppRunning = false;
    String uuid;
    byte sensorOutputFormat = 0;
    byte taskOutputFormat = 0;
    Set<String> dataFormat;
    TasksFragmentState tasksFragmentState;
    ServersFragmentState serversFragmentState;
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;
    final List<DCSensor> sensors = new ArrayList<>();
    final List<DCServer> servers = new ArrayList<>();
    final List<DCTask> tasks = new ArrayList<>();
    final HashMap<String, DCSensor> hashMapDCSensors = new HashMap<>();
    final HashMap<String, DCServer> hashMapDCServers = new HashMap<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        backgroundService = sharedPreferences.getBoolean(KEY_BACKGROUND_SERVICE, false);
        uuid = sharedPreferences.getString(KEY_UUID, "");
        dataFormat = sharedPreferences.getStringSet(KEY_DATA_FORMAT, null);
        if (dataFormat != null) {
            for (String dataFormatItem : dataFormat) {
                if (dataFormatItem.equals(getString(R.string.data_format_include_task_name))) {
                    taskOutputFormat += 1;
                } else if (dataFormatItem.equals(getString(R.string.data_format_include_uuid))) {
                    taskOutputFormat += 2;
                } else if (dataFormatItem.equals(getString(R.string.data_format_include_sensor_name))) {
                    sensorOutputFormat += 1;
                } else if (dataFormatItem.equals(getString(R.string.data_format_include_sensor_units))) {
                    sensorOutputFormat += 2;
                }
            }
        }
        DCServer.TIMEOUT = Integer.parseInt(sharedPreferences.getString(KEY_SERVER_TIMEOUT, "").split(" ")[0]);
        if (intent != null) {
            switch (intent.getIntExtra("launchInitiator", 0)) {
                case broadcastReceiverInitiator:
                    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        try {
                            notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Data Collector", NotificationManager.IMPORTANCE_DEFAULT));
                            Intent resultIntent = new Intent(this, MainActivity.class);
                            PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE);
                            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_logo)
                                    .setContentTitle("Data Collector")
                                    .setContentText("Share sensors data")
                                    .setContentIntent(resultPendingIntent)
                                    .build();
                            startForeground(1, notification);
                        } catch (Exception e) {
                            Log.i("ru.vukit.dc", e.getMessage());
                        }
                    }
                    break;
                case applicationInitiator:
                    isAppRunning = true;
                    break;
                default:
                    isAppRunning = false;
                    break;
            }
        }
        start();
        watchDogServers();
        watchDogTasks();
        return backgroundService && !isAppRunning ? START_STICKY : START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stop();
        if (backgroundService) {
            restartService();
        }
        super.onTaskRemoved(rootIntent);
    }

    @SuppressLint("MissingPermission")
    private void start() {
        DCSensor dcSensor;
        DCServer dcServer;
        DCTask dcTask;
        if (isAppRunning) {
            serversFragmentState = ServersFragmentState.getInstance();
            tasksFragmentState = TasksFragmentState.getInstance();
            DCSensor.sensorsFragmentState = SensorsFragmentState.getInstance();
        } else {
            serversFragmentState = null;
            tasksFragmentState = null;
            DCSensor.sensorsFragmentState = null;
        }
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Читаем из БД список задач и инициализируем tasks, а также необходимые датчики и сервера
        sensors.clear();
        servers.clear();
        tasks.clear();
        ArrayList<HashMap<String, String>> dbTasks = DBDriver.getInstance().selectEnabledTasks();
        for (HashMap<String, String> dbTask : dbTasks) {
            ArrayList<DCSensor> dcTaskSensors = new ArrayList<>();
            String[] taskSensors = new Gson().fromJson(dbTask.get("sensors"), String[].class);
            for (String taskSensor : taskSensors) {
                if (hashMapDCSensors.get(taskSensor) != null) {
                    dcTaskSensors.add(hashMapDCSensors.get(taskSensor));
                } else {
                    HashMap<String, String> newSensor = DBDriver.getInstance().selectSensor(taskSensor);
                    if (newSensor != null && Objects.equals(newSensor.get("enable"), "1")) {
                        dcSensor = SensorFactory.makeSensor(Integer.valueOf(Objects.requireNonNull(newSensor.get("id"))), Integer.valueOf(Objects.requireNonNull(newSensor.get("code"))), newSensor.get("name"), newSensor.get("settings"), sensorOutputFormat);
                        if (dcSensor != null) {
                            dcTaskSensors.add(dcSensor);
                            hashMapDCSensors.put(taskSensor, dcSensor);
                            if (dcSensor.code < 500) {
                                mSensorManager.registerListener((SensorEventListener) dcSensor, mSensorManager.getDefaultSensor(dcSensor.code), SensorManager.SENSOR_DELAY_FASTEST);
                            }
                            if (dcSensor.code == SensorFactory.SENSOR_TYPE_LOCATION) {
                                if ((ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                                        (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                                    mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                                    if (mLocationManager != null) {
                                        String[] settingsParts = dcSensor.settings.split(":");
                                        if (!settingsParts[0].isEmpty()) {
                                            mLocationManager.requestLocationUpdates(settingsParts[0], 1000, 1, (LocationListener) dcSensor);
                                        } else {
                                            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, (LocationListener) dcSensor);
                                        }
                                    }
                                }
                            }
                            sensors.add(dcSensor);
                        }
                    }
                }
            }
            DCServer dcTaskServer = null;
            String taskServer = dbTask.get("servers");
            if (hashMapDCServers.get(taskServer) != null) {
                dcTaskServer = hashMapDCServers.get(taskServer);
            } else {
                HashMap<String, String> newServer = DBDriver.getInstance().selectServer(taskServer);
                if (newServer != null && Objects.equals(newServer.get("enable"), "1")) {
                    dcServer = ServerFactory.makeServer(this, newServer.get("id"), Objects.requireNonNull(newServer.get("url")), newServer.get("username"), newServer.get("password"), newServer.get("protocol"));
                    if (dcServer != null) {
                        dcTaskServer = dcServer;
                        hashMapDCServers.put(taskServer, dcServer);
                        servers.add(dcServer);
                    }
                }
            }
            dcTask = TaskFactory.makeTask(dbTask.get("id"), uuid, dbTask.get("name"), dcTaskSensors, dcTaskServer, dbTask.get("schedule"), taskOutputFormat);
            if (dcTask != null) {
                tasks.add(dcTask);
            }
        }
        hashMapDCSensors.clear();
        hashMapDCServers.clear();
        for (DCTask task : tasks) {
            task.run();
        }
    }

    private void stop() {
        for (DCTask task : tasks) {
            task.stop();
        }
        for (DCSensor sensor : sensors) {
            if (sensor.code < 500) {
                mSensorManager.unregisterListener((SensorEventListener) sensor);
            }
            if (sensor.code == SensorFactory.SENSOR_TYPE_LOCATION && mLocationManager != null) {
                mLocationManager.removeUpdates((LocationListener) sensor);
            }
        }
        if (watchDogTasksTimer != null) {
            watchDogTasksTimer.cancel();
            watchDogTasksTimer = null;
        }
        if (watchDogServersTimer != null) {
            watchDogServersTimer.cancel();
            watchDogServersTimer = null;
        }
        for (DCServer server : servers) {
            server.disconnect();
        }
        DCSensor.sensorsFragmentState = null;
        sensors.clear();
        servers.clear();
        tasks.clear();
    }

    private void watchDogServers() {
        watchDogServersTimer = new Timer();
        watchDogServersTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (DCServer server : servers) {
                    if (serversFragmentState != null) {
                        serversFragmentState.setServerStatus(server.id, server.status);
                    }
                    server.connect();
                }

            }
        }, 0, DCServer.TIMEOUT);
    }

    private void watchDogTasks() {
        watchDogTasksTimer = new Timer();
        watchDogTasksTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (DCTask task : tasks) {
                    if (tasksFragmentState != null) {
                        tasksFragmentState.setTaskStatus(task.id, task.status);
                    }
                }
            }
        }, 0, DCTask.TIMEOUT);
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void restartService() {
        Log.d("DC", "RESTART");
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("ru.vukit.dc.RESTART_MAIN_SERVICE");
        broadcastIntent.setClass(getApplicationContext(), MainBroadcastReceiver.class);
        PendingIntent restartServicePI = PendingIntent.getBroadcast(getApplicationContext(), 1, broadcastIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmService != null) {
            alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 3000, restartServicePI);
        }
    }
}
