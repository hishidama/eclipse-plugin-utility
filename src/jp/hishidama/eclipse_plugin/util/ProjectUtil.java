package jp.hishidama.eclipse_plugin.util;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class ProjectUtil {

	public static IProject getProject(String projectName) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(projectName);
		if (project.exists()) {
			return project;
		}
		return null;
	}

	public static IProject getProject(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof TextSelection) {
			IEditorPart editor = HandlerUtil.getActiveEditor(event);
			IFile file = FileUtil.getFile(editor);
			if (file != null) {
				return file.getProject();
			}
		} else {
			IProject project = getProject(selection);
			if (project != null) {
				return project;
			}
		}

		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		IFile file = FileUtil.getFile(editor);
		if (file != null) {
			return file.getProject();
		}

		return null;
	}

	public static IProject getProject(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sselection = (IStructuredSelection) selection;
			Object element = sselection.getFirstElement();
			if (element instanceof IResource) {
				return ((IResource) element).getProject();
			} else if (element instanceof IJavaElement) {
				return ((IJavaElement) element).getJavaProject().getProject();
			} else if (element instanceof IAdaptable) {
				IAdaptable adaptable = (IAdaptable) element;
				return (IProject) adaptable.getAdapter(IProject.class);
			}
		}

		return null;
	}
}
