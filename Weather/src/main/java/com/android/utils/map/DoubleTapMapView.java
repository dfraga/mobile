package com.android.utils.map;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

import com.google.android.maps.MapView;

public class DoubleTapMapView extends MapView {

	private final GestureDetector gestureDetector;

	public DoubleTapMapView(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		gestureDetector = new GestureDetector((OnGestureListener) context);
		gestureDetector.setOnDoubleTapListener((OnDoubleTapListener) context);
	}

	@Override
	public boolean onTouchEvent(final MotionEvent ev) {
		if (this.gestureDetector.onTouchEvent(ev)) {
			return true;
		}
		return super.onTouchEvent(ev);
	}

}