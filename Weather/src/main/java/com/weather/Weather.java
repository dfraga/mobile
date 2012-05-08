package com.weather;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.android.utils.ColorUtils;
import com.android.weather.WeatherProcessListener;
import com.weather.populate.Class3X01Y192;
import com.weather.populate.LabelData;
import com.weather.populate.Populator;

public class Weather {

	private static final int FINAL_STEP = (((byte) 0x00 & 0xFF) << 24) + (((byte) 0x37 & 0xFF) << 16) + (((byte) 0x37 & 0xFF) << 8) + (((byte) 0x37 & 0xFF) << 0);

	private static int BUFR_SECTION = 0;

	private static boolean imagenFlag = false;
	private static boolean generalDataFlag = false;

	private static Bitmap bitMap;
	@Deprecated
	private static int[][] imagen;
	private static int rows;
	private static int columns;

	public static final Properties props = new Properties();

	public static void process(final File inputFile, final WeatherProcessListener listener) {


		final long initTime = System.currentTimeMillis();
		try {
			// Cargamos properties
			final InputStream porpertiesIs = Weather.class.getClassLoader().getResourceAsStream("sizes2.properties");
			Weather.props.load(porpertiesIs);

			// Cargamos fichero de datos
			final String fileName = inputFile.getName();
			final InputStream is = new FileInputStream(inputFile);
			Toast.makeText(listener.getContext(), "PROCESS PPI " + fileName, Toast.LENGTH_LONG).show();

			boolean section = false;

			int step = 8;
			byte[] data = new byte[step];
			List<Label> labels = new ArrayList<Label>();
			boolean optionalPresence = false;
			while (is.read(data, 0, step) != -1) {
				if (section) {
					section = false;

					step = Weather.int3(data);

					step = step == Weather.FINAL_STEP ? 1 : step - 3;
					Weather.BUFR_SECTION++;

				} else {
					if (Weather.BUFR_SECTION == SectionType.OPTIONAL_SECTION.getId() && !optionalPresence) {
						Weather.BUFR_SECTION++;
					}
					section = true;
					step = 3;
				}

				final StringBuffer sb = new StringBuffer();

				if (section) {
					if (Weather.BUFR_SECTION == SectionType.INDICATOR_SECTION.getId()) {
						ByteBuffer bb = ByteBuffer.wrap(data);
						byte[] dst = new byte[4];
						bb.get(dst, 0, 4);
						sb.append("\n\t· ").append(Weather.getChars(dst));

						dst = new byte[3];
						bb.get(dst, 0, 3);
						int totalLength = Weather.int3(dst);
						byte bufrVersion = bb.get();
						sb.append("\n\t· totalLength ").append(totalLength).append("\n\t· bufrVersion ").append(bufrVersion);

					} else if (Weather.BUFR_SECTION == SectionType.IDENTIFICATION_SECTION.getId()) {
						ByteBuffer bb = ByteBuffer.wrap(data);
						// Bufr master table (zero if standard WMO FM 94-IX BUFR
						// tables are used)
						byte masterTableNumber = bb.get();
						// Centro y subcentro a (Byte.MAX_VALUE -
						// Byte.MIN_VALUE) = 255 para standard tables
						short centre = bb.getShort();
						short subCentre = bb.getShort();
						byte updateSequence = bb.get();
						byte optionalSection = bb.get();
						byte dataCategoryTableA = bb.get();
						byte internationalDataSubCategory = bb.get();
						byte localSubCategory = bb.get();
						byte masterTableVersion = bb.get();
						byte localTableVersion = bb.get();
						short year = bb.getShort();
						byte month = bb.get();
						byte day = bb.get();
						byte hour = bb.get();
						byte minute = bb.get();
						byte second = bb.get();

						if (optionalSection != (byte) 0) {
							optionalPresence = true;
						}
						sb.append("\n\t· masterTableNumber ").append(masterTableNumber).append("\n\t· centre ").append(centre).append("\n\t· subCentre ").append(subCentre)
						.append("\n\t· updateSequence ").append(updateSequence).append("\n\t· optionalSection ").append(optionalSection).append("\n\t· dataCategoryTableA ")
						.append(dataCategoryTableA).append("\n\t· internationalDataSubCategory ").append(internationalDataSubCategory).append("\n\t· localSubCategory ")
						.append(localSubCategory).append("\n\t· masterTableVersion ").append(masterTableVersion).append("\n\t· localTableVersion ").append(localTableVersion)
						.append("\n\t· year ").append(year).append("\n\t· month ").append(month).append("\n\t· day ").append(day).append("\n\t· hour ").append(hour)
						.append("\n\t· minute ").append(minute).append("\n\t· second ").append(second);

					} else if (Weather.BUFR_SECTION == SectionType.LABELS_SECTION.getId()) {
						ByteBuffer bb = ByteBuffer.wrap(data);
						// reserved 0
						bb.get();
						short dataSubsets = bb.getShort();
						byte dataType = bb.get();
						byte observedDataMask = (byte) 0x80;
						byte compressedDataMask = (byte) 0x40;
						boolean observed = (dataType & observedDataMask) == observedDataMask;
						boolean compressed = (dataType & compressedDataMask) == compressedDataMask;
						sb.append("\n\t· dataSubsets ").append(dataSubsets).append("\n\t· observed ").append(observed).append("\n\t· compressed ").append(compressed).append("\n");

						while (bb.position() < bb.limit() - 1) {
							Weather.getLabel(bb.get(), bb.get(), labels, 1);
						}
						for (Label label : labels) {
							sb.append("\t").append(label).append("\n");
						}
					} else if (Weather.BUFR_SECTION == SectionType.OPTIONAL_SECTION.getId()) {
						sb.append("\n\t").append(Weather.getHex(data));
					} else if (Weather.BUFR_SECTION == SectionType.DATA_SECTION.getId()) {

						// bloque pruebas
						// Log.d(Weather.class.getSimpleName(),"DataBitSet:" +
						// Weather.formattedBitSet(data));

						// Primer byte reservado --> index = 8
						int index = 8;
						final Label[] arrayLabels = labels.toArray(new Label[0]);
						index = Weather.processLabels(index, data, arrayLabels, 1, 1);
						// bloque pruebas
						sb.append("\n\t Interpretados:" + index + " Bits = " + ((index / 8) + (index % 8 == 0 ? 0 : 1)) + " BYTES ### Data[" + data.length + "]:\t").append(Weather.getHex(data));
					} else if (Weather.BUFR_SECTION == SectionType.END_SECTION.getId()) {
						try {
							BMP bmp = new BMP();
							bmp.saveBMP("Salida/" + fileName + ".bmp", Weather.imagen);

							/* TODO almacenar o aprovechar decentemente los datos de Weather.imageGeneralData */
							(new OutputStreamWriter(new FileOutputStream(new File("Salida", fileName + ".txt")))).append(fileName).append("\n").append(Weather.imageGeneralData.toString()).close();

							Log.d("Resultado","resultado de imagen:" + Weather.bitMap.getWidth() + "*" + Weather.bitMap.getHeight());

							listener.openImage(Weather.bitMap);
							Toast.makeText(listener.getContext(), "Saved file: " + fileName+".png", Toast.LENGTH_LONG).show();
							FileOutputStream out = new FileOutputStream(fileName+".png");
							Weather.bitMap.compress(Bitmap.CompressFormat.PNG, 90, out);
							out.flush();
							out.close();
						} catch (Throwable e) {
							listener.processException(e);
						}
					}
				} else {
					// Nada sb.append(Weather.getHex(data));
				}

				if (sb.length() > 0) {
					Log.d(Weather.class.getSimpleName(),"@@[" + SectionType.values()[Weather.BUFR_SECTION] + "]" + sb + "\n\n");
				}

				data = new byte[step];

			}
			final String finalMsg = "Fin de proceso en: " + (System.currentTimeMillis() - initTime) + " ms";
			Log.d(Weather.class.getSimpleName(),finalMsg);
			//System.out.println(finalMsg);

		} catch (final Exception e) {
			listener.processException(e);
			e.printStackTrace();
		}

	}

