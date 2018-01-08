package com.jsoniter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jsoniter.spi.Binding;
import com.jsoniter.spi.ClassDescriptor;
import com.jsoniter.spi.ClassInfo;
import com.jsoniter.spi.Config;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.DecodingMode;
import com.jsoniter.spi.Extension;
import com.jsoniter.spi.GenericsHelper;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.TypeLiteral;

/**
 * class Codegen
 * 
 * @author MaxiBon
 *
 */
class Codegen {
	/**
	 * codegen
	 */
	private Codegen() {
	}

	/**
	 * HASHset
	 */
	private final static Set<String> GENETATEDCLASSNAMES = new HashSet<String>();
	/**
	 * static CodegenAccess.StaticCodegenTarget isDoingStaticCodegen
	 */
	static CodegenAccess.StaticCodegenTarget isDoingStaticCodegen = new CodegenAccess.StaticCodegenTarget("");

	/**
	 * 
	 * @param cacheKey
	 * @param type
	 * @return
	 */
	static Decoder getDecoder(String cacheKey, Type type) {
		Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
		if (decoder != null) {
			return decoder;
		}
		return gen(cacheKey, type);
	}

	/**
	 * 
	 * @param decoder
	 * @return
	 */
	private static Decoder genNull(Decoder decoder) {
		Decoder dec = null;
		if (decoder != null) {
			dec = decoder;
		}
		return dec;
	}

	/**
	 * 
	 * @param cacheKey
	 * @param mode
	 * @param classInfo
	 * @return
	 */
	private static String genSupport(String cacheKey, DecodingMode mode, ClassInfo classInfo) {
		String source = genSource(mode, classInfo);
		source = "public static java.lang.Object decode_(com.jsoniter.JsonIterator iter) throws java.io.IOException { "
				+ source + "}";
		if ("true".equals(System.getenv("JSONITER_DEBUG"))) {
			System.out.println(">>> " + cacheKey);
			System.out.println(source);
		}
		return source;
	}

	/**
	 * 
	 * @param decoder
	 * @param classInfo
	 * @param mode
	 * @return
	 */
	private static void genSupport(Decoder decoder, ClassInfo classInfo, DecodingMode mode) {
		if (mode == DecodingMode.REFLECTION_MODE) {
			decoder = ReflectionDecoderFactory.create(classInfo);
		}
	}

	/**
	 * 
	 * @param decoder
	 * @param cacheKey
	 * @param mode
	 * @return
	 */
	private static void genSupport(Decoder decoder, String cacheKey, DecodingMode mode) {
		if (isDoingStaticCodegen.outputDir == "") {
			try {
				if (Class.forName(cacheKey).newInstance() instanceof Decoder) {
					decoder = (Decoder) Class.forName(cacheKey).newInstance();
				}
			} catch (Exception e) {
				if (mode == DecodingMode.STATIC_MODE) {
					throw new JsonException(
							"static gen should provide the decoder we need, but failed to create the decoder");
				}
			}
		}
	}

	/**
	 * 
	 * @param decoder
	 * @param cacheKey
	 * @param source
	 * @param classInfo
	 * @return
	 */
	private static Decoder genSupport(Decoder decoder, String cacheKey, String source, ClassInfo classInfo) {
		Decoder dec = decoder;
		try {
			GENETATEDCLASSNAMES.add(cacheKey);
			if (isDoingStaticCodegen.outputDir == "") {
				dec = DynamicCodegen.gen(cacheKey, source);
			} else {
				staticGen(cacheKey, source);
			}
			return dec;
		} catch (Exception e) {
			String msg = "failed to generate decoder for: " + classInfo + " with "
					+ java.util.Arrays.toString(classInfo.typeArgs) + ", exception: " + e;
			msg = msg + "\n" + source;
			throw new JsonException("Error: Exception");
		}
	}

	/**
	 * 
	 * @param decoder
	 * @param cacheKey
	 * @param classInfo
	 * @return
	 */
	private static Decoder genSupport(Decoder decoder, String cacheKey, ClassInfo classInfo) {
		try {
			Config currentConfig = JsoniterSpi.getCurrentConfig();
			DecodingMode mode = currentConfig.decodingMode();
			genSupport(decoder, classInfo, mode);
			genSupport(decoder, cacheKey, mode);
			String source = genSupport(cacheKey, mode, classInfo);
			return genSupport(decoder, cacheKey, source, classInfo);
		} finally {
			JsoniterSpi.addNewDecoder(cacheKey, decoder);
		}
	}

