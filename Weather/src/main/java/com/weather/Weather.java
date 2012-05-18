package com.weather;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import com.android.utils.ColorUtils;
import com.android.weather.WeatherProcessListener;
import com.weather.populate.Class3X01Y192;
import com.weather.populate.LabelData;
import com.weather.populate.Populator;

public class Weather {

	public static String WORKING_DIRECTORY = "SalidaWeather";
	public static final Properties props = new Properties();

	private static boolean DEBUG_LOG = false;
	private static boolean DRAW_UNKNOWN = false;
	private static final int FINAL_STEP = (((byte) 0x00 & 0xFF) << 24) + (((byte) 0x37 & 0xFF) << 16) + (((byte) 0x37 & 0xFF) << 8) + (((byte) 0x37 & 0xFF) << 0);

	private static int MIN_PIXEL_VALUE = 25;
	private int minValue = 0;
	private int maxValue = 0;

	private boolean imagenFlag = false;
	private boolean generalDataFlag = false;

	private final WeatherProcessListener listener;
	private Bitmap bitMap;
	private int rows;
	private int columns;
	private int totalPixels;
	private int bufrSection;

	private int pixelIndex = 0;
	private int dataRepetition = 1;

	private Populator<Class3X01Y192> imageGeneralData;

	public Weather(final WeatherProcessListener listener) {
		this.listener = listener;
	}

