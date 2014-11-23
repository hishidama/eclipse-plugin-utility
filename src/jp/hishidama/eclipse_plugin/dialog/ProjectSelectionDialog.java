package jp.hishidama.eclipse_plugin.dialog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ProjectSelectionDialog extends ElementTreeSelectionDialog {

	public ProjectSelectionDialog(Shell parent) {
		this(parent, new WorkbenchLabelProvider(), new ProjectContentProvider());
	}

	public ProjectSelectionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		setInput(root);

		setTitle("Project");
		setMessage("Select project.");
	}

	@Override
	public void setInitialSelection(Object initialPath) {
		if (initialPath == null) {
			return;
		}

		IProject project;
		if (initialPath instanceof IProject) {
			project = (IProject) initialPath;
		} else {
			String name = (String) initialPath;
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			project = root.getProject(name);
		}

		super.setInitialSelection(project);
	}

	protected static class ProjectContentProvider extends BaseWorkbenchContentProvider {

		@Override
		public Object[] getElements(Object element) {
			IWorkspaceRoot root = (IWorkspaceRoot) element;
			IProject[] projects = root.getProjects();
			List<Object> list = new ArrayList<Object>(projects.length);
			for (IProject project : projects) {
				if (project.isOpen()) {
					list.add(project);
				}
			}
			return list.toArray();
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	}

	public IProject getResultProject() {
		Object[] result = super.getResult();
		if (result == null || result.length == 0) {
			return null;
		}
		for (Object object : result) {
			if (object instanceof IProject) {
				return (IProject) object;
			}
		}
		return null;
	}
}