	/**
	 * 
	 * @param cacheKey
	 * @param type
	 * @return
	 */
	private static Decoder gen(String cacheKey, Type type) {
		synchronized (gen(cacheKey, type)) {
			Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
			decoder = genNull(decoder);
			List<Extension> extensions = JsoniterSpi.getExtensions();
			for (Extension extension : extensions) {
				type = extension.chooseImplementation(type);
			}
			type = chooseImpl(type);
			for (Extension extension : extensions) {
				decoder = extension.createDecoder(cacheKey, type);
				if (decoder != null) {
					JsoniterSpi.addNewDecoder(cacheKey, decoder);
				}
			}
			ClassInfo classInfo = new ClassInfo(type);
			decoder = CodegenImplNative.NATIVE_DECODERS.get(classInfo.clazz);
			if (decoder != null) {
				return decoder;
			}
			addPlaceholderDecoderToSupportRecursiveStructure(cacheKey);
			return genSupport(decoder, cacheKey, classInfo);
		}
	}

	/**
	 * 
	 * @param cacheKey
	 */
	private static void addPlaceholderDecoderToSupportRecursiveStructure(final String cacheKey) {
		JsoniterSpi.addNewDecoder(cacheKey, new Decoder() {
			@Override
			public Object decode(JsonIterator iter) throws IOException {
				Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
				try {
					if (this == decoder) {
						for (int i = 0; i < 30; i++) {
							decoder = JsoniterSpi.getDecoder(cacheKey);
							if (this == decoder) {
								int n = 1000;
								Thread.sleep(n);

							} else {
								break;
							}
						}
						if (this == decoder) {
							throw new JsonException("internal error: placeholder is not replaced with real decoder");
						}
					}
				} catch (InterruptedException e) {
					throw new JsonException("Error : InterruptedException");
				}
				return decoder.decode(iter);
			}
		});
	}

	/**
	 * canStaticAccess
	 * 
	 * @param cacheKey
	 * @return
	 */
	public static boolean canStaticAccess(String cacheKey) {
		return GENETATEDCLASSNAMES.contains(cacheKey);
	}

	/**
	 * 
	 * @param typeArgs
	 * @param implClazz
	 * @return
	 */
	private static Type chooseImplSupp2(Type[] typeArgs, Class implClazz) {
		if (implClazz != null) {
			if (typeArgs.length == 0) {
				return implClazz;
			}
		}
		return GenericsHelper.createParameterizedType(typeArgs, null, implClazz);
	}

	/**
	 * 
	 * @param typeArgs
	 * @param clazz
	 * @param implClazz
	 * @return
	 */
	private static Type chooseImplSupp1(Type[] typeArgs, Class clazz, Class implClazz) {
		if (Map.class.isAssignableFrom(clazz)) {
			Type keyType = String.class;
			Type valueType = Object.class;
			if (typeArgs.length == 2) {
				keyType = typeArgs[0];
				valueType = typeArgs[1];
			} else {
				throw new IllegalArgumentException("can not bind to generic collection without argument types, "
						+ "try syntax like TypeLiteral<Map<String, String>>{}");
			}
			if (clazz == Map.class) {
				clazz = implClazz == null ? HashMap.class : implClazz;
			}
			if (keyType == Object.class) {
				keyType = String.class;
			}
			DefaultMapKeyDecoder.registerOrGetExisting(keyType);
			return GenericsHelper.createParameterizedType(new Type[] { keyType, valueType }, null, clazz);
		}
		return chooseImplSupp2(typeArgs, implClazz);

	}

