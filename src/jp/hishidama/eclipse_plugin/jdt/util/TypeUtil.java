package jp.hishidama.eclipse_plugin.jdt.util;

import java.util.Collections;
import java.util.Set;

import jp.hishidama.eclipse_plugin.util.StringUtil;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

public class TypeUtil {

	public static IType resolveType(String name, IType type) {
		String resolvedName = resolveTypeName(name, type);
		if (resolvedName == null) {
			return null;
		}

		IJavaProject project = type.getJavaProject();
		try {
			return project.findType(resolvedName);
		} catch (JavaModelException e) {
			return null;
		}
	}

	public static String resolveTypeName(String name, IType type) {
		if (name == null) {
			return null;
		}
		try {
			String[][] types = type.resolveType(name);
			if (types != null) {
				for (String[] ss : types) {
					return ss[0] + "." + ss[1];
				}
			}
			return null;
		} catch (JavaModelException e) {
			return null;
		}
	}

	public static boolean isExtends(IType type, final String name) {
		IsExtends delegator = new IsExtends() {
			@Override
			protected boolean accept(String className) {
				return className.equals(name);
			}
		};
		return delegator.isExtends(type);
	}

	public static boolean isExtends(IType type, final String prefix, final String suffix) {
		IsExtends delegator = new IsExtends() {
			@Override
			protected boolean accept(String className) {
				String name = StringUtil.getSimpleName(className);
				return name.startsWith(prefix) && name.endsWith(suffix);
			}
		};
		return delegator.isExtends(type);
	}

	private static abstract class IsExtends {
		public boolean isExtends(IType type) {
			if (type == null) {
				return false;
			}
			try {
				String superClass = resolveTypeName(type.getSuperclassName(), type);
				if (superClass == null || "java.lang.Object".equals(superClass)) {
					return false;
				}
				if (accept(superClass)) {
					return true;
				}
				IType superType = type.getJavaProject().findType(superClass);
				return isExtends(superType);
			} catch (JavaModelException e) {
				// do nothing
			}
			return false;
		}

		protected abstract boolean accept(String className);
	}

	public static boolean isImplements(IType type, String interfaceName) {
		return isImplements(type, Collections.singleton(interfaceName));
	}

	public static boolean isImplements(IType type, Set<String> interfaceName) {
		return findImplements(type, interfaceName) != null;
	}

	public static String findImplements(IType type, Set<String> interfaceName) {
		if (type == null) {
			return null;
		}
		try {
			String[] ss = type.getSuperInterfaceNames();
			for (String s : ss) {
				if (interfaceName.contains(s)) {
					return s;
				}
			}
			String superClass = type.getSuperclassName();
			if (superClass == null || "java.lang.Object".equals(superClass)) {
				return null;
			}
			String[][] resolved = type.resolveType(superClass);
			if (resolved == null) {
				return null;
			}
			for (String[] names : resolved) {
				String resPack = names[0];
				String resName = names[1];
				IJavaProject javaProject = type.getJavaProject();
				IType superType = javaProject.findType(resPack, resName);
				String r = findImplements(superType, interfaceName);
				if (r != null) {
					return r;
				}
			}
			return null;
		} catch (JavaModelException e) {
			return null;
		}
	}

	public static IMethod findMethod(IType type, String methodName) {
		try {
			for (IMethod method : type.getMethods()) {
				if (methodName.equals(method.getElementName())) {
					return method;
				}
			}
		} catch (JavaModelException e) {
			return null;
		}
		return null;
	}

	public static String getMethodReturnTypeName(IMethod method) {
		try {
			String signature = method.getReturnType();
			String name = Signature.toString(signature);
			return resolveTypeName(name, method.getDeclaringType());
		} catch (JavaModelException e) {
			return null;
		}
	}

	public static String getVariableTypeName(ILocalVariable variable) {
		String signature = variable.getTypeSignature();
		String name = Signature.toString(signature);
		return resolveTypeNameAll(name, variable.getDeclaringMember().getDeclaringType());
	}

	public static String resolveTypeNameAll(String name, IType type) {
		StringBuilder sb = new StringBuilder(name.length() * 2);
		for (int i = 0; i < name.length();) {
			int n = indexOf(name, "<,>", i);
			if (n < 0) {
				sb.append(resolveTypeName(name.substring(i), type));
				break;
			}

			sb.append(resolveTypeName(name.substring(i, n), type));
			sb.append(name.charAt(n));
			i = n + 1;
		}
		return sb.toString();
	}

	private static int indexOf(String s, String search, int start) {
		for (int i = start; i < s.length(); i++) {
			char c = s.charAt(i);
			if (search.indexOf(c) >= 0) {
				return i;
			}
		}
		return -1;
	}
}
