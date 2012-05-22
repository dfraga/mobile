package com.android.utils.color;

import android.graphics.Color;

public class ColorUtils {

	public static int getInvertedColor(final int baseColor) {
		return getHuePhasedColor(baseColor, 180.0f);
	}

	public static int getAlphaColor(final int alpha, final int baseColor) {
		return Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
	}

	public static int getHuePhasedColor(final int baseColor, final float hueDegrees) {
		final float[] hsv = new float[3];
		Color.colorToHSV(baseColor, hsv);
		hsv[0] = hsv[0] + hueDegrees;
		return Color.HSVToColor(hsv);
	}

	public static int getSaturationPhasedColor(final int baseColor, final float saturation) {
		final float[] hsv = new float[3];
		Color.colorToHSV(baseColor, hsv);
		hsv[1] = normalizedPercentage(hsv[1], saturation);
		return Color.HSVToColor(hsv);
	}

	public static int getValuePhasedColor(final int baseColor, final float value) {
		final float[] hsv = new float[3];
		Color.colorToHSV(baseColor, hsv);
		hsv[2] = normalizedPercentage(hsv[2], value);
		return Color.HSVToColor(hsv);
	}

	private static float normalizedPercentage(final float value1, final float value2){
		float res = (value1*100.0f) + value2;
		res = res > 100.0f ? 100.0f : res;
		res = res < 0.0f ? 0.0f : res;
		return res == 0 ? res : (res/100.0f);
	}

}
