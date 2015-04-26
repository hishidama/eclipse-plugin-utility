package jp.hishidama.eclipse_plugin.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static jp.hishidama.eclipse_plugin.util.StringUtil.*;

public abstract class ClassGenerator {
	protected static final Set<String> PRIMITIVE_SET = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"boolean", "byte", "short", "char", "int", "long", "float", "double")));

	protected String packageName;
	protected String className;
	protected final Set<String> typeParameterSet = new HashSet<String>();

	private Map<String, String> classNameMap;
	private Set<String> simpleNameSet;

	public String generate(String packageName, String className) {
		this.packageName = packageName;
		this.className = className;
		this.classNameMap = new HashMap<String, String>();
		this.simpleNameSet = new HashSet<String>();
		initialize();
		return generate();
	}

	public final String getFullClassName() {
		return packageName + "." + className;
	}

	protected abstract void initialize();

	private String generate() {
		StringBuilder body = new StringBuilder(1024);
		appendClass(body);

		StringBuilder sb = new StringBuilder(body.length() + 512);
		appendPackage(sb);
		appendImport(sb);
		sb.append(body);
		return sb.toString();
	}

	private void appendPackage(StringBuilder sb) {
		sb.append("package ");
		sb.append(packageName);
		sb.append(";\n\n");
	}

	private void appendImport(StringBuilder sb) {
		List<String> list = new ArrayList<String>(classNameMap.keySet());
		Collections.sort(list);
		for (String s : list) {
			if (!s.isEmpty() && !s.startsWith("java.lang.")) {
				sb.append("import ");
				sb.append(s);
				sb.append(";\n");
			}
		}
		sb.append("\n");
	}

	protected abstract void appendClass(StringBuilder sb);

	protected final void setClassJavadoc(StringBuilder sb, String title) {
		sb.append("/**\n");
		sb.append(" * ");
		sb.append(title);
		sb.append("\n */\n");
	}

	protected final void setLineJavadoc(StringBuilder sb, int tab, String title) {
		for (int i = 0; i < tab; i++) {
			sb.append('\t');
		}
		sb.append("/** ");
		sb.append(nonNull(title));
		sb.append(" */\n");
	}

	protected final void setParamJavadoc(StringBuilder sb, int tab, String name, String description) {
		for (int i = 0; i < tab; i++) {
			sb.append('\t');
		}
		sb.append(" * @param ");
		sb.append(name);
		sb.append(" ");
		sb.append(nonNull(description));
		sb.append("\n");
	}

	protected final String getCachedClassName(String className) {
		if (isTypeParameter(className)) {
			return className;
		}
		if (isPrimitive(className)) {
			return className;
		}
		if (className == null) {
			className = "";
		}
		String name = classNameMap.get(className);
		if (name != null) {
			return name;
		}
		String sname = getSimpleName(className);
		if (simpleNameSet.contains(sname)) {
			classNameMap.put(className, className);
			return className;
		} else {
			classNameMap.put(className, sname);
			simpleNameSet.add(sname);
			return sname;
		}
	}

	protected boolean isTypeParameter(String className) {
		return typeParameterSet.contains(className);
	}

	protected final boolean isPrimitive(String className) {
		return PRIMITIVE_SET.contains(className);
	}

	protected static String getSimpleName(String name) {
		if (name == null) {
			return null;
		}
		int n = name.lastIndexOf('.');
		if (n >= 0) {
			return name.substring(n + 1);
		} else {
			return name;
		}
	}
}
