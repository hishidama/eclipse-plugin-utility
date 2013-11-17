package jp.hishidama.eclipse_plugin.dialog;

import jp.hishidama.eclipse_plugin.dialog.ClassSelectionDialog.Filter;
import jp.hishidama.eclipse_plugin.jdt.util.AnnotationUtil;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;

public class ClassSelectionAnnotationFilter extends Filter {
	private String annotationName;

	public ClassSelectionAnnotationFilter(String annotationName) {
		this.annotationName = annotationName;
	}

	@Override
	public ITypeInfoFilterExtension getFilterExtension() {
		return new ITypeInfoFilterExtension() {

			@Override
			public boolean select(ITypeInfoRequestor typeInfoRequestor) {
				String name = typeInfoRequestor.getPackageName() + "." + typeInfoRequestor.getTypeName();
				try {
					IType type = project.findType(name);
					if (type == null) {
						return false;
					}
					return AnnotationUtil.getAnnotation(type, annotationName) != null;
				} catch (JavaModelException e) {
					return false;
				}
			}
		};
	}
}
