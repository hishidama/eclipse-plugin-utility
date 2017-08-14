package jp.hishidama.eclipse_plugin.wizard.page;

import java.util.Iterator;

import jp.hishidama.eclipse_plugin.util.FileUtil;
import jp.hishidama.eclipse_plugin.util.ProjectUtil;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

// @see org.eclipse.ui.dialogs.WizardNewFileCreationPage
public class ProjectFileSelectionWizardPage extends EditWizardPage {
	private IStructuredSelection initialSelection;
	private IProject project;
	private boolean check;
	private boolean multi;

	private Text folderText;
	private TreeViewer folderViewer;
	private Text fileText;

	public ProjectFileSelectionWizardPage(IStructuredSelection selection) {
		this("ProjectFileSelectionWizardPage", "File", "Select a existing file or input a new file name.", selection);
	}

	public ProjectFileSelectionWizardPage(String pageName, String title, String description, IStructuredSelection selection) {
		super(pageName);
		this.initialSelection = selection;
		this.project = ProjectUtil.getProject(selection);
		this.check = false;
		this.multi = false;

		setTitle(title);
		setDescription(description);
	}

	public void setMultiSelection(boolean multi) {
		this.multi = multi;
	}

	@Override
	protected Composite createComposite(Composite parent) {
		Font font = parent.getFont();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		composite.setFont(font);

		createLabel(composite, "Enter or select the parent folder:").setFont(font);
		folderText = createText(composite, 1);

		int style = SWT.BORDER;
		if (multi) {
			style |= SWT.MULTI;
		}
		folderViewer = check ? new CheckboxTreeViewer(composite, style) : new TreeViewer(composite, style);
		{
			folderViewer.getTree().setFont(composite.getFont());
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.heightHint = 150;
			folderViewer.getTree().setLayoutData(data);
			folderViewer.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
			folderViewer.setFilters(new ViewerFilter[] { getViewerFilter() });
			folderViewer.setContentProvider(getContentProvider());
			folderViewer.setComparator(new ViewerComparator() {
				@Override
				public int category(Object element) {
					if (element instanceof IFile) {
						return 1;
					}
					return 0;
				}
			});
			if (project != null) {
				folderViewer.setInput(project);
			} else {
				folderViewer.setInput(ResourcesPlugin.getWorkspace());
			}
			folderViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					ISelection selection = event.getSelection();
					setFileText(selection, false);
				}
			});
		}

		Composite field = new Composite(composite, SWT.NONE);
		{
			field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			GridLayout layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			field.setLayout(layout);
			createLabel(field, "File name:").setFont(font);
			fileText = createText(field, 1);
		}

		setFileText(initialSelection, true);

		return composite;
	}

	protected IStructuredContentProvider getContentProvider() {
		return new WorkbenchContentProvider();
	}

	protected ViewerFilter getViewerFilter() {
		return new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return filterResource((IResource) element);
			}
		};
	}

	protected boolean filterResource(IResource resource) {
		if (resource.getName().startsWith(".")) {
			return false;
		}
		if (resource instanceof IFile) {
			return filterFile((IFile) resource);
		}
		return true;
	}

	protected boolean filterFile(IFile file) {
		return true; // do override
	}

	protected void setFileText(IStructuredSelection selection, boolean expand) {
		if (multi) {
			for (Iterator<?> i = selection.iterator(); i.hasNext();) {
				Object element = i.next();
				ISelection s = new StructuredSelection(element);
				setFileText(s, expand);
			}
			folderViewer.setSelection(selection);
		} else {
			setFileText((ISelection) selection, expand);
		}
	}

	protected void setFileText(ISelection selection, boolean expand) {
		IFile file = FileUtil.getFile(selection);
		if (file != null) {
			setFileText(file);
			if (expand) {
				if (file.exists()) {
					folderViewer.setSelection(new StructuredSelection(file), true);
				} else {
					IContainer container = FileUtil.getExistingFolder(file);
					if (container != null) {
						folderViewer.expandToLevel(container, 1);
						folderViewer.setSelection(new StructuredSelection(container), false);
					}
				}
			}
		} else {
			IFolder folder = FileUtil.getFolder(selection);
			if (expand) {
				IContainer container = FileUtil.getExistingFolder(folder);
				if (container != null) {
					folderViewer.expandToLevel(container, 1);
					folderViewer.setSelection(new StructuredSelection(container), false);
				}
			}
			setFolderText(folder);
		}
	}

	protected void setFileText(IFile file) {
		if (file != null) {
			setFolderText(file.getParent());
			fileText.setText(file.getName());
		}
	}

	protected void setFolderText(IContainer container) {
		if (container != null) {
			String s = container.getFullPath().toPortableString();
			if (s.startsWith("/")) {
				s = s.substring(1);
			}
			folderText.setText(s);
		}
	}

	@Override
	protected String validate() {
		if (folderText != null && fileText != null) {
			if (folderText.getText().trim().isEmpty()) {
				return "No folder specified.";
			}
			if (fileText.getText().trim().isEmpty()) {
				return "Name cannot be empty.";
			}

			IFile file = getSelectedFile();
			if (file == null) {
				return "Invalid folder or file name.";
			}
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IStatus result = workspace.validatePath(file.getFullPath().toString(), IResource.FILE);
			if (!result.isOK()) {
				return result.getMessage();
			}

			String message = validateFile(file);
			if (message != null) {
				return message;
			}
		}

		return null;
	}

	protected String validateFile(IFile file) {
		return null; // do override
	}

	public IFile getSelectedFile() {
		try {
			String s = folderText.getText().trim();
			if (s.isEmpty()) {
				return null;
			}
			String f = fileText.getText().trim();
			if (f.isEmpty()) {
				return null;
			}

			IPath path = Path.fromPortableString(s);
			if (path.segmentCount() == 1) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
				IFile file = project.getFile(f);
				return file;
			}
			IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
			IFile file = folder.getFile(f);
			return file;
		} catch (Exception e) {
			return null;
		}
	}
}
