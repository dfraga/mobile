package com.android.weather;

import android.content.Context;
import android.graphics.Bitmap;

public interface WeatherProcessListener {

	void setPercentageProgression(final int x, final int y, final int yMax);
	void openImage(final Bitmap bitmap);
	Context getContext();
	void processException(Throwable e);

}
