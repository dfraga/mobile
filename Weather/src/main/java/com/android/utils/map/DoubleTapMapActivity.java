package com.android.utils.map;

import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.android.weather.RadarCenter;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.Overlay;

public class DoubleTapMapActivity extends MapActivity implements GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {

	private DoubleTapMapView mapView;
	private Location lastKnownLocation;
	//	private LocationListener locationListener;

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/** Called when the activity is first created. */
	public void initMapView(final DoubleTapMapView mapView) {
		this.mapView = mapView;
		this.mapView.setBuiltInZoomControls(true);
		this.mapView.setStreetView(false);
		this.mapView.setTraffic(false);

		//		locationListener = new LocationListener() {
		//			@Override
		//			public void onLocationChanged(final Location location) {
		//				lastKnownLocation = location;
		//			}
		//
		//			@Override
		//			public void onProviderDisabled(final String provider) {
		//
		//			}
		//			@Override
		//			public void onProviderEnabled(final String provider) {
		//
		//			}
		//
		//			@Override
		//			public void onStatusChanged(final String provider, final int status, final Bundle extras) {
		//
		//			}
		//		};

	}

	public void drawOverlay(final Bitmap imBitmap, final GeoPoint center, final GeoPoint nw, final GeoPoint se) {
		if(center != null) {
			centerMap(center, false);

			List<Overlay> overlays = getMapView().getOverlays();
			overlays.clear();

			MapOverlay mapOverlay = new MapOverlay(center, imBitmap, nw, se);
			overlays.add(mapOverlay);
		}
		mapView.invalidate();
	}

	@Override
	public boolean onDoubleTap(final MotionEvent ev) {
		if(mapView.getZoomLevel() > 5 && mapView.getZoomLevel() < (mapView.getMaxZoomLevel()-1)) {
			mapView.getController().animateTo(mapView.getProjection().fromPixels((int) ev.getX(), (int) ev.getY()));
			mapView.getController().zoomIn();
		}
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(final MotionEvent e) {
		return false;
	}

	@Override
	public void onLongPress(final MotionEvent ev) {
		centerMap(mapView.getProjection().fromPixels((int) ev.getX(), (int) ev.getY()), true);
		mapView.invalidate();
	}

	@Override
	public boolean onSingleTapConfirmed(final MotionEvent e) {
		return false;
	}

	@Override
	public boolean onDown(final MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
		return false;
	}

	@Override
	public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,final float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(final MotionEvent e) {}

	@Override
	public boolean onSingleTapUp(final MotionEvent e) {
		return false;
	}

	protected GeoPoint setMapUserCenter(final boolean fineZoom) {

		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		//		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		//		if(lastKnownLocation == null) {
		lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		//		}

		// obetener centro por defecto del seleccionado
		final GeoPoint center = (lastKnownLocation == null ?
				getGeoPoint(getSelectedRadar().getLatitude(), getSelectedRadar().getLongitude()):
					getGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));

		centerMap(center, fineZoom);
		if(fineZoom) {
			mapView.invalidate();
		}

		return center;

	}

	protected double getFakeDistance(final GeoPoint currentGeo, final GeoPoint center) {
		if(currentGeo == null || center == null) {
			return Double.MAX_VALUE;
		}
		return Math.pow((currentGeo.getLatitudeE6() - center.getLatitudeE6()), 2)
				+ Math.pow((currentGeo.getLongitudeE6() - center.getLongitudeE6()),2);
	}

	protected void centerMap(final GeoPoint center, final boolean fineZoom) {
		final MapController mapController = mapView.getController();
		mapController.setCenter(center);
		mapController.animateTo(center);
		mapController.setZoom(fineZoom ? 14:8);
	}

	protected GeoPoint getGeoPoint(final double latitude, final double longitude) {
		return new GeoPoint((int)(1e6 * latitude), (int)(1e6 * longitude));
	}

	protected RadarCenter getSelectedRadar() {
		//por defecto si no se sobreescribe
		return RadarCenter.MADRID;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	public DoubleTapMapView getMapView() {
		return mapView;
	}

}
