package jp.hishidama.eclipse_plugin.jdt.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.SearchUtil;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.internal.core.text.PatternConstructor;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

// @see org.eclipse.search.internal.ui.text.TextSearchPage
@SuppressWarnings("restriction")
public class JavaStringSearchPage extends DialogPage implements ISearchPage {
	public static final String ID = "jp.hishidama.eclipse_plugin.jdt.search.javaStringSearchPage"; //$NON-NLS-1$
	private static final int HISTORY_SIZE = 12;

	private static final String PAGE_NAME = "JavaStringSearchPage"; //$NON-NLS-1$
	private static final String STORE_CASE_SENSITIVE = "CASE_SENSITIVE"; //$NON-NLS-1$
	private static final String STORE_IS_REG_EX_SEARCH = "REG_EX_SEARCH"; //$NON-NLS-1$
	private static final String STORE_IS_WHOLE_WORD = "WHOLE_WORD"; //$NON-NLS-1$
	private static final String STORE_HISTORY = "HISTORY"; //$NON-NLS-1$
	private static final String STORE_HISTORY_SIZE = "HISTORY_SIZE"; //$NON-NLS-1$

	private List<JavaStringSearchData> fPreviousSearchPatterns = new ArrayList<JavaStringSearchData>(HISTORY_SIZE);

	private boolean fFirstTime = true;
	private boolean fIsCaseSensitive;
	private boolean fIsRegExSearch;
	private boolean fIsWholeWord;

