//package com.weather;
//
//import java.io.File;
//
//import java.io.FileNotFoundException;
//
//import java.io.FileOutputStream;
//
//import java.io.IOException;
//
//public class BMP {
//
//	private final static int BMP_CODE = 19778;
//
//	byte[] bytes;
//
//	public void saveBMP(String filename, int[][] rgbValues) {
//
//		try {
//
//			FileOutputStream fos = new FileOutputStream(new File(filename));
//
//			bytes = new byte[54 + 3 * rgbValues.length * rgbValues[0].length + getPadding(rgbValues[0].length) * rgbValues.length];
//
//			saveFileHeader();
//
//			saveInfoHeader(rgbValues.length, rgbValues[0].length);
//
//			saveRgbQuad();
//
//			saveBitmapData(rgbValues);
//
//			fos.write(bytes);
//
//			fos.close();
//
//		} catch (FileNotFoundException e) {
//
//		} catch (IOException e) {
//
//		}
//
//	}
//
//	private void saveFileHeader() {
//
//		byte[] a = intToByteCouple(BMP_CODE);
//
//		bytes[0] = a[1];
//
//		bytes[1] = a[0];
//
//		a = intToFourBytes(bytes.length);
//
//		bytes[5] = a[0];
//
//		bytes[4] = a[1];
//
//		bytes[3] = a[2];
//
//		bytes[2] = a[3];
//
//		// data offset
//
//		bytes[10] = 54;
//
//	}
//
//	private void saveInfoHeader(int height, int width) {
//
//		bytes[14] = 40;
//
//		byte[] a = intToFourBytes(width);
//
//		bytes[22] = a[3];
//
//		bytes[23] = a[2];
//
//		bytes[24] = a[1];
//
//		bytes[25] = a[0];
//
//		a = intToFourBytes(height);
//
//		bytes[18] = a[3];
//
//		bytes[19] = a[2];
//
//		bytes[20] = a[1];
//
//		bytes[21] = a[0];
//
//		bytes[26] = 1;
//
//		bytes[28] = 24;
//
//	}
//
//	private void saveRgbQuad() {
//
//	}
//
//	private void saveBitmapData(int[][] rgbValues) {
//
//		int i;
//
//		for (i = 0; i < rgbValues.length; i++) {
//
//			writeLine(i, rgbValues);
//
//		}
//
//	}
//
//	private void writeLine(int row, int[][] rgbValues) {
//
//		final int offset = 54;
//
//		final int rowLength = rgbValues[row].length;
//
//		final int padding = getPadding(rgbValues[0].length);
//
//		int i;
//
//		for (i = 0; i < rowLength; i++) {
//
//			int rgb = rgbValues[row][i];
//
//			int temp = offset + 3 * (i + rowLength * row) + row * padding;
//
//			bytes[temp] = (byte) (rgb >> 16);
//
//			bytes[temp + 1] = (byte) (rgb >> 8);
//
//			bytes[temp + 2] = (byte) rgb;
//
//		}
//
//		i--;
//
//		int temp = offset + 3 * (i + rowLength * row) + row * padding + 3;
//
//		for (int j = 0; j < padding; j++)
//
//			bytes[temp + j] = 0;
//
//	}
//
//	private byte[] intToByteCouple(int x) {
//
//		byte[] array = new byte[2];
//
//		array[1] = (byte) x;
//
//		array[0] = (byte) (x >> 8);
//
//		return array;
//
//	}
//
//	private byte[] intToFourBytes(int x) {
//
//		byte[] array = new byte[4];
//
//		array[3] = (byte) x;
//
//		array[2] = (byte) (x >> 8);
//
//		array[1] = (byte) (x >> 16);
//
//		array[0] = (byte) (x >> 24);
//
//		return array;
//
//	}
//
//	private int getPadding(int rowLength) {
//
//		int padding = (3 * rowLength) % 4;
//
//		if (padding != 0)
//
//			padding = 4 - padding;
//
//		return padding;
//
//	}
//
//}