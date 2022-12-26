package ru.vukit.dc;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.google.gson.Gson;
import com.rarepebble.colorpicker.ColorPickerView;

import java.util.Timer;
import java.util.TimerTask;

import ru.vukit.dc.database.DBDriver;
import ru.vukit.dc.sensors.SensorMessage;

@Keep
public class MosaicSensorFragment extends Fragment implements View.OnTouchListener {

    final MosaicSensorFragmentState state = MosaicSensorFragmentState.getInstance();
    MainActivityInterface mainActivityInterface;
    EditText name, rowsNumberEditText, colsNumberEditText;
    ImageView sensorImage;
    CheckBox enable;
    ImageView colorPicker;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleDetector;
    private boolean onScaleAction = false;
    private boolean onLongPressAction = false;
    private BitmapShader bitmapShader;
    private Paint tilePaint;
    private static final int HALF_FONT_SIZE = 8;
    Timer updateSensorTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().addMenuProvider(new MosaicSensorFragment.FragmentMenuProvider(), getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        final View view = inflater.inflate(R.layout.mosaic_sensor, container, false);
        name = view.findViewById(R.id.sensor_name);
        name.setText(state.name);
        enable = view.findViewById(R.id.sensor_enable);
        enable.setOnClickListener(view1 -> state.enable = enable.isChecked());
        enable.setChecked(state.enable);
        rowsNumberEditText = view.findViewById(R.id.rows_number);
        colsNumberEditText = view.findViewById(R.id.cols_number);
        TextWatcher gridTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!rowsNumberEditText.getText().toString().trim().isEmpty() && !colsNumberEditText.getText().toString().trim().isEmpty()) {
                    try {
                        state.maxRows = Integer.parseInt(rowsNumberEditText.getText().toString().trim());
                        if (state.maxRows == 0) {
                            state.maxRows = 1;
                            rowsNumberEditText.setText("1");
                        }
                    } catch (NumberFormatException ex) {
                        state.maxRows = 1;
                        rowsNumberEditText.setText("1");
                    }
                    try {
                        state.maxCols = Integer.parseInt(colsNumberEditText.getText().toString().trim());
                        if (state.maxCols == 0) {
                            state.maxCols = 1;
                            colsNumberEditText.setText("1");
                        }
                    } catch (NumberFormatException ex) {
                        state.maxCols = 1;
                        colsNumberEditText.setText("1");
                    }
                    if (state.maxRows != state.mosaicArray.length || state.maxCols != state.mosaicArray[0].length) {
                        state.mosaicArray = new Integer[state.maxRows][state.maxCols];
                    }
                    state.startRow = 0;
                    state.endRow = state.maxRows - 1;
                    state.startCol = 0;
                    state.endCol = state.maxCols - 1;
                    state.scaleX = state.scaleY = 1f;
                    state.offsetX = state.offsetY = 0;
                    state.deltaX = state.scaleX * (state.imageSensorWidth - state.imageSensorLeft) / state.maxCols;
                    state.deltaY = state.scaleY * (state.imageSensorHeight - state.imageSensorTop) / state.maxRows;
                    redrawMosaicSensor();
                    if (state.dataTransferMode == MosaicSensorFragmentState.DATA_TRANSFER_MODE_CHANGE) {
                        updateSensorValue();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
        rowsNumberEditText.addTextChangedListener(gridTextWatcher);
        colsNumberEditText.addTextChangedListener(gridTextWatcher);
        if (state.mosaicArray == null) {
            state.mosaicArray = new Gson().fromJson(state.value, Integer[][].class);
            if (state.mosaicArray == null) {
                state.mosaicArray = new Integer[16][16];
            }
        }
        rowsNumberEditText.setText(String.valueOf(state.mosaicArray.length));
        colsNumberEditText.setText(String.valueOf(state.mosaicArray[0].length));
        state.maxRows = state.mosaicArray.length;
        state.maxCols = state.mosaicArray[0].length;
        if (state.startRow == -1 && state.endRow == -1 && state.startCol == -1 && state.endCol == -1) {
            state.startRow = 0;
            state.endRow = state.maxRows - 1;
            state.startCol = 0;
            state.endCol = state.maxCols - 1;
        }
        colorPicker = view.findViewById(R.id.mosaic_sensor_color);
        colorPicker.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> setupColorPicker(state.currentColor));
        colorPicker.setOnClickListener(v -> {
            final ColorPickerView picker = new ColorPickerView(getActivity(), null);
            picker.setColor(state.currentColor);
            picker.setupSelectedColors(getActivity(), state.selectedColors);
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setView(picker);
            builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
                state.updateSelectedColors(picker.getColor());
                setupColorPicker(state.currentColor);
            });
            builder.setNegativeButton(android.R.string.no, (dialog, which) -> {
            });
            builder.create();
            builder.show();
        });
        sensorImage = view.findViewById(R.id.sensor_image);
        sensorImage.setOnTouchListener(this);
        sensorImage.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            state.imageSensorWidth = sensorImage.getWidth();
            state.imageSensorHeight = sensorImage.getHeight();
            if (sensorImage.getWidth() > 0 && sensorImage.getHeight() > 0) {
                bitmap = Bitmap.createBitmap(state.imageSensorWidth, state.imageSensorHeight, Bitmap.Config.ARGB_8888);
                canvas = new Canvas(bitmap);
                paint = new Paint();
                paint.setColor(state.currentColor);
                paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
                paint.setTextSize(2 * HALF_FONT_SIZE);
                sensorImage.setImageBitmap(bitmap);
                bitmapShader = new BitmapShader(getBitmapFromVectorDrawable(R.drawable.checkered), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                tilePaint = new Paint();
                tilePaint.setAntiAlias(true);
                tilePaint.setShader(bitmapShader);
                state.deltaX = state.scaleX * (state.imageSensorWidth - state.imageSensorLeft) / state.maxCols;
                state.deltaY = state.scaleY * (state.imageSensorHeight - state.imageSensorTop) / state.maxRows;
                redrawMosaicSensor();
            }
        });
        mGestureDetector = new GestureDetector(getActivity(), new MyGestureDetector());
        mScaleDetector = new ScaleGestureDetector(getActivity(), new MyScaleListener());
        return view;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public Bitmap getBitmapFromVectorDrawable(int drawableId) {
        Drawable drawable = getResources().getDrawable(drawableId, null);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void setupColorPicker(int backgroundColor) {
        int width = colorPicker.getWidth(), height = colorPicker.getHeight();
        if (width > 0 && height > 0) {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColor(backgroundColor);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, width - 1, height - 1, paint);
            paint.setColor(0xFF888888);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(0, 0, width - 1, height - 1, paint);
            colorPicker.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            mainActivityInterface = (MainActivityInterface) getActivity();
        } catch (ClassCastException e) {
            mainActivityInterface = null;
        }
        if (state.dataTransferMode == MosaicSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC && state.period > 0) {
            updateSensorTask();
        }
    }

    @Override
    public void onPause() {
        if (updateSensorTimer != null) {
            updateSensorTimer.cancel();
            updateSensorTimer = null;
        }
        state.value = new Gson().toJson(state.mosaicArray);
        if (!name.getText().toString().trim().isEmpty()) {
            String restartService = "0";
            if (state.oldEnable != enable.isChecked() || !name.getText().toString().trim().equals(state.oldName)) {
                state.oldName = name.getText().toString().trim();
                state.oldEnable = enable.isChecked();
                restartService = "1";
            }
            DBDriver.getInstance().updateSensor(state.id, name.getText().toString().trim(), new Gson().toJson(state.selectedColors) + ":" + state.dataTransferMode + ":" + state.period, state.value.trim(), (enable.isChecked() ? "1" : "0"), restartService);
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
        state.value = new Gson().toJson(state.mosaicArray);
        SensorsFragmentState.getInstance().notifySensors(new SensorMessage(Integer.parseInt(state.id), SensorMessage.TYPE_NEW_DATA, state.value.trim(), (enable.isChecked() ? "1" : "0")));
    }

    private void setGrid() {
        float gX, gY;
        float labelXEdge = state.imageSensorLeft - 1, labelYEdge = state.imageSensorTop - 1;
        int countCol, countRow;
        float labelOffsetX;

        canvas.drawRect(state.imageSensorLeft, state.imageSensorTop, state.imageSensorWidth - 1, state.imageSensorHeight - 1, tilePaint);
        paint.setColor(Color.GRAY);
        canvas.drawLine(state.imageSensorLeft, state.imageSensorTop, state.imageSensorWidth - 1, state.imageSensorTop, paint);
        canvas.drawLine(state.imageSensorLeft, state.imageSensorTop, state.imageSensorLeft, state.imageSensorHeight - 1, paint);
        canvas.drawLine(state.imageSensorWidth - 1, state.imageSensorHeight - 1, state.imageSensorLeft, state.imageSensorHeight - 1, paint);
        canvas.drawLine(state.imageSensorWidth - 1, state.imageSensorHeight - 1, state.imageSensorWidth - 1, state.imageSensorTop, paint);
        countCol = state.endCol - state.startCol + 1;
        for (int x = 0; x < countCol; x++) {
            gX = x * state.deltaX - state.offsetX + state.imageSensorLeft;
            labelOffsetX = paint.measureText(String.valueOf(x + state.startCol + 1)) / 2;
            if (gX + state.deltaX / 2 - labelOffsetX - labelXEdge > 5 && gX + state.deltaX / 2 + labelOffsetX < state.imageSensorWidth - 1) {
                paint.setColor(Color.BLACK);
                canvas.drawText(String.valueOf(x + state.startCol + 1), gX + state.deltaX / 2 - labelOffsetX, (float) state.imageSensorTop / 2 + HALF_FONT_SIZE, paint);
                labelXEdge = gX + state.deltaX / 2 + labelOffsetX;
            }
            if (gX <= state.imageSensorLeft || gX >= state.imageSensorWidth - 1) {
                continue;
            }
            paint.setColor(Color.GRAY);
            canvas.drawLine(gX, state.imageSensorTop, gX, state.imageSensorHeight - 1, paint);
        }
        countRow = state.endRow - state.startRow + 1;
        for (int y = 0; y < countRow; y++) {
            gY = y * state.deltaY - state.offsetY + state.imageSensorTop;
            labelOffsetX = paint.measureText(String.valueOf(y + state.startRow + 1));
            if (gY + state.deltaY / 2 - HALF_FONT_SIZE - labelYEdge > 5 && gY + state.deltaY / 2 + HALF_FONT_SIZE < state.imageSensorHeight - 1) {
                paint.setColor(Color.BLACK);
                canvas.drawText(String.valueOf(y + state.startRow + 1), state.imageSensorLeft - 5 - labelOffsetX, gY + state.deltaY / 2 + HALF_FONT_SIZE, paint);
                labelYEdge = gY + state.deltaY / 2 + HALF_FONT_SIZE;
            }
            if (gY <= state.imageSensorTop || gY >= state.imageSensorHeight - 1) {
                continue;
            }
            paint.setColor(Color.GRAY);
            canvas.drawLine(state.imageSensorLeft, gY, state.imageSensorWidth - 1, gY, paint);
        }
    }

    private void redrawMosaicSensor() {
        if (paint != null && canvas != null && sensorImage != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            setGrid();
            drawValues();
            sensorImage.invalidate();
        }
    }

    private void drawValues() {
        float startX, startY, stopX, stopY, currentOffsetX, currentOffsetY;
        for (int row = state.startRow; row <= state.endRow; row++) {
            for (int col = state.startCol; col <= state.endCol; col++) {
                startX = (float) (col - state.startCol) * state.deltaX - state.offsetX + state.imageSensorLeft + 2;
                currentOffsetX = 0;
                if (startX <= state.imageSensorLeft) {
                    startX = state.imageSensorLeft + 2;
                    currentOffsetX = state.offsetX;
                }
                if (startX > state.imageSensorWidth - 1) {
                    continue;
                }
                stopX = startX + state.deltaX - 4 - currentOffsetX;
                if (stopX <= state.imageSensorLeft) {
                    continue;
                }
                if (stopX >= state.imageSensorWidth - 1) {
                    stopX = state.imageSensorWidth - 2;
                }
                startY = (float) (row - state.startRow) * state.deltaY - state.offsetY + state.imageSensorTop + 2;
                currentOffsetY = 0;
                if (startY <= state.imageSensorTop) {
                    startY = state.imageSensorTop + 2;
                    currentOffsetY = state.offsetY;
                }
                if (startY > state.imageSensorHeight - 1) {
                    continue;
                }
                stopY = startY + state.deltaY - 4 - currentOffsetY;
                if (stopY <= state.imageSensorTop) {
                    continue;
                }
                if (stopY >= state.imageSensorHeight - 1) {
                    stopY = state.imageSensorHeight - 2;
                }
                if (state.mosaicArray[row][col] != null) {
                    paint.setColor(0xFF000000 | state.mosaicArray[row][col]);
                    canvas.drawRect(startX, startY, stopX, stopY, paint);
                } else {
                    canvas.drawRect(startX, startY, stopX, stopY, tilePaint);
                }
            }
        }
    }

    private void fillCell(float x, float y, boolean isClear) {
        int row, col;
        float startX, startY, stopX, stopY, currentOffsetX, currentOffsetY, tempX, tempY;
        for (col = 0; col <= state.endCol - state.startCol + 1; col++) {
            tempX = x + state.offsetX - state.imageSensorLeft - 1;
            if (col * state.deltaX < tempX && tempX < (col + 1) * state.deltaX) {
                break;
            }
        }
        col += state.startCol;
        for (row = 0; row <= state.endRow - state.startRow + 1; row++) {
            tempY = y + state.offsetY - state.imageSensorTop - 1;
            if (row * state.deltaY < tempY && tempY < (row + 1) * state.deltaY) {
                break;
            }
        }
        row += state.startRow;
        startX = (float) (col - state.startCol) * state.deltaX - state.offsetX + state.imageSensorLeft + 2;
        currentOffsetX = 0;
        if (startX <= state.imageSensorLeft) {
            startX = state.imageSensorLeft + 2;
            currentOffsetX = state.offsetX;
        }
        if (startX > state.imageSensorWidth - 1) {
            return;
        }
        startY = (float) (row - state.startRow) * state.deltaY - state.offsetY + state.imageSensorTop + 2;
        currentOffsetY = 0;
        if (startY <= state.imageSensorTop) {
            startY = state.imageSensorTop + 2;
            currentOffsetY = state.offsetY;
        }
        if (startY > state.imageSensorHeight - 1) {
            return;
        }
        stopX = startX + state.deltaX - 4 - currentOffsetX;
        if (stopX <= state.imageSensorLeft) {
            return;
        }
        if (stopX >= state.imageSensorWidth - 1) {
            stopX = state.imageSensorWidth - 2;
        }
        stopY = startY + state.deltaY - 4 - currentOffsetY;
        if (stopY <= state.imageSensorTop) {
            return;
        }
        if (stopY >= state.imageSensorHeight - 1) {
            stopY = state.imageSensorHeight - 2;
        }
        if (isClear) {
            canvas.drawRect(startX, startY, stopX, stopY, tilePaint);
            state.mosaicArray[row][col] = null;
        } else {
            paint.setColor(state.currentColor);
            canvas.drawRect(startX, startY, stopX, stopY, paint);
            state.mosaicArray[row][col] = (state.currentColor & 0x00FFFFFF);
        }
        sensorImage.invalidate();
        if (state.dataTransferMode == MosaicSensorFragmentState.DATA_TRANSFER_MODE_CHANGE) {
            updateSensorValue();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (onLongPressAction) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    fillCell(event.getX(), event.getY(), false);
                    break;
                case MotionEvent.ACTION_UP:
                    onLongPressAction = false;
                    break;
            }
        } else {
            mScaleDetector.onTouchEvent(event);
            if (!onScaleAction) {
                mGestureDetector.onTouchEvent(event);
            }
        }
        v.performClick();
        return true;
    }

    class MyGestureDetector implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
        float scrollX, scrollY;

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            fillCell(e.getX(), e.getY(), false);
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            onLongPressAction = true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float dX, dY;
            if (e2.getX() < state.imageSensorLeft || e2.getX() > state.imageSensorWidth) {
                return false;
            }
            if (e2.getY() < state.imageSensorTop || e2.getY() > state.imageSensorHeight) {
                return false;
            }
            dX = e2.getX() - e1.getX();
            dY = e2.getY() - e1.getY();
            if (state.scaleX == 1f && state.scaleY == 1f) {
                state.startCol = 0;
                state.endCol = state.maxCols - 1;
                state.offsetX = 0;
                scrollX = 0;
                state.startRow = 0;
                state.endRow = state.maxRows - 1;
                state.offsetY = 0;
                scrollY = 0;
                return false;
            }
            state.offsetX = 0;
            state.startCol += scrollX;
            scrollX = (int) Math.floor(dX / state.deltaX);
            state.startCol -= scrollX;
            if (state.startCol < 0) {
                state.startCol = 0;
            }
            state.endCol = state.startCol + state.currentCols - 1;
            if (state.endCol >= state.maxCols - 1) {
                state.endCol = state.maxCols - 1;
                state.offsetX = state.deltaX * state.currentCols - (state.imageSensorWidth - state.imageSensorLeft);
                state.startCol = state.endCol - state.currentCols + 1;
                if (state.startCol < 0) {
                    state.startCol = 0;
                    state.endCol = state.currentCols - 1;
                    state.offsetX = 0;
                }
            }
            state.offsetY = 0;
            state.startRow += scrollY;
            scrollY = (int) Math.floor(dY / state.deltaY);
            state.startRow -= scrollY;
            if (state.startRow < 0) {
                state.startRow = 0;
            }
            state.endRow = state.startRow + state.currentRows - 1;
            if (state.endRow >= state.maxRows - 1) {
                state.endRow = state.maxRows - 1;
                state.offsetY = state.deltaY * state.currentRows - (state.imageSensorHeight - state.imageSensorTop);
                state.startRow = state.endRow - state.currentRows + 1;
                if (state.startRow < 0) {
                    state.startRow = 0;
                    state.endRow = state.currentRows - 1;
                    state.offsetY = 0;
                }
            }
            redrawMosaicSensor();
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            scrollX = scrollY = 0;
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
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            fillCell(e.getX(), e.getY(), true);
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

    }

    private class MyScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        int focusCol, focusRow;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            state.scaleX *= detector.getScaleFactor();
            if (state.scaleX < 1f || state.scaleX > state.maxCols || focusCol > state.maxCols - 1) {
                state.scaleX = 1f;
                state.startCol = 0;
                state.endCol = state.maxCols - 1;
                state.deltaX = state.scaleX * (state.imageSensorWidth - state.imageSensorLeft) / state.maxCols;
            } else {
                state.deltaX = Math.round(state.scaleX * (state.imageSensorWidth - state.imageSensorLeft) / state.maxCols);
                state.currentCols = (int) Math.floor((state.imageSensorWidth - state.imageSensorLeft) / state.deltaX);
                state.startCol = focusCol - (state.currentCols * (focusCol + 1) / state.maxCols);
                if (state.startCol < 0) {
                    state.startCol = 0;
                }
                state.endCol = state.startCol + state.currentCols;
                if (state.endCol > state.maxCols - 1) {
                    state.endCol = state.maxCols - 1;
                }
            }
            state.scaleY *= detector.getScaleFactor();
            if (state.scaleY < 1f || state.scaleY > state.maxRows || focusRow > state.maxRows - 1) {
                state.scaleY = 1f;
                state.startRow = 0;
                state.endRow = state.maxRows - 1;
                state.deltaY = state.scaleY * (state.imageSensorHeight - state.imageSensorTop) / state.maxRows;
            } else {
                state.deltaY = Math.round(state.scaleY * (state.imageSensorHeight - state.imageSensorTop) / state.maxRows);
                state.currentRows = (int) Math.floor((state.imageSensorHeight - state.imageSensorTop) / state.deltaY);
                state.startRow = focusRow - (state.currentRows * (focusRow + 1) / state.maxRows);
                if (state.startRow < 0) {
                    state.startRow = 0;
                }
                state.endRow = state.startRow + state.currentRows;
                if (state.endRow > state.maxRows - 1) {
                    state.endRow = state.maxRows - 1;
                }
            }
            state.currentRows = state.endRow - state.startRow + 1;
            state.currentCols = state.endCol - state.startCol + 1;
            state.offsetX = state.offsetY = 0;
            redrawMosaicSensor();
            onScaleAction = true;
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            focusCol = (int) Math.floor((detector.getFocusX() - state.offsetX - state.imageSensorLeft) / state.deltaX) + state.startCol;
            focusRow = (int) Math.floor((detector.getFocusY() - state.offsetY - state.imageSensorTop) / state.deltaY) + state.startRow;
            onScaleAction = true;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            onScaleAction = false;
            super.onScaleEnd(detector);
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
                        state.startRow = 0;
                        state.endRow = state.maxRows - 1;
                        state.startCol = 0;
                        state.endCol = state.maxCols - 1;
                        state.scaleX = state.scaleY = 1f;
                        state.offsetX = state.offsetY = 0;
                        state.deltaX = state.scaleX * (state.imageSensorWidth - state.imageSensorLeft) / state.maxCols;
                        state.deltaY = state.scaleY * (state.imageSensorHeight - state.imageSensorTop) / state.maxRows;
                        for (int row = 0; row < state.maxRows; row++) {
                            for (int col = 0; col < state.maxCols; col++) {
                                state.mosaicArray[row][col] = null;
                            }
                        }
                        redrawMosaicSensor();
                        if (state.dataTransferMode == MosaicSensorFragmentState.DATA_TRANSFER_MODE_CHANGE) {
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
                        case MosaicSensorFragmentState.DATA_TRANSFER_MODE_CHANGE:
                            radioButtonChange.setChecked(true);
                            break;
                        case MosaicSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC:
                            radioButtonPeriodic.setChecked(true);
                            period.setVisibility(View.VISIBLE);
                            break;
                        default:
                        case MosaicSensorFragmentState.DATA_TRANSFER_MODE_MANUALLY:
                            radioButtonManually.setChecked(true);
                            break;
                    }
                    radioGroupDataTransfer.setOnCheckedChangeListener((radioGroup, checkedId) -> {
                        switch (checkedId) {
                            case R.id.data_transfer_manually:
                                state.dataTransferMode = MosaicSensorFragmentState.DATA_TRANSFER_MODE_MANUALLY;
                                period.setVisibility(View.GONE);
                                break;
                            case R.id.data_transfer_change:
                                state.dataTransferMode = MosaicSensorFragmentState.DATA_TRANSFER_MODE_CHANGE;
                                period.setVisibility(View.GONE);
                                break;
                            case R.id.data_transfer_periodic:
                                state.dataTransferMode = MosaicSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC;
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
                        if (state.dataTransferMode == MosaicSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC && state.period > 0) {
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
                        if (state.dataTransferMode == MosaicSensorFragmentState.DATA_TRANSFER_MODE_PERIODIC && state.period > 0) {
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