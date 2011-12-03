package com.weather;

import java.io.InputStream;

public class Weather {

	private static final int FINAL_STEP = (((byte)0x00  & 0xFF) << 24) + (((byte)0x37 & 0xFF) << 16)  + (((byte)0x37 & 0xFF) << 8) + (((byte)0x37 & 0xFF)<< 0);

	public static void main(final String[] args) {


		try {

			final InputStream is = Weather.class.getClassLoader().getResourceAsStream("COR110915203801.BPPI001");

			//			final byte[] oneB = new byte[1];
			//			final StringBuffer tsb = new StringBuffer();
			//			while(is.read(oneB, 0, 1) != -1) {
			//				tsb.append( getHex(oneB) );
			//			}
			//			final BufferedWriter out = new BufferedWriter(new FileWriter("resources/salida.txt"));
			//			out.write(tsb.toString());
			//			out.close();
			//			System.exit(1);

			boolean section = false;

			int step = 8;
			byte[] data = new byte[step];
			int readedBytes = 0;

			while(is.read(data, 0, step) != -1) {
				if(section) {
					section = false;

					step = (((byte)0x00  & 0xFF) << 24) + ((data[0] & 0xFF) << 16)  + ((data[1] & 0xFF) << 8) + ((data[2] & 0xFF)<< 0);

					step = step == FINAL_STEP ? 1 : step-3;

				} else {
					section = true;
					step = 3;
				}
				final StringBuffer sb = new StringBuffer();
				for(final byte b : data) {
					readedBytes++;
					if (readedBytes < 4) {
						sb.append( (char)b);
					} else if (readedBytes == 4) {
						sb.append( (char)b).append( "\n");
					}
				}
				sb.append( getHex(data) );
				System.out.println("--> " + sb);
				System.out.println("Readed bytes--> " + readedBytes + " next step: " + step);

				data = new byte[step];
			}
			System.out.println("Final Readed bytes--> " + readedBytes);

		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	public static int unsignedByteToInt(final byte b) {
		return b & 0xFF;
	}

	static final String HEXES = "0123456789ABCDEF";
	public static String getHex( final byte [] raw ) {
		if ( raw == null ) {
			return null;
		}
		final StringBuilder hex = new StringBuilder( 2 * raw.length );
		for ( final byte b : raw ) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4))
			.append(HEXES.charAt((b & 0x0F))).append(" ");
		}
		return hex.toString();
	}

}
