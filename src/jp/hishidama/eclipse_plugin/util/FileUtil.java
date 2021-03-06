package jp.hishidama.eclipse_plugin.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Properties;

import jp.hishidama.eclipse_plugin.util.internal.LogUtil;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

public class FileUtil {
	public static String addExtension(String path, String ext) {
		assert ext.startsWith(".");
		if (path.endsWith(ext)) {
			return path;
		}
		if (path.endsWith(".")) {
			return path.substring(0, path.length() - 1) + ext;
		} else {
			return path + ext;
		}
	}

	public static void createFolder(IProject project, IFile file) throws CoreException {
		IPath path = file.getProjectRelativePath().removeLastSegments(1);
		createFolder(project, path);
	}

	public static void createFolder(IProject project, IPath path) throws CoreException {
		IFolder folder = project.getFolder(path);
		if (!folder.exists()) {
			createFolder(project, path.removeLastSegments(1));
			folder.create(false, true, null);
		}
	}

	public static IFile getFile(IEditorPart editor) {
		if (editor != null) {
			IEditorInput input = editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				return ((IFileEditorInput) input).getFile();
			}
		}
		return null;
	}

	public static IFile getFile(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sselection = (IStructuredSelection) selection;
			Object element = sselection.getFirstElement();
			if (element instanceof IFile) {
				return (IFile) element;
			}
			if (element instanceof IJavaElement) {
				element = ((IJavaElement) element).getResource();
			} else if (element instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) element;
				element = adaptable.getAdapter(IResource.class);
			}
			if (element instanceof IFile) {
				return (IFile) element;
			}
		}

		return null;
	}

	public static IFolder getFolder(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sselection = (IStructuredSelection) selection;
			Object element = sselection.getFirstElement();
			if (element instanceof IFolder) {
				return (IFolder) element;
			}
			if (element instanceof IFile) {
				element = ((IFile) element).getParent();
			}
			if (element instanceof IJavaElement) {
				element = ((IJavaElement) element).getResource();
			} else if (element instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) element;
				element = adaptable.getAdapter(IResource.class);
			}
			if (element instanceof IFile) {
				element = ((IFile) element).getParent();
			}
			if (element instanceof IFolder) {
				return (IFolder) element;
			}
		}

		return null;
	}

	public static IContainer getExistingFolder(IResource resource) {
		if (resource == null) {
			return null;
		}
		if (resource.exists()) {
			if (resource instanceof IContainer) {
				return (IContainer) resource;
			}
			return resource.getParent();
		}
		for (IContainer c = resource.getParent(); c != null; c = c.getParent()) {
			if (c.exists()) {
				return c;
			}
		}

		return null;
	}

	public static String getLocation(IFile file) {
		return file.getLocation().toOSString();
	}

	public static boolean exists(IResource file) {
		File f = new File(file.getLocationURI());
		return f.exists();
	}

	public static boolean isFile(IResource file) {
		File f = new File(file.getLocationURI());
		return f.isFile();
	}

	public static boolean openFile(IFile file, String className) {
		return openFile(file, className, null);
	}

	public static boolean openFile(IFile file, String className, String methodName) {
		if (openFile(file.getProject(), className, methodName)) {
			return true;
		}

		return openEditor(file);
	}

	public static boolean openFile(IProject project, String className) {
		return openFile(project, className, null);
	}

	public static boolean openFile(IProject project, String className, String methodName) {
		IType type = findClass(project, className);
		if (type != null) {
			IMethod method = findMethod(type, methodName);
			if (method != null) {
				try {
					JavaUI.openInEditor(method);
					return true;
				} catch (Exception e) {
					// fall through
				}
			}

			try {
				JavaUI.openInEditor(type);
				return true;
			} catch (Exception e) {
				// fall through
			}
		}
		return false;
	}

	private static IType findClass(IProject project, String className) {
		if (project == null || className == null) {
			return null;
		}
		IJavaProject javaProject = JavaCore.create(project);
		if (javaProject != null) {
			try {
				IType type = javaProject.findType(className);
				return type;
			} catch (Exception e) {
				// fall through
			}
		}
		return null;
	}

	private static IMethod findMethod(IType type, String methodName) {
		if (methodName == null) {
			return null;
		}
		try {
			IMethod[] ms = type.getMethods();
			for (IMethod m : ms) {
				if (m.getElementName().equals(methodName)) {
					return m;
				}
			}
		} catch (JavaModelException e) {
			// fall through
		}
		return null;
	}

	public static ITextEditor findTextEditor(IFile file) {
		if (file.exists()) {
			try {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IEditorInput input = new FileEditorInput(file);
				IEditorPart part = page.findEditor(input);
				if (part instanceof ITextEditor) {
					return (ITextEditor) part;
				}
			} catch (Exception e) {
				// fall through
			}
		}
		return null;
	}

	public static boolean openEditor(IFile file) {
		if (file.exists()) {
			try {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IDE.openEditor(page, file);
				return true;
			} catch (Exception e) {
				// fall through
			}
		}
		return false;
	}

	public static boolean openSystemEditor(IFile file) {
		if (file.exists()) {
			try {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IDE.openEditor(page, file, IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
				return true;
			} catch (Exception e) {
				// fall through
			}
		}
		return false;
	}

	public static void save(IFile file, String contents) throws CoreException {
		ByteArrayInputStream is;
		try {
			is = new ByteArrayInputStream(contents.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		if (file.exists()) {
			file.setContents(is, true, false, null);
		} else {
			file.create(is, true, null);
		}
	}

	public static StringBuilder load(IFile file) throws CoreException {
		InputStream is = file.getContents();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			try {
				StringBuilder sb = new StringBuilder(1024);
				for (;;) {
					String s = br.readLine();
					if (s == null) {
						break;
					}
					sb.append(s);
					sb.append("\n");
				}
				return sb;
			} catch (IOException e) {
				IStatus status = LogUtil.errorStatus("read error", e);
				throw new CoreException(status);
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					LogUtil.warnStatus("load()", e);
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				LogUtil.warnStatus("load()", e);
			}
		}
	}

	public static Properties loadProperties(IProject project, String fname) throws IOException {
		if (fname == null) {
			throw new FileNotFoundException("null");
		}

		IFile file = null;
		try {
			file = project.getFile(fname);
		} catch (Exception e) {
			LogUtil.warnStatus("loadProperties()", e);
		}
		if (file == null || !file.exists()) {
			LogUtil.logWarn(MessageFormat.format("not found property file. file={0}", fname));
			throw new FileNotFoundException(fname);
		}

		InputStream is = null;
		Reader reader = null;
		try {
			is = file.getContents();
			String cs;
			try {
				cs = file.getCharset();
			} catch (Exception e) {
				cs = "UTF-8";
			}
			reader = new InputStreamReader(is, cs);
			Properties p = new Properties();
			p.load(reader);
			return p;
		} catch (CoreException e) {
			LogUtil.log(e.getStatus());
			throw new IOException(e);
		} catch (IOException e) {
			LogUtil.logWarn(MessageFormat.format("property file read error. file={0}", fname), e);
			throw e;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					LogUtil.warnStatus("loadProperties()", e);
				}
			} else if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					LogUtil.warnStatus("loadProperties()", e);
				}
			}
		}
	}

	public static boolean copy(IFile src, IFile dst) {
		try {
			createFolder(dst.getProject(), dst);
			dst.create(src.getContents(), true, null);
			return true;
		} catch (CoreException e) {
			LogUtil.logWarn("copy error.", e);
			return false;
		}
	}
}
