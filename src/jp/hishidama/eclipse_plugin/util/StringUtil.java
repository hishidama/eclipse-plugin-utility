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

	public static String trim(String s) {
		return (s != null) ? s.trim() : null;
	}

	public static String removeBlank(String s) {
		if (s == null) {
			return s;
		}
		return s.replaceAll("[ \t\r\n]", "");
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

	public static String toSnakeCase(String name) {
		if (name == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(name.length());
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				if (sb.length() > 0) {
					sb.append('_');
				}
				c = Character.toLowerCase(c);
			}
			sb.append(c);
		}
		return sb.toString();
	}

	public static String toSmallCamelCase(String name) {
		if (name == null) {
			return null;
		}
		String[] ss = name.split("\\_");
		if (ss.length <= 1) {
			return name.toLowerCase();
		} else {
			StringBuilder sb = new StringBuilder(name.length());
			sb.append(ss[0].toLowerCase());
			for (int i = 1; i < ss.length; i++) {
				sb.append(toFirstUpper(ss[i]));
			}
			return sb.toString();
		}
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

	public static String removeEnds(String s, String remove) {
		if (s.endsWith(remove)) {
			s = s.substring(0, s.length() - remove.length());
		}
		return s;
	}

	public static String mkString(List<String> list) {
		if (list == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		mkString(sb, list);
		return sb.toString();
	}

	public static void mkString(StringBuilder sb, List<String> list) {
		boolean first = true;
		for (String s : list) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(s);
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

	public static boolean equals(String s1, String s2) {
		if (s1 != null) {
			return s1.equals(s2);
		}
		return s2 == null;
	}
}
