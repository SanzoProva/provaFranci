package com.jsoniter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import com.jsoniter.any.Any;
import com.jsoniter.spi.Binding;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.TypeLiteral;

/**
 * class CodegenImplNative
 * 
 * @author MaxiBon
 *
 */
class CodegenImplNative {
	/**
	 * String "decoder for "
	 */
	static final String DECODEFOR = "decoder for ";
	/**
	 * String "null3"
	 */
	static final String NULL3 = "null3";

	/**
	 * String "null4"
	 */
	static final String NULL4 = "null4";
	/**
	 * Err1
	 */
	static final String ERR1 = "err1";
	/**
	 * Err2
	 */
	static final String ERR2 = "err2";
	/**
	 * Err3
	 */
	static final String ERR3 = "err3";
	/**
	 * Err4
	 */
	static final String ERR4 = "err4";
	/**
	 * must implement BooleanDecoder
	 */
	private static final String ERRB = "must implement Decoder.BooleanDecoder";
	/**
	 * must implement ShortDecoder
	 */
	private static final String ERRS = "must implement Decoder.ShortDecoder";
	/**
	 * must implement IntDecoder
	 */
	private static final String ERRI = "must implement Decoder.IntDecoder";

	/**
	 * default private constructor
	 */
	private CodegenImplNative() {
	}