	public void process(final File inputFile) {


		listener.setPercentageMessage("Decodificando fichero...");
		final long initTime = System.currentTimeMillis();
		try {
			// Cargamos properties
			if(Weather.props == null || Weather.props.isEmpty()) {
				final InputStream porpertiesIs = Weather.class.getClassLoader().getResourceAsStream("sizes2.properties");
				Weather.props.load(porpertiesIs);
			}

			// Cargamos fichero de datos
			final String fileName = inputFile.getName();
			final InputStream is = new FileInputStream(inputFile);
			//Toast.makeText(listener.getContext(), "PROCESS PPI " + fileName, Toast.LENGTH_LONG).show();

			boolean section = false;

			int step = 8;
			byte[] data = new byte[step];
			List<Label> labels = new ArrayList<Label>();
			boolean optionalPresence = false;
			while (is.read(data, 0, step) != -1) {
				if (section) {
					section = false;

					step = this.int3(data);

					step = step == Weather.FINAL_STEP ? 1 : step - 3;
					bufrSection++;

				} else {
					if (bufrSection == SectionType.OPTIONAL_SECTION.getId() && !optionalPresence) {
						bufrSection++;
					}
					section = true;
					step = 3;
				}

				final StringBuffer sb = new StringBuffer();

				if (section) {
					if (bufrSection == SectionType.INDICATOR_SECTION.getId()) {
						ByteBuffer bb = ByteBuffer.wrap(data);
						byte[] dst = new byte[4];
						bb.get(dst, 0, 4);
						if(Weather.DEBUG_LOG) {
							sb.append("\n\t· ").append(this.getChars(dst));
						}

						dst = new byte[3];
						bb.get(dst, 0, 3);
						int totalLength = this.int3(dst);
						byte bufrVersion = bb.get();
						if(Weather.DEBUG_LOG) {
							sb.append("\n\t· totalLength ").append(totalLength).append("\n\t· bufrVersion ").append(bufrVersion);
						}

					} else if (bufrSection == SectionType.IDENTIFICATION_SECTION.getId()) {
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
						if(Weather.DEBUG_LOG) {
							sb.append("\n\t· masterTableNumber ").append(masterTableNumber).append("\n\t· centre ").append(centre).append("\n\t· subCentre ").append(subCentre)
							.append("\n\t· updateSequence ").append(updateSequence).append("\n\t· optionalSection ").append(optionalSection).append("\n\t· dataCategoryTableA ")
							.append(dataCategoryTableA).append("\n\t· internationalDataSubCategory ").append(internationalDataSubCategory).append("\n\t· localSubCategory ")
							.append(localSubCategory).append("\n\t· masterTableVersion ").append(masterTableVersion).append("\n\t· localTableVersion ").append(localTableVersion)
							.append("\n\t· year ").append(year).append("\n\t· month ").append(month).append("\n\t· day ").append(day).append("\n\t· hour ").append(hour)
							.append("\n\t· minute ").append(minute).append("\n\t· second ").append(second);
						}
					} else if (bufrSection == SectionType.LABELS_SECTION.getId()) {
						ByteBuffer bb = ByteBuffer.wrap(data);
						// reserved 0
						bb.get();
						short dataSubsets = bb.getShort();
						byte dataType = bb.get();
						byte observedDataMask = (byte) 0x80;
						byte compressedDataMask = (byte) 0x40;
						boolean observed = (dataType & observedDataMask) == observedDataMask;
						boolean compressed = (dataType & compressedDataMask) == compressedDataMask;
						if(Weather.DEBUG_LOG) {
							sb.append("\n\t· dataSubsets ").append(dataSubsets).append("\n\t· observed ").append(observed).append("\n\t· compressed ").append(compressed).append("\n");
						}

						while (bb.position() < bb.limit() - 1) {
							this.getLabel(bb.get(), bb.get(), labels, 1);
						}
						for (Label label : labels) {
							if(Weather.DEBUG_LOG) {
								sb.append("\t").append(label).append("\n");
							}
						}
					} else if (bufrSection == SectionType.OPTIONAL_SECTION.getId()) {
						if(Weather.DEBUG_LOG) {
							sb.append("\n\t").append(this.getHex(data));
						}
					} else if (bufrSection == SectionType.DATA_SECTION.getId()) {

						// bloque pruebas
						// Log.d(Weather.class.getSimpleName(),"DataBitSet:" +
						// this.formattedBitSet(data));

						// Primer byte reservado --> index = 8
						int index = 8;
						final Label[] arrayLabels = labels.toArray(new Label[0]);
						index = this.processLabels(index, data, arrayLabels, 1, 1);
						// bloque pruebas
						if(Weather.DEBUG_LOG) {
							sb.append("\n\t Interpretados:" + index + " Bits = " + ((index / 8) + (index % 8 == 0 ? 0 : 1)) + " BYTES ### Data[" + data.length + "]:\t").append(this.getHex(data));
						}
					} else if (bufrSection == SectionType.END_SECTION.getId()) {
						File outPng = null;
						try {
							File ruta_sd = Environment.getExternalStorageDirectory();
							File localFolder = new File(ruta_sd.getAbsolutePath(), Weather.WORKING_DIRECTORY);

							//							(new OutputStreamWriter(new FileOutputStream(new File(localFolder, fileName + ".txt")))).append(fileName).append("\n").append(this.imageGeneralData.toString()).close();

							if(Weather.DEBUG_LOG) {
								Log.d("Resultado","resultado de imagen:" + this.bitMap.getWidth() + "*" + this.bitMap.getHeight());
							}
							outPng = new File(localFolder, fileName+".png");
							outPng.deleteOnExit();
							FileOutputStream out = new FileOutputStream(outPng);
							this.bitMap.compress(Bitmap.CompressFormat.PNG, 90, out);
							out.flush();
							out.close();

							listener.setProcessedImage(this.bitMap, this.imageGeneralData);
						} catch (Throwable e) {
							listener.processException(e);
						} finally {
							if(outPng!= null && outPng.exists()) {
								outPng.delete();
							}
						}
					}
				} else {
					// Nada sb.append(this.getHex(data));
				}

				if (sb.length() > 0) {
					if(Weather.DEBUG_LOG) {
						Log.d(Weather.class.getSimpleName(),"@@[" + SectionType.values()[bufrSection] + "]" + sb + "\n\n");
						Log.d(Weather.class.getSimpleName(),"min:max @@[" + this.minValue + " : " + this.maxValue + "]");


					}
				}

				data = new byte[step];

			}
			final String finalMsg = "Fin de proceso en: " + (System.currentTimeMillis() - initTime) + " ms";
			if(Weather.DEBUG_LOG) {
				Log.d(Weather.class.getSimpleName(),finalMsg);
			}
			//System.out.println(finalMsg);

		} catch (final Exception e) {
			listener.processException(e);
			e.printStackTrace();
		}

	}

