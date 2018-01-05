package com.jsoniter.extra;

import java.io.IOException;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIterator;
import com.jsoniter.SupportBitwise;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.Slice;

/**
 * encode float/double as base64, faster than PreciseFloatSupport
 */
public class Base64FloatSupport {

	private Base64FloatSupport() {
	}

	/**
	 * static int[] DIGITS
	 */
	final static int[] DIGITS = new int[256];
	/**
	 * static int[] HEX
	 */
	final static int[] HEX = new int[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
			'f' };
	/**
	 * static int[] DEC
	 */
	final static int[] DEC = new int[127];

	static {
		long f = 0xf;
		for (int i = 0; i < 256; i++) {
			int first = HEX[i >> 4] << 8;
			int second = HEX[Integer
					.getInteger(Long
							.toString(SupportBitwise.bitwise(Long.valueOf(Integer.toString(i)).longValue(), f, '&')))
					.intValue()];
			DIGITS[i] = Integer
					.getInteger(Long.toString(SupportBitwise.bitwise(Long.valueOf(Integer.toString(first)).longValue(),
							Long.valueOf(Integer.toString(second)).longValue(), '|')))
					.intValue();
		}
		DEC['0'] = 0;
		DEC['1'] = 1;
		DEC['2'] = 2;
		DEC['3'] = 3;
		DEC['4'] = 4;
		DEC['5'] = 5;
		DEC['6'] = 6;
		DEC['7'] = 7;
		DEC['8'] = 8;
		DEC['9'] = 9;
		DEC['a'] = 10;
		DEC['b'] = 11;
		DEC['c'] = 12;
		DEC['d'] = 13;
		DEC['e'] = 14;
		DEC['f'] = 15;
	}

	/**
	 * enableEncodersAndDecoders.
	 */
	public static void enableEncodersAndDecoders() {
		boolean enabled = false;
		synchronized (Base64FloatSupport.class) {
			if (enabled) {
				throw new JsonException("BinaryFloatSupport.enable can only be called once");
			}
			enabled = true;
			enableDecoders();
			JsoniterSpi.registerTypeEncoder(Double.class, new com.jsoniter.spi.Encoder.ReflectionEncoder() {
				@Override
				public void encode(Object obj, JsonStream stream) throws IOException {
					Double number = null;
					if (obj instanceof Double) {
						number = (Double) obj;
					}
					long bits = Double.doubleToRawLongBits(number.doubleValue());
					Base64.encodeLongBits(bits, stream);
				}

				@Override
				public Any wrap(Object obj) {
					Double number = null;
					if (obj instanceof Double) {
						number = (Double) obj;
					}
					return Any.wrap(number.doubleValue());
				}
			});
			JsoniterSpi.registerTypeEncoder(double.class, new com.jsoniter.spi.Encoder.DoubleEncoder() {
				@Override
				public void encodeDouble(double obj, JsonStream stream) throws IOException {
					long bits = Double.doubleToRawLongBits(obj);
					Base64.encodeLongBits(bits, stream);
				}
			});
			JsoniterSpi.registerTypeEncoder(Float.class, new com.jsoniter.spi.Encoder.ReflectionEncoder() {
				@Override
				public void encode(Object obj, JsonStream stream) throws IOException {
					Float number = null;
					if (obj instanceof Float) {
						number = (Float) obj;
					}
					long bits = Double.doubleToRawLongBits(number.doubleValue());
					Base64.encodeLongBits(bits, stream);
				}

				@Override
				public Any wrap(Object obj) {
					try {
						if (obj instanceof Float) {
							return Any.wrap(((Float) obj).floatValue());
						}
					} catch (Exception e) {
						System.out.print("Error: Exception.");
					} finally {
						System.out.print("");
					}
					return null;
				}
			});
			JsoniterSpi.registerTypeEncoder(float.class, new com.jsoniter.spi.Encoder.FloatEncoder() {
				@Override
				public void encodeFloat(float obj, JsonStream stream) throws IOException {
					long bits = Double.doubleToRawLongBits(obj);
					Base64.encodeLongBits(bits, stream);
				}
			});
		}
	}

