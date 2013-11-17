package jp.hishidama.eclipse_plugin.dialog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jp.hishidama.eclipse_plugin.dialog.ClassSelectionDialog.Filter;
import jp.hishidama.eclipse_plugin.jdt.util.TypeUtil;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;

public class ClassSelectionImplementsFilter extends Filter {
	private Set<String> interfaceName;

	public ClassSelectionImplementsFilter(String... interfaceName) {
		this.interfaceName = new HashSet<String>(Arrays.asList(interfaceName));
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
					return TypeUtil.isImplements(type, interfaceName);
				} catch (JavaModelException e) {
					return false;
				}
			}
		};
	}
}
