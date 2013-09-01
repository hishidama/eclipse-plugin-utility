package jp.hishidama.eclipse_plugin.util;

import java.util.List;

public class StringUtil {

	private StringUtil() {
	}

	public static boolean isEmpty(String s) {
		return (s == null) || s.isEmpty();
	}

	public static boolean nonEmpty(String s) {
		return (s != null) && !s.isEmpty();
	}

	public static String nonNull(String s) {
		return (s != null) ? s : "";
	}

	public static String get(String s, String defaultString) {
		if (nonEmpty(s)) {
			return s;
		} else {
			return defaultString;
		}
	}

	public static String toCamelCase(String name) {
		if (name == null) {
			return null;
		}
		String[] ss = name.split("\\_");
		StringBuilder sb = new StringBuilder(name.length());
		for (String s : ss) {
			sb.append(toFirstUpper(s));
		}
		return sb.toString();
	}

	public static String toLowerCamelCase(String name) {
		return toFirstLower(toCamelCase(name));
	}

	public static String toFirstUpper(String s) {
		if (s == null) {
			return null;
		}
		if (s.length() < 1) {
			return s;
		}
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	public static String toFirstLower(String s) {
		if (s == null) {
			return null;
		}
		if (s.length() < 1) {
			return s;
		}
		return Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}

	public static String append(String packageName, String name) {
		if (packageName.endsWith(".")) {
			packageName = packageName.substring(0, packageName.length() - 1);
		}
		if (name.startsWith(".")) {
			name = name.substring(1);
		}
		if (packageName.isEmpty()) {
			return name;
		} else {
			return packageName + "." + name;
		}
	}

	public static String getPackageName(String name) {
		int n = name.lastIndexOf('.');
		if (n >= 0) {
			return name.substring(0, n);
		} else {
			return "";
		}
	}

	public static String getSimpleName(String name) {
		int n = name.lastIndexOf('.');
		if (n >= 0) {
			return name.substring(n + 1);
		} else {
			return name;
		}
	}

	public static String toString(List<?> list) {
		if (list == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(512);
		for (Object obj : list) {
			if (sb.length() != 0) {
				sb.append("\n");
			}
			sb.append(obj);
		}
		return sb.toString();
	}
}
