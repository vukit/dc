/*
 * Copyright (C) 2015 Martin Stone
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rarepebble.colorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ColorPickerView extends FrameLayout {

	private final EditText hexEdit;
	private final EditText decEdit;
	private final ObservableColor observableColor = new ObservableColor(0);
	private final SwatchView swatchView;
	OnLayoutChangeListener layoutChangeListener;

	public ColorPickerView(final Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.picker, this);

		swatchView = findViewById(R.id.swatchView);
		swatchView.observeColor(observableColor);

		HueSatView hueSatView = findViewById(R.id.hueSatView);
		hueSatView.observeColor(observableColor);

		ValueView valueView = findViewById(R.id.valueView);
		valueView.observeColor(observableColor);

		hexEdit = findViewById(R.id.hexEdit);
		HexEdit.setUpListeners(hexEdit, observableColor);

		decEdit = findViewById(R.id.decEdit);
		DecEdit.setUpListeners(decEdit, observableColor);

		applyAttributes(attrs);

		// We get all our state saved and restored for free,
		// thanks to the EditText and its listeners!
	}

	private void applyAttributes(AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ColorPicker, 0, 0);
			showHex(a.getBoolean(R.styleable.ColorPicker_showHex, true));
			showDec(a.getBoolean(R.styleable.ColorPicker_showDec, true));
			showPreview(a.getBoolean(R.styleable.ColorPicker_showPreview, true));
		}
	}

	public void setupSelectedColors(final Context context, final Integer[] selectedColors) {
		final LinearLayout selectedColorsLayout = findViewById(R.id.selectedColors);
		selectedColorsLayout.setVisibility(VISIBLE);
		layoutChangeListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left;
            int height = bottom - top;
            if (width < height) { // Land
                height = Math.round((float)(height - 20) / 5);
                for(int k = 0; k < 5; k++) {
                    ImageView imageView = new ImageView(context);
                    if (k != 4) {
                        imageView.setPadding(0,0,0,5);
                    }
                    setupSelectedColor(context, imageView, width, height, selectedColors[k]);
                    selectedColorsLayout.addView(imageView);
                }
            } else { // Portrait
                width = (int) Math.floor((float)(width - 20) / 5);
                for(int k = 0; k < 5; k++) {
                    ImageView imageView = new ImageView(context);
                    if (k != 4) {
                        imageView.setPadding(0,0,5,0);
                    }
                    setupSelectedColor(context, imageView, width, height, selectedColors[k]);
                    selectedColorsLayout.addView(imageView);
                }
            }
            selectedColorsLayout.removeOnLayoutChangeListener(layoutChangeListener);
        };
		selectedColorsLayout.addOnLayoutChangeListener(layoutChangeListener);
	}

	private void setupSelectedColor(Context context, ImageView imageView, int width, int height, final Integer backgroundColor) {
		if (width > 0 && height > 0) {
			Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			if (backgroundColor != null) {
				imageView.setOnClickListener(v -> setColor(backgroundColor));
				Paint paint = new Paint();
				paint.setColor(backgroundColor);
				paint.setStyle(Paint.Style.FILL);
				canvas.drawRect(0, 0, width - 1, height - 1, paint);
			} else {
				canvas.drawRect(0, 0, width - 1, height - 1, Resources.makeCheckerPaint(context));
			}
			canvas.drawRect(0, 0, width - 1, height - 1, Resources.makeLinePaint());
			imageView.setImageBitmap(bitmap);
		}
	}
	/** Returns the color selected by the user */
	public int getColor() {
		return observableColor.getColor();
	}

	/** Sets the original color swatch and the current color to the specified value. */
	public void setColor(int color) {
		setOriginalColor(color);
		setCurrentColor(color);
		setSourceColor(color);
	}

	/** Sets the original color swatch without changing the current color. */
	public void setOriginalColor(int color) {
		swatchView.setOriginalColor(color);
	}

	/** Updates the current color without changing the original color swatch. */
	public void setCurrentColor(int color) {
		observableColor.updateColor(color, null);
	}

	public void setSourceColor(int color) {
		observableColor.setSourceColor(color);
	}


	public void showHex(boolean showHex) {
		hexEdit.setVisibility(showHex ? View.VISIBLE : View.GONE);
	}

	public void showDec(boolean showDec) {
		decEdit.setVisibility(showDec ? View.VISIBLE : View.GONE);
	}

	public void showPreview(boolean showPreview) {
		swatchView.setVisibility(showPreview ? View.VISIBLE : View.GONE);
	}

}
