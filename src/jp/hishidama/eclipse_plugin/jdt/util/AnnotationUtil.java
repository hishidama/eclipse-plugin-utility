package jp.hishidama.eclipse_plugin.jdt.util;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class AnnotationUtil {

	public static IAnnotation getAnnotation(IType type, String annotationName) {
		return getAnnotation(type, type, annotationName);
	}

	public static IAnnotation getAnnotation(IType type, IAnnotatable a, String annotationName) {
		return getAnnotation(type, a, Collections.singleton(annotationName));
	}

	public static IAnnotation getAnnotation(IType type, IAnnotatable a, Set<String> annotationName) {
		try {
			// http://www.eclipse.org/forums/index.php/m/257652/
			for (IAnnotation ann : a.getAnnotations()) {
				String[][] annTypes = type.resolveType(ann.getElementName());
				if (annTypes != null) {
					for (String[] name : annTypes) {
						String annName = name[0] + "." + name[1];
						if (annotationName.contains(annName)) {
							return ann;
						}
					}
				}
			}
		} catch (JavaModelException e) {
			return null;
		}
		return null;
	}

	public static <T> T getAnnotationValue(IType type, String annotationName, String memberName) {
		return getAnnotationValue(type, type, annotationName, memberName);
	}

	public static <T> T getAnnotationValue(IType type, IAnnotatable a, String annotationName, String memberName) {
		IAnnotation ann = getAnnotation(type, a, annotationName);
		return getAnnotationValue(ann, memberName);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getAnnotationValue(IAnnotation annotation, String memberName) {
		if (annotation != null) {
			try {
				for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
					if (memberName.equals(pair.getMemberName())) {
						return (T) pair.getValue();
					}
				}
			} catch (JavaModelException e) {
				return null;
			}
		}
		return null;
	}
}
