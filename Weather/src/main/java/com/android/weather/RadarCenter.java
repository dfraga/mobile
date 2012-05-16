package com.android.weather;

import com.weather.populate.EnumIdInterface;


public enum RadarCenter implements EnumIdInterface<String> {
	CORUNHA("A Coruña","corunha.tar", 43.170, -8.526),
	ASTURIAS("Asturias","santander.tar", 43.464, -6.301),
	ALMERIA("Almería","almeria.tar", 36.834, -2.081),
	BARCELONA("Barcelona","barcelona.tar", 41.409, 1.886),
	DONOSTI("Bilbao","donosti.tar", 43.404, -2.841),
	CACERES("Cáceres","caceres.tar", 39.430, -6.284),
	CANARIA("Canarias","canaria.tar", 28.019, -15.614),
	MADRID("Madrid","madrid.tar", 40.177, -3.713),
	MALAGA("Málaga","malaga.tar", 36.615, -4.658),
	MALLORCA("Mallorca","mallorca.tar", 39.381, 2.786),
	MURCIA("Murcia","murcia.tar", 38.266, -1.189),
	PALENCIA("Palencia","palencia.tar", 41.997, -4.601),
	SEVILLA("Sevilla","sevilla.tar", 37.689, -6.333),
	VALENCIA("Valencia","valencia.tar", 39.178, -0.250),
	ZARAGOZA("Zaragoza","zaragoza.tar", 41.735, -0.545)
	;

	final String id;
	final String filter;
	final double latitude;
	final double longitude;
	RadarCenter(final String id, final String filter, final double latitude, final double longitude) {
		this.id = id;
		this.filter = filter;
		this.latitude = latitude;
		this.longitude = longitude;
	}


	@Override
	public String getId() {
		return id;
	}


	public String getFilter() {
		return filter;
	}


	public double getLatitude() {
		return latitude;
	}


	public double getLongitude() {
		return longitude;
	}


	public static RadarCenter getById(final String id) {
		for (RadarCenter radar : RadarCenter.values()) {
			if (id.toLowerCase().equals(radar.getId().toLowerCase())) {
				return radar;
			}
		}
		return null;
	}


	@Override
	public boolean isNoMoreData() {
		//No aplica
		return true;
	}

	@Override
	public String toString() {
		return id;
	}

}
