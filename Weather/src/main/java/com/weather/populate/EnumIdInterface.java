package com.weather.populate;

import java.io.Serializable;

public interface EnumIdInterface<K> extends Serializable {

	K getId();
	boolean isNoMoreData();
	String name();

}