	private int processLabels(int index, final byte[] data, final Label[] arrayLabels, final int repeats, final int level) {
		String levelSt = "";
		for (int n = 0; n < level; n++) {
			levelSt += "\t";
		}
		for (int n = 0; n < repeats; n++) {
			if(Weather.DEBUG_LOG) {
				Log.d(Weather.class.getSimpleName(),levelSt + "## Nº" + (n + 1));
			}

			for (int arrayIndex = 0; arrayIndex < arrayLabels.length; arrayIndex++) {
				Label label = arrayLabels[arrayIndex];
				if(Weather.DEBUG_LOG) {
					Log.d(Weather.class.getSimpleName(),levelSt + label + " SIZE:" + label.size + " SCALE:" + label.scale);
				}

				if (label.labelPropKey.equals("0.30.21")) {
					this.rows = this.bitSetToInt(data, index, label.size, label.scale, label.referenceValue);
				}
				if (label.labelPropKey.equals("0.30.22")) {
					this.columns = this.bitSetToInt(data, index, label.size, label.scale, label.referenceValue);
				}
				if (label.labelPropKey.equals("3.21.193")) {
					this.bitMap = Bitmap.createBitmap(this.columns, this.rows, Bitmap.Config.ARGB_8888);
					//					this.imagen = new int[this.rows][this.columns];
					this.imagenFlag = true;
					this.totalPixels = this.columns * this.rows;

					this.listener.setPercentageMessage("Decodificando imagen...");
				}
				if (label.labelPropKey.equals("3.1.192")) {
					/* clase para datos de fecha /latitud / longitud.... */
					this.imageGeneralData = new Populator<Class3X01Y192>();
					this.generalDataFlag = true;
				}

				if (label.type == LabelType.DESCRIPTOR) {
					// escala asociada a etiquetas: 9087 con escala 2 representa el valor 90.87
					Number numberData = label.scale == 0 ? (label.size < 33 ? this.bitSetToInt(data, index, label.size, label.scale, label.referenceValue) : this.bitSetToLong(data, index, label.size, label.scale, label.referenceValue)) : (this.bitSetToDouble(data, index, label.size, label.scale, label.referenceValue));
					if(Weather.DEBUG_LOG) {
						Log.d(Weather.class.getSimpleName(),levelSt
								+ "\t" + numberData + (this.isUnknownValue(data, index, label.size) ? "\t# NO DATA #" : ""));
					}

					if (this.generalDataFlag) {
						final Class3X01Y192 lastOffer = this.imageGeneralData.getLastOffer();
						final Class3X01Y192 current = Class3X01Y192.getById(lastOffer==null ? 0 :lastOffer.getId().intValue()+1);
						if (current.isNoMoreData()) {
							this.generalDataFlag = false;
						} else {
							LabelData labelData = new LabelData(label, numberData);
							this.imageGeneralData.offer(current, labelData);
						}

					}
					if (this.imagenFlag) {
						if (label.labelPropKey.equals("0.31.12")) {
							this.dataRepetition = this.bitSetToInt(data, index, label.size, label.scale, label.referenceValue);
						}
						if (label.labelPropKey.equals("0.30.2")) {
							for(int i = 0; i< this.dataRepetition; i++){
								int pixelData = this.bitSetToInt(data, index, label.size, label.scale, label.referenceValue);

								int alpha = 128;
								int baseColor = Color.GREEN;
								if(this.isUnknownValue(data, index, label.size)) {
									//valor translucido/sombreado
									alpha = Weather.DRAW_UNKNOWN ? 100:0;
									baseColor = Color.GRAY;
								} else if (pixelData < Weather.MIN_PIXEL_VALUE) {
									//XXX rango en decibelios de -31 a 59, menor a 15 despreciable
									alpha = 0;
								}
								int color = ColorUtils.getAlphaColor(alpha, ColorUtils.getHuePhasedColor(baseColor, (pixelData * 3)));

								if(this.minValue > pixelData) {
									this.minValue = pixelData;
								}
								if(pixelData > this.maxValue) {
									this.maxValue = pixelData;
								}

								int row = this.pixelIndex / this.columns;
								int col = this.pixelIndex % this.columns;
								//La imagen se describe de abajo-izquierda a arriba-derecha
								//								this.imagen[this.rows -1 - row][col] = pixelData;
								this.bitMap.setPixel(col, row, color);
								this.pixelIndex++;

								this.listener.setPercentageProgression(this.pixelIndex, this.totalPixels);
							}
							this.dataRepetition = 1;
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

					StringBuffer sbm = new StringBuffer();

					if(Weather.DEBUG_LOG) {
						sbm.append(levelSt + "@@ MULTIPLE:"
								// + " index de labels:" + arrayIndex
								+ " Contador en " + countLabel + " secuencia --> ");
					}

					int repIndex = 0;
					arrayIndex++;
					for (int i = arrayIndex; i < arrayIndex + label.x; i++) {
						if(Weather.DEBUG_LOG) {
							sbm.append(arrayLabels[i]).append(" | ");
						}
						labelSequence[repIndex] = arrayLabels[i];
						repIndex++;
					}
					Integer count = this.bitSetToInt(data, index, countLabel.size, countLabel.scale, countLabel.referenceValue);
					index += countLabel.size;
					arrayIndex += label.x - 1;
					if(Weather.DEBUG_LOG) {
						sbm.append("\t#Iteraciones: " + count);
					}

					if (this.imagenFlag) {
						if(Weather.DEBUG_LOG) {
							Log.d(Weather.class.getSimpleName(),"Multiple :" + countLabel.labelPropKey + " iteracciones:" + count);
						}
					}

					if(Weather.DEBUG_LOG) {
						Log.d(Weather.class.getSimpleName(),sbm.toString());
					}
					index = this.processLabels(index, data, labelSequence, count, level + 1);
				}

			}
		}
		return index;
	}

	private void getLabel(final byte b0, final byte b1, final List<Label> labels, final int nTimes) {
		final Label currentLabel = new Label(new byte[]{b0, b1});
		this.getLabel(currentLabel.type.id, currentLabel.x, currentLabel.y, labels, nTimes);
	}

	private void getLabel(final int labelType, final int x, final int y, final List<Label> labels, final int nTimes) {
		Label currentLabel = new Label(labelType, x, y);
		for (int k = 0; k < nTimes; k++) {
			labels.add(currentLabel);

			if (currentLabel.type == LabelType.SEQUENCE_DESCRIPTOR_TABLE_D) {
				this.getSequenceLabel(currentLabel, labels, 1);
			}
			// LabelType.MULTIPLE; realmente, representan una estructura, la
			// repeticion es en tiempo de interpretacion de datos *ver
			// Info_desarrollos/ProtocoloBUFR/bufr_sw_desc.pdf
		}
	}

	private void getSequenceLabel(final Label sequenceLabel, final List<Label> labels, final int nTimes) {
		for (int k = 0; k < nTimes; k++) {
			if (Weather.props.containsKey(sequenceLabel.labelPropKey)) {
				final String sequence = Weather.props.getProperty(sequenceLabel.labelPropKey);
				final String[] subLabels = sequence.split(",");
				for (final String subLabel : subLabels) {
					final String[] labelKey = subLabel.split(":")[0].split("\\.");
					final int instances = subLabel.contains(":") ? Integer.valueOf(subLabel.split(":")[1]) : 1;

					this.getLabel(Integer.valueOf(labelKey[0]), Integer.valueOf(labelKey[1]), Integer.valueOf(labelKey[2]), labels, instances);
				}
			} else {
				Log.e(Weather.class.getSimpleName(),"@@@ NO EXISTE PROPERTY " + sequenceLabel.labelPropKey);
			}
		}
	}

	private int int3(final byte[] data) {
		return data.length < 3 ? Integer.MIN_VALUE : (((byte) 0x00 & 0xFF) << 24) + ((data[0] & 0xFF) << 16) + ((data[1] & 0xFF) << 8) + ((data[2] & 0xFF) << 0);
	}

	private boolean getBit(final byte[] bitSet, final int i) {
		return (bitSet[i / 8] & (1 << (8 - (i % 8) - 1))) != 0;
	}

	private boolean isUnknownValue(final byte[] bitSet, final int beginBit, final int offSet) {
		for (int i = 0; i < offSet; i++) {
			if (!this.getBit(bitSet, beginBit + i)) {
				return false;
			}
		}
		return true;
	}

	private int bitSetToInt(final byte[] bitSet, final int beginBit, final int offSet, final int scale, final int referenceValue) {
		int bitInteger = 0;
		for (int i = 0; i < offSet; i++) {
			bitInteger += this.getBit(bitSet, beginBit + i) ? (1 << (offSet - (i + 1))) : 0;
		}
		bitInteger += referenceValue;
		return bitInteger / (int)Math.pow(10, scale);//(scale == 0 ? 1 : (scale * 10));
	}

	private long bitSetToLong(final byte[] bitSet, final int beginBit, final int offSet, final int scale, final int referenceValue) {
		long bitLong = 0;
		for (int i = 0; i < offSet; i++) {
			bitLong += this.getBit(bitSet, beginBit + i) ? (1L << (offSet - (i + 1))) : 0L;
		}
		bitLong += referenceValue;
		return bitLong / (long)Math.pow(10, scale);//(scale == 0 ? 1l : (scale * 10));
	}

	private double bitSetToDouble(final byte[] bitSet, final int beginBit, final int offSet, final int scale, final int referenceValue) {
		double bitLong = 0;
		for (int i = 0; i < offSet; i++) {
			bitLong += this.getBit(bitSet, beginBit + i) ? (1L << (offSet - (i + 1))) : 0L;
		}
		bitLong += referenceValue;
		return bitLong / Math.pow(10, scale);//(scale == 0 ? 1 : (scale * 10));
	}

	@SuppressWarnings("unused")
	private String printBitSet(final byte[] bitSet, final int beginBit, final int offSet) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < offSet; i++) {
			sb.append((this.getBit(bitSet, beginBit + i)) ? "1" : "0");
		}
		return sb.toString();
	}

	@SuppressWarnings("unused")
	private String formattedBitSet(final byte[] bitSet) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bitSet.length * 8; i++) {
			sb.append((i % 8 == 0) ? "\n\t" : "");
			sb.append((this.getBit(bitSet, i)) ? "1" : "0");
		}
		return sb.toString();
	}

	private final String HEXES = "0123456789ABCDEF";

	private String getHex(final byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(this.HEXES.charAt((b & 0xF0) >> 4)).append(this.HEXES.charAt((b & 0x0F))).append(" ");
		}
		return hex.toString();
	}

	private String getChars(final byte[] raw) {
		StringBuffer sb = new StringBuffer();
		for (final byte b : raw) {
			sb.append((char) b);
		}
		return sb.toString();
	}

}
