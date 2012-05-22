package com.android.utils.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class MapOverlay extends com.google.android.maps.Overlay
{
	final GeoPoint ctPoint;
	final GeoPoint nwPoint;
	final GeoPoint sePoint;
	final Bitmap overlay;

	public MapOverlay(final GeoPoint ctPoint, final Bitmap overlay, final GeoPoint nwPoint, final GeoPoint sePoint) {
		this.ctPoint = ctPoint;
		this.overlay = overlay;
		this.nwPoint = nwPoint;
		this.sePoint = sePoint;
	}

	@Override
	public boolean draw(final Canvas canvas, final MapView mapView,
			final boolean shadow, final long when)
	{
		super.draw(canvas, mapView, shadow);

		// GeoPoint a screen pixels
		Point centerPoint = new Point();
		Point topPoint = new Point();
		Point bottomPoint = new Point();
		mapView.getProjection().toPixels(ctPoint, centerPoint);
		mapView.getProjection().toPixels(nwPoint, topPoint);
		mapView.getProjection().toPixels(sePoint, bottomPoint);

		// Marcador (imagen procesada)
		Rect rect = new Rect(topPoint.x, topPoint.y, bottomPoint.x, bottomPoint.y);
		canvas.drawBitmap(overlay, null, rect, new Paint(Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG|Paint.ANTI_ALIAS_FLAG));

		// dibujar circulo limite y no representar los valores desconocidos en la imagen
		Paint borderPaint = new Paint();
		borderPaint.setARGB(255, 255, 25, 25);
		borderPaint.setAntiAlias(true);
		borderPaint.setStyle(Style.STROKE);
		borderPaint.setStrokeWidth(2);

		canvas.drawCircle(centerPoint.x, centerPoint.y, Math.abs(topPoint.x - centerPoint.x), borderPaint);
		return true;
	}
}
