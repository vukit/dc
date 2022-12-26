package ru.vukit.dc.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;

import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.vukit.dc.FragmentInterface;
import ru.vukit.dc.MainService;
import ru.vukit.dc.R;
import ru.vukit.dc.SnackBar;
import ru.vukit.dc.StartApplication;

@Keep
public class DBDriver {

    private static DBDriver INSTANCE = null;

    private DBDriver() {
    }

    public static synchronized DBDriver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DBDriver();
        }
        return (INSTANCE);
    }

    private final DataBaseHelper databaseHelper = DataBaseHelper.getInstance(StartApplication.getInstance().getApplicationContext());
    private final SQLiteDatabase db = databaseHelper.getWritableDatabase();

    private FragmentInterface controller;
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final Handler mainLooperHandler = new Handler(Looper.getMainLooper());

    public Cursor rawQuery(String query) {
        return db.rawQuery(query, null);
    }

    public static final class DataBaseContract {

        private DataBaseContract() {
        }

        static final int DATABASE_VERSION = 1;
        static final String DATABASE_NAME = "dc.db";
        static final String[] SQL_CREATE_TABLE_ARRAY = {Sensors.CREATE_TABLE, Servers.CREATE_TABLE, Tasks.CREATE_TABLE};
        static final String[] SQL_UPGRADE_TABLE_ARRAY = {Sensors.UPGRADE_TABLE, Servers.UPGRADE_TABLE, Tasks.UPGRADE_TABLE};

        public static final class Sensors implements BaseColumns {
            private Sensors() {
            }

            public final static String TABLE_NAME = "sensors";
            public final static String CODE = "code";
            public final static String NAME = "name";
            public final static String SETTINGS = "settings";
            public final static String VALUE = "value";
            public final static String ENABLE = "enable";
            final static String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                    + _ID + " INTEGER PRIMARY KEY,"
                    + CODE + " TEXT,"
                    + NAME + " TEXT,"
                    + SETTINGS + " TEXT,"
                    + VALUE + " TEXT,"
                    + ENABLE + " TEXT"
                    + ");";
            final static String UPGRADE_TABLE = "";
        }

        public static final class Servers implements BaseColumns {
            private Servers() {
            }

            public final static String TABLE_NAME = "servers";
            public final static String NAME = "name";
            public final static String URL = "url";
            public final static String USERNAME = "username";
            public final static String PASSWORD = "password";
            public final static String PROTOCOL = "protocol";
            public final static String ENABLE = "enable";
            final static String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                    + _ID + " INTEGER PRIMARY KEY,"
                    + NAME + " TEXT,"
                    + URL + " TEXT,"
                    + USERNAME + " TEXT,"
                    + PASSWORD + " TEXT,"
                    + PROTOCOL + " TEXT,"
                    + ENABLE + " TEXT"
                    + ");";
            final static String UPGRADE_TABLE = "";
        }

        public static final class Tasks implements BaseColumns {
            private Tasks() {
            }

            public final static String TABLE_NAME = "tasks";
            public final static String NAME = "name";
            public final static String SENSORS = "sensors";
            public final static String SERVERS = "servers";
            public final static String SCHEDULE = "schedule";
            public final static String ENABLE = "enable";
            final static String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                    + _ID + " INTEGER PRIMARY KEY,"
                    + NAME + " TEXT,"
                    + SENSORS + " TEXT,"
                    + SERVERS + " TEXT,"
                    + SCHEDULE + " TEXT,"
                    + ENABLE + " TEXT"
                    + ");";
            final static String UPGRADE_TABLE = "";
        }
    }

    private static class DataBaseHelper extends SQLiteOpenHelper {

        private static DataBaseHelper DatabaseHelperHolder = null;

        public static DataBaseHelper getInstance(Context context) {
            if (DatabaseHelperHolder == null) DatabaseHelperHolder = new DataBaseHelper(context);
            return DatabaseHelperHolder;
        }

        private DataBaseHelper(Context context) {
            super(context, DataBaseContract.DATABASE_NAME, null, DataBaseContract.DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            for (String table : DataBaseContract.SQL_CREATE_TABLE_ARRAY) db.execSQL(table);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            for (String table : DataBaseContract.SQL_UPGRADE_TABLE_ARRAY) db.execSQL(table);
        }
    }

    public void connectController(final FragmentInterface controller) {
        this.controller = controller;
    }

    public void disconnectController() {
        this.controller = null;
    }

    public void updateFragment(String dbMessage) {
        if (this.controller != null) {
            this.controller.updateView();
            new SnackBar().ShowLong(dbMessage);
        }
    }

    public void createSensor(String code, String name, String settings) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DataBaseContract.Sensors.CODE, code);
            values.put(DataBaseContract.Sensors.NAME, name);
            values.put(DataBaseContract.Sensors.SETTINGS, settings);
            values.put(DataBaseContract.Sensors.VALUE, "");
            values.put(DataBaseContract.Sensors.ENABLE, "1");
            DBDriver.getInstance().db.insert(DataBaseContract.Sensors.TABLE_NAME, null, values);
            mainLooperHandler.post(() -> updateFragment(StartApplication.getInstance().getString(R.string.sensor_created)));
        });
    }

    public HashMap<String, String> selectSensor(String id) {
        HashMap<String, String> sensor = null;
        Cursor cursor = db.query(
                DataBaseContract.Sensors.TABLE_NAME,
                null,
                DataBaseContract.Sensors._ID + " = ?",
                new String[]{id},
                null,
                null,
                null
        );
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            sensor = new HashMap<>();
            sensor.put("id", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors._ID)));
            sensor.put("code", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.CODE)));
            sensor.put("name", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.NAME)));
            sensor.put("settings", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.SETTINGS)));
            sensor.put("value", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.VALUE)));
            sensor.put("enable", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.ENABLE)));
        }
        cursor.close();
        return sensor;
    }

    public Boolean isPresentSensorByCode(int code) {
        boolean result;
        Cursor cursor = db.query(
                DataBaseContract.Sensors.TABLE_NAME,
                null,
                DataBaseContract.Sensors.CODE + " = ?",
                new String[]{String.valueOf(code)},
                null,
                null,
                null
        );
        result = cursor.getCount() != 0;
        cursor.close();
        return result;
    }

    public String selectSensorValue(String id) {
        String value = "";
        Cursor cursor = db.query(
                DataBaseContract.Sensors.TABLE_NAME,
                null,
                DataBaseContract.Sensors._ID + " = ?",
                new String[]{id},
                null,
                null,
                null
        );
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            value = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.VALUE));
        }
        cursor.close();
        return value;
    }

    public String selectEnabledSensorName(String id) {
        String sensorName = "";
        Cursor cursor = db.query(
                DataBaseContract.Sensors.TABLE_NAME,
                null,
                DataBaseContract.Sensors._ID + " = ? and " + DataBaseContract.Sensors.ENABLE + " = 1",
                new String[]{id},
                null,
                null,
                null
        );
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            sensorName = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.NAME));
        }
        cursor.close();
        return sensorName;
    }

    public ArrayList<HashMap<String, String>> selectEnabledSensors() {
        ArrayList<HashMap<String, String>> sensors = new ArrayList<>();
        Cursor cursor = db.query(
                DataBaseContract.Sensors.TABLE_NAME,
                null,
                DataBaseContract.Sensors.ENABLE + " = ?",
                new String[]{"1"},
                null,
                null,
                null
        );
        if (cursor.getCount() != 0) {
            HashMap<String, String> sensor;
            while (cursor.moveToNext()) {
                sensor = new HashMap<>();
                sensor.put("id", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors._ID)));
                sensor.put("code", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.CODE)));
                sensor.put("name", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.NAME)));
                sensor.put("settings", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.SETTINGS)));
                sensor.put("value", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.VALUE)));
                sensor.put("enable", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Sensors.ENABLE)));
                sensors.add(sensor);
            }
        }
        cursor.close();
        return sensors;
    }

    public void updateSensor(String id, String name, String settings, String value, String enable, String restartService) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DataBaseContract.Sensors.NAME, name);
            values.put(DataBaseContract.Sensors.SETTINGS, settings);
            values.put(DataBaseContract.Sensors.VALUE, value);
            values.put(DataBaseContract.Sensors.ENABLE, enable);
            DBDriver.getInstance().db.update(DataBaseContract.Sensors.TABLE_NAME, values, DataBaseContract.Sensors._ID + " = ?", new String[]{id});
            if (restartService.equals("1")) {
                DBDriver.getInstance().restartService();
            }
            mainLooperHandler.post(() -> updateFragment(StartApplication.getInstance().getString(R.string.sensor_updated)));
        });
    }

    public void deleteSensor(String sensorId) {
        executor.execute(() -> {
            DBDriver.getInstance().db.delete(DataBaseContract.Sensors.TABLE_NAME, DataBaseContract.Sensors._ID + " = ?", new String[]{sensorId});
            DBDriver.getInstance().restartService();
            mainLooperHandler.post(() -> updateFragment(StartApplication.getInstance().getString(R.string.sensor_deleted)));
        });
    }

    public void createServer(String name, String url, String username, String password, String protocol, String enable) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DataBaseContract.Servers.NAME, name);
            values.put(DataBaseContract.Servers.URL, url);
            values.put(DataBaseContract.Servers.USERNAME, username);
            values.put(DataBaseContract.Servers.PASSWORD, password);
            values.put(DataBaseContract.Servers.PROTOCOL, protocol);
            values.put(DataBaseContract.Servers.ENABLE, enable);
            DBDriver.getInstance().db.insert(DataBaseContract.Servers.TABLE_NAME, null, values);
            mainLooperHandler.post(() -> updateFragment(StartApplication.getInstance().getString(R.string.server_created)));
        });
    }

    public HashMap<String, String> selectServer(String id) {
        HashMap<String, String> server = null;
        Cursor cursor = db.query(
                DataBaseContract.Servers.TABLE_NAME,
                null,
                DataBaseContract.Servers._ID + " = ?",
                new String[]{id},
                null,
                null,
                null
        );
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            server = new HashMap<>();
            server.put("id", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers._ID)));
            server.put("name", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.NAME)));
            server.put("url", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.URL)));
            server.put("username", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.USERNAME)));
            server.put("password", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.PASSWORD)));
            server.put("protocol", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.PROTOCOL)));
            server.put("enable", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.ENABLE)));
        }
        cursor.close();
        return server;
    }

    public String selectServerName(String id) {
        String serverName = "";
        Cursor cursor = db.query(
                DataBaseContract.Servers.TABLE_NAME,
                null,
                DataBaseContract.Servers._ID + " = ?",
                new String[]{id},
                null,
                null,
                null
        );
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            serverName = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.NAME));
        }
        cursor.close();
        return serverName;
    }

    public ArrayList<HashMap<String, String>> selectEnabledServers() {
        ArrayList<HashMap<String, String>> servers = new ArrayList<>();
        Cursor cursor = db.query(
                DataBaseContract.Servers.TABLE_NAME,
                null,
                DataBaseContract.Servers.ENABLE + " = ?",
                new String[]{"1"},
                null,
                null,
                null
        );
        if (cursor.getCount() != 0) {
            HashMap<String, String> server;
            while (cursor.moveToNext()) {
                server = new HashMap<>();
                server.put("id", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers._ID)));
                server.put("name", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.NAME)));
                server.put("url", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.URL)));
                server.put("username", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.USERNAME)));
                server.put("password", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.PASSWORD)));
                server.put("protocol", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.PROTOCOL)));
                server.put("enable", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.ENABLE)));
                servers.add(server);
            }
        }
        cursor.close();
        return servers;
    }

    public ArrayList<HashMap<String, String>> selectServers() {
        ArrayList<HashMap<String, String>> servers = new ArrayList<>();
        Cursor cursor = db.query(
                DataBaseContract.Servers.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null
        );
        if (cursor.getCount() != 0) {
            HashMap<String, String> server;
            while (cursor.moveToNext()) {
                server = new HashMap<>();
                server.put("id", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers._ID)));
                server.put("name", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.NAME)));
                server.put("url", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.URL)));
                server.put("username", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.USERNAME)));
                server.put("password", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.PASSWORD)));
                server.put("protocol", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.PROTOCOL)));
                server.put("enable", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Servers.ENABLE)));
                servers.add(server);
            }
        }
        cursor.close();
        return servers;
    }

    public void updateServer(String id, String name, String url, String username, String password, String protocol, String enable) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DataBaseContract.Servers.NAME, name);
            values.put(DataBaseContract.Servers.URL, url);
            values.put(DataBaseContract.Servers.USERNAME, username);
            values.put(DataBaseContract.Servers.PASSWORD, password);
            values.put(DataBaseContract.Servers.PROTOCOL, protocol);
            values.put(DataBaseContract.Servers.ENABLE, enable);
            DBDriver.getInstance().db.update(DataBaseContract.Servers.TABLE_NAME, values, DataBaseContract.Servers._ID + " = ?", new String[]{id});
            DBDriver.getInstance().restartService();
            mainLooperHandler.post(() -> updateFragment(StartApplication.getInstance().getString(R.string.server_updated)));
        });
    }

    public void deleteServer(String serverId) {
        executor.execute(() -> {
            DBDriver.getInstance().db.delete(DataBaseContract.Servers.TABLE_NAME, DataBaseContract.Servers._ID + " = ?", new String[]{serverId});
            DBDriver.getInstance().restartService();
            mainLooperHandler.post(() -> updateFragment(StartApplication.getInstance().getString(R.string.server_deleted)));
        });
    }

    public void createTask(String name, String sensors, String servers, String schedule, String enable, String restartService) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DataBaseContract.Tasks.NAME, name);
            values.put(DataBaseContract.Tasks.SENSORS, sensors);
            values.put(DataBaseContract.Tasks.SERVERS, servers);
            values.put(DataBaseContract.Tasks.SCHEDULE, schedule);
            values.put(DataBaseContract.Tasks.ENABLE, enable);
            DBDriver.getInstance().db.insert(DataBaseContract.Tasks.TABLE_NAME, null, values);
            if (restartService.equals("1")) {
                DBDriver.getInstance().restartService();
            }
            mainLooperHandler.post(() -> updateFragment(StartApplication.getInstance().getString(R.string.task_created)));
        });
    }

    public ArrayList<HashMap<String, String>> selectEnabledTasks() {
        ArrayList<HashMap<String, String>> tasks = new ArrayList<>();
        Cursor cursor = db.query(
                DataBaseContract.Tasks.TABLE_NAME,
                null,
                DataBaseContract.Tasks.ENABLE + " = ?",
                new String[]{"1"},
                null,
                null,
                null
        );
        if (cursor.getCount() != 0) {
            HashMap<String, String> task;
            while (cursor.moveToNext()) {
                task = new HashMap<>();
                task.put("id", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Tasks._ID)));
                task.put("name", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Tasks.NAME)));
                task.put("sensors", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Tasks.SENSORS)));
                task.put("servers", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Tasks.SERVERS)));
                task.put("schedule", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Tasks.SCHEDULE)));
                task.put("enable", cursor.getString(cursor.getColumnIndexOrThrow(DataBaseContract.Tasks.ENABLE)));
                tasks.add(task);
            }
        }
        cursor.close();
        return tasks;
    }

    public void updateTask(String id, String name, String sensors, String servers, String schedule, String enable) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DataBaseContract.Tasks.NAME, name);
            values.put(DataBaseContract.Tasks.SENSORS, sensors);
            values.put(DataBaseContract.Tasks.SERVERS, servers);
            values.put(DataBaseContract.Tasks.SCHEDULE, schedule);
            values.put(DataBaseContract.Tasks.ENABLE, enable);
            DBDriver.getInstance().db.update(DataBaseContract.Tasks.TABLE_NAME, values, DataBaseContract.Tasks._ID + " = ?", new String[]{id});
            DBDriver.getInstance().restartService();
            mainLooperHandler.post(() -> updateFragment(StartApplication.getInstance().getString(R.string.task_updated)));
        });
    }

    public void deleteTask(String taskId) {
        executor.execute(() -> {
            DBDriver.getInstance().db.delete(DataBaseContract.Tasks.TABLE_NAME, DataBaseContract.Tasks._ID + " = ?", new String[]{taskId});
            DBDriver.getInstance().restartService();
            mainLooperHandler.post(() -> updateFragment(StartApplication.getInstance().getString(R.string.task_deleted)));
        });
    }

    private void restartService() {
        Context context = StartApplication.getInstance().getApplicationContext();
        Intent mainServiceIntent = new Intent(context, MainService.class);
        mainServiceIntent.putExtra("launchInitiator", MainService.applicationInitiator);
        context.stopService(mainServiceIntent);
        context.startService(mainServiceIntent);
    }

}
