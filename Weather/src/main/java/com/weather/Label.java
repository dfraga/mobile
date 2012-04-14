package com.weather;

public class Label {

	final LabelType type;
	final int x;
	final int y;

	final int size;

	Label(final byte[] label) {
		// XXX if (label.length != 2) error
		type = LabelType.getLabelType(label[0]);

		x = (((byte) 0x00 & 0xFF) << 24) + (((byte) 0x00 & 0xFF) << 16)
				+ (((byte) 0x00 & 0xFF) << 8) + ((label[0] & 0x3F) << 0);
		y = (((byte) 0x00 & 0xFF) << 24) + (((byte) 0x00 & 0xFF) << 16)
				+ (((byte) 0x00 & 0xFF) << 8) + ((label[1] & 0xFF) << 0);

		size = Integer.parseInt(Weather.props.getProperty(LabelType.getId(type)
				+ "." + x + "." + y));
	}

	@Override
	public String toString() {
		return type.name() + " , " + x + " , " + y;
	}

}
