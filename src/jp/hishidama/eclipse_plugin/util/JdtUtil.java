package jp.hishidama.eclipse_plugin.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jp.hishidama.eclipse_plugin.util.internal.LogUtil;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

public class JdtUtil {

	@SuppressWarnings("deprecation")
	public static ASTParser newASTParser() {
		ASTParser parser;
		try {
			parser = ASTParser.newParser(AST.JLS8);
		} catch (IllegalArgumentException e) {
			parser = ASTParser.newParser(AST.JLS4);
		}
		return parser;
	}

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

	public static List<IJavaElement> getJavaElements(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			return getJavaElements((IStructuredSelection) selection);
		}
		if (selection instanceof ITextSelection) {
			IEditorPart editor = HandlerUtil.getActiveEditor(event);
			IEditorInput input = editor.getEditorInput();
			IJavaElement element = (IJavaElement) input.getAdapter(IJavaElement.class);
			if (element != null) {
				ITypeRoot root = (ITypeRoot) element.getAdapter(ITypeRoot.class);
				if (root != null) {
					return Arrays.asList((IJavaElement) root);
				} else {
					return Arrays.asList(element);
				}
			}
		}
		IJavaElement element = getJavaElement(event);
		if (element != null) {
			return Arrays.asList(element);
		}
		return Collections.emptyList();
	}

	public static List<IJavaElement> getJavaElements(IStructuredSelection selection) {
		List<IJavaElement> list = new ArrayList<IJavaElement>();
		for (Iterator<?> i = selection.iterator(); i.hasNext();) {
			Object object = i.next();
			StructuredSelection ss = new StructuredSelection(object);
			IJavaElement element = getJavaElement(ss);
			if (element != null) {
				list.add(element);
			}
		}
		return list;
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

	public static int getOffset(ITextEditor editor) {
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		return selection.getOffset();
	}

	public static IJavaElement getJavaElement(IEditorPart editor, int offset) {
		IEditorInput input = editor.getEditorInput();
		IJavaElement element = (IJavaElement) input.getAdapter(IJavaElement.class);
		if (element == null) {
			return null;
		}

		try {
			ITypeRoot root = (ITypeRoot) element.getAdapter(ITypeRoot.class);
			return root.getElementAt(offset);
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

	public static void collectProjectClassPath(List<URL> list, IJavaProject project) {
		collectOutputLocationClassPath(list, project);

		try {
			IClasspathEntry[] cp = project.getRawClasspath();
			collectClassPath(list, project, cp);
		} catch (JavaModelException e) {
			LogUtil.logWarn("JdtUtil.collectProjectClassPath()", e);
		}
	}

	private static void collectOutputLocationClassPath(List<URL> list, IJavaProject project) {
		try {
			IPath path = project.getOutputLocation();
			URL url = toURL(project.getProject(), path);
			if (url != null) {
				list.add(url);
			}
		} catch (JavaModelException e) {
		} catch (MalformedURLException e) {
		}
	}

	private static void collectClassPath(List<URL> list, IJavaProject project, IClasspathEntry[] cp) {
		for (IClasspathEntry ce : cp) {
			URL url = null;
			try {
				switch (ce.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					url = toURL(project.getProject(), ce.getOutputLocation());
					break;
				case IClasspathEntry.CPE_VARIABLE:
					url = toURL(project.getProject(), JavaCore.getResolvedVariablePath(ce.getPath()));
					break;
				case IClasspathEntry.CPE_LIBRARY:
					url = toURL(project.getProject(), ce.getPath());
					break;
				case IClasspathEntry.CPE_CONTAINER:
					if (!ce.getPath().toPortableString().contains("JRE_CONTAINER")) {
						IClasspathContainer cr = JavaCore.getClasspathContainer(ce.getPath(), project);
						collectClassPath(list, project, cr.getClasspathEntries());
					}
					break;
				default:
					break;
				}
			} catch (Exception e) {
				LogUtil.logWarn("JdtUtil.getClassPath()", e);
			}
			if (url != null) {
				list.add(url);
			}
		}
	}

	public static URL toURL(IProject project, IPath path) throws MalformedURLException {
		if (path == null) {
			return null;
		}
		if (path.toFile().exists()) {
			URL url = path.toFile().toURI().toURL();
			return url;
		}
		IPath vp = JavaCore.getResolvedVariablePath(path);
		if (vp != null) {
			URL url = vp.toFile().toURI().toURL();
			return url;
		}
		try {
			IFile file = project.getFile(path);
			if (file.exists()) {
				URI uri = file.getLocationURI();
				if (uri != null) {
					return uri.toURL();
				}
			}
		} catch (Exception e) {
			LogUtil.logWarn("JdtUtil.toURL()", e);
		}
		try {
			IFile file = project.getParent().getFile(path);
			if (file.exists()) {
				URI uri = file.getLocationURI();
				if (uri != null) {
					return uri.toURL();
				}
			}
		} catch (Exception e) {
			LogUtil.logWarn("JdtUtil.toURL()", e);
		}
		try {
			IPath raw = project.getRawLocation();
			if (raw != null) {
				File file = raw.append(path).toFile();
				if (file.exists()) {
					return file.toURI().toURL();
				}
				file = raw.removeLastSegments(1).append(path).toFile();
				if (file.exists()) {
					return file.toURI().toURL();
				}
			}
		} catch (Exception e) {
			LogUtil.logWarn("JdtUtil.toURL()", e);
		}
		try {
			IPath root = project.getProject().getWorkspace().getRoot().getRawLocation();
			File file = root.append(path).toFile();
			if (file.exists()) {
				return file.toURI().toURL();
			}
		} catch (Exception e) {
			LogUtil.logWarn("JdtUtil.toURL()", e);
		}
		return null;
	}
}
