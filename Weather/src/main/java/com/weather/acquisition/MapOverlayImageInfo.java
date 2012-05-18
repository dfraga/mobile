package com.weather.acquisition;

import java.io.Serializable;

import android.graphics.Bitmap;

import com.weather.populate.Class3X01Y192;
import com.weather.populate.Populator;

public class MapOverlayImageInfo implements Serializable {

	private static final long serialVersionUID = 3478447513121319792L;
	private Bitmap imBitmap;
	private long downloadTarFileSize;
	private Populator<Class3X01Y192> imageGeneralData;

	public MapOverlayImageInfo(final Bitmap imBitmap, final long downloadTarFileSize,
			final Populator<Class3X01Y192> imageGeneralData) {
		this.imBitmap = imBitmap;
		this.downloadTarFileSize = downloadTarFileSize;
		this.imageGeneralData = imageGeneralData;
	}

	public Bitmap getImBitmap() {
		return imBitmap;
	}

	public void setImBitmap(final Bitmap imBitmap) {
		this.imBitmap = imBitmap;
	}

	public long getDownloadTarFileSize() {
		return downloadTarFileSize;
	}

	public void setDownloadTarFileSize(final long downloadTarFileSize) {
		this.downloadTarFileSize = downloadTarFileSize;
	}

	public Populator<Class3X01Y192> getImageGeneralData() {
		return imageGeneralData;
	}

	public void setImageGeneralData(final Populator<Class3X01Y192> imageGeneralData) {
		this.imageGeneralData = imageGeneralData;
	}

}