	private Combo fPattern;
	private Button fIsCaseSensitiveCheckbox;
	private Button fIsRegExCheckbox;
	private Button fIsWholeWordCheckbox;
	private CLabel fStatusLabel;
	private ContentAssistCommandAdapter fPatterFieldContentAssist;

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
		readConfiguration();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);

		addTextPatternControls(composite);

		Label separator = new Label(composite, SWT.NONE);
		separator.setVisible(false);
		GridData data = new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
		data.heightHint = convertHeightInCharsToPixels(1) / 3;
		separator.setLayoutData(data);

		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	private void addTextPatternControls(Composite group) {
		// grid layout with 2 columns

		// Info text
		Label label = new Label(group, SWT.LEAD);
		label.setText(SearchMessages.SearchPage_containingText_text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		label.setFont(group.getFont());

		// Pattern combo
		fPattern = new Combo(group, SWT.SINGLE | SWT.BORDER);
		// Not done here to prevent page from resizing
		// fPattern.setItems(getPreviousSearchPatterns());
		fPattern.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected();
				updateOKStatus();
			}
		});
		// add some listeners for regex syntax checking
		fPattern.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateOKStatus();
			}
		});
		fPattern.setFont(group.getFont());
		GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, 1, 2);
		data.widthHint = convertWidthInCharsToPixels(50);
		fPattern.setLayoutData(data);

		ComboContentAdapter contentAdapter = new ComboContentAdapter();
		FindReplaceDocumentAdapterContentProposalProvider findProposer = new FindReplaceDocumentAdapterContentProposalProvider(
				true);
		fPatterFieldContentAssist = new ContentAssistCommandAdapter(fPattern, contentAdapter, findProposer,
				ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[0], true);
		fPatterFieldContentAssist.setEnabled(fIsRegExSearch);

		fIsCaseSensitiveCheckbox = new Button(group, SWT.CHECK);
		fIsCaseSensitiveCheckbox.setText(SearchMessages.SearchPage_caseSensitive);
		fIsCaseSensitiveCheckbox.setSelection(fIsCaseSensitive);
		fIsCaseSensitiveCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fIsCaseSensitive = fIsCaseSensitiveCheckbox.getSelection();
			}
		});
		fIsCaseSensitiveCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		fIsCaseSensitiveCheckbox.setFont(group.getFont());

		// RegEx checkbox
		fIsRegExCheckbox = new Button(group, SWT.CHECK);
		fIsRegExCheckbox.setText(SearchMessages.SearchPage_regularExpression);
		fIsRegExCheckbox.setSelection(fIsRegExSearch);

		fIsRegExCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fIsRegExSearch = fIsRegExCheckbox.getSelection();
				updateOKStatus();

				writeConfiguration();
				fPatterFieldContentAssist.setEnabled(fIsRegExSearch);
				fIsWholeWordCheckbox.setEnabled(!fIsRegExSearch);
			}
		});
		fIsRegExCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		fIsRegExCheckbox.setFont(group.getFont());

		// Text line which explains the special characters
		fStatusLabel = new CLabel(group, SWT.LEAD);
		fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 2));
		fStatusLabel.setFont(group.getFont());
		fStatusLabel.setAlignment(SWT.LEFT);
		fStatusLabel.setText(SearchMessages.SearchPage_containingText_hint);

		// Whole Word checkbox
		fIsWholeWordCheckbox = new Button(group, SWT.CHECK);
		fIsWholeWordCheckbox.setText(SearchMessages.SearchPage_wholeWord);
		fIsWholeWordCheckbox.setSelection(fIsWholeWord);
		fIsWholeWordCheckbox.setEnabled(!fIsRegExSearch);
		fIsWholeWordCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fIsWholeWord = fIsWholeWordCheckbox.getSelection();
			}
		});
		fIsWholeWordCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		fIsWholeWordCheckbox.setFont(group.getFont());
	}

	private void handleWidgetSelected() {
		int selectionIndex = fPattern.getSelectionIndex();
		if (selectionIndex < 0 || selectionIndex >= fPreviousSearchPatterns.size())
			return;

		JavaStringSearchData patternData = fPreviousSearchPatterns.get(selectionIndex);
		if (!fPattern.getText().equals(patternData.getTextPattern())) {
			return;
		}
		fIsCaseSensitiveCheckbox.setSelection(patternData.isCaseSensitive());
		fIsRegExSearch = patternData.isRegExSearch();
		fIsRegExCheckbox.setSelection(fIsRegExSearch);
		fIsWholeWord = patternData.isWholeWord();
		fIsWholeWordCheckbox.setSelection(fIsWholeWord);
		fIsWholeWordCheckbox.setEnabled(!fIsRegExSearch);
		fPattern.setText(patternData.getTextPattern());
		fPatterFieldContentAssist.setEnabled(fIsRegExSearch);
		if (patternData.getWorkingSets() != null) {
			getContainer().setSelectedWorkingSets(patternData.getWorkingSets());
		} else {
			getContainer().setSelectedScope(patternData.getScope());
		}
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible && fPattern != null) {
			if (fFirstTime) {
				fFirstTime = false;
				// Set item and text here to prevent page from resizing
				fPattern.setItems(getPreviousSearchPatterns());
				if (!initializePatternControl()) {
					fPattern.select(0);
					handleWidgetSelected();
				}
			}
			fPattern.setFocus();
		}
		updateOKStatus();

		IEditorInput editorInput = getContainer().getActiveEditorInput();
		getContainer().setActiveEditorCanProvideScopeSelection(
				editorInput != null && editorInput.getAdapter(IFile.class) != null);

		super.setVisible(visible);
	}

	private String[] getPreviousSearchPatterns() {
		int size = fPreviousSearchPatterns.size();
		String[] patterns = new String[size];
		for (int i = 0; i < size; i++) {
			patterns[i] = fPreviousSearchPatterns.get(i).getTextPattern();
		}
		return patterns;
	}

	private boolean initializePatternControl() {
		ISelection selection = getSelection();
		if (selection instanceof ITextSelection && !selection.isEmpty() && ((ITextSelection) selection).getLength() > 0) {
			String text = ((ITextSelection) selection).getText();
			if (text != null) {
				if (fIsRegExSearch) {
					fPattern.setText(FindReplaceDocumentAdapter.escapeForRegExPattern(text));
				} else {
					fPattern.setText(insertEscapeChars(text));
				}
				return true;
			}
		}
		return false;
	}

	private ISelection getSelection() {
		return fContainer.getSelection();
	}

	private String insertEscapeChars(String text) {
		if (text == null || text.equals("")) { //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		}
		StringBuilder sbIn = new StringBuilder(text);
		BufferedReader reader = new BufferedReader(new StringReader(text));
		int lengthOfFirstLine = 0;
		try {
			lengthOfFirstLine = reader.readLine().length();
		} catch (IOException ex) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder sbOut = new StringBuilder(lengthOfFirstLine + 5);
		int i = 0;
		while (i < lengthOfFirstLine) {
			char ch = sbIn.charAt(i);
			if (ch == '*' || ch == '?' || ch == '\\') {
				sbOut.append("\\"); //$NON-NLS-1$
			}
			sbOut.append(ch);
			i++;
		}
		return sbOut.toString();
	}

	final void updateOKStatus() {
		boolean regexStatus = validateRegex();
		getContainer().setPerformActionEnabled(regexStatus);
	}

	private boolean validateRegex() {
		if (fIsRegExCheckbox.getSelection()) {
			try {
				PatternConstructor.createPattern(fPattern.getText(), fIsCaseSensitive, true);
			} catch (PatternSyntaxException e) {
				String locMessage = e.getLocalizedMessage();
				int i = 0;
				while (i < locMessage.length() && "\n\r".indexOf(locMessage.charAt(i)) == -1) { //$NON-NLS-1$
					i++;
				}
				statusMessage(true, locMessage.substring(0, i));
				return false;
			}
			statusMessage(false, ""); //$NON-NLS-1$
		} else {
			statusMessage(false, SearchMessages.SearchPage_containingText_hint);
		}
		return true;
	}

	private void statusMessage(boolean error, String message) {
		fStatusLabel.setText(message);
		if (error) {
			fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel.getDisplay()));
		} else {
			fStatusLabel.setForeground(null);
		}
	}

	@Override
	public boolean performAction() {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInBackground(newQuery());
		return true;
	}

	private ISearchQuery newQuery() {
		initializeScope();
		JavaStringSearchData data = createData();
		return new JavaStringSearchQuery(data, scope, scopeDescription);
	}

	private IJavaSearchScope scope;
	private String scopeDescription;

	private void initializeScope() {
		JavaSearchScopeFactory factory = JavaSearchScopeFactory.getInstance();
		int includeMask = JavaSearchScopeFactory.PROJECTS | JavaSearchScopeFactory.SOURCES;

		switch (getContainer().getSelectedScope()) {
		default:
			scopeDescription = factory.getWorkspaceScopeDescription(includeMask);
			scope = factory.createWorkspaceScope(includeMask);
			break;
		case ISearchPageContainer.SELECTION_SCOPE:
			IJavaElement[] javaElements = new IJavaElement[0];
			if (getContainer().getActiveEditorInput() != null) {
				IFile file = (IFile) getContainer().getActiveEditorInput().getAdapter(IFile.class);
				if (file != null && file.exists()) {
					IJavaElement javaElement = JavaCore.create(file);
					if (javaElement != null) {
						javaElements = new IJavaElement[] { javaElement };
					}
				}
			} else {
				javaElements = factory.getJavaElements(getContainer().getSelection());
			}
			scope = factory.createJavaSearchScope(javaElements, includeMask);
			scopeDescription = factory.getSelectionScopeDescription(javaElements, includeMask);
			break;
		case ISearchPageContainer.SELECTED_PROJECTS_SCOPE: {
			String[] projectNames = getContainer().getSelectedProjectNames();
			scope = factory.createJavaProjectSearchScope(projectNames, includeMask);
			scopeDescription = factory.getProjectScopeDescription(projectNames, includeMask);
			break;
		}
		case ISearchPageContainer.WORKING_SET_SCOPE: {
			IWorkingSet[] workingSets = getContainer().getSelectedWorkingSets();
			// should not happen - just to be sure
			if (workingSets == null || workingSets.length < 1) {
				return;
			}
			scopeDescription = factory.getWorkingSetScopeDescription(workingSets, includeMask);
			scope = factory.createJavaSearchScope(workingSets, includeMask);
			SearchUtil.updateLRUWorkingSets(workingSets);
			break;
		}
		}
	}

	private JavaStringSearchData createData() {
		JavaStringSearchData match = findInPrevious(fPattern.getText());
		if (match != null) {
			fPreviousSearchPatterns.remove(match);
		}
		match = new JavaStringSearchData(getPattern(), isCaseSensitive(), fIsRegExCheckbox.getSelection(),
				fIsWholeWordCheckbox.getSelection(), getContainer().getSelectedScope(), getContainer()
						.getSelectedWorkingSets());
		fPreviousSearchPatterns.add(0, match);
		return match;
	}

	private JavaStringSearchData findInPrevious(String pattern) {
		for (JavaStringSearchData element : fPreviousSearchPatterns) {
			if (pattern.equals(element.getTextPattern())) {
				return element;
			}
		}
		return null;
	}

	private String getPattern() {
		return fPattern.getText();
	}

	private boolean isCaseSensitive() {
		return fIsCaseSensitiveCheckbox.getSelection();
	}

	@Override
	public void dispose() {
		writeConfiguration();
		super.dispose();
	}

	private void readConfiguration() {
		IDialogSettings s = getDialogSettings();
		fIsCaseSensitive = s.getBoolean(STORE_CASE_SENSITIVE);
		fIsRegExSearch = s.getBoolean(STORE_IS_REG_EX_SEARCH);
		fIsWholeWord = s.getBoolean(STORE_IS_WHOLE_WORD);

		try {
			int historySize = s.getInt(STORE_HISTORY_SIZE);
			for (int i = 0; i < historySize; i++) {
				IDialogSettings histSettings = s.getSection(STORE_HISTORY + i);
				if (histSettings != null) {
					JavaStringSearchData data = JavaStringSearchData.create(histSettings);
					if (data != null) {
						fPreviousSearchPatterns.add(data);
					}
				}
			}
		} catch (NumberFormatException e) {
			// ignore
		}
	}

	private void writeConfiguration() {
		IDialogSettings s = getDialogSettings();
		s.put(STORE_CASE_SENSITIVE, fIsCaseSensitive);
		s.put(STORE_IS_REG_EX_SEARCH, fIsRegExSearch);
		s.put(STORE_IS_WHOLE_WORD, fIsWholeWord);

		int historySize = Math.min(fPreviousSearchPatterns.size(), HISTORY_SIZE);
		s.put(STORE_HISTORY_SIZE, historySize);
		for (int i = 0; i < historySize; i++) {
			IDialogSettings histSettings = s.addNewSection(STORE_HISTORY + i);
			JavaStringSearchData data = fPreviousSearchPatterns.get(i);
			data.store(histSettings);
		}
	}

	private IDialogSettings getDialogSettings() {
		return SearchPlugin.getDefault().getDialogSettingsSection(PAGE_NAME);
	}
}
