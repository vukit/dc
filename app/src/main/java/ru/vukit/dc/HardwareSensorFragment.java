package ru.vukit.dc;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import ru.vukit.dc.database.DBDriver;
import ru.vukit.dc.sensors.SensorFactory;

@Keep
public class HardwareSensorFragment extends Fragment implements SensorEventListener, OnChartValueSelectedListener {

    final HardwareSensorFragmentState state = HardwareSensorFragmentState.getInstance();
    Context context;
    EditText name;
    CheckBox enable;
    TextView tvValues;
    Float[] values;
    SensorManager mSensorManager;
    private LineChart graph;
    private LineData data;
    private int historySize;
    SeekBar seekBar;
    Timer plotTimer;
    final private int MAX_ENTRY = 100;
    final private int sampleRate = 50;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = StartApplication.getInstance().getApplicationContext();
        View view = inflater.inflate(R.layout.hardware_sensor, container, false);
        name = view.findViewById(R.id.hardware_sensor_name);
        name.setText(state.name);
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
                if (state.enable) {
                    restartPlotting();
                }
            }
        });
        CharSequence valueNamesInfo = "";
        SpannableString valueNameInfo;
        String newLines = "\n\n";
        state.valuesLength = Integer.parseInt(SensorFactory.parameters.get(state.code)[1]);
        values = new Float[state.valuesLength];
        for (int i = 0; i < state.valuesLength; i++) {
            if (i == state.valuesLength - 1) newLines = "";
            if (SensorFactory.parameters.get(state.code)[i + 3].isEmpty()) {
                valueNameInfo = new SpannableString(SensorFactory.parameters.get(state.code)[i + 3 + state.valuesLength] + ": " + newLines);
            } else {
                valueNameInfo = new SpannableString(SensorFactory.parameters.get(state.code)[i + 3 + state.valuesLength] + " (" + SensorFactory.parameters.get(state.code)[i + 3] + "): " + newLines);
            }
            valueNameInfo.setSpan(new ForegroundColorSpan(state.colors[i]), 0, SensorFactory.parameters.get(state.code)[i + 3 + state.valuesLength].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            valueNamesInfo = TextUtils.concat(valueNamesInfo, valueNameInfo);
        }
        graph = view.findViewById(R.id.graph);
        initGraph();
        ((TextView) view.findViewById(R.id.hardware_sensor_values_name)).setText(valueNamesInfo);
        tvValues = view.findViewById(R.id.hardware_sensor_values);
        enable = view.findViewById(R.id.hardware_sensor_enable);
        enable.setOnClickListener(view1 -> {
            if (enable.isChecked()) {
                state.enable = true;
                restartSensorManager();
                restartPlotting();
            } else {
                state.enable = false;
                stopSensorManager();
                stopPlotting();
            }
        });
        enable.setChecked(state.enable);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getResources().getString(R.string.edit_sensor_dialog_title));
        }
        if (state.enable) {
            restartSensorManager();
            restartPlotting();
        }
    }

    @Override
    public void onPause() {
        stopPlotting();
        stopSensorManager();
        if ((!name.getText().toString().trim().isEmpty() && !name.getText().toString().trim().equals(state.oldName)) ||
                enable.isChecked() != state.oldEnable) {
            DBDriver.getInstance().updateSensor(state.id, name.getText().toString().trim(), "", "", (enable.isChecked() ? "1" : "0"), "1");
        }
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        boolean initDataSet = false;
        CharSequence values_info = "";
        SpannableString value_info;
        String newLines = "\n\n";
        for (int k = 0; k < state.valuesLength; k++) {
            values[k] = sensorEvent.values[k];
            if (!(values[k].isNaN() || values[k].isInfinite())) {
                if (state.graphSeries.size() < state.valuesLength) {
                    LineDataSet lineDataSet = new LineDataSet(new ArrayList<>(), "");
                    lineDataSet.setColor(state.colors[k]);
                    lineDataSet.setDrawCircles(false);
                    lineDataSet.setDrawValues(false);
                    lineDataSet.addEntry(new Entry(0, values[k]));
                    state.graphSeries.add(k, lineDataSet);
                    if (state.graphSeries.size() == state.valuesLength) {
                        initDataSet = true;
                    }
                }
            }
            if (k == state.valuesLength - 1) newLines = "";
            value_info = new SpannableString(values[k] + newLines);
            values_info = TextUtils.concat(values_info, value_info);
        }
        if (initDataSet || (data == null && state.graphSeries.size() == state.valuesLength)) {
            data = new LineData(state.graphSeries);
            graph.setData(data);
        }
        tvValues.setText(values_info);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void startSensorManager() {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(state.code), SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void stopSensorManager() {
        if (mSensorManager != null) mSensorManager.unregisterListener(this);
    }

    private void restartSensorManager() {
        stopSensorManager();
        startSensorManager();
    }

    private void startPlotting() {
        final Runnable plotUpdate = () -> {
            LineDataSet dataSet;
            int delta;
            if (data != null && graph != null) {
                for (int k = 0; k < state.valuesLength; k++) {
                    dataSet = (LineDataSet) state.graphSeries.get(k);
                    delta = dataSet.getEntryCount() - historySize;
                    if (delta > 0) {
                        for (int i = 0; i < delta; i++) {
                            dataSet.removeFirst();
                        }
                        for (Entry entry : dataSet.getValues()) {
                            entry.setX(entry.getX() - delta);
                        }
                    }
                    if (!(values[k].isNaN() || values[k].isInfinite())) {
                        dataSet.addEntry(new Entry(dataSet.getEntryCount(), values[k]));
                    }
                }
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
        graph.getXAxis().setValueFormatter(new graphXAxisValueFormatter());
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

    @Override
    public void onValueSelected(Entry e, Highlight h) {
    }

    @Override
    public void onNothingSelected() {
    }

}
