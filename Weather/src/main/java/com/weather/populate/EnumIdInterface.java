package com.weather.populate;

public interface EnumIdInterface<K> {

	K getId();
	boolean isNoMoreData();
	String name();

}
