package ru.vukit.dc;

import android.database.Cursor;

import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.math.BigDecimal;
import java.util.ArrayList;

import ru.vukit.dc.database.DBDriver;

class XYSensorFragmentState {
    static final int DATA_TRANSFER_MODE_MANUALLY = 0;
    static final int DATA_TRANSFER_MODE_CHANGE = 1;
    static final int DATA_TRANSFER_MODE_PERIODIC = 2;

    String id;
    String name;
    boolean enable;
    boolean sendXValue;
    boolean showCounter;
    String value;
    int progress = -1;
    String functionText;

    String oldName;
    boolean oldEnable;
    boolean oldSendXValue;

    int dataTransferMode;
    int period;

    BigDecimal xMin, xMax, xDelta;

    final ArrayList<ILineDataSet> graphSeries = new ArrayList<>();

    private static XYSensorFragmentState INSTANCE = null;

    private XYSensorFragmentState() {
    }

    public static synchronized XYSensorFragmentState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new XYSensorFragmentState();
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
        try {
            xMin = new BigDecimal(settingsParts[2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            xMin = new BigDecimal(0);
        }
        try {
            xMax = new BigDecimal(settingsParts[3]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            xMax = new BigDecimal(100);
        }
        try {
            xDelta = new BigDecimal(settingsParts[4]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            xDelta = new BigDecimal(1);
        }
        try {
            sendXValue = settingsParts[5].equals("1");
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            sendXValue = false;
        }
        oldSendXValue = sendXValue;
        try {
            showCounter = settingsParts[6].equals("1");
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            showCounter = true;
        }
        try {
            functionText = settingsParts[7];
        } catch (ArrayIndexOutOfBoundsException ex) {
            functionText = "sin(x)/x";
        }
        if (graphSeries.size() != 0) {
            graphSeries.clear();
        }
    }

}
