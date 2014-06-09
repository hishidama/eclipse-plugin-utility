package jp.hishidama.eclipse_plugin.dialog;

import java.util.HashSet;
import java.util.Set;

import jp.hishidama.eclipse_plugin.util.StringUtil;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ProjectFileSelectionDialog extends ElementTreeSelectionDialog {

	private IProject project;

	public ProjectFileSelectionDialog(Shell parent, IProject project) {
		this(parent, project, new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
	}

	public ProjectFileSelectionDialog(Shell parent, IProject project, ILabelProvider labelProvider,
			ITreeContentProvider contentProvider) {
		super(parent, labelProvider, contentProvider);
		setInput(project);
	}

	@Override
	public final void setInput(Object input) {
		setInput((IProject) input);
	}

	public void setInput(IProject input) {
		this.project = input;
		super.setInput(input);
	}

	@Override
	public final void setInitialSelection(Object initialPath) {
		setInitialSelection((String) initialPath);
	}

	public void setInitialSelection(String initialPath) {
		if (StringUtil.isEmpty(initialPath)) {
			return;
		}

		IPath proj = project.getLocation();
		IPath path = proj.append(initialPath);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IPath rel = path.makeRelativeTo(root.getLocation());
		IResource r = root.findMember(rel);
		if (r != null) {
			super.setInitialSelection(r);
			return;
		}
		IFile file = root.getFile(rel);
		setInitialSelection(file);
	}

	public void setInitialSelection(IResource file) {
		if (file != null) {
			if (file.exists()) {
				super.setInitialSelection(file);
			} else {
				for (IContainer folder = file.getParent(); folder != null; folder = folder.getParent()) {
					if (folder.exists()) {
						super.setInitialSelection(folder);
						break;
					}
				}
			}
		}
	}

	public void addFileterExtension(String... ext) {
		final Set<String> set = new HashSet<String>(ext.length);
		for (String s : ext) {
			set.add(s);
		}

		addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IFile) {
					IFile file = (IFile) element;
					return set.contains(file.getFileExtension());
				}
				return true;
			}
		});
	}

	protected final Object[] getOriginalResult() {
		return super.getResult();
	}

	@Override
	public String[] getResult() {
		Object[] result = getOriginalResult();
		if (result == null) {
			return null;
		}
		String[] r = new String[result.length];
		for (int i = 0; i < result.length; i++) {
			IResource file = (IResource) result[i];
			r[i] = convert(file);
		}
		return r;
	}

	protected String convert(IResource file) {
		IPath path = file.getLocation();
		IPath base = project.getLocation();
		IPath rel = path.makeRelativeTo(base);
		return rel.toPortableString();
	}
}
