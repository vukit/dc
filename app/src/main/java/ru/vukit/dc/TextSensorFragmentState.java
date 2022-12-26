package ru.vukit.dc;

import android.database.Cursor;

import ru.vukit.dc.database.DBDriver;

class TextSensorFragmentState {
    static final int DATA_TRANSFER_MODE_MANUALLY = 0;
    static final int DATA_TRANSFER_MODE_CHANGE = 1;
    static final int DATA_TRANSFER_MODE_PERIODIC = 2;

    String id;
    String name;
    boolean enable;
    String value;

    String oldName;
    boolean oldEnable;

    int dataTransferMode;
    int period;

    private static TextSensorFragmentState INSTANCE = null;

    private TextSensorFragmentState() {
    }

    public static synchronized TextSensorFragmentState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TextSensorFragmentState();
        }
        return (INSTANCE);
    }

    void setupSensor(Cursor cursor) {
        id = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors._ID));
        name = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors.NAME));
        value = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors.VALUE));
        enable = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors.ENABLE)).equals("1");
        oldName = name;
        oldEnable = enable;
        String[] settingsParts = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors.SETTINGS)).split(":");
        try {
            dataTransferMode = Integer.parseInt(settingsParts[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            dataTransferMode = DATA_TRANSFER_MODE_MANUALLY;
        }
        try {
            period = Integer.parseInt(settingsParts[1]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            period = 0;
        }
    }

}
