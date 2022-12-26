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

class DecEdit {

	private static final InputFilter[] decEditDigits = {new InputFilter.LengthFilter(8)};

	static void setUpListeners(final EditText decEdit, final ObservableColor observableColor) {

		class MultiObserver implements ColorObserver, TextWatcher {

			@Override
			public void updateColor(ObservableColor observableColor) {
				final String colorString = formatColor(observableColor.getColor());
				// Prevent onTextChanged getting called when we update text programmatically
				decEdit.removeTextChangedListener(this);
				decEdit.setText(colorString);
				decEdit.addTextChangedListener(this);
			}

			private String formatColor(int color) {
				return String.valueOf(color & 0xffffff);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				try {
					int color = (int) Long.parseLong(s.toString());
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
		decEdit.addTextChangedListener(multiObserver);
		observableColor.addObserver(multiObserver);
		setDecEditDigits(decEdit);
	}


	private static void setDecEditDigits(final EditText decEdit) {
		decEdit.setFilters(decEditDigits);
		decEdit.setText(decEdit.getText()); // trigger a reformat of text
	}

}
