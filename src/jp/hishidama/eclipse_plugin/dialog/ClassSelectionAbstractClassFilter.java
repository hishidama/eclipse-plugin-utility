package jp.hishidama.eclipse_plugin.dialog;

import jp.hishidama.eclipse_plugin.dialog.ClassSelectionDialog.Filter;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;

public class ClassSelectionAbstractClassFilter extends Filter {

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
					int flag = type.getFlags();
					return Flags.isAbstract(flag);
				} catch (JavaModelException e) {
					return false;
				}
			}
		};
	}
}
