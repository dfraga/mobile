package com.android.utils.menu;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.android.weather.R;

public class OptionsMenu {

	/* Set ID's */
	private final int MENU_CENTER = 0;
	private final int MENU_SHOW_PROCESS = 1;
	private final int MENU_SHOW_SCALE = 2;
	private final int MENU_SATELLITE_VIEW = 3;
	private final int MENU_QUIT = 4;

	private final OptionsMenuListener listener;

	public OptionsMenu(final OptionsMenuListener listener, final Menu menu, final Context context) {
		this.listener = listener;

		menu.add(0, MENU_SATELLITE_VIEW, MENU_SATELLITE_VIEW, listener.getSatelliteViewMenuId());
		menu.add(0, MENU_SHOW_PROCESS, MENU_SHOW_PROCESS, R.string.showProgress);
		menu.add(0, MENU_SHOW_SCALE, MENU_SHOW_SCALE, listener.getShowScaleMenuId());
		menu.add(0, MENU_CENTER, MENU_CENTER, R.string.centerMap);
		menu.add(0, MENU_QUIT, MENU_QUIT, R.string.exit);

		menu.getItem(MENU_SATELLITE_VIEW).setIcon(listener.getSatelliteViewMenuIconId());
		menu.getItem(MENU_SHOW_PROCESS).setIcon(R.drawable.progreso);
		menu.getItem(MENU_SHOW_SCALE).setIcon(R.drawable.escala);
		menu.getItem(MENU_CENTER).setIcon(R.drawable.centrar);
		menu.getItem(MENU_QUIT).setIcon(R.drawable.salir);
	}

	/* Handles Item Selection */
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SHOW_PROCESS:
			return this.listener.showProgressMenuSelected(item);

		case MENU_SHOW_SCALE:
			return listener.showScaleMenuSelected(item);

		case MENU_SATELLITE_VIEW:
			return listener.satelliteViewMenuSelected(item);

		case MENU_CENTER:
			return listener.centerMenuSelected(item);

		case MENU_QUIT:
			return listener.quitMenuSelected(item);
		}
		return false;
	}

}
