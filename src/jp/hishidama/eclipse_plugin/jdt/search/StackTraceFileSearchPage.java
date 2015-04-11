package jp.hishidama.eclipse_plugin.jdt.search;

import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.internal.ui.text.LineElement;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;

// @see org.eclipse.search.internal.ui.text.TextSearchPage
@SuppressWarnings("restriction")
public class StackTraceFileSearchPage extends DialogPage implements ISearchPage {
	public static final String ID = "jp.hishidama.eclipse_plugin.jdt.search.stackTraceFileSearchPage"; //$NON-NLS-1$

	private Text fStackTraceText;

	private boolean fSearchDerived = false;

	private ISearchPageContainer fContainer;

	@Override
	public void setContainer(ISearchPageContainer container) {
		this.fContainer = container;
	}

	private ISearchPageContainer getContainer() {
		return fContainer;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(parent.getFont());
		GridLayout layout = new GridLayout(1, false);
		composite.setLayout(layout);

		addStackTraceTextControls(composite);

		Label separator = new Label(composite, SWT.NONE);
		separator.setVisible(false);
		GridData data = new GridData(GridData.FILL, GridData.FILL, false, false, 1, 1);
		data.heightHint = convertHeightInCharsToPixels(1) / 3;
		separator.setLayoutData(data);

		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	private void addStackTraceTextControls(Composite group) {
		// Info text
		Label label = new Label(group, SWT.LEAD);
		label.setText("Java StackTrace:");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		label.setFont(group.getFont());

		// StackTrace input area
		fStackTraceText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 128;
		fStackTraceText.setLayoutData(data);
	}

	@Override
	public boolean performAction() {
		StackTraceFileSearchData data = createSearchData();
		FileTextSearchScope scope = createTextSearchScope();
		StackTraceFileSearchQuery query = new StackTraceFileSearchQuery(data, scope);
		NewSearchUI.runQueryInBackground(query);
		return true;
	}

	private StackTraceFileSearchData createSearchData() {
		StackTraceFileSearchData data = new StackTraceFileSearchData();

		String text = fStackTraceText.getText();
		String[] ss = text.split("[\r\n]+");
		Pattern pattern = Pattern
				.compile("(.*\\s|^)(?<qualifier>.+)\\s*\\.\\s*(?<simpleName>.+)\\s*\\.\\s*(?<method>.+)\\s*\\(\\s*(?<file>.+\\.java)\\s*\\:\\s*(?<line>\\d+)\\s*\\).*");

		for (String s : ss) {
			Matcher matcher = pattern.matcher(s);
			if (matcher.matches()) {
				String qualifier = matcher.group("qualifier");
				String file = matcher.group("file");
				int line = Integer.parseInt(matcher.group("line"));
				data.add(qualifier, file, line);
			}
		}
		return data;
	}

	protected String[] getFileNames() {
		return new String[] { "*.java" };
	}

	public FileTextSearchScope createTextSearchScope() {
		// Setup search scope
		switch (getContainer().getSelectedScope()) {
		case ISearchPageContainer.WORKSPACE_SCOPE:
			return FileTextSearchScope.newWorkspaceScope(getFileNames(), fSearchDerived);
		case ISearchPageContainer.SELECTION_SCOPE:
			return getSelectedResourcesScope();
		case ISearchPageContainer.SELECTED_PROJECTS_SCOPE:
			return getEnclosingProjectScope();
		case ISearchPageContainer.WORKING_SET_SCOPE:
			IWorkingSet[] workingSets = getContainer().getSelectedWorkingSets();
			return FileTextSearchScope.newSearchScope(workingSets, getFileNames(), fSearchDerived);
		default:
			// unknown scope
			return FileTextSearchScope.newWorkspaceScope(getFileNames(), fSearchDerived);
		}
	}

	private FileTextSearchScope getSelectedResourcesScope() {
		HashSet<IResource> resources = new HashSet<IResource>();
		ISelection sel = getContainer().getSelection();
		if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
			Iterator<?> iter = ((IStructuredSelection) sel).iterator();
			while (iter.hasNext()) {
				Object curr = iter.next();
				if (curr instanceof IWorkingSet) {
					IWorkingSet workingSet = (IWorkingSet) curr;
					if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
						return FileTextSearchScope.newWorkspaceScope(getFileNames(), fSearchDerived);
					}
					IAdaptable[] elements = workingSet.getElements();
					for (int i = 0; i < elements.length; i++) {
						IResource resource = (IResource) elements[i].getAdapter(IResource.class);
						if (resource != null && resource.isAccessible()) {
							resources.add(resource);
						}
					}
				} else if (curr instanceof LineElement) {
					IResource resource = ((LineElement) curr).getParent();
					if (resource != null && resource.isAccessible())
						resources.add(resource);
				} else if (curr instanceof IAdaptable) {
					IResource resource = (IResource) ((IAdaptable) curr).getAdapter(IResource.class);
					if (resource != null && resource.isAccessible()) {
						resources.add(resource);
					}
				}
			}
		} else if (getContainer().getActiveEditorInput() != null) {
			resources.add((IFile) getContainer().getActiveEditorInput().getAdapter(IFile.class));
		}
		IResource[] arr = (IResource[]) resources.toArray(new IResource[resources.size()]);
		return FileTextSearchScope.newSearchScope(arr, getFileNames(), fSearchDerived);
	}

	private FileTextSearchScope getEnclosingProjectScope() {
		String[] enclosingProjectName = getContainer().getSelectedProjectNames();
		if (enclosingProjectName == null) {
			return FileTextSearchScope.newWorkspaceScope(getFileNames(), fSearchDerived);
		}

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource[] res = new IResource[enclosingProjectName.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = root.getProject(enclosingProjectName[i]);
		}

		return FileTextSearchScope.newSearchScope(res, getFileNames(), fSearchDerived);
	}
}
