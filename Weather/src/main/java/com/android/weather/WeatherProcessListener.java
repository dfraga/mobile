package com.android.weather;

import android.content.Context;
import android.graphics.Bitmap;

import com.weather.populate.Class3X01Y192;
import com.weather.populate.Populator;

public interface WeatherProcessListener {

	void setPercentageProgression(int partial, int max);
	void setPercentageMessage(String message);
	void setProcessedImage(Bitmap bitmap, Populator<Class3X01Y192> imageGeneralData);
	Context getContext();
	void processException(Throwable e);
	void processGetFtpEnded();
	void setDownloadTarFile(String name, long size);

}
