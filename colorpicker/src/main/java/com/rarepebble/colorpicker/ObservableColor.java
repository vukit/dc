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

import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SameParameterValue")
public class ObservableColor {

	// Store as HSV & A, otherwise round-trip to int causes color drift.
	private final float[] hsv = {0, 0, 0};
	private int alpha;
	private int sourceColor;
	private boolean isSetSourceColor;
	private final List<ColorObserver> observers = new ArrayList<>();

	ObservableColor(int color) {
		Color.colorToHSV(color, hsv);
		alpha = Color.alpha(color);
		isSetSourceColor = false;
	}

	void getHsv(float[] hsvOut) {
		hsvOut[0] = hsv[0];
		hsvOut[1] = hsv[1];
		hsvOut[2] = hsv[2];
	}

	public int getColor() {
		if (isSetSourceColor) {
			return sourceColor;
		} else {
			return Color.HSVToColor(alpha, hsv);
		}
	}

	float getHue() {
		return hsv[0];
	}

	float getSat() {
		return hsv[1];
	}

	public float getValue() {
		return hsv[2];
	}

	float getLightness() {
		return getLightnessWithValue(hsv[2]);
	}

	float getLightnessWithValue(float value) {
		float[] hsV = {hsv[0], hsv[1], value};
		final int color = Color.HSVToColor(hsV);
		return (Color.red(color) * 0.2126f + Color.green(color) * 0.7152f + Color.blue(color) * 0.0722f)/0xff;
	}

	public void addObserver(ColorObserver observer) {
		observers.add(observer);
	}

	void updateHueSat(float hue, float sat, ColorObserver sender) {
		hsv[0] = hue;
		hsv[1] = sat;
		isSetSourceColor = false;
		notifyOtherObservers(sender);
	}

	void updateValue(float value, ColorObserver sender) {
		hsv[2] = value;
		isSetSourceColor = false;
		notifyOtherObservers(sender);
	}

	void updateColor(int color, ColorObserver sender) {
		Color.colorToHSV(color, hsv);
		alpha = Color.alpha(color);
		isSetSourceColor =false;
		notifyOtherObservers(sender);
	}

	private void notifyOtherObservers(ColorObserver sender) {
		for (ColorObserver observer : observers) {
			if (observer != sender) {
				observer.updateColor(this);
			}
		}
	}

	void setSourceColor(int color) {
		isSetSourceColor = true;
		sourceColor = color;
	}
}
