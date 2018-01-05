package com.jsoniter;

import com.jsoniter.spi.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

/**
 * class Codegen
 * 
 * @author MaxiBon
 *
 */
class Codegen {

	private Codegen() {
	}

	// only read/write when generating code with synchronized protection
	private final static Set<String> generatedClassNames = new HashSet<String>();
	/**
	 * static CodegenAccess.StaticCodegenTarget isDoingStaticCodegen
	 */
	static CodegenAccess.StaticCodegenTarget isDoingStaticCodegen = new CodegenAccess.StaticCodegenTarget("");

	static Decoder getDecoder(String cacheKey, Type type) {
		Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
		Decoder dec = null;
		if (decoder != null) {
			dec = decoder;
		}else {
			dec = gen(cacheKey, type);
		}
		return dec;
	}

	private static Decoder genNull(Decoder decoder) {
		Decoder dec = null;
		if (decoder != null) {
			dec = decoder;
		}
		return dec;
	}

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

	private static Decoder genSupport(Decoder decoder, ClassInfo classInfo, DecodingMode mode) {
		Decoder dec = decoder;
		if (mode == DecodingMode.REFLECTION_MODE) {
			dec = ReflectionDecoderFactory.create(classInfo);
		}
		return dec;
	}

	private static Decoder genSupport(Decoder decoder, String cacheKey, DecodingMode mode) {
		Decoder dec = decoder;
		if (isDoingStaticCodegen.outputDir == "") {
			try {
				if (Class.forName(cacheKey).newInstance() instanceof Decoder) {
					dec = (Decoder) Class.forName(cacheKey).newInstance();
				}
			} catch (Exception e) {
				if (mode == DecodingMode.STATIC_MODE) {
					throw new JsonException(
							"static gen should provide the decoder we need, but failed to create the decoder");
				}
			}
		}

		return dec;
	}

	private static Decoder genSupport(Decoder decoder, String cacheKey, String source, ClassInfo classInfo) {
		Decoder dec = decoder;
		try {
			generatedClassNames.add(cacheKey);
			if (isDoingStaticCodegen.outputDir == "") {
				dec = DynamicCodegen.gen(cacheKey, source);
			} else {
				staticGen(cacheKey, source);
			}
			return dec;
		} catch (Exception e) {
			String msg = "failed to generate decoder for: " + classInfo + " with " + Arrays.toString(classInfo.typeArgs)
					+ ", exception: " + e;
			msg = msg + "\n" + source;
			throw new JsonException("Error: Exception");
		}
	}

	private static Decoder genSupport(Decoder decoder, String cacheKey, ClassInfo classInfo) {
		try {
			Config currentConfig = JsoniterSpi.getCurrentConfig();
			DecodingMode mode = currentConfig.decodingMode();
			Decoder deco = genSupport(decoder, classInfo, mode);
			Decoder dec = genSupport(deco, cacheKey, mode);
			String source = genSupport(cacheKey, mode, classInfo);
			return genSupport(dec, cacheKey, source, classInfo);
		} finally {
			JsoniterSpi.addNewDecoder(cacheKey, decoder);
		}
	}

	private static Decoder gen(String cacheKey, Type type) {
		synchronized (gen(cacheKey, type)) {
			Decoder decoder = JsoniterSpi.getDecoder(cacheKey);
			decoder = genNull(decoder);
			List<Extension> extensions = JsoniterSpi.getExtensions();
			for (Extension extension : extensions) {
				type = extension.chooseImplementation(type);
			}
			Type typ = chooseImpl(type);
			for (Extension extension : extensions) {
				decoder = extension.createDecoder(cacheKey, typ);
				if (decoder != null) {
					JsoniterSpi.addNewDecoder(cacheKey, decoder);
				}
			}
			ClassInfo classInfo = new ClassInfo(typ);
			decoder = CodegenImplNative.NATIVE_DECODERS.get(classInfo.clazz);
			decoder = genNull(decoder);
			addPlaceholderDecoderToSupportRecursiveStructure(cacheKey);
			return decoder = genSupport(decoder, cacheKey, classInfo);

		}
	}

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
		return generatedClassNames.contains(cacheKey);
	}

	private static Type chooseImplSupp1(Type[] typeArgs, Class clazz, Class implClazz) {
		Type t = null;
		Type[] typeArg = typeArgs;
		if (Map.class.isAssignableFrom(clazz)) {
			Type keyType = String.class;
			Type valueType = Object.class;
			if (typeArg.length == 2) {
				keyType = typeArg[0];
				valueType = typeArg[1];
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
			t = GenericsHelper.createParameterizedType(new Type[] { keyType, valueType }, null, clazz);
		}
		return t;
	}

	private static Type chooseImplSupp(Type[] typeArgs, Class clazz, Class implClazz) {
		Type t = null;
		Type[] typeArg = typeArgs;
		if (Collection.class.isAssignableFrom(clazz)) {
			Type compType = Object.class;
			if (typeArg.length == 1) {
				compType = typeArg[0];
			} else {
				throw new IllegalArgumentException("can not bind to generic collection without argument types, "
						+ "try syntax like TypeLiteral<List<Integer>>{}");
			}
			if (clazz == List.class) {
				clazz = implClazz == null ? ArrayList.class : implClazz;
			} else if (clazz == Set.class) {
				clazz = implClazz == null ? HashSet.class : implClazz;
			}
			t = GenericsHelper.createParameterizedType(new Type[] { compType }, null, clazz);
		}

		return t;
	}

	private static Type chooseImpl(Type type) {
		Type[] typeArgs = new Type[0];
		Class clazz = null;
		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) type;
			if (type instanceof ParameterizedType) {
				clazz = (Class) pType.getRawType();
				typeArgs = pType.getActualTypeArguments();
			}
		} else if (type instanceof WildcardType) {
			type = Object.class;
		} else {
			if (type instanceof Class) {
				clazz = (Class) type;
			}
		}
		Class implClazz = JsoniterSpi.getTypeImplementation(clazz);

		type = chooseImplSupp(typeArgs, clazz, implClazz);
		type = chooseImplSupp1(typeArgs, clazz, implClazz);
		type = chooseImplSupp2(typeArgs, implClazz);

		return type;
	}

	private static Type chooseImplSupp2(Type[] typeArgs, Class implClazz) {
		Type type = null;
		if (implClazz != null) {
			if (typeArgs.length == 0) {
				type = implClazz;
			} else {
				type = GenericsHelper.createParameterizedType(typeArgs, null, implClazz);
			}
		}

		return type;

	}

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
		supp =shouldUseStrictModeSupp(allBindings);
		return supp;
	}

	private static boolean shouldUseStrictModeSupp(List<Binding> allBindings) {
		boolean hasBinding = false;
		for (Binding allBinding : allBindings) {
			if (allBinding.fromNames.length > 0) {
				hasBinding = true;
			}
		}
		if (!hasBinding) {
			hasBinding =  true;
		}
		return hasBinding;
	}

	public static void staticGenDecoders(TypeLiteral[] typeLiterals,
			CodegenAccess.StaticCodegenTarget staticCodegenTarget) {
		isDoingStaticCodegen = staticCodegenTarget;
		for (TypeLiteral typeLiteral : typeLiterals) {
			gen(typeLiteral.getDecoderCacheKey(), typeLiteral.getType());
		}
	}
}
