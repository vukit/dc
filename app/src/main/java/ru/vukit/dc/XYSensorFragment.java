package ru.vukit.dc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udojava.evalex.Expression;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import ru.bullyboo.view.CircleSeekBar;
import ru.vukit.dc.database.DBDriver;
import ru.vukit.dc.sensors.SensorMessage;

@Keep
public class XYSensorFragment extends Fragment {

    final XYSensorFragmentState state = XYSensorFragmentState.getInstance();
    MainActivityInterface mainActivityInterface;
    EditText name, xValue;
    TextView xValueName, yValueName, yValue;
    CheckBox enable;
    Timer updateSensorTimer;
    CircleSeekBar xSeekBar;
    InputMethodManager imm;
    BigDecimal currentX, currentY;
    private LineChart graph;
    private LineData data;
    SeekBar seekBar;
    private int historySize;
    final private int MAX_ENTRY = 100;
    final private int sampleRate = 50;
    Timer plotTimer;
    Expression expression;
    ImageButton minusButton, plusButton;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new XYSensorFragment.FragmentMenuProvider(), getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        View view = inflater.inflate(R.layout.xy_sensor, container, false);
        name = view.findViewById(R.id.sensor_name);
        name.setText(state.name);
        enable = view.findViewById(R.id.sensor_enable);
        enable.setOnClickListener(v -> state.enable = enable.isChecked());
        enable.setChecked(state.enable);
        expression = new Expression(state.functionText);
        SpannableString valueNameInfo;
        xValueName = view.findViewById(R.id.x_sensor_value_name);
        valueNameInfo = new SpannableString("X : ");
        valueNameInfo.setSpan(new ForegroundColorSpan(0xff000000), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        xValueName.setText(valueNameInfo);
        yValueName = view.findViewById(R.id.y_sensor_value_name);
        valueNameInfo = new SpannableString("Y : ");
        valueNameInfo.setSpan(new ForegroundColorSpan(0xff0000ff), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        yValueName.setText(valueNameInfo);
        xSeekBar = view.findViewById(R.id.x_seek_bar);
        xSeekBar.setMaxValue(state.xMax.subtract(state.xMin).divide(state.xDelta, RoundingMode.UP).intValue());
        xSeekBar.setShowCounter(state.showCounter);
        xSeekBar.setOnValueChangedListener(xSeekBarListener);
        yValue = view.findViewById(R.id.y_sensor_value);
        xValue = view.findViewById(R.id.x_sensor_value);
        xValue.addTextChangedListener(xTextWatcher);
        try {
            xValue.setText(String.valueOf(state.value.split(",")[1]));
        } catch (ArrayIndexOutOfBoundsException | NullPointerException ex) {
            xValue.setText(String.valueOf(state.xMax.subtract(state.xMin).multiply(new BigDecimal("0.25"))));
        }
        imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        minusButton = view.findViewById(R.id.xy_sensor_minus_button);
        plusButton = view.findViewById(R.id.xy_sensor_plus_button);
        plusButton.setOnTouchListener(plusButtonOnTouchListener);
        minusButton.setOnTouchListener(minusButtonOnTouchListener);
        seekBar = view.findViewById(R.id.seekBar);
        if (state.progress != -1) {
            seekBar.setProgress(state.progress);
        } else {
            state.progress = seekBar.getProgress();
        }
        historySize = MAX_ENTRY * (state.progress + 1);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                state.progress = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                restartPlotting();
            }
        });
        if (state.graphSeries.size() != 1) {
            LineDataSet lineDataSet = new LineDataSet(new ArrayList<>(), "");
            lineDataSet.setColor(0xff0000ff);
            lineDataSet.setDrawCircles(false);
            lineDataSet.setDrawValues(false);
            if (currentY != null) {
                lineDataSet.addEntry(new Entry(0, currentY.floatValue()));
            } else {
                lineDataSet.addEntry(new Entry(0, Float.NaN));
            }
            state.graphSeries.add(0, lineDataSet);
        }
        graph = view.findViewById(R.id.graph);
        initGraph();
        data = new LineData(state.graphSeries);
        graph.setData(data);
        return view;
    }

    final View.OnTouchListener plusButtonOnTouchListener = new View.OnTouchListener() {
        Timer longPressTimer;

        final Runnable changeXValue = new Runnable() {
            @Override
            public void run() {
                BigDecimal newValue = currentX;
                newValue = newValue.add(state.xDelta);
                if (newValue.compareTo(state.xMax) <= 0) {
                    xValue.setText(String.valueOf(newValue));
                } else {
                    xValue.setText(String.valueOf(state.xMin));
                }
            }
        };

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    xValue.clearFocus();
                    imm.hideSoftInputFromWindow(xValue.getWindowToken(), 0);
                    BigDecimal newValue = currentX;
                    newValue = newValue.add(state.xDelta);
                    if (newValue.compareTo(state.xMax) <= 0) {
                        xValue.setText(String.valueOf(newValue));
                    } else {
                        xValue.setText(String.valueOf(state.xMin));
                    }
                    longPressTimer = new Timer();
                    longPressTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            requireActivity().runOnUiThread(changeXValue);
                        }
                    }, 500, 100);
                    break;
                case MotionEvent.ACTION_UP:
                    if (longPressTimer != null) {
                        longPressTimer.cancel();
                        longPressTimer = null;
                    }
                    break;
            }
            view.performClick();
            return false;
        }
    };

    final View.OnTouchListener minusButtonOnTouchListener = new View.OnTouchListener() {
        Timer longPressTimer;

        final Runnable changeXValue = new Runnable() {
            @Override
            public void run() {
                BigDecimal newValue = currentX;
                newValue = newValue.subtract(state.xDelta);
                if (newValue.compareTo(state.xMin) >= 0) {
                    xValue.setText(String.valueOf(newValue));
                } else {
                    xValue.setText(String.valueOf(state.xMax));
                }
            }
        };

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    xValue.clearFocus();
                    imm.hideSoftInputFromWindow(xValue.getWindowToken(), 0);
                    BigDecimal newValue = currentX;
                    newValue = newValue.subtract(state.xDelta);
                    if (newValue.compareTo(state.xMin) >= 0) {
                        xValue.setText(String.valueOf(newValue));
                    } else {
                        xValue.setText(String.valueOf(state.xMax));
                    }
                    longPressTimer = new Timer();
                    longPressTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            requireActivity().runOnUiThread(changeXValue);
                        }
                    }, 500, 100);
                    break;
                case MotionEvent.ACTION_UP:
                    if (longPressTimer != null) {
                        longPressTimer.cancel();
                        longPressTimer = null;
                    }
                    break;
            }
            view.performClick();
            return false;
        }
    };

    final TextWatcher xTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            xSeekBar.setOnValueChangedListener(null);
            try {
                BigDecimal x = new BigDecimal(xValue.getText().toString().trim());
                if (x.compareTo(state.xMin) >= 0 && x.compareTo(state.xMax) <= 0) {
                    BigDecimal tmpY = setValue(x);
                    yValue.setText((tmpY != null) ? String.valueOf(tmpY) : "");
                    xSeekBar.setValue(x.subtract(state.xMin).divide(state.xDelta, RoundingMode.UP).intValue());
                } else {
                    yValue.setText("");
                    xSeekBar.setValue(0);
                    new SnackBar().ShowLong(getString(R.string.xy_sensor_x_out_of_range));
                }
            } catch (NumberFormatException ex) {
                yValue.setText("");
                xSeekBar.setValue(0);
            }
            xSeekBar.setOnValueChangedListener(xSeekBarListener);
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    final CircleSeekBar.OnValueChangedListener xSeekBarListener = new CircleSeekBar.OnValueChangedListener() {
        @SuppressWarnings("unused")
        @Override
        public void onValueChanged(int value) {
            xValue.clearFocus();
            imm.hideSoftInputFromWindow(xValue.getWindowToken(), 0);
            xValue.removeTextChangedListener(xTextWatcher);
            BigDecimal tmpX = state.xDelta.multiply(new BigDecimal(value)).add(state.xMin);
            BigDecimal tmpY = setValue(tmpX);
            yValue.setText((tmpY != null) ? String.valueOf(tmpY) : "");
            xValue.setText(String.valueOf(tmpX));
            xValue.addTextChangedListener(xTextWatcher);
        }
    };

    private BigDecimal setValue(BigDecimal x) {
        currentX = x;
        if (expression != null) {
            try {
                currentY = expression.with("x", currentX).eval();
            } catch (Exception ex) {
                currentY = null;
            }
        } else {
            currentY = null;
        }
        state.value = ((currentY != null) ? String.valueOf(currentY) : "") + "," + ((currentX != null) ? String.valueOf(currentX) : "");
        if (state.dataTransferMode == XYSensorFragmentState.DATA_TRANSFER_MODE_CHANGE) {
            updateSensorValue();
        }
        return currentY;
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            mainActivityInterface = (MainActivityInterface) getActivity();
        } catch (ClassCastException e) {
            mainActivityInterface = null;
        }
        if (state.dataTransferMode == XYSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC && state.period > 0) {
            updateSensorTask();
        }
        restartPlotting();
    }

    @Override
    public void onPause() {
        if (updateSensorTimer != null) {
            updateSensorTimer.cancel();
            updateSensorTimer = null;
        }
        if (!name.getText().toString().trim().isEmpty()) {
            String restartService = "0";
            if (state.oldEnable != enable.isChecked() || state.oldSendXValue != state.sendXValue || !name.getText().toString().trim().equals(state.oldName)) {
                state.oldName = name.getText().toString().trim();
                state.oldEnable = enable.isChecked();
                state.oldSendXValue = state.sendXValue;
                restartService = "1";
            }
            String settings = state.dataTransferMode + ":" +
                    state.period + ":" +
                    state.xMin + ":" +
                    state.xMax + ":" +
                    state.xDelta + ":" +
                    ((state.sendXValue) ? "1" : "0") + ":" +
                    ((state.showCounter) ? "1" : "0") + ":" +
                    state.functionText;
            DBDriver.getInstance().updateSensor(state.id, name.getText().toString().trim(), settings, state.value, (enable.isChecked() ? "1" : "0"), restartService);
        }
        stopPlotting();
        super.onPause();
    }

    private void updateSensorTask() {
        updateSensorTimer = new Timer();
        updateSensorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateSensorValue();
            }
        }, 0, state.period);
    }

    private void updateSensorValue() {
        SensorsFragmentState.getInstance().notifySensors(new SensorMessage(Integer.parseInt(state.id), SensorMessage.TYPE_NEW_DATA, state.value, (enable.isChecked() ? "1" : "0") + ":" + (state.sendXValue ? "2" : "1")));
    }

    private void startPlotting() {
        final Runnable plotUpdate = () -> {
            LineDataSet dataSet;
            int delta;
            dataSet = (LineDataSet) state.graphSeries.get(0);
            delta = dataSet.getEntryCount() - historySize;
            if (delta > 0) {
                for (int i = 0; i < delta; i++) {
                    dataSet.removeFirst();
                }
                for (Entry entry : dataSet.getValues()) {
                    entry.setX(entry.getX() - delta);
                }
            }
            if (currentY != null) {
                float valueY = currentY.floatValue();
                if (!(Float.isInfinite(valueY) || Float.isNaN(valueY))) {
                    dataSet.addEntry(new Entry(dataSet.getEntryCount(), valueY));
                } else {
                    currentY = null;
                }
            }
            if (delta > 0 || currentY != null) {
                data.notifyDataChanged();
                graph.notifyDataSetChanged();
                graph.moveViewToX(state.graphSeries.get(0).getEntryCount());
            }
        };
        plotTimer = new Timer();
        plotTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                requireActivity().runOnUiThread(plotUpdate);
            }
        }, 200, sampleRate);
    }

    private void stopPlotting() {
        if (plotTimer != null) {
            plotTimer.cancel();
            plotTimer = null;
        }
    }

    private void restartPlotting() {
        stopPlotting();
        historySize = MAX_ENTRY * (state.progress + 1);
        initGraph();
        startPlotting();
    }

    private void initGraph() {
        graph.setDrawGridBackground(false);
        graph.getDescription().setEnabled(false);
        graph.setDrawBorders(true);
        graph.setBorderColor(Color.LTGRAY);
        graph.getLegend().setEnabled(false);
        graph.getAxisRight().setEnabled(false);
        graph.getAxisLeft().setDrawAxisLine(false);
        graph.getAxisLeft().setDrawGridLines(true);
        graph.getAxisLeft().setGridColor(Color.LTGRAY);
        graph.getXAxis().setDrawAxisLine(false);
        graph.getXAxis().setDrawGridLines(true);
        graph.getXAxis().setGridColor(Color.LTGRAY);
        graph.getXAxis().setLabelCount(5);
        graph.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        graph.getXAxis().setAxisMinimum(0);
        graph.getXAxis().setAxisMaximum(historySize);
        graph.getXAxis().setValueFormatter(new XYSensorFragment.graphXAxisValueFormatter());
        graph.setTouchEnabled(false);
        graph.setDragEnabled(false);
        graph.setScaleEnabled(false);
    }

    private class graphXAxisValueFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            return String.valueOf(Math.round(sampleRate * value / 1000));
        }
    }

    private class FragmentMenuProvider implements MenuProvider {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setTitle(getResources().getString(R.string.edit_sensor_dialog_title));
            }
            menuInflater.inflate(R.menu.xy_sensor_menu, menu);
        }

        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            String selectedAction = (String) item.getTitle();
            if (selectedAction != null) {
                if (selectedAction.equals(getString(R.string.send_sensor))) {
                    updateSensorValue();
                } else if (selectedAction.equals(getString(R.string.xy_sensor_function))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                    View view = View.inflate(getActivity(), R.layout.function_xy_sensor, null);
                    final EditText functionText = view.findViewById(R.id.xy_sensor_function_text);
                    functionText.setText(state.functionText);
                    (view.findViewById(R.id.xy_sensor_function_reference)).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.xy_sensor_function_reference_url)))));
                    builder.setPositiveButton(android.R.string.yes, (dialog, id) -> {
                        state.functionText = functionText.getText().toString();
                        expression = new Expression(state.functionText);
                        yValue.setText(String.valueOf(setValue(currentX)));
                    });
                    builder.setNegativeButton(android.R.string.no, (dialog, which) -> {
                    });
                    builder.setView(view);
                    builder.setTitle(R.string.xy_sensor_function);
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
                    View view = View.inflate(getActivity(), R.layout.settings_xy_sensor, null);
                    final EditText xMax = view.findViewById(R.id.x_sensor_max);
                    xMax.setText(String.valueOf(state.xMax));
                    final EditText xMin = view.findViewById(R.id.x_sensor_min);
                    xMin.setText(String.valueOf(state.xMin));
                    final EditText xDelta = view.findViewById(R.id.x_sensor_delta);
                    xDelta.setText(String.valueOf(state.xDelta));
                    final CheckBox showCounter = view.findViewById(R.id.x_sensor_show_counter);
                    showCounter.setChecked((state.showCounter));
                    final CheckBox sendXValue = view.findViewById(R.id.send_x_value);
                    sendXValue.setChecked((state.sendXValue));
                    final EditText period = view.findViewById(R.id.data_transfer_period);
                    if (state.period > 0) {
                        period.setText(String.valueOf(state.period));
                    }
                    final RadioGroup radioGroupDataTransfer = view.findViewById(R.id.radio_group_data_transfer);
                    final RadioButton radioButtonManually = view.findViewById(R.id.data_transfer_manually);
                    final RadioButton radioButtonChange = view.findViewById(R.id.data_transfer_change);
                    final RadioButton radioButtonPeriodic = view.findViewById(R.id.data_transfer_periodic);
                    switch (state.dataTransferMode) {
                        case XYSensorFragmentState.DATA_TRANSFER_MODE_CHANGE:
                            radioButtonChange.setChecked(true);
                            break;
                        case XYSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC:
                            radioButtonPeriodic.setChecked(true);
                            period.setVisibility(View.VISIBLE);
                            break;
                        default:
                        case XYSensorFragmentState.DATA_TRANSFER_MODE_MANUALLY:
                            radioButtonManually.setChecked(true);
                            break;
                    }
                    radioGroupDataTransfer.setOnCheckedChangeListener((radioGroup, checkedId) -> {
                        switch (checkedId) {
                            case R.id.data_transfer_manually:
                                state.dataTransferMode = XYSensorFragmentState.DATA_TRANSFER_MODE_MANUALLY;
                                period.setVisibility(View.GONE);
                                break;
                            case R.id.data_transfer_change:
                                state.dataTransferMode = XYSensorFragmentState.DATA_TRANSFER_MODE_CHANGE;
                                period.setVisibility(View.GONE);
                                break;
                            case R.id.data_transfer_periodic:
                                state.dataTransferMode = XYSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC;
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
                        if (state.dataTransferMode == XYSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC && state.period > 0) {
                            updateSensorTask();
                        }
                        state.showCounter = showCounter.isChecked();
                        xSeekBar.setShowCounter(state.showCounter);
                        state.sendXValue = sendXValue.isChecked();
                        try {
                            BigDecimal tmpXMax = new BigDecimal(xMax.getText().toString().trim());
                            BigDecimal tmpXMin = new BigDecimal(xMin.getText().toString().trim());
                            BigDecimal tmpXDelta = new BigDecimal(xDelta.getText().toString().trim());
                            if (tmpXMax.compareTo(tmpXMin) > 0) {
                                if (tmpXDelta.signum() == 1 && tmpXMax.subtract(tmpXMin).divide(tmpXDelta, RoundingMode.DOWN).compareTo(new BigDecimal(1)) >= 1) {
                                    state.xMax = tmpXMax;
                                    state.xMin = tmpXMin;
                                    state.xDelta = tmpXDelta;
                                    xSeekBar.setMaxValue(state.xMax.subtract(state.xMin).divide(state.xDelta, RoundingMode.UP).intValue());
                                    if (currentX.compareTo(state.xMin) <= -1 || currentX.compareTo(state.xMax) >= 1) {
                                        currentX = state.xMin;
                                        xValue.setText(String.valueOf(currentX));
                                    }
                                } else {
                                    new SnackBar().ShowLong(getString(R.string.xy_sensor_invalid_x_parameters));
                                }
                            } else {
                                new SnackBar().ShowLong(getString(R.string.xy_sensor_invalid_x_parameters));
                            }
                        } catch (NumberFormatException ex) {
                            new SnackBar().ShowLong(getString(R.string.xy_sensor_invalid_x_parameters));
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        state.period = oldPeriod;
                        state.dataTransferMode = oldDataTransferMode;
                        if (updateSensorTimer != null) {
                            updateSensorTimer.cancel();
                            updateSensorTimer = null;
                        }
                        if (state.dataTransferMode == XYSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC && state.period > 0) {
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
