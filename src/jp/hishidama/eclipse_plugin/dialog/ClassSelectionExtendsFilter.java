package jp.hishidama.eclipse_plugin.dialog;

import jp.hishidama.eclipse_plugin.dialog.ClassSelectionDialog.Filter;
import jp.hishidama.eclipse_plugin.jdt.util.TypeUtil;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;

public class ClassSelectionExtendsFilter extends Filter {
	private String parentName;

	public ClassSelectionExtendsFilter(String parentName) {
		this.parentName = parentName;
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
					int flag = type.getFlags();
					if (Flags.isAbstract(flag) || Flags.isInterface(flag)) {
						return false;
					}
					return TypeUtil.isExtends(type, parentName);
				} catch (JavaModelException e) {
					return false;
				}
			}
		};
	}
}
