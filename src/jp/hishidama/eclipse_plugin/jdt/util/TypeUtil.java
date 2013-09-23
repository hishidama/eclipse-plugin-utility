package jp.hishidama.eclipse_plugin.jdt.util;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

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

	public static boolean isExtends(IType type, String name) {
		if (type == null) {
			return false;
		}
		try {
			String superClass = resolveTypeName(type.getSuperclassName(), type);
			if (superClass == null || "java.lang.Object".equals(superClass)) {
				return false;
			}
			if (name.equals(superClass)) {
				return true;
			}
			IType superType = type.getJavaProject().findType(superClass);
			return isExtends(superType, name);
		} catch (JavaModelException e) {
			// do nothing
		}
		return false;
	}

	public static boolean isImplements(IType type, String interfaceName) {
		if (type == null) {
			return false;
		}
		try {
			String[] ss = type.getSuperInterfaceNames();
			for (String s : ss) {
				if (interfaceName.equals(s)) {
					return true;
				}
			}
			String superClass = type.getSuperclassName();
			if (superClass == null || "java.lang.Object".equals(superClass)) {
				return false;
			}
			String[][] resolved = type.resolveType(superClass);
			for (String[] names : resolved) {
				String resPack = names[0];
				String resName = names[1];
				IJavaProject javaProject = type.getJavaProject();
				IType superType = javaProject.findType(resPack, resName);
				boolean r = isImplements(superType, interfaceName);
				if (r) {
					return r;
				}
			}
			return false;
		} catch (JavaModelException e) {
			return false;
		}
	}
}
