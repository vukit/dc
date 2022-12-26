package ru.vukit.dc;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import java.util.Timer;
import java.util.TimerTask;

import ru.vukit.dc.database.DBDriver;
import ru.vukit.dc.sensors.SensorMessage;

@Keep
public class TextSensorFragment extends Fragment {

    final TextSensorFragmentState state = TextSensorFragmentState.getInstance();
    MainActivityInterface mainActivityInterface;
    EditText name;
    EditText value;
    CheckBox enable;
    Timer updateSensorTimer;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new FragmentMenuProvider(), getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        View view = inflater.inflate(R.layout.text_sensor, container, false);
        name = view.findViewById(R.id.sensor_name);
        name.setText(state.name);
        value = view.findViewById(R.id.sensor_value);
        value.setText(state.value);
        value.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (state.dataTransferMode == TextSensorFragmentState.DATA_TRANSFER_MODE_CHANGE) {
                    updateSensorValue();
                }
            }
        });
        enable = view.findViewById(R.id.sensor_enable);
        enable.setOnClickListener(v -> state.enable = enable.isChecked());
        enable.setChecked(state.enable);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            mainActivityInterface = (MainActivityInterface) getActivity();
        } catch (ClassCastException e) {
            mainActivityInterface = null;
        }
        if (state.dataTransferMode == TextSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC && state.period > 0) {
            updateSensorTask();
        }
    }

    @Override
    public void onPause() {
        if (updateSensorTimer != null) {
            updateSensorTimer.cancel();
            updateSensorTimer = null;
        }
        if (!name.getText().toString().trim().isEmpty()) {
            String restartService = "0";
            if (state.oldEnable != enable.isChecked() || !name.getText().toString().trim().equals(state.oldName)) {
                state.oldName = name.getText().toString().trim();
                state.oldEnable = enable.isChecked();
                restartService = "1";
            }
            DBDriver.getInstance().updateSensor(state.id, name.getText().toString().trim(), state.dataTransferMode + ":" + state.period, value.getText().toString().trim(), (enable.isChecked() ? "1" : "0"), restartService);
        }
        super.onPause();
    }

    private void updateSensorTask() {
        updateSensorTimer = new Timer();
        updateSensorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateSensorValue();
            }
        }, 0, 1000L * state.period);
    }

    private void updateSensorValue() {
        SensorsFragmentState.getInstance().notifySensors(new SensorMessage(Integer.parseInt(state.id), SensorMessage.TYPE_NEW_DATA, value.getText().toString().trim(), (enable.isChecked() ? "1" : "0")));
    }

    private class FragmentMenuProvider implements MenuProvider {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setTitle(getResources().getString(R.string.edit_sensor_dialog_title));
            }
            menuInflater.inflate(R.menu.sensor_menu, menu);
        }

        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            String selectedAction = (String) item.getTitle();
            if (selectedAction != null) {
                if (selectedAction.equals(getString(R.string.send_sensor))) {
                    updateSensorValue();
                } else if (selectedAction.equals(getString(R.string.clear_sensor))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                    builder.setPositiveButton(android.R.string.yes, (dialog, id) -> {
                        value.setText("");
                        if (state.dataTransferMode == TextSensorFragmentState.DATA_TRANSFER_MODE_CHANGE) {
                            updateSensorValue();
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, (dialog, which) -> {
                    });
                    builder.setMessage(R.string.are_you_sure);
                    builder.setTitle(R.string.clear_sensor);
                    builder.create();
                    builder.show();
                } else if (selectedAction.equals(getString(R.string.delete_sensor))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                    builder.setPositiveButton(android.R.string.yes, (dialog, id) -> {
                        DBDriver.getInstance().deleteSensor(state.id);
                        MainActivityState.getInstance().fragmentStack.poll();
                        MainActivityState.getInstance().fragmentStack.poll();
                        if (mainActivityInterface != null) {
                            mainActivityInterface.selectAction(getString(R.string.action_sensors));
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, (dialog, which) -> {
                    });
                    builder.setMessage(R.string.are_you_sure);
                    builder.setTitle(R.string.delete_sensor);
                    builder.create();
                    builder.show();
                } else if (selectedAction.equals(getString(R.string.settings_sensor))) {
                    final int oldPeriod = state.period;
                    final int oldDataTransferMode = state.dataTransferMode;
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                    View view = View.inflate(getActivity(), R.layout.settings_sensor, null);
                    final EditText period = view.findViewById(R.id.data_transfer_period);
                    if (state.period > 0) {
                        period.setText(String.valueOf(state.period));
                    }
                    final RadioGroup radioGroupDataTransfer = view.findViewById(R.id.radio_group_data_transfer);
                    final RadioButton radioButtonManually = view.findViewById(R.id.data_transfer_manually);
                    final RadioButton radioButtonChange = view.findViewById(R.id.data_transfer_change);
                    final RadioButton radioButtonPeriodic = view.findViewById(R.id.data_transfer_periodic);
                    switch (state.dataTransferMode) {
                        case TextSensorFragmentState.DATA_TRANSFER_MODE_CHANGE:
                            radioButtonChange.setChecked(true);
                            break;
                        case TextSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC:
                            radioButtonPeriodic.setChecked(true);
                            period.setVisibility(View.VISIBLE);
                            break;
                        default:
                        case TextSensorFragmentState.DATA_TRANSFER_MODE_MANUALLY:
                            radioButtonManually.setChecked(true);
                            break;
                    }
                    radioGroupDataTransfer.setOnCheckedChangeListener((radioGroup, checkedId) -> {
                        switch (checkedId) {
                            case R.id.data_transfer_manually:
                                state.dataTransferMode = TextSensorFragmentState.DATA_TRANSFER_MODE_MANUALLY;
                                period.setVisibility(View.GONE);
                                break;
                            case R.id.data_transfer_change:
                                state.dataTransferMode = TextSensorFragmentState.DATA_TRANSFER_MODE_CHANGE;
                                period.setVisibility(View.GONE);
                                break;
                            case R.id.data_transfer_periodic:
                                state.dataTransferMode = TextSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC;
                                period.setVisibility(View.VISIBLE);
                                break;
                            default:
                            case -1:
                                break;
                        }
                    });
                    builder.setView(view);
                    builder.setPositiveButton(R.string.save, (dialog, id) -> {
                        if (period.getVisibility() == View.VISIBLE) {
                            try {
                                state.period = Integer.parseInt(period.getText().toString());
                            } catch (NumberFormatException ex) {
                                state.period = 0;
                            }
                        }
                        if (updateSensorTimer != null) {
                            updateSensorTimer.cancel();
                            updateSensorTimer = null;
                        }
                        if (state.dataTransferMode == TextSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC && state.period > 0) {
                            updateSensorTask();
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        state.period = oldPeriod;
                        state.dataTransferMode = oldDataTransferMode;
                        if (updateSensorTimer != null) {
                            updateSensorTimer.cancel();
                            updateSensorTimer = null;
                        }
                        if (state.dataTransferMode == TextSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC && state.period > 0) {
                            updateSensorTask();
                        }
                    });
                    builder.setTitle(R.string.settings_sensor);
                    builder.create();
                    builder.show();
                }
            }
            return true;
        }
    }
}