	private static int pixelIndex = 0;
	private static int dataRepetition = 1;
	private static Populator<Class3X01Y192> imageGeneralData;

	private static int processLabels(int index, final byte[] data, final Label[] arrayLabels, final int repeats, final int level) {
		String levelSt = "";
		for (int n = 0; n < level; n++) {
			levelSt += "\t";
		}
		for (int n = 0; n < repeats; n++) {
			Log.d(Weather.class.getSimpleName(),levelSt + "## Nº" + (n + 1));

			for (int arrayIndex = 0; arrayIndex < arrayLabels.length; arrayIndex++) {
				Label label = arrayLabels[arrayIndex];
				Log.d(Weather.class.getSimpleName(),levelSt + label + " SIZE:" + label.size + " SCALE:" + label.scale);

				if (label.labelPropKey.equals("0.30.21")) {
					Weather.rows = Weather.bitSetToInt(data, index, label.size, label.scale, label.referenceValue);
				}
				if (label.labelPropKey.equals("0.30.22")) {
					Weather.columns = Weather.bitSetToInt(data, index, label.size, label.scale, label.referenceValue);
				}
				if (label.labelPropKey.equals("3.21.193")) {
					Weather.bitMap = Bitmap.createBitmap(Weather.columns, Weather.rows, Bitmap.Config.ARGB_8888);
					Weather.imagen = new int[Weather.rows][Weather.columns];
					Weather.imagenFlag = true;

				}
				if (label.labelPropKey.equals("3.1.192")) {
					/* clase para datos de fecha /latitud / longitud.... */
					Weather.imageGeneralData = new Populator<Class3X01Y192>();
					Weather.generalDataFlag = true;
				}

				if (label.type == LabelType.DESCRIPTOR) {
					// escala asociada a etiquetas: 9087 con escala 2 representa el valor 90.87
					Number numberData = label.scale == 0 ? (label.size < 33 ? Weather.bitSetToInt(data, index, label.size, label.scale, label.referenceValue) : Weather.bitSetToLong(data, index, label.size, label.scale, label.referenceValue)) : (Weather.bitSetToDouble(data, index, label.size, label.scale, label.referenceValue));
					Log.d(Weather.class.getSimpleName(),levelSt
							+ "\t" + numberData + (Weather.isUnknownValue(data, index, label.size) ? "\t# NO DATA #" : ""));

					if (Weather.generalDataFlag) {
						final Class3X01Y192 lastOffer = Weather.imageGeneralData.getLastOffer();
						final Class3X01Y192 current = Class3X01Y192.getById(lastOffer==null ? 0 :lastOffer.getId()+1);
						if (current.isNoMoreData()) {
							Weather.generalDataFlag = false;
						} else {
							LabelData labelData = new LabelData(label, numberData);
							Weather.imageGeneralData.offer(current, labelData);
						}

					}
					if (Weather.imagenFlag) {
						if (label.labelPropKey.equals("0.31.12")) {
							Weather.dataRepetition = Weather.bitSetToInt(data, index, label.size, label.scale, label.referenceValue);
						}
						if (label.labelPropKey.equals("0.30.2")) {
							for(int i = 0; i< Weather.dataRepetition; i++){
								int pixelData = Weather.bitSetToInt(data, index, label.size, label.scale, label.referenceValue);

								int alpha = 255;
								int color = ColorUtils.getAlphaColor(alpha, ColorUtils.getHuePhasedColor(Color.CYAN, ((float)pixelData)/((float)3)));
								if(Weather.isUnknownValue(data, index, label.size)) {
									//valor translucido/sombreado
									alpha = 125;
									color = Color.BLACK;
								} else if (pixelData < 0) {
									alpha = 125 + pixelData;
								}

								int row = Weather.pixelIndex / Weather.columns;
								int col = Weather.pixelIndex % Weather.columns;
								//La imagen se describe de abajo-izquierda a arriba-derecha
								Weather.imagen[Weather.rows -1 - row][col] = pixelData;
								Weather.bitMap.setPixel(Weather.rows -1 - row, col, color);
								Weather.pixelIndex++;
							}
							Weather.dataRepetition = 1;
						}
					}


				}

				index += label.size;

				if (label.type == LabelType.MULTIPLE) {
					// LabelType.MULTIPLE; 1.Y.0 --> la siguiente etiqueta es el
					// contador de repeticiones, las Y-siguientes la secuencia a
					// repetir
					arrayIndex++;
					Label countLabel = arrayLabels[arrayIndex];
					Label[] labelSequence = new Label[label.x];

					StringBuffer sbm = new StringBuffer(levelSt + "@@ MULTIPLE:"
							// + " index de labels:" + arrayIndex
							+ " Contador en " + countLabel + " secuencia --> ");

					int repIndex = 0;
					arrayIndex++;
					for (int i = arrayIndex; i < arrayIndex + label.x; i++) {
						sbm.append(arrayLabels[i]).append(" | ");
						labelSequence[repIndex] = arrayLabels[i];
						repIndex++;
					}
					Integer count = Weather.bitSetToInt(data, index, countLabel.size, countLabel.scale, countLabel.referenceValue);
					index += countLabel.size;
					arrayIndex += label.x - 1;
					sbm.append("\t#Iteraciones: " + count);

					if (Weather.imagenFlag) {
						Log.d(Weather.class.getSimpleName(),"Multiple :" + countLabel.labelPropKey + " iteracciones:" + count);
					}

					Log.d(Weather.class.getSimpleName(),sbm.toString());
					index = Weather.processLabels(index, data, labelSequence, count, level + 1);
				}

			}
		}
		return index;
	}
	private static void getLabel(final byte b0, final byte b1, final List<Label> labels, final int nTimes) {
		final Label currentLabel = new Label(new byte[]{b0, b1});
		Weather.getLabel(currentLabel.type.id, currentLabel.x, currentLabel.y, labels, nTimes);
	}

