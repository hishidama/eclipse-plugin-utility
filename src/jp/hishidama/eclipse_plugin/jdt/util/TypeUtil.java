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
}
