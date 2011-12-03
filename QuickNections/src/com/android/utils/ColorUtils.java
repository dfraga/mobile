package com.android.utils;

import android.graphics.Color;

public class ColorUtils {

	public static int getInvertedColor(final int baseColor) {
		final float[] hsv = new float[3];
		Color.colorToHSV(baseColor, hsv);
		hsv[0] = hsv[0] - 180.0f;
		return Color.HSVToColor(hsv);
	}

	public static int getAlphaColor(final int alpha, final int baseColor) {
		return Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
	}

}
