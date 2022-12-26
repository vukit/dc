package ru.vukit.dc;

import android.annotation.SuppressLint;
import android.database.Cursor;

import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

import ru.vukit.dc.database.DBDriver;

class HardwareSensorFragmentState {

    String id;
    String name;
    int code;
    boolean enable;
    int valuesLength;
    int progress = -1;
    String oldName;
    boolean oldEnable;

    final ArrayList<ILineDataSet> graphSeries = new ArrayList<>();

    final int[] colors = {0xff0000ff, 0xffff0000, 0xff008000, 0xff00ffff, 0xff000000, 0xffff00ff, 0xff808080, 0xff00ff00, 0xff800000, 0xff000080, 0xff808000, 0xff800080, 0xff008080, 0xffffff00, 0xffc0c0c0};

    private static HardwareSensorFragmentState INSTANCE = null;

    private HardwareSensorFragmentState() {
    }

    public static synchronized HardwareSensorFragmentState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new HardwareSensorFragmentState();
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
        if (graphSeries.size() != 0) {
            graphSeries.clear();
        }
    }
}
