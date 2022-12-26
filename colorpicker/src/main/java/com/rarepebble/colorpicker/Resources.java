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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

class Resources {

	private static final int VIEW_OUTLINE_COLOR = 0xff888888;

	static Paint makeLinePaint() {
		Paint paint = new Paint();
		paint.setColor(VIEW_OUTLINE_COLOR);
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);
		return paint;
	}

	@SuppressLint("UseCompatLoadingForDrawables")
	static Paint makeCheckerPaint(Context context) {
		Paint paint = new Paint();
		Drawable drawable = context.getResources().getDrawable(R.drawable.checkered, null);
		Bitmap checkerBmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(checkerBmp);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		paint.setShader(new BitmapShader(checkerBmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
		return paint;
	}

	static Path makePointerPath() {
		Path pointerPath = new Path();
		final float radiusPx = 10f;
		pointerPath.addCircle(0, 0, radiusPx, Path.Direction.CW);
		return pointerPath;
	}

}
