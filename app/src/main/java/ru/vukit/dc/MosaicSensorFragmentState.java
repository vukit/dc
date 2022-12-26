package ru.vukit.dc;

import android.database.Cursor;
import android.graphics.Color;

import com.google.gson.Gson;

import ru.vukit.dc.database.DBDriver;

class MosaicSensorFragmentState {
    static final int DATA_TRANSFER_MODE_MANUALLY = 0;
    static final int DATA_TRANSFER_MODE_CHANGE = 1;
    static final int DATA_TRANSFER_MODE_PERIODIC = 2;

    String id;
    String name;
    boolean enable;
    String value;

    String oldName;
    boolean oldEnable;

    int maxCols, maxRows;
    int currentColor;
    Integer[] selectedColors;
    int dataTransferMode;
    int period;
    int imageSensorWidth, imageSensorHeight;
    final int imageSensorLeft = 26;
    final int imageSensorTop = 26;
    int currentCols, currentRows;
    float deltaX, deltaY;
    float offsetX, offsetY;
    float scaleX, scaleY;
    int startCol, endCol, startRow, endRow;
    Integer[][] mosaicArray;

    private static MosaicSensorFragmentState INSTANCE = null;

    private MosaicSensorFragmentState() {
    }

    public static synchronized MosaicSensorFragmentState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MosaicSensorFragmentState();
        }
        return (INSTANCE);
    }

    void setupSensor(Cursor cursor) {
        id = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors._ID));
        name = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors.NAME));
        enable = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors.ENABLE)).equals("1");
        oldName = name;
        oldEnable = enable;
        mosaicArray = null;
        value = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors.VALUE));
        String[] settingsParts = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors.SETTINGS)).split(":");
        selectedColors = new Integer[]{Color.GREEN, null, null, null, null, null};
        if (!settingsParts[0].isEmpty()) {
            int k = 0;
            for (Integer selectedColorFromSettings : new Gson().fromJson(settingsParts[0], Integer[].class)) {
                selectedColors[k] = selectedColorFromSettings;
                if (++k == 5) {
                    break;
                }
            }
        }
        currentColor = selectedColors[0];
        try {
            dataTransferMode = Integer.parseInt(settingsParts[1]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            dataTransferMode = DATA_TRANSFER_MODE_MANUALLY;
        }
        try {
            period = Integer.parseInt(settingsParts[2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            period = 0;
        }
        scaleX = scaleY = 1f;
        startRow = endRow = startCol = endCol = -1;
        offsetX = offsetY = 0;
    }

    void updateSelectedColors(int currentColor) {
        this.currentColor = currentColor;
        for (int k = 0; k <= 5; k++) {
            if (selectedColors[k] != null && this.currentColor == selectedColors[k]) {
                break;
            }
            if (selectedColors[k] == null) {
                System.arraycopy(selectedColors, 0, selectedColors, 1, k);
                selectedColors[0] = this.currentColor;
                break;
            }
        }
        selectedColors[5] = null;
    }
}
