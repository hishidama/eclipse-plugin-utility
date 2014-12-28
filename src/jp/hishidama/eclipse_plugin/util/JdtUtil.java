package jp.hishidama.eclipse_plugin.util;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class JdtUtil {

	public static ICompilationUnit getJavaUnit(IFile file) {
		IJavaElement element = JavaCore.create(file);
		if (element == null) {
			return null;
		}
		ICompilationUnit unit = (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (unit == null) {
			return null;
		}
		return unit;
	}

	private static class DummyContainerPage extends NewTypeWizardPage {

		public DummyContainerPage() {
			super(true, "dummy");
		}

		@Override
		public void createControl(Composite parent) {
		}

		@Override
		public IJavaElement getInitialJavaElement(IStructuredSelection selection) {
			return super.getInitialJavaElement(selection);
		}

		public IPackageFragmentRoot getPackageFragmentRoot(IJavaElement element) {
			super.initContainerPage(element);
			return super.getPackageFragmentRoot();
		}

		public String getPackageText(IJavaElement element) {
			super.initTypePage(element);
			return super.getPackageText();
		}
	}

	public static IJavaElement getJavaElement(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			return getJavaElement((IStructuredSelection) selection);
		}
		if (selection instanceof ITextSelection) {
			IEditorPart editor = HandlerUtil.getActiveEditor(event);
			int offset = ((ITextSelection) selection).getOffset();
			return getJavaElement(editor, offset);
		}

		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		ISelection s = editor.getSite().getSelectionProvider().getSelection();
		if (s instanceof ITextSelection) {
			int offset = ((ITextSelection) s).getOffset();
			return getJavaElement(editor, offset);
		}

		return null;
	}

	public static IJavaElement getJavaElement(IStructuredSelection selection) {
		return new DummyContainerPage().getInitialJavaElement(selection);
	}

	public static IJavaElement getJavaElement(IEditorPart editor, int offset) {
		IEditorInput input = editor.getEditorInput();
		IJavaElement element = (IJavaElement) input.getAdapter(IJavaElement.class);
		if (element == null) {
			return null;
		}

		try {
			ITypeRoot root = (ITypeRoot) element.getAdapter(ITypeRoot.class);
			IJavaElement[] codes = root.codeSelect(offset, 0);
			for (IJavaElement code : codes) {
				return code;
			}
		} catch (JavaModelException e) {
			// fall through
		}

		return element;
	}

	public static IPackageFragmentRoot getPackageFragmentRoot(IJavaElement element) {
		if (element == null) {
			return null;
		}
		return new DummyContainerPage().getPackageFragmentRoot(element);
	}

	public static String getDirectory(IPackageFragmentRoot root) {
		if (root == null) {
			return null;
		}
		IProject project = root.getJavaProject().getProject();
		IPath path = root.getPath().makeRelativeTo(project.getFullPath());
		return path.toPortableString();
	}

	public static String getPackage(IJavaElement element) {
		if (element == null) {
			return null;
		}
		String text = new DummyContainerPage().getPackageText(element);
		if (text.isEmpty()) {
			return null;
		}
		return text;
	}

	public static String getFQCN(Type type) {
		ITypeBinding bind = type.resolveBinding();
		if (bind == null) {
			throw new IllegalStateException("bind=null. do ASTParser#setResolveBindings(true)");
		}
		return bind.getQualifiedName();
	}
}
