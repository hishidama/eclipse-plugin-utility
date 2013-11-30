package jp.hishidama.eclipse_plugin.dialog;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
public class ClassSelectionDialog extends FilteredTypesSelectionDialog {

	public static ClassSelectionDialog create(Shell shell, IProject project, IRunnableContext context, Filter extension) {
		return create(shell, project, context, IJavaSearchConstants.CLASS, extension);
	}

	public static ClassSelectionDialog create(Shell shell, IProject project, IRunnableContext context, int kind,
			Filter extension) {
		IJavaProject javaProject = JavaCore.create(project);
		IJavaElement[] elements = new IJavaElement[] { javaProject };
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements);

		if (context == null) {
			context = PlatformUI.getWorkbench().getProgressService();
		}

		if (extension != null) {
			extension.setProject(javaProject);
		}

		return new ClassSelectionDialog(shell, context, scope, kind, extension);
	}

	public ClassSelectionDialog(Shell shell, IRunnableContext context, IJavaSearchScope scope, int kind,
			TypeSelectionExtension extension) {
		super(shell, false, context, scope, kind, extension);
		setHelpAvailable(false);
	}

	@Override
	public void setTitle(String title) {
		super.setTitle(title);
	}

	@Override
	public void setInitialPattern(String text) {
		super.setInitialPattern(text);
	}

	@Override
	public int open() {
		return super.open();
	}

	public String getSelectedClass() {
		Object[] r = super.getResult();
		if (r.length > 0) {
			IType type = (IType) r[0];
			return type.getFullyQualifiedName();
		}
		return null;
	}

	public static abstract class Filter extends TypeSelectionExtension {
		protected IJavaProject project;

		public void setProject(IJavaProject project) {
			this.project = project;
		}
	}
}
