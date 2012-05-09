package com.android.weather;

import android.content.Context;
import android.graphics.Bitmap;

public interface WeatherProcessListener {

	void setPercentageProgression(final int partial, final int max);
	void setPercentageMessage(final String message);
	void openImage(final Bitmap bitmap);
	Context getContext();
	void processException(Throwable e);

}