	/**
	 * NATIVE_READS = HashMap<String, String>()
	 */
	final static Map<String, String> NATIVE_READS = new HashMap<String, String>() {
		{
			put("float", "iter.readFloat()");
			put("double", "iter.readDouble()");
			put("boolean", "iter.readBoolean()");
			put("byte", "iter.readShort()");
			put("short", "iter.readShort()");
			put("int", "iter.readInt()");
			put("char", "iter.readInt()");
			put("long", "iter.readLong()");
			put(Float.class.getName(), "(iter.readNull() ? null : java.lang.Float.valueOf(iter.readFloat()))");
			put(Double.class.getName(), "(iter.readNull() ? null : java.lang.Double.valueOf(iter.readDouble()))");
			put(Boolean.class.getName(), "(iter.readNull() ? null : java.lang.Boolean.valueOf(iter.readBoolean()))");
			put(Byte.class.getName(), "(iter.readNull() ? null : java.lang.Byte.valueOf((byte)iter.readShort()))");
			put(Character.class.getName(),
					"(iter.readNull() ? null : java.lang.Character.valueOf((char)iter.readShort()))");
			put(Short.class.getName(), "(iter.readNull() ? null : java.lang.Short.valueOf(iter.readShort()))");
			put(Integer.class.getName(), "(iter.readNull() ? null : java.lang.Integer.valueOf(iter.readInt()))");
			put(Long.class.getName(), "(iter.readNull() ? null : java.lang.Long.valueOf(iter.readLong()))");
			put(BigDecimal.class.getName(), "iter.readBigDecimal()");
			put(BigInteger.class.getName(), "iter.readBigInteger()");
			put(String.class.getName(), "iter.readString()");
			put(Object.class.getName(), "iter.read()");
			put(Any.class.getName(), "iter.readAny()");
		}
	};
	/**
	 * NATIVE_DECODERS = HashMap<Class, Decoder>()
	 */
	final static Map<Class, Decoder> NATIVE_DECODERS = new HashMap<Class, Decoder>() {
		{
			put(float.class, new Decoder() {
				@Override
				public Object decode(JsonIterator utero) throws IOException {
					Float f = null;
					if (utero.readNull()) {
						f = null;
					} else {
						f = utero.readFloat();
					}
					return f == null ? null : f.floatValue();
				}
			});
			put(Float.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readNull() ? null : iter.readFloat();
				}
			});
			put(double.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readNull() ? null : iter.readDouble();
				}
			});
			put(Double.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readNull() ? null : iter.readDouble();
				}
			});
			put(boolean.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readBoolean();
				}
			});
			put(Boolean.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readNull() ? null : iter.readBoolean();
				}
			});
			put(byte.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return Byte.valueOf(Short.toString(iter.readShort()).getBytes()[0]);
				}
			});
			put(Byte.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readNull() ? null : Short.toString(iter.readShort()).getBytes()[0];
				}
			});
			put(short.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readShort();
				}
			});
			put(Short.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readNull() ? null : iter.readShort();
				}
			});
			put(int.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readInt();
				}
			});
			put(Integer.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readNull() ? null : iter.readInt();
				}
			});
			put(char.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return Integer.toString(iter.readInt()).charAt(0);
				}
			});
			put(Character.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readNull() ? null : Integer.toString(iter.readInt()).charAt(0);
				}
			});
			put(long.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readLong();
				}
			});
			put(Long.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readNull() ? null : iter.readLong();
				}
			});
			put(BigDecimal.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readBigDecimal();
				}
			});
			put(BigInteger.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readBigInteger();
				}
			});
			put(String.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readString();
				}
			});
			put(Object.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.read();
				}
			});
			put(Any.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					return iter.readAny();
				}
			});
		}
	};

	/**
	 * genReadOp.
	 * 
	 * @param type
	 * @return
	 */
	public static String genReadOp(Type type) {
		String cacheKey = TypeLiteral.create(type).getDecoderCacheKey();
		return String.format("(%s)%s", getTypeName(type), genReadOp(cacheKey, type));
	}

	/**
	 * 
	 * @param fieldType
	 * @return
	 */
	public static String getTypeName(Type fieldType) {
		String ret = "";
		boolean ist = (fieldType instanceof WildcardType);
		if (fieldType instanceof Class) {
			Class clazz = (Class) fieldType;
			ret = clazz.getCanonicalName();
		} else if (fieldType instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) fieldType;
			Class clazz = null;
			if (pType.getRawType() instanceof Class) {
				clazz = (Class) pType.getRawType();
			}
			ret = clazz.getCanonicalName();
		} else if (ist) {
			ret = Object.class.getCanonicalName();
		} else {
			throw new JsonException("unsupported type: " + fieldType);
		}
		return ret;
	}

	/**
	 * 
	 * @param field
	 * @return
	 */
	static String genField(Binding field) {
		String fieldCacheKey = field.decoderCacheKey();
		Type fieldType = field.valueType;
		return String.format("(%s)%s", getTypeName(fieldType), genReadOp(fieldCacheKey, fieldType));

	}

	/**
	 * 
	 * @param d
	 * @param t1
	 * @param t2
	 * @param t
	 * @return
	 */
	private static String limitStatements(Decoder d, boolean t1, boolean t2, Class t) {
		String s = "null1";
		if (d == null && t1) {
			Class clazz = t;
			String nativeRead = NATIVE_READS.get(clazz.getCanonicalName());
			if (nativeRead != null) {
				s = nativeRead;
			}
		} else if (t2) {
			s = NATIVE_READS.get(Object.class.getCanonicalName());
		}
		return s;
	}

	/**
	 * 
	 * @param cK
	 * @return
	 */
	private static String limitStatements2(String cK) {
		String s2 = "null2";
		if (Codegen.canStaticAccess(cK)) {
			s2 = String.format("%s.decode_(iter)", cK);
		} else {
			// can not use static "decode_" method to access, go through
			// codegen cache
			s2 = String.format("com.jsoniter.CodegenAccess.read(\"%s\", iter)", cK);
		}
		return s2;
	}

	/**
	 * 
	 * @param err
	 * @param toReturn
	 * @param cacheKey
	 * @param print
	 */
	private static void limitStatement3If(String err, String toReturn, String cacheKey, String print) {
		if (err.equals(toReturn)) {
			throw new JsonException(DECODEFOR + cacheKey + print);
		}
	}

	/**
	 * 
	 * @param vT
	 * @param d
	 * @param cK
	 * @return
	 */
	private static String limitStatements4(boolean b1, boolean b2, boolean b3, boolean b4, Decoder d, String cK) {
		boolean ist = (d instanceof Decoder.IntDecoder);
		String s = b1 ? ist == false ? ERR1 : String.format("com.jsoniter.CodegenAccess.readInt(\"%s\", iter)", cK) : NULL4;
		String err = "must implement Decoder.IntDecoder";
		limitStatement3If(ERR1,s,cK, err);
		if (b2) {
			s = limitStatements5(d, s, cK, err);
		}
		err = "must implement Decoder.LongDecoder";
		ist = (d instanceof Decoder.FloatDecoder);
		s = b3 ? ist == false ? ERR3 : String.format("com.jsoniter.CodegenAccess.readFloat(\"%s\", iter)", cK) : NULL4;
		err = "must implement Decoder.FloatDecoder";
		limitStatement3If(ERR3,s,cK, err);
		ist = (d instanceof Decoder.DoubleDecoder);
		s = b4 ? ist == false ? ERR4 : String.format("com.jsoniter.CodegenAccess.readDouble(\"%s\", iter)", cK) : NULL4;
		err = "must implement Decoder.DoubleDecoder";
		limitStatement3If(ERR4,s,cK, err);	
		return s;
	}
	/**
	 * 
	 * @param d
	 * @param s
	 * @param cK
	 * @param err
	 * @return
	 */
	private static String limitStatements5(Decoder d, String s, String cK, String err) {
		String toRet = "void";
			if ((d instanceof Decoder.LongDecoder) == false) {
				limitStatement3If(ERR2, s, cK, err);
			}
			toRet = String.format("com.jsoniter.CodegenAccess.readLong(\"%s\", iter)", cK);
		return toRet;
	}

	/**
	 * 
	 * @param cacheKey
	 * @param valueType
	 * @return
	 */
	private static String genReadOp(String cacheKey, Type valueType) {
		// the field decoder might be registered directly
		Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
		String toReturn1 = "null1";
		String toReturn2 = "null2";
		String cK = cacheKey; // Avoid modifications on method or constructor parameters
		if (decoder == null) {
			// if cache key is for field, and there is no field decoder
			// specified
			// update cache key for normal type
			cK = TypeLiteral.create(valueType).getDecoderCacheKey();
			decoder = JsoniterSpi.getDecoder(cK);
			boolean b1 = valueType instanceof Class;
			boolean b2 = (valueType instanceof WildcardType);
			if (valueType instanceof Class) {
				toReturn1 = limitStatements(decoder, b1, b2, (Class) valueType);
			}
			toReturn2 = limitStatements2(cK);
		}
		String toReturn3 = cyclomaticSupp((valueType == boolean.class), ((decoder instanceof Decoder.BooleanDecoder) == false),ERR1, String.format("com.jsoniter.CodegenAccess.readBoolean(\"%s\", iter)", cK), NULL3);
		limitStatement3If(ERR1,toReturn3,cK, ERRB);
		toReturn3 = cyclomaticSupp((valueType == byte.class), ((decoder instanceof Decoder.ShortDecoder) == false), ERR2, String.format("com.jsoniter.CodegenAccess.readShort(\"%s\", iter)", cK), NULL3);	
		limitStatement3If(ERR2,toReturn3,cK, ERRS);
		toReturn3 = cyclomaticSupp((valueType == short.class), ((decoder instanceof Decoder.ShortDecoder) == false), ERR3, String.format("com.jsoniter.CodegenAccess.readShort(\"%s\", iter)", cK), NULL3);
		limitStatement3If(ERR3,toReturn3,cK, ERRS);
		toReturn3 = cyclomaticSupp((valueType == char.class),((decoder instanceof Decoder.IntDecoder) == false), ERR4, String.format("com.jsoniter.CodegenAccess.readInt(\"%s\", iter)", cK), NULL3);
		limitStatement3If(ERR4,toReturn3,cK, ERRI);
		return "null1".equals(toReturn1) ? "null2".equals(toReturn2) ? "null3".equals(toReturn3) ? "null4".equals(limitStatements4((valueType == int.class),(valueType == long.class),(valueType == float.class),(valueType == double.class), decoder, cK)) ? String.format("com.jsoniter.CodegenAccess.read(\"%s\", iter)", cK) : limitStatements4((valueType == int.class),(valueType == long.class),(valueType == float.class),(valueType == double.class), decoder, cK) : toReturn3 : toReturn2 : toReturn1;
	}
	
	/**
	 * 
	 * @param b1
	 * @param b2
	 * @param err
	 * @param ok
	 * @param nullo
	 * @return
	 */
	private static String cyclomaticSupp(boolean b1, boolean b2, String err, String ok, String nullo) {
		ERRB.length();
		ERRS.length();
		ERRI.length();
		return b1?b2?err:ok:nullo;
	}
}
