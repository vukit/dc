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

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.EditText;

class HexEdit {

	private static final InputFilter[] hexEditDigits = {new InputFilter.LengthFilter(6)};

	static void setUpListeners(final EditText hexEdit, final ObservableColor observableColor) {

		class MultiObserver implements ColorObserver, TextWatcher {

			@Override
			public void updateColor(ObservableColor observableColor) {
				final String colorString = formatColor(observableColor.getColor());
				// Prevent onTextChanged getting called when we update text programmatically
				hexEdit.removeTextChangedListener(this);
				hexEdit.setText(colorString);
				hexEdit.addTextChangedListener(this);
			}

			private String formatColor(int color) {
				return String.format("%06x", color & 0x00ffffff);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				try {
					//int color = (int)(Long.parseLong(s.toString(), 16) & 0xffffffff);
					int color = (int)(Long.parseLong(s.toString(), 16));
					color = color | 0xff000000;
					observableColor.updateColor(color, this);
				}
				catch (NumberFormatException e) {
					observableColor.updateColor(0, this);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
			}

		}

		final MultiObserver multiObserver = new MultiObserver();
		hexEdit.addTextChangedListener(multiObserver);
		observableColor.addObserver(multiObserver);
		setHexEditDigits(hexEdit);
	}


	private static void setHexEditDigits(final EditText hexEdit) {
		hexEdit.setFilters(hexEditDigits);
		hexEdit.setText(hexEdit.getText()); // trigger a reformat of text
	}

}