	private static void getLabel(final int labelType, final int x, final int y, final List<Label> labels, final int nTimes) {
		Label currentLabel = new Label(labelType, x, y);
		for (int k = 0; k < nTimes; k++) {
			labels.add(currentLabel);

			if (currentLabel.type == LabelType.SEQUENCE_DESCRIPTOR_TABLE_D) {
				Weather.getSequenceLabel(currentLabel, labels, 1);
			}
			// LabelType.MULTIPLE; realmente, representan una estructura, la
			// repeticion es en tiempo de interpretacion de datos *ver
			// Info_desarrollos/ProtocoloBUFR/bufr_sw_desc.pdf
		}
	}

	private static void getSequenceLabel(final Label sequenceLabel, final List<Label> labels, final int nTimes) {
		for (int k = 0; k < nTimes; k++) {
			if (Weather.props.containsKey(sequenceLabel.labelPropKey)) {
				final String sequence = Weather.props.getProperty(sequenceLabel.labelPropKey);
				final String[] subLabels = sequence.split(",");
				for (final String subLabel : subLabels) {
					final String[] labelKey = subLabel.split(":")[0].split("\\.");
					final int instances = subLabel.contains(":") ? Integer.valueOf(subLabel.split(":")[1]) : 1;

					Weather.getLabel(Integer.valueOf(labelKey[0]), Integer.valueOf(labelKey[1]), Integer.valueOf(labelKey[2]), labels, instances);
				}
			} else {
				Log.d(Weather.class.getSimpleName(),"@@@ NO EXISTE PROPERTY " + sequenceLabel.labelPropKey);
			}
		}
	}

