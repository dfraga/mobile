package com.android.utils.menu;

import android.view.MenuItem;


public interface OptionsMenuListener {

	boolean showProgressMenuSelected(final MenuItem item);
	boolean showScaleMenuSelected(final MenuItem item);
	boolean satelliteViewMenuSelected(MenuItem item);
	boolean centerMenuSelected(MenuItem item);
	boolean quitMenuSelected(MenuItem item);

	int getSatelliteViewMenuId();
	int getShowScaleMenuId();
	int getSatelliteViewMenuIconId();

}
