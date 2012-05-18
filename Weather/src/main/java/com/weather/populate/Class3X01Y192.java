package com.weather.populate;


public enum Class3X01Y192 implements EnumIdInterface<Integer> {

	YEAR(0, false),
	MONTH(1, false),
	DAY(2, false),
	HOUR(3, false),
	MINUTE(4, false),
	LATITUDE_NW(5, false),
	LONGITUDE_NW(6, false),
	LATITUDE_NE(7, false),
	LONGITUDE_NE(8, false),
	LATITUDE_SE(9, false),
	LONGITUDE_SE(10, false),
	LATITUDE_SW(11, false),
	LONGITUDE_SW(12, false),
	PROJECTION_TYPE(13, false),
	RADAR_LATITUDE(14, false),
	RADAR_LONGITUDE(15, false),
	PIXEL_SIZE_ON_HORIZONTAL_1(16, false),
	PIXEL_SIZE_ON_HORIZONTAL_2(17, false),
	NUMBER_OF_PIXELS_PER_ROW(18, false),
	NUMBER_OF_PIXELS_PER_COLUMN(19, false),

	NO_MORE_DATA(-1, true);


	final int id;
	final boolean noMoreData;

	private Class3X01Y192(final int id, final boolean noMoreData) {
		this.id = id;
		this.noMoreData = noMoreData;
	}

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public boolean isNoMoreData() {
		return noMoreData;
	}

	public static Class3X01Y192 getById(final int id) {
		for (Class3X01Y192 type : Class3X01Y192.values()) {
			if (id == type.getId().intValue()) {
				return type;
			}
		}
		return NO_MORE_DATA;
	}

}
