package com.weather.populate;


public enum Class3X01Y192 implements EnumIdInterface {

	YEAR(0),
	MONTH(1),
	DAY(2),
	HOUR(3),
	MINUTE(4),
	LATITUDE_NW(5),
	LONGITUDE_NW(6),
	LATITUDE_NE(7),
	LONGITUDE_NE(8),
	LATITUDE_SE(9),
	LONGITUDE_SE(10),
	LATITUDE_SW(11),
	LONGITUDE_SW(12),
	PROJECTION_TYPE(13),
	RADAR_LATITUDE(14),
	RADAR_LONGITUDE(15),
	PIXEL_SIZE_ON_HORIZONTAL_1(16),
	PIXEL_SIZE_ON_HORIZONTAL_2(17),
	NUMBER_OF_PIXELS_PER_ROW(18),
	NUMBER_OF_PIXELS_PER_COLUMN(19),

	NO_MORE_DATA(-1);

	final int id;

	private Class3X01Y192(final int id) {
		this.id = id;
	}

	@Override
	public int getId() {
		return id;
	}

	public static Class3X01Y192 getById(final int id) {
		for (Class3X01Y192 type : Class3X01Y192.values()) {
			if (id == type.getId()) {
				return type;
			}
		}
		return NO_MORE_DATA;
	}

}
