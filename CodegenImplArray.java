package com.jsoniter;

import com.jsoniter.spi.ClassInfo;

import java.lang.reflect.Type;
import java.util.*;

/**
 * class CodegenImplArray
 * 
 * @author MaxiBon
 *
 */
class CodegenImplArray {

	private CodegenImplArray() {
	}

	/**
	 * parentesi1
	 */
	static final String PARENTESI= "}";
	static final String STRINGAIF = "if (!com.jsoniter.CodegenAccess.nextTokenIsComma(iter)) {";
	final static String STRINGA = "{{clazz}} obj = col == null ? new {{clazz}}(): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);";
	static final String OBJ3 = "obj.add(a3);";
	final static String OBJ2 = "obj.add(a2);";
	final static String OBJ1 = "obj.add(a1);";
	final static String OBJ4 = "obj.add(a4);";
	final static String RETURNOBJ = "return obj;";
	final static String STRINGAIF2 = "if (com.jsoniter.CodegenAccess.nextToken(iter) != ',') {";
	
	private final static int SBSIZE = 128;
	/**
	 * static Set<Class> WITH_CAPACITY_COLLECTION_CLASSES
	 */
	final static Set<Class> WITH_CAPACITY_COLLECTION_CLASSES = new HashSet<Class>() {
		/**
		* 
		*/
		private static final long serialVersionUID = 1723371799837389402L;

		{
			add(ArrayList.class);
			add(HashSet.class);
			add(Vector.class);
		}
	};


	public static StringBuilder genArraySupport(StringBuilder lines){
		append(lines, "com.jsoniter.CodegenAccess.resetExistingObject(iter);");
		append(lines, "byte nextToken = com.jsoniter.CodegenAccess.readByte(iter);");
		append(lines, "if (nextToken != '[') {");
		append(lines, "if (nextToken == 'n') {");
		append(lines, "com.jsoniter.CodegenAccess.skipFixedBytes(iter, 3);");
		append(lines, "com.jsoniter.CodegenAccess.resetExistingObject(iter); return null;");
		append(lines, "} else {");
		append(lines, "nextToken = com.jsoniter.CodegenAccess.nextToken(iter);");
		append(lines, "if (nextToken == 'n') {");
		append(lines, "com.jsoniter.CodegenAccess.skipFixedBytes(iter, 3);");
		append(lines, "com.jsoniter.CodegenAccess.resetExistingObject(iter); return null;");
		append(lines, PARENTESI);
		append(lines, PARENTESI);
		append(lines, PARENTESI);
		String stringa7 = "nextToken = com.jsoniter.CodegenAccess.nextToken(iter);";
		append(lines, stringa7);
		append(lines, "if (nextToken == ']') {");
		append(lines, "return new {{comp}}[0];");
		append(lines, PARENTESI);
		append(lines, "com.jsoniter.CodegenAccess.unreadByte(iter);");
		append(lines, "{{comp}} a1 = {{op}};");
		return lines;
	}
	
	public static StringBuilder genArraySupport1(StringBuilder lines){
		append(lines, STRINGAIF);
		append(lines, "return new {{comp}}[]{ a1 };");
		append(lines, PARENTESI);
		append(lines, "{{comp}} a2 = {{op}};");
		append(lines, STRINGAIF);
		append(lines, "return new {{comp}}[]{ a1, a2 };");
		append(lines, PARENTESI);
		append(lines, "{{comp}} a3 = {{op}};");
		append(lines, STRINGAIF);
		append(lines, "return new {{comp}}[]{ a1, a2, a3 };");
		append(lines, PARENTESI);
		append(lines, "{{comp}} a4 = ({{comp}}) {{op}};");
		append(lines, STRINGAIF);
		append(lines, "return new {{comp}}[]{ a1, a2, a3, a4 };");
		append(lines, PARENTESI);
		append(lines, "{{comp}} a5 = ({{comp}}) {{op}};");
		append(lines, "{{comp}}[] arr = new {{comp}}[10];");
		append(lines, "arr[0] = a1;");
		append(lines, "arr[1] = a2;");
		append(lines, "arr[2] = a3;");
		append(lines, "arr[3] = a4;");
		append(lines, "arr[4] = a5;");	
		return lines;
	}
	
	public static StringBuilder genArraySupport2(StringBuilder lines){
		append(lines, "int i = 5;");
		append(lines, "while (com.jsoniter.CodegenAccess.nextTokenIsComma(iter)) {");
		append(lines, "if (i == arr.length) {");
		append(lines, "{{comp}}[] newArr = new {{comp}}[arr.length * 2];");
		append(lines, "System.arraycopy(arr, 0, newArr, 0, arr.length);");
		append(lines, "arr = newArr;");
		append(lines, PARENTESI);
		append(lines, "arr[i++] = {{op}};");
		append(lines, PARENTESI);
		append(lines, "{{comp}}[] result = new {{comp}}[i];");
		append(lines, "System.arraycopy(arr, 0, result, 0, i);");
		append(lines, "return result;");
		return lines;
	}
	
	public static Class genArraySupport(ClassInfo classInfo){
		Class compType = classInfo.clazz.getComponentType();
		if (compType.isArray()) {
			throw new IllegalArgumentException("nested array not supported: " + classInfo.clazz.getCanonicalName());
		}
		return compType;
	}
	
	/**
	 * genArray.
	 * 
	 * @param classInfo
	 * @return
	 */
	public static String genArray(ClassInfo classInfo) {
		Class compType = genArraySupport(classInfo);
		StringBuilder lines = new StringBuilder(SBSIZE);
		lines.append("lines:");
		lines = genArraySupport(lines);
		lines = genArraySupport1(lines);
		
		return lines.toString().replace("{{comp}}", compType.getCanonicalName()).replace("{{op}}",
				CodegenImplNative.genReadOp(compType));
	}

