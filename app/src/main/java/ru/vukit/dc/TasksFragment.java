package ru.vukit.dc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.fragment.app.ListFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import ru.vukit.dc.database.DBDriver;
import ru.vukit.dc.tasks.DCTask;

@Keep
public class TasksFragment extends ListFragment implements FragmentInterface {

    final Handler mainLooperHandler = new Handler(Looper.getMainLooper());
    final TasksFragmentState state = TasksFragmentState.getInstance();
    final DBDriver dbDriver = DBDriver.getInstance();
    TasksFragment.TasksAdapter tasksAdapter;
    FloatingActionButton fabAddTask;
    final ArrayList<String> selectedSensors = new ArrayList<>();
    String selectedServer;
    private GestureDetector mGestureDetector;

    private class TasksAdapter extends BaseAdapter {

        private final Context context;
        private final int resource;
        private final DBDriver dbDriver;
        private int count;
        private Cursor rowIds;
        int currentPosition;

        TasksAdapter(Context context, int resource, DBDriver dbDriver) {
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
                TextView taskItem = row.findViewById(R.id.task_list_item);
                Cursor cursor = getTask(position);
                CharSequence task_info;
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Tasks.NAME));
                SpannableString task_parameter = new SpannableString("\n" + name + "\n");
                task_parameter.setSpan(new ForegroundColorSpan(0xFF3F51B5), 1, task_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                task_parameter.setSpan(new TypefaceSpan("monospace"), 1, task_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                task_info = TextUtils.concat(task_parameter);
                String[] sensors = new Gson().fromJson(cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Tasks.SENSORS)), String[].class);
                StringBuilder sensorList = new StringBuilder();
                for (String sensor : sensors) {
                    String dbSensor = dbDriver.selectEnabledSensorName(sensor);
                    if (!dbSensor.trim().isEmpty()) {
                        sensorList.append(dbSensor);
                        sensorList.append(", ");
                    }
                }
                if (sensorList.length() > 2) {
                    sensorList.delete(sensorList.length() - 2, sensorList.length() - 1);
                }
                task_parameter = new SpannableString(getString(R.string.task_sensors) + ": " + sensorList + "\n");
                task_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.task_sensors).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                task_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.task_sensors).length() + 2, task_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                task_info = TextUtils.concat(task_info, task_parameter);
                taskItem.setText(task_info);
                String serverName = dbDriver.selectServerName(cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Tasks.SERVERS)));
                task_parameter = new SpannableString(getString(R.string.task_servers) + ": " + serverName + "\n");
                task_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.task_servers).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                task_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.task_servers).length() + 2, task_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                task_info = TextUtils.concat(task_info, task_parameter);
                taskItem.setText(task_info);
                String[] scheduleParts = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Tasks.SCHEDULE)).split(":");
                switch (scheduleParts[0]) {
                    case "simple":
                        String scheduleSimpleSample;
                        try {
                            scheduleSimpleSample = scheduleParts[1];
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            scheduleSimpleSample = "";
                        }
                        task_parameter = new SpannableString(getString(R.string.schedule_simple_sample) + ": " + scheduleSimpleSample + "\n");
                        task_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.schedule_simple_sample).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        task_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.schedule_simple_sample).length() + 2, task_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        task_info = TextUtils.concat(task_info, task_parameter);
                        taskItem.setText(task_info);
                        break;
                    case "cron":
                        String scheduleCronString;
                        String scheduleCronPeriod;
                        String scheduleCronAmount;
                        try {
                            scheduleCronString = scheduleParts[1];
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            scheduleCronString = "";
                        }
                        try {
                            scheduleCronAmount = scheduleParts[2];
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            scheduleCronAmount = "";
                        }
                        try {
                            scheduleCronPeriod = scheduleParts[3];
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            scheduleCronPeriod = "";
                        }
                        task_parameter = new SpannableString(getString(R.string.schedule_cron) + ": " + scheduleCronString + "\n");
                        task_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.schedule_cron).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        task_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.schedule_cron).length() + 2, task_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        task_info = TextUtils.concat(task_info, task_parameter);
                        task_parameter = new SpannableString(getString(R.string.schedule_cron_amount) + ": " + scheduleCronAmount + "\n");
                        task_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.schedule_cron_amount).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        task_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.schedule_cron_amount).length() + 2, task_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        task_info = TextUtils.concat(task_info, task_parameter);
                        task_parameter = new SpannableString(getString(R.string.schedule_cron_period) + ": " + scheduleCronPeriod + "\n");
                        task_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.schedule_cron_period).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        task_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.schedule_cron_period).length() + 2, task_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        task_info = TextUtils.concat(task_info, task_parameter);
                        taskItem.setText(task_info);
                        break;
                }
                String status;
                if (cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Tasks.ENABLE)).equals("1")) {
                    status = getStatus(position);
                } else {
                    status = getResources().getString(R.string.task_disable);
                }
                task_parameter = new SpannableString(getString(R.string.task_status) + ": " + status + "\n");
                task_parameter.setSpan(new StyleSpan(Typeface.BOLD), 0, getString(R.string.task_status).length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                task_parameter.setSpan(new TypefaceSpan("monospace"), getString(R.string.task_status).length() + 2, task_parameter.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                task_info = TextUtils.concat(task_info, task_parameter);
                taskItem.setText(task_info);
                cursor.close();
                row.setOnTouchListener((view, motionEvent) -> {
                    currentPosition = position;
                    mGestureDetector.onTouchEvent(motionEvent);
                    return view.performClick();
                });
            }
            return row;
        }

        String getStatus(int position) {
            return state.tasksStatus.getOrDefault(String.valueOf(getItemId(position)), DCTask.STATUS_UNKNOWN);
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
            return dbDriver.rawQuery("SELECT * FROM tasks");
        }

        Cursor getRowById(long rowId) {
            return dbDriver.rawQuery("SELECT * FROM " + DBDriver.DataBaseContract.Tasks.TABLE_NAME + " WHERE _ID = " + rowId);
        }

        Cursor getTask(int position) {
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
            editTask(tasksAdapter.currentPosition);
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

    @Override
    public void onResume() {
        super.onResume();
        dbDriver.connectController(this);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getResources().getString(R.string.action_tasks));
        }
        tasksAdapter = new TasksFragment.TasksAdapter(getActivity(), R.layout.task_list_item, dbDriver);
        setListAdapter(tasksAdapter);
        mainLooperHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tasksAdapter != null) {
                    requireActivity().invalidateOptionsMenu();
                    tasksAdapter.notifyDataSetChanged();
                    mainLooperHandler.postDelayed(this, DCTask.TIMEOUT);
                }
            }
        });
    }

    @Override
    public void onPause() {
        dbDriver.disconnectController();
        tasksAdapter = null;
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tasks, container, false);
        fabAddTask = view.findViewById(R.id.task_fab_add);
        fabAddTask.setOnClickListener(onClickListenerFabAddTask);
        mGestureDetector = new GestureDetector(getActivity(), new MyGestureDetector());
        return view;
    }

    @Override
    public void updateView() {
        requireActivity().invalidateOptionsMenu();
        tasksAdapter.notifyDataSetChanged();
    }

    @SuppressLint({"NonConstantResourceId"})
    public void editTask(int position) {
        Cursor cursor = tasksAdapter.getTask(position);
        final String dbId = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Tasks._ID));
        String dbName = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Tasks.NAME));
        String[] dbSensors = new Gson().fromJson(cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Tasks.SENSORS)), String[].class);
        String dbServers = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Tasks.SERVERS));
        String dbSchedule = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Tasks.SCHEDULE));
        boolean dbEnable = cursor.getString(cursor.getColumnIndexOrThrow(DBDriver.DataBaseContract.Tasks.ENABLE)).equals("1");
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        View view = View.inflate(getActivity(), R.layout.add_new_task, null);
        final EditText name = view.findViewById(R.id.task_name);
        name.setText(dbName);
        final CheckBox enable = view.findViewById(R.id.task_enable);
        enable.setChecked(dbEnable);
        final TextView sensorListTitle = view.findViewById(R.id.sensor_list_title);
        final LinearLayout sensorsList = view.findViewById(R.id.sensor_list);
        sensorListTitle.setText(getString(R.string.sensor_select_down));
        sensorListTitle.setOnClickListener(view1 -> {
            if (sensorsList.getVisibility() == View.VISIBLE) {
                sensorsList.setVisibility(View.GONE);
                sensorListTitle.setText(getString(R.string.sensor_select_down));
            } else {
                sensorsList.setVisibility(View.VISIBLE);
                sensorListTitle.setText(getString(R.string.sensor_select_up));
            }
        });
        selectedSensors.clear();
        ArrayList<HashMap<String, String>> sensors = dbDriver.selectEnabledSensors();
        for (final HashMap<String, String> sensor : sensors) {
            final CheckBox checkBox = new AppCompatCheckBox(requireActivity());
            checkBox.setText(sensor.get("name"));
            if (Arrays.asList(dbSensors).contains(sensor.get("id"))) {
                checkBox.setChecked(true);
                selectedSensors.add(sensor.get("id"));
            }
            checkBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                String idSensor = sensor.get("id");
                if (isChecked) {
                    selectedSensors.add(idSensor);
                } else {
                    selectedSensors.remove(idSensor);
                }
            });
            sensorsList.addView(checkBox);
        }
        final TextView serverListTitle = view.findViewById(R.id.server_list_title);
        final RadioGroup serversList = view.findViewById(R.id.server_list);
        serverListTitle.setText(getString(R.string.server_select_down));
        serverListTitle.setOnClickListener(view12 -> {
            if (serversList.getVisibility() == View.VISIBLE) {
                serversList.setVisibility(View.GONE);
                serverListTitle.setText(getString(R.string.server_select_down));
            } else {
                serversList.setVisibility(View.VISIBLE);
                serverListTitle.setText(getString(R.string.server_select_up));
            }
        });
        selectedServer = "";
        final ArrayList<HashMap<String, String>> servers = dbDriver.selectServers();
        for (final HashMap<String, String> server : servers) {
            final RadioButton radioButton = new AppCompatRadioButton(requireActivity());
            radioButton.setText(server.get("name"));
            serversList.addView(radioButton);
            if (dbServers.equals(server.get("id"))) {
                selectedServer = server.get("id");
                radioButton.setChecked(true);
                serversList.invalidate();
            }
        }
        if (servers.size() != 0) {
            serversList.setOnCheckedChangeListener((radioGroup, i) -> {
                View radioButton = radioGroup.findViewById(i);
                int index = radioGroup.indexOfChild(radioButton);
                HashMap<String, String> server = servers.get(index);
                selectedServer = server.get("id");
            });
        }
        final LinearLayout scheduleSimpleLayout = view.findViewById(R.id.schedule_simple_layout);
        final LinearLayout scheduleCronLayout = view.findViewById(R.id.schedule_cron_layout);
        final RadioGroup radioGroupSchedules = view.findViewById(R.id.radio_group_schedules);
        final RadioButton radioButtonSimple = view.findViewById(R.id.schedule_simple);
        final RadioButton radioButtonCron = view.findViewById(R.id.schedule_cron);
        final EditText scheduleSimpleString = view.findViewById(R.id.schedule_simple_string);
        final EditText scheduleCronString = view.findViewById(R.id.schedule_cron_string);
        final EditText scheduleCronAmount = view.findViewById(R.id.schedule_cron_amount);
        final EditText scheduleCronPeriod = view.findViewById(R.id.schedule_cron_period);
        radioGroupSchedules.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            switch (checkedId) {
                case R.id.schedule_simple:
                    scheduleCronLayout.setVisibility(View.GONE);
                    scheduleSimpleLayout.setVisibility(View.VISIBLE);
                    break;
                case R.id.schedule_cron:
                    scheduleSimpleLayout.setVisibility(View.GONE);
                    scheduleCronLayout.setVisibility(View.VISIBLE);
                    break;
                default:
                case -1:
                    break;
            }
        });
        String[] scheduleParts = dbSchedule.split(":");
        switch (scheduleParts[0]) {
            case "simple":
                try {
                    scheduleSimpleString.setText(scheduleParts[1]);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    scheduleSimpleString.setText("");
                }
                radioButtonSimple.setChecked(true);
                break;
            case "cron":
                try {
                    scheduleCronString.setText(scheduleParts[1]);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    scheduleCronString.setText("");
                }
                try {
                    scheduleCronAmount.setText(scheduleParts[2]);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    scheduleCronAmount.setText("");
                }
                try {
                    scheduleCronPeriod.setText(scheduleParts[3]);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    scheduleCronPeriod.setText("");
                }
                radioButtonCron.setChecked(true);
                break;
        }
        builder.setView(view);
        builder.setPositiveButton(R.string.save, (dialog, id) -> {
            String schedule = "";
            if (scheduleSimpleLayout.getVisibility() == View.VISIBLE) {
                if (!scheduleSimpleString.getText().toString().trim().isEmpty()) {
                    schedule = "simple:" + scheduleSimpleString.getText().toString().trim();
                }
            }
            if (scheduleCronLayout.getVisibility() == View.VISIBLE) {
                if (!scheduleCronString.getText().toString().trim().isEmpty()) {
                    schedule = "cron:" + scheduleCronString.getText().toString().trim();
                    schedule += ":" + scheduleCronAmount.getText().toString().trim();
                    schedule += ":" + scheduleCronPeriod.getText().toString().trim();
                }
            }
            if (!name.getText().toString().trim().isEmpty() && selectedSensors.size() != 0 && !selectedServer.isEmpty() && !schedule.isEmpty()) {
                String jsonSelectedSensors = new Gson().toJson(selectedSensors);
                DBDriver.getInstance().updateTask(
                        dbId,
                        name.getText().toString().trim(),
                        jsonSelectedSensors,
                        selectedServer,
                        schedule,
                        (enable.isChecked() ? "1" : "0")
                );
            }
        });
        builder.setNegativeButton(R.string.delete, (dialog, which) -> DBDriver.getInstance().deleteTask(dbId));
        builder.setTitle(R.string.edit_task_dialog_title);
        builder.create();
        builder.show();
    }

    final View.OnClickListener onClickListenerFabAddTask = new View.OnClickListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View v) {
            View view = View.inflate(getActivity(), R.layout.add_new_task, null);
            final EditText name = view.findViewById(R.id.task_name);
            final CheckBox enable = view.findViewById(R.id.task_enable);
            final TextView sensorListTitle = view.findViewById(R.id.sensor_list_title);
            final LinearLayout sensorsList = view.findViewById(R.id.sensor_list);
            sensorListTitle.setText(getString(R.string.sensor_select_down));
            sensorListTitle.setOnClickListener(view1 -> {
                if (sensorsList.getVisibility() == View.VISIBLE) {
                    sensorsList.setVisibility(View.GONE);
                    sensorListTitle.setText(getString(R.string.sensor_select_down));
                } else {
                    sensorsList.setVisibility(View.VISIBLE);
                    sensorListTitle.setText(getString(R.string.sensor_select_up));
                }
            });
            selectedSensors.clear();
            ArrayList<HashMap<String, String>> sensors = dbDriver.selectEnabledSensors();
            for (final HashMap<String, String> sensor : sensors) {
                final CheckBox checkBox = new AppCompatCheckBox(requireActivity());
                checkBox.setText(sensor.get("name"));
                checkBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                    String idSensor = sensor.get("id");
                    if (isChecked) {
                        selectedSensors.add(idSensor);
                    } else {
                        selectedSensors.remove(idSensor);
                    }

                });
                sensorsList.addView(checkBox);
            }
            final TextView serverListTitle = view.findViewById(R.id.server_list_title);
            final RadioGroup serversList = view.findViewById(R.id.server_list);
            serverListTitle.setText(getString(R.string.server_select_down));
            serverListTitle.setOnClickListener(view12 -> {
                if (serversList.getVisibility() == View.VISIBLE) {
                    serversList.setVisibility(View.GONE);
                    serverListTitle.setText(getString(R.string.server_select_down));
                } else {
                    serversList.setVisibility(View.VISIBLE);
                    serverListTitle.setText(getString(R.string.server_select_up));
                }
            });
            selectedServer = "";
            final ArrayList<HashMap<String, String>> servers = dbDriver.selectEnabledServers();
            for (final HashMap<String, String> server : servers) {
                final RadioButton radioButton = new AppCompatRadioButton(requireActivity());
                radioButton.setText(server.get("name"));
                serversList.addView(radioButton);
                radioButton.setChecked(true);
                serversList.invalidate();
                selectedServer = server.get("id");
            }
            if (servers.size() != 0) {
                serversList.setOnCheckedChangeListener((radioGroup, i) -> {
                    View radioButton = radioGroup.findViewById(i);
                    int index = radioGroup.indexOfChild(radioButton);
                    HashMap<String, String> server = servers.get(index);
                    selectedServer = server.get("id");
                });
            }
            final LinearLayout scheduleSimpleLayout = view.findViewById(R.id.schedule_simple_layout);
            final LinearLayout scheduleCronLayout = view.findViewById(R.id.schedule_cron_layout);
            final RadioGroup radioGroupSchedules = view.findViewById(R.id.radio_group_schedules);
            final EditText scheduleSimpleString = view.findViewById(R.id.schedule_simple_string);
            final EditText scheduleCronString = view.findViewById(R.id.schedule_cron_string);
            final EditText scheduleCronAmount = view.findViewById(R.id.schedule_cron_amount);
            final EditText scheduleCronPeriod = view.findViewById(R.id.schedule_cron_period);
            radioGroupSchedules.setOnCheckedChangeListener((radioGroup, checkedId) -> {
                switch (checkedId) {
                    case R.id.schedule_simple:
                        scheduleCronLayout.setVisibility(View.GONE);
                        scheduleSimpleLayout.setVisibility(View.VISIBLE);
                        break;
                    case R.id.schedule_cron:
                        scheduleSimpleLayout.setVisibility(View.GONE);
                        scheduleCronLayout.setVisibility(View.VISIBLE);
                        break;
                    default:
                    case -1:
                        break;
                }
            });
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setView(view);
            builder.setPositiveButton(R.string.save, (dialog, id) -> {
                String schedule = "";
                if (scheduleSimpleLayout.getVisibility() == View.VISIBLE) {
                    if (!scheduleSimpleString.getText().toString().trim().isEmpty()) {
                        schedule = "simple:" + scheduleSimpleString.getText().toString().trim();
                    }
                }
                if (scheduleCronLayout.getVisibility() == View.VISIBLE) {
                    if (!scheduleCronString.getText().toString().trim().isEmpty()) {
                        schedule = "cron:" + scheduleCronString.getText().toString().trim();
                        schedule += ":" + scheduleCronAmount.getText().toString().trim();
                        schedule += ":" + scheduleCronPeriod.getText().toString().trim();
                    }
                }
                if (!name.getText().toString().trim().isEmpty() && selectedSensors.size() != 0 && !selectedServer.isEmpty() && !schedule.isEmpty()) {
                    String jsonSelectedSensors = new Gson().toJson(selectedSensors);
                    DBDriver.getInstance().createTask(
                            name.getText().toString().trim(),
                            jsonSelectedSensors,
                            selectedServer,
                            schedule,
                            (enable.isChecked() ? "1" : "0"),
                            (enable.isChecked() ? "1" : "0")
                    );
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            });
            builder.setTitle(R.string.new_task_dialog_title);
            builder.create();
            builder.show();
        }
    };

}
