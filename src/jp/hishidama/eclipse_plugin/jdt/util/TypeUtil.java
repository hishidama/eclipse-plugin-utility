package jp.hishidama.eclipse_plugin.jdt.util;

import java.util.Collections;
import java.util.Set;

import jp.hishidama.eclipse_plugin.util.StringUtil;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

public class TypeUtil {

	public static IType findType(IJavaProject project, String name) {
		if (project == null || name == null) {
			return null;
		}
		try {
			return project.findType(name);
		} catch (JavaModelException e) {
			return null;
		}
	}

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

	public static IType getPublicType(ICompilationUnit cu) {
		try {
			for (IType type : cu.getTypes()) {
				int flag = type.getFlags();
				if ((flag & Flags.AccPublic) != 0) {
					return type;
				}
			}
		} catch (JavaModelException e) {
			// fall through
		}
		return null;
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
				String resolved = resolveTypeName(s, type);
				if (interfaceName.contains(resolved)) {
					return resolved;
				}

				IType itype = TypeUtil.findType(type.getJavaProject(), resolved);
				String found = findImplements(itype, interfaceName);
				if (found != null) {
					return found;
				}
			}

			String superClass = type.getSuperclassName();
			if (superClass == null || "java.lang.Object".equals(superClass)) {
				return null;
			}

			IType superType = resolveType(superClass, type);
			String found = findImplements(superType, interfaceName);
			if (found != null) {
				return found;
			}
			return null;
		} catch (JavaModelException e) {
			return null;
		}
	}

	public static IMethod findConsructor(IType type) {
		try {
			for (IMethod method : type.getMethods()) {
				if (method.isConstructor()) {
					return method;
				}
			}
		} catch (JavaModelException e) {
			return null;
		}
		return null;
	}

	public static IMethod findMethod(IType type, String methodName) {
		if (type == null || methodName == null) {
			return null;
		}
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

	public static String getVariableTypeSimpleName(ILocalVariable variable) {
		String signature = variable.getTypeSignature();
		String name = Signature.toString(signature);
		return resolveTypeSimpleNameAll(name);
	}

	public static String getFieldTypeName(IField field) {
		try {
			String signature = field.getTypeSignature();
			String name = Signature.toString(signature);
			return resolveTypeNameAll(name, field.getDeclaringType());
		} catch (JavaModelException e) {
			return null;
		}
	}

	public static String resolveTypeNameAll(String name, IType type) {
		StringBuilder sb = new StringBuilder(name.length() * 2);
		for (int i = 0; i < name.length();) {
			int n = indexOf(name, "<,>", i);
			if (n < 0) {
				String resolveName = resolveTypeName(name.substring(i), type);
				if (resolveName == null) {
					resolveName = name;
				}
				sb.append(resolveName);
				break;
			}
			String part = name.substring(i, n);
			String resolveName = resolveTypeName(part, type);
			if (resolveName == null) {
				resolveName = part;
			}
			sb.append(resolveName);
			sb.append(name.charAt(n));
			i = n + 1;
		}
		return sb.toString();
	}

	private static String resolveTypeSimpleNameAll(String name) {
		StringBuilder sb = new StringBuilder(name.length() * 2);
		for (int i = 0; i < name.length();) {
			int n = indexOf(name, "<,>", i);
			if (n < 0) {
				sb.append(StringUtil.getSimpleName(name.substring(i)));
				break;
			}

			sb.append(StringUtil.getSimpleName(name.substring(i, n)));
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