	private static int int3(final byte[] data) {
		return data.length < 3 ? Integer.MIN_VALUE : (((byte) 0x00 & 0xFF) << 24) + ((data[0] & 0xFF) << 16) + ((data[1] & 0xFF) << 8) + ((data[2] & 0xFF) << 0);
	}

	public static boolean getBit(final byte[] bitSet, final int i) {
		return (bitSet[i / 8] & (1 << (8 - (i % 8) - 1))) != 0;
	}

	public static boolean isUnknownValue(final byte[] bitSet, final int beginBit, final int offSet) {
		for (int i = 0; i < offSet; i++) {
			if (!Weather.getBit(bitSet, beginBit + i)) {
				return false;
			}
		}
		return true;
	}

	public static int bitSetToInt(final byte[] bitSet, final int beginBit, final int offSet, final int scale, final int referenceValue) {
		int bitInteger = 0;
		for (int i = 0; i < offSet; i++) {
			bitInteger += Weather.getBit(bitSet, beginBit + i) ? (1 << (offSet - (i + 1))) : 0;
		}
		bitInteger += referenceValue;
		return bitInteger / (int)Math.pow(10, scale);//(scale == 0 ? 1 : (scale * 10));
	}

	public static long bitSetToLong(final byte[] bitSet, final int beginBit, final int offSet, final int scale, final int referenceValue) {
		long bitLong = 0;
		for (int i = 0; i < offSet; i++) {
			bitLong += Weather.getBit(bitSet, beginBit + i) ? (1L << (offSet - (i + 1))) : 0L;
		}
		bitLong += referenceValue;
		return bitLong / (long)Math.pow(10, scale);//(scale == 0 ? 1l : (scale * 10));
	}

	public static double bitSetToDouble(final byte[] bitSet, final int beginBit, final int offSet, final int scale, final int referenceValue) {
		double bitLong = 0;
		for (int i = 0; i < offSet; i++) {
			bitLong += Weather.getBit(bitSet, beginBit + i) ? (1L << (offSet - (i + 1))) : 0L;
		}
		bitLong += referenceValue;
		return bitLong / Math.pow(10, scale);//(scale == 0 ? 1 : (scale * 10));
	}

	@SuppressWarnings("unused")
	private static String printBitSet(final byte[] bitSet, final int beginBit, final int offSet) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < offSet; i++) {
			sb.append((Weather.getBit(bitSet, beginBit + i)) ? "1" : "0");
		}
		return sb.toString();
	}

	@SuppressWarnings("unused")
	private static String formattedBitSet(final byte[] bitSet) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bitSet.length * 8; i++) {
			sb.append((i % 8 == 0) ? "\n\t" : "");
			sb.append((Weather.getBit(bitSet, i)) ? "1" : "0");
		}
		return sb.toString();
	}

	private static final String HEXES = "0123456789ABCDEF";

	private static String getHex(final byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(Weather.HEXES.charAt((b & 0xF0) >> 4)).append(Weather.HEXES.charAt((b & 0x0F))).append(" ");
		}
		return hex.toString();
	}

	private static String getChars(final byte[] raw) {
		StringBuffer sb = new StringBuffer();
		for (final byte b : raw) {
			sb.append((char) b);
		}
		return sb.toString();
	}

}
