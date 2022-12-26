package ru.vukit.dc;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.ListFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import ru.vukit.dc.database.DBDriver;
import ru.vukit.dc.sensors.SensorFactory;

@Keep
public class SensorsFragment extends ListFragment implements FragmentInterface {

    MainActivityInterface mainActivityInterface;
    final DBDriver dbDriver = DBDriver.getInstance();
    SensorsFragment.SensorsAdapter sensorsAdapter;
    FloatingActionButton fabAddSensor;
    private GestureDetector mGestureDetector;

    @Override
    public void onResume() {
        super.onResume();
        dbDriver.connectController(this);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getResources().getString(R.string.action_sensors));
        }
        try {
            mainActivityInterface = (MainActivityInterface) getActivity();
        } catch (ClassCastException e) {
            mainActivityInterface = null;
        }
        sensorsAdapter = new SensorsFragment.SensorsAdapter(getActivity(), R.layout.sensor_list_item, dbDriver);
        setListAdapter(sensorsAdapter);
        updateView();
    }

    @Override
    public void onPause() {
        dbDriver.disconnectController();
        sensorsAdapter = null;
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sensors, container, false);
        fabAddSensor = view.findViewById(R.id.sensor_fab_add);
        fabAddSensor.setOnClickListener(onClickListenerFabAddSensor);
        mGestureDetector = new GestureDetector(getActivity(), new MyGestureDetector());
        return view;
    }

    @Override
    public void updateView() {
        requireActivity().invalidateOptionsMenu();
        sensorsAdapter.notifyDataSetChanged();
    }

    private class SensorsAdapter extends BaseAdapter {

        private final Context context;
        private final int resource;
        private final DBDriver dbDriver;
        private int count;
        private Cursor rowIds;
        int currentPosition;

        SensorsAdapter(Context context, int resource, DBDriver dbDriver) {
            this.context = context;
            this.resource = resource;
            this.dbDriver = dbDriver;
            closeCursor();
            this.rowIds = getRowIds();
            this.count = this.rowIds.getCount();
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = convertView;
            if (inflater != null) {
                if (row == null) row = inflater.inflate(resource, parent, false);
                TextView serverItem = row.findViewById(R.id.sensor_list_item);
                final Cursor cursor = getSensor(position);
                CharSequence sensor_info;
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors.NAME));
                SpannableString sensor_parameter = new SpannableString("\n" + name + "\n");
                sensor_parameter.setSpan(new ForegroundColorSpan(0xFF3F51B5), 1, sensor_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sensor_parameter.setSpan(new TypefaceSpan("monospace"), 1, sensor_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sensor_info = TextUtils.concat(sensor_parameter);
                String status;
                if (cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors.ENABLE)).equals("1")) {
                    status = getResources().getString(R.string.sensor_enable).toLowerCase();
                } else {
                    status = getResources().getString(R.string.sensor_disable);
                }
                sensor_parameter = new SpannableString(getString(R.string.sensor_status) + ": " + status + "\n");
                sensor_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.sensor_status).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sensor_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.sensor_status).length() + 2, sensor_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sensor_info = TextUtils.concat(sensor_info, sensor_parameter);
                serverItem.setText(sensor_info);
                cursor.close();
                row.setOnTouchListener((view, motionEvent) -> {
                    currentPosition = position;
                    mGestureDetector.onTouchEvent(motionEvent);
                    return view.performClick();
                });
            }
            return row;
        }

        @Override
        public Object getItem(int position) {
            if (rowIds.moveToPosition(position)) {
                long rowId = rowIds.getLong(0);
                return getRowById(rowId);
            } else return null;
        }

        @Override
        public long getItemId(int position) {
            if (rowIds.moveToPosition(position)) return rowIds.getLong(0);
            else return 0;
        }

        Cursor getRowIds() {
            return dbDriver.rawQuery("SELECT * FROM sensors ORDER BY _id DESC");
        }

        Cursor getRowById(long rowId) {
            return dbDriver.rawQuery("SELECT * FROM " + DBDriver.DataBaseContract.Sensors.TABLE_NAME + " WHERE _ID = " + rowId);
        }

        Cursor getSensor(int position) {
            rowIds.moveToPosition(position);
            long rowId = rowIds.getLong(0);
            Cursor cursor = getRowById(rowId);
            cursor.moveToFirst();
            return cursor;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            closeCursor();
            this.rowIds = getRowIds();
            this.count = this.rowIds.getCount();
        }

        private void closeCursor() {
            if (this.rowIds != null && !this.rowIds.isClosed()) this.rowIds.close();
        }

        @Override
        protected void finalize() throws Throwable {
            closeCursor();
            super.finalize();
        }
    }

    class MyGestureDetector implements GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            editSensor(sensorsAdapter.currentPosition);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    final View.OnClickListener onClickListenerFabAddSensor = v -> {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View view = View.inflate(getActivity(), R.layout.add_new_sensor, null);
        final Spinner type = view.findViewById(R.id.sensor_type);
        final EditText name = view.findViewById(R.id.sensor_name);
        builder.setView(view);
        builder.setPositiveButton(R.string.save, (dialog, id) -> {
            if (!name.getText().toString().trim().isEmpty()) {
                String sensorCode = "";
                switch ((byte) type.getSelectedItemId()) {
                    case 0:
                        sensorCode = String.valueOf(SensorFactory.SENSOR_TYPE_TEXT);
                        break;
                    case 1:
                        sensorCode = String.valueOf(SensorFactory.SENSOR_TYPE_MOSAIC);
                        break;
                    case 2:
                        sensorCode = String.valueOf(SensorFactory.SENSOR_TYPE_XY);
                        break;
                }
                DBDriver.getInstance().createSensor(
                        sensorCode,
                        name.getText().toString().trim(),
                        ""
                );
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
        });
        builder.setTitle(R.string.new_sensor_dialog_title);
        builder.create();
        builder.show();
    };

    public void editSensor(int position) {
        Cursor cursor = sensorsAdapter.getSensor(position);
        switch (cursor.getInt(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Sensors.CODE))) {
            case SensorFactory.SENSOR_TYPE_TEXT:
                TextSensorFragmentState.getInstance().setupSensor(cursor);
                if (mainActivityInterface != null) {
                    mainActivityInterface.selectAction(getString(R.string.action_show_text_sensor));
                }
                break;
            case SensorFactory.SENSOR_TYPE_MOSAIC:
                MosaicSensorFragmentState.getInstance().setupSensor(cursor);
                if (mainActivityInterface != null) {
                    mainActivityInterface.selectAction(getString(R.string.action_show_mosaic_sensor));
                }
                break;
            case SensorFactory.SENSOR_TYPE_XY:
                XYSensorFragmentState.getInstance().setupSensor(cursor);
                if (mainActivityInterface != null) {
                    mainActivityInterface.selectAction(getString(R.string.action_show_xy_sensor));
                }
                break;
            case SensorFactory.SENSOR_TYPE_LOCATION:
                LocationSensorFragmentState.getInstance().setupSensor(cursor);
                if (mainActivityInterface != null) {
                    mainActivityInterface.selectAction(getString(R.string.action_show_location_sensor));
                }
                break;
            default:
                HardwareSensorFragmentState.getInstance().setupSensor(cursor);
                if (mainActivityInterface != null) {
                    mainActivityInterface.selectAction(getString(R.string.action_show_hardware_sensor));
                }
                break;
        }
        cursor.close();
    }

}