	public static String genCollection(ClassInfo classInfo) {
		if (WITH_CAPACITY_COLLECTION_CLASSES.contains(classInfo.clazz)) {
			return CodegenImplArray.genCollectionWithCapacity(classInfo.clazz, classInfo.typeArgs[0]);
		} else {
			return CodegenImplArray.genCollectionWithoutCapacity(classInfo.clazz, classInfo.typeArgs[0]);
		}
	}

	private static StringBuilder genCollectionWithCapacitySupport(StringBuilder lines){
		append(lines, "{{clazz}} col = ({{clazz}})com.jsoniter.CodegenAccess.resetExistingObject(iter);");
		append(lines, "if (iter.readNull()) { com.jsoniter.CodegenAccess.resetExistingObject(iter); return null; }");
		append(lines, "if (!com.jsoniter.CodegenAccess.readArrayStart(iter)) {");
		append(lines,
				"return col == null ? new {{clazz}}(0): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
		append(lines, PARENTESI);
		append(lines, "Object a1 = {{op}};");
		append(lines, STRINGAIF2); 
		append(lines,
				"{{clazz}} obj = col == null ? new {{clazz}}(1): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
		
		append(lines, OBJ1);
		append(lines, RETURNOBJ);
		append(lines, PARENTESI);
		append(lines, "Object a2 = {{op}};");
		append(lines, STRINGAIF2);
		append(lines,
				"{{clazz}} obj = col == null ? new {{clazz}}(2): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
		append(lines, OBJ1);
		append(lines, OBJ2);
		append(lines, RETURNOBJ);
		append(lines, PARENTESI);
		return lines;
	}
	
	private static String genCollectionWithCapacity(Class clazz, Type compType) {
		StringBuilder lines = new StringBuilder(SBSIZE);
		lines = genCollectionWithCapacitySupport(lines);
		lines.append("lines:");
		append(lines, "Object a3 = {{op}};");
		append(lines, STRINGAIF2);
		append(lines,
				"{{clazz}} obj = col == null ? new {{clazz}}(3): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
		append(lines, OBJ1);
		append(lines, OBJ2);
		append(lines, OBJ3);
		append(lines, RETURNOBJ);
		append(lines, PARENTESI);
		append(lines, "Object a4 = {{op}};");
		append(lines,
				"{{clazz}} obj = col == null ? new {{clazz}}(8): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
		append(lines, OBJ1);
		append(lines, OBJ2);
		append(lines, OBJ3);
		append(lines, OBJ4);
		append(lines, "while (com.jsoniter.CodegenAccess.nextToken(iter) == ',') {");
		append(lines, "obj.add({{op}});");
		append(lines, PARENTESI);
		append(lines, RETURNOBJ);
		return lines.toString().replace("{{clazz}}", clazz.getName()).replace("{{op}}",
				CodegenImplNative.genReadOp(compType));
	}
	
	private static StringBuilder genCollectionWithoutCapacitySupport(StringBuilder lines){
		append(lines, "if (iter.readNull()) { com.jsoniter.CodegenAccess.resetExistingObject(iter); return null; }");
		append(lines, "{{clazz}} col = ({{clazz}})com.jsoniter.CodegenAccess.resetExistingObject(iter);");
		append(lines, "if (!com.jsoniter.CodegenAccess.readArrayStart(iter)) {");
		append(lines,
				"return col == null ? new {{clazz}}(): ({{clazz}})com.jsoniter.CodegenAccess.reuseCollection(col);");
		append(lines, PARENTESI);
		append(lines, "Object a1 = {{op}};");
		append(lines, STRINGAIF2);
		append(lines, STRINGA);
		append(lines, OBJ1);
		append(lines, RETURNOBJ);
		append(lines, PARENTESI);
		append(lines, "Object a2 = {{op}};");
		append(lines, STRINGAIF2);
		append(lines, STRINGA);
		append(lines, OBJ1);
		append(lines, OBJ2);
		append(lines, RETURNOBJ);
		append(lines, PARENTESI);
		append(lines, "Object a3 = {{op}};");
		return lines;
	}
	
	private static StringBuilder genCollectionWithoutCapacitySupport1(StringBuilder lines){
		append(lines, STRINGAIF2);
		append(lines, STRINGA);
		append(lines, OBJ1);
		append(lines, OBJ2);
		append(lines, OBJ3);
		append(lines, RETURNOBJ);
		append(lines, PARENTESI);
		append(lines, "Object a4 = {{op}};");
		append(lines, STRINGA);
		append(lines, OBJ1);
		append(lines, OBJ2);
		append(lines, OBJ3);
		append(lines, OBJ4);
		append(lines, "while (com.jsoniter.CodegenAccess.nextToken(iter) == ',') {");
		append(lines, "obj.add({{op}});");
		append(lines, PARENTESI);
		append(lines, RETURNOBJ);
		return lines;
	}

	private static String genCollectionWithoutCapacity(Class clazz, Type compType) {
		StringBuilder lines = new StringBuilder(SBSIZE);
		lines.append("lines:");
		lines = genCollectionWithoutCapacitySupport(lines);
		lines = genCollectionWithoutCapacitySupport1(lines);
		return lines.toString().replace("{{clazz}}", clazz.getName()).replace("{{op}}",
				CodegenImplNative.genReadOp(compType));
	}

	private static void append(StringBuilder lines, String str) {
		lines.append(str);
		lines.append("\n");
	}
}
