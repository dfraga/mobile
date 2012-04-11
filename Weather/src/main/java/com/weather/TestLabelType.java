package com.weather;

public class TestLabelType {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("CD 09 " +	new Label(new byte[]{(byte)0xCD,(byte)0x09}));
		System.out.println("C1 01 " +	new Label(new byte[]{(byte)0xC1,(byte)0x01}));
		System.out.println("07 01 " +	new Label(new byte[]{(byte)0x07,(byte)0x01}));
		System.out.println("1D C7 " +	new Label(new byte[]{(byte)0x1D,(byte)0xC7}));
		System.out.println("21 03 " +	new Label(new byte[]{(byte)0x21,(byte)0x03}));
		System.out.println("D5 C1 " +	new Label(new byte[]{(byte)0xD5,(byte)0xC1}));
	}

}