	/**
	 * 
	 * @param typeArgs
	 * @param clazz
	 * @param implClazz
	 * @return
	 */
	private static Type chooseImplSupp(Type[] typeArgs, Class clazz, Class implClazz) {
		if (Collection.class.isAssignableFrom(clazz)) {
			Type compType = Object.class;
			if (typeArgs.length == 1) {
				compType = typeArgs[0];
			} else {
				throw new IllegalArgumentException("can not bind to generic collection without argument types, "
						+ "try syntax like TypeLiteral<List<Integer>>{}");
			}
			if (clazz == List.class) {
				clazz = implClazz == null ? java.util.ArrayList.class : implClazz;
			} else if (clazz == Set.class) {
				clazz = implClazz == null ? HashSet.class : implClazz;
			}
			return GenericsHelper.createParameterizedType(new Type[] { compType }, null, clazz);
		}
		return chooseImplSupp1(typeArgs, clazz, implClazz);
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	private static Type chooseImpl(Type type) {
		Type[] typeArgs = new Type[0];
		boolean b1 = type instanceof WildcardType;
		Class clazz = null;
		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) type;
			if (pType.getRawType() instanceof Class) {
				clazz = (Class) pType.getRawType();
				typeArgs = pType.getActualTypeArguments();
			}
		} else if (b1) {
			type = Object.class;
		} else if (type instanceof Class) {
			clazz = (Class) type;
		}
		Class implClazz = JsoniterSpi.getTypeImplementation(clazz);
		return chooseImplSupp(typeArgs, clazz, implClazz);
	}

	/**
	 * 
	 * @param cacheKey
	 * @param source
	 * @throws IOException
	 */
	private static void staticGen(String cacheKey, String source) throws IOException {
		createDir(cacheKey);
		String fileName = cacheKey.replace('.', '/') + ".java";
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(new File(isDoingStaticCodegen.outputDir, fileName));
			OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
			try {
				staticGen(cacheKey, writer, source);
			} finally {
				writer.close();
			}
		} finally {
			fileOutputStream.close();
		}
	}

	/**
	 * 
	 * @param cacheKey
	 * @param writer
	 * @param source
	 * @throws IOException
	 */
	private static void staticGen(String cacheKey, OutputStreamWriter writer, String source) throws IOException {
		String className = cacheKey.substring(cacheKey.lastIndexOf('.') + 1);
		String packageName = cacheKey.substring(0, cacheKey.lastIndexOf('.'));
		writer.write("package " + packageName + ";\n");
		writer.write("public class " + className + " implements com.jsoniter.spi.Decoder {\n");
		writer.write(source);
		writer.write("public java.lang.Object decode(com.jsoniter.JsonIterator iter) throws java.io.IOException {\n");
		writer.write("return decode_(iter);\n");
		writer.write("}\n");
		writer.write("}\n");
	}

	/**
	 * 
	 * @param cacheKey
	 */
	private static void createDir(String cacheKey) {
		String[] parts = cacheKey.split("\\.");
		File parent = new File(isDoingStaticCodegen.outputDir);
		File current = null;
		for (int i = 0; i < parts.length - 1; i++) {
			String part = parts[i];
			current = new File(parent, part);
			current.mkdir();
			parent = current;
		}
	}

	/**
	 * 
	 * @param mode
	 * @param classInfo
	 * @return
	 */
	private static String genSource(DecodingMode mode, ClassInfo classInfo) {
		String stringaRitorno = null;
		if (classInfo.clazz.isArray()) {
			stringaRitorno = CodegenImplArray.genArray(classInfo);
		}
		if (Map.class.isAssignableFrom(classInfo.clazz)) {
			stringaRitorno = CodegenImplMap.genMap(classInfo);
		}
		if (Collection.class.isAssignableFrom(classInfo.clazz)) {
			stringaRitorno = CodegenImplArray.genCollection(classInfo);
		}
		if (classInfo.clazz.isEnum()) {
			stringaRitorno = CodegenImplEnum.genEnum(classInfo);
		}
		ClassDescriptor desc = ClassDescriptor.getDecodingClassDescriptor(classInfo, false);
		if (shouldUseStrictMode(mode, desc)) {
			stringaRitorno = CodegenImplObjectStrict.genObjectUsingStrict(desc);
		} else {
			stringaRitorno = CodegenImplObjectHash.genObjectUsingHash(desc);
		}

		return stringaRitorno;
	}

	/**
	 * 
	 * @param mode
	 * @param desc
	 * @return
	 */
	private static boolean shouldUseStrictMode(DecodingMode mode, ClassDescriptor desc) {
		boolean supp = false;
		if (mode == DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_STRICTLY) {
			supp = true;
		}
		List<Binding> allBindings = desc.allDecoderBindings();
		for (Binding binding : allBindings) {
			if (binding.asMissingWhenNotPresent || binding.asExtraWhenPresent || binding.shouldSkip) {
				supp = true;
			}
		}
		if (desc.asExtraForUnknownProperties) {
			supp = true;
		}
		if (!desc.keyValueTypeWrappers.isEmpty()) {
			supp = true;
		}
		supp = shouldUseStrictModeSupp(allBindings);
		return supp;
	}

	/**
	 * 
	 * @param allBindings
	 * @return
	 */
	private static boolean shouldUseStrictModeSupp(List<Binding> allBindings) {
		boolean hasBinding = false;
		for (Binding allBinding : allBindings) {
			if (allBinding.fromNames.length > 0) {
				hasBinding = true;
			}
		}
		if (!hasBinding) {
			hasBinding = true;
		}
		return hasBinding;
	}

	/**
	 * staticGenDecoders
	 * 
	 * @param typeLiterals
	 * @param staticCodegenTarget
	 */
	public static void staticGenDecoders(TypeLiteral[] typeLiterals,
			CodegenAccess.StaticCodegenTarget staticCodegenTarget) {
		isDoingStaticCodegen = staticCodegenTarget;
		for (TypeLiteral typeLiteral : typeLiterals) {
			gen(typeLiteral.getDecoderCacheKey(), typeLiteral.getType());
		}
	}
}
