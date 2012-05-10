package com.android.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class GroundOverlay extends OverlayItem {

	private final MapView map;
	private final Bitmap overlay;
	private final GeoPoint topGeoPoint;
	private final GeoPoint bottomGeoPoint;

	public Drawable drawable;

	public GroundOverlay(
			final MapView map,
			final GeoPoint topLeftGeoPoint,
			final GeoPoint bottomRightGeoPoint,
			final Bitmap overlay,
			final String title,
			final String snippet) {
		super(bottomRightGeoPoint, title, snippet);

		this.map = map;
		this.overlay = overlay;
		this.topGeoPoint = topLeftGeoPoint;
		this.bottomGeoPoint = bottomRightGeoPoint;

		_init();
	}

	private void _init() {
		Point topPoint = new Point();
		Point bottomPoint = new Point();

		map.getProjection().toPixels(topGeoPoint, topPoint);
		map.getProjection().toPixels(bottomGeoPoint, bottomPoint);

		int width = bottomPoint.x - topPoint.x;
		int height = bottomPoint.y - topPoint.y;

		drawable = overlayDrawable(overlay, width, height);
	}

	private BitmapDrawable overlayDrawable(final Bitmap bitmap, final int newWidth, final int newHeight) {
		Matrix scale = new Matrix();

		float scaleFloat = newWidth / (float)bitmap.getWidth();

		scale.postScale(scaleFloat, scaleFloat);
		Bitmap bitmapScaled = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), scale, false);

		BitmapDrawable bm = new BitmapDrawable(bitmapScaled);
		bm.setBounds(0,0,bitmapScaled.getWidth(),bitmapScaled.getHeight());

		return bm;
	}

}