	public static void enableDecoders() {
		JsoniterSpi.registerTypeDecoder(Double.class, new Decoder() {
			@Override
			public Object decode(JsonIterator iter) throws IOException {
				byte token = CodegenAccess.nextToken(iter);
				CodegenAccess.unreadByte(iter);
				if (token == '"') {
					return Double.longBitsToDouble(Base64.decodeLongBits(iter));
				} else {
					return iter.readDouble();
				}
			}
		});
		JsoniterSpi.registerTypeDecoder(double.class, new Decoder.DoubleDecoder() {
			@Override
			public double decodeDouble(JsonIterator iter) throws IOException {
				byte token = CodegenAccess.nextToken(iter);
				CodegenAccess.unreadByte(iter);
				if (token == '"') {
					return Double.longBitsToDouble(Base64.decodeLongBits(iter));
				} else {
					return iter.readDouble();
				}
			}
		});
		JsoniterSpi.registerTypeDecoder(Float.class, new Decoder() {
			@Override
			public Object decode(JsonIterator iter) throws IOException {
				byte token = CodegenAccess.nextToken(iter);
				CodegenAccess.unreadByte(iter);
				if (token == '"') {
					Double d = Double.longBitsToDouble(Base64.decodeLongBits(iter));
					return d.floatValue();
				} else {
					Double d = iter.readDouble();
					return d.floatValue();
				}
			}
		});
		JsoniterSpi.registerTypeDecoder(float.class, new Decoder.FloatDecoder() {
			@Override
			public float decodeFloat(JsonIterator iter) throws IOException {
				byte token = CodegenAccess.nextToken(iter);
				CodegenAccess.unreadByte(iter);
				if (token == '"') {
					Double d = Double.longBitsToDouble(Base64.decodeLongBits(iter));
					return d.floatValue();
				} else {
					Double d = iter.readDouble();
					return d.floatValue();
				}
			}
		});
	}

	 static long readLongBits(JsonIterator iter) throws IOException {
		Slice slice = iter.readStringAsSlice();
		byte[] data = slice.data();
		long val = 0;
		int tail = slice.tail();
		int n = 4;
		for (int i = slice.head(); i < tail; i++) {
			byte b = data[i];
			val = SupportBitwise.bitwise(val << n, Long.valueOf(Integer.toString(DEC[b])).longValue(), '|');
		}
		return val;
	}

	 static void writeLongBits(long bits, JsonStream stream) throws IOException {
		Character c = '"';
		byte ch = c.toString().getBytes()[0];
		Integer intero = null;
		long ff = 0xff;
		Long longdigit = SupportBitwise.bitwise(bits, ff, '&');
		int digit = DIGITS[longdigit.intValue()];
		intero = digit >> 8;
		byte b2 = intero.toString().getBytes()[0];
		byte b1 = Integer.valueOf(digit).byteValue();
		bits = bits >> 8;
		if (bits == 0) {
			stream.write(ch, b2, b1, ch);
		}
		digit = DIGITS[longdigit.intValue()];
		intero = digit >> 8;
		byte b4 = intero.toString().getBytes()[0];
		byte b3 = Integer.valueOf(digit).byteValue();
		bits = bits >> 8;
		if (bits == 0) {
			stream.write(ch, b4, b3, b2, b1, ch);
		}
		digit = DIGITS[longdigit.intValue()];
		intero = digit >> 8;
		byte b6 = intero.toString().getBytes()[0];
		byte b5 = Integer.valueOf(digit).byteValue();
		bits = bits >> 8;
		if (bits == 0) {
			stream.write(ch, b6, b5, b4, b3);
			stream.write(b2, b1, ch);
		}
		digit = DIGITS[longdigit.intValue()];
		intero = digit >> 8;
		byte b8 = intero.toString().getBytes()[0];
		byte b7 = Integer.valueOf(digit).byteValue();
		bits = bits >> 8;
		if (bits == 0) {
			stream.write(ch, b8, b7, b6, b5, b4);
			stream.write(b3, b2, b1, ch);
		}
		digit = DIGITS[longdigit.intValue()];
		intero = digit >> 8;
		byte b10 = intero.toString().getBytes()[0];
		byte b9 = Integer.valueOf(digit).byteValue();
		bits = bits >> 8;
		if (bits == 0) {
			stream.write(ch, b10, b9, b8, b7, b6);
			stream.write(b5, b4, b3, b2, b1, ch);
		}
		digit = DIGITS[longdigit.intValue()];
		intero = digit >> 8;
		byte b12 = intero.toString().getBytes()[0];
		byte b11 = Integer.valueOf(digit).byteValue();
		bits = bits >> 8;
		if (bits == 0) {
			stream.write(ch, b12, b11, b10, b9, b8);
			stream.write(b7, b6, b5, b4, b3, b2);
			stream.write(b1, ch);
		}
		digit = DIGITS[longdigit.intValue()];
		intero = digit >> 8;
		byte b14 = intero.toString().getBytes()[0];
		byte b13 = Integer.valueOf(digit).byteValue();
		bits = bits >> 8;
		if (bits == 0) {
			stream.write(ch, b14, b13, b12, b11, b10);
			stream.write(b9, b8, b7, b6, b5, b4);
			stream.write(b3, b2, b1, ch);
		}
		digit = DIGITS[longdigit.intValue()];
		intero = digit >> 8;
		byte b16 = intero.toString().getBytes()[0];
		byte b15 = Integer.valueOf(digit).byteValue();
		stream.write(ch, b16, b15, b14, b13, b12);
		stream.write(b11, b10, b9, b8, b7, b6);
		stream.write(b5, b4, b3, b2, b1, ch);
	}
}
