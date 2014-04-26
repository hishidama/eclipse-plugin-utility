package jp.hishidama.eclipse_plugin.wizard.page;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

/**
 * @see org.eclipse.jdt.ui.wizards.NewClassWizardPage
 */
@SuppressWarnings("restriction")
public class NewClassWizardPage extends NewTypeWizardPage {

	private final static String SETTINGS_PAGE_NAME = "NewClassWizardPage"; //$NON-NLS-1$

	private final static String SETTINGS_CREATECONSTR = "create_constructor"; //$NON-NLS-1$
	private final static String SETTINGS_CREATEUNIMPLEMENTED = "create_unimplemented"; //$NON-NLS-1$

	private SelectionButtonDialogFieldGroup fMethodStubsButtons;

	/**
	 * Creates a new <code>NewClassWizardPage</code>
	 */
	public NewClassWizardPage(String pageName) {
		super(true, pageName);

		String[] buttonNames3 = { NewWizardMessages.NewClassWizardPage_methods_main,
				NewWizardMessages.NewClassWizardPage_methods_constructors,
				NewWizardMessages.NewClassWizardPage_methods_inherited };
		fMethodStubsButtons = new SelectionButtonDialogFieldGroup(SWT.CHECK, buttonNames3, 1);
		fMethodStubsButtons.setLabelText(NewWizardMessages.NewClassWizardPage_methods_label);
	}

	// -------- Initialization ---------

	/**
	 * The wizard owning this page is responsible for calling this method with
	 * the current selection. The selection is used to initialize the fields of
	 * the wizard page.
	 * 
	 * @param selection
	 *            used to initialize the fields
	 */
	public void init(IStructuredSelection selection) {
		IJavaElement jelem = getInitialJavaElement(selection);
		initContainerPage(jelem);
		initTypePage(jelem);
		doStatusUpdate();

		init();
	}

	protected void init() {
		initMethodStubSelection();
	}

	protected void initMethodStubSelection() {
		boolean createMain = false;
		boolean createConstructors = false;
		boolean createUnimplemented = true;
		IDialogSettings dialogSettings = getDialogSettings();
		if (dialogSettings != null) {
			IDialogSettings section = dialogSettings.getSection(SETTINGS_PAGE_NAME);
			if (section != null) {
				createConstructors = section.getBoolean(SETTINGS_CREATECONSTR);
				createUnimplemented = section.getBoolean(SETTINGS_CREATEUNIMPLEMENTED);
			}
		}

		setMethodStubSelection(createMain, createConstructors, createUnimplemented, true);
	}

	// ------ validation --------
	private void doStatusUpdate() {
		// status of all used components
		IStatus[] status = getAllComponentStatus();

		// the mode severe status will be displayed and the OK button
		// enabled/disabled.
		updateStatus(status);
	}

	protected IStatus[] getAllComponentStatus() {
		// status of all used components
		IStatus[] status = { fContainerStatus, isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus,
				fTypeNameStatus, fModifierStatus, fSuperClassStatus, fSuperInterfacesStatus };
		return status;
	}

	/*
	 * @see NewContainerWizardPage#handleFieldChanged
	 */
	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);

		doStatusUpdate();
	}

	// ------ UI --------

	/*
	 * @see WizardPage#createControl
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());

		int nColumns = 4;

		GridLayout layout = new GridLayout();
		layout.numColumns = nColumns;
		composite.setLayout(layout);

		// pick & choose the wanted UI components

		createContainerControls(composite, nColumns);
		createPackageControls(composite, nColumns);
		createEnclosingTypeControls(composite, nColumns);

		createSeparator(composite, nColumns);

		createTypeNameControls(composite, nColumns);
		createModifierControls(composite, nColumns);

		createSuperClassControls(composite, nColumns);
		createSuperInterfacesControls(composite, nColumns);

		createMethodStubSelectionControls(composite, nColumns);

		createCommentControls(composite, nColumns);
		enableCommentControl(true);

		setControl(composite);

		Dialog.applyDialogFont(composite);
		initHelp(composite);
	}

	protected void initHelp(Composite composite) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.NEW_CLASS_WIZARD_PAGE);
	}

	/*
	 * @see WizardPage#becomesVisible
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			setFocus();
		}
	}

	protected void createMethodStubSelectionControls(Composite composite, int nColumns) {
		Control labelControl = fMethodStubsButtons.getLabelControl(composite);
		LayoutUtil.setHorizontalSpan(labelControl, nColumns);

		DialogField.createEmptySpace(composite);

		Control buttonGroup = fMethodStubsButtons.getSelectionButtonsGroup(composite);
		LayoutUtil.setHorizontalSpan(buttonGroup, nColumns - 1);
	}

	/**
	 * Returns the current selection state of the 'Create Main' checkbox.
	 * 
	 * @return the selection state of the 'Create Main' checkbox
	 */
	public boolean isCreateMain() {
		return fMethodStubsButtons.isSelected(0);
	}

	/**
	 * Returns the current selection state of the 'Create Constructors'
	 * checkbox.
	 * 
	 * @return the selection state of the 'Create Constructors' checkbox
	 */
	public boolean isCreateConstructors() {
		return fMethodStubsButtons.isSelected(1);
	}

	/**
	 * Returns the current selection state of the 'Create inherited abstract
	 * methods' checkbox.
	 * 
	 * @return the selection state of the 'Create inherited abstract methods'
	 *         checkbox
	 */
	public boolean isCreateInherited() {
		return fMethodStubsButtons.isSelected(2);
	}

	/**
	 * Sets the selection state of the method stub checkboxes.
	 * 
	 * @param createMain
	 *            initial selection state of the 'Create Main' checkbox.
	 * @param createConstructors
	 *            initial selection state of the 'Create Constructors' checkbox.
	 * @param createInherited
	 *            initial selection state of the 'Create inherited abstract
	 *            methods' checkbox.
	 * @param canBeModified
	 *            if <code>true</code> the method stub checkboxes can be changed
	 *            by the user. If <code>false</code> the buttons are "read-only"
	 */
	public void setMethodStubSelection(boolean createMain, boolean createConstructors, boolean createInherited,
			boolean canBeModified) {
		fMethodStubsButtons.setSelection(0, createMain);
		fMethodStubsButtons.setSelection(1, createConstructors);
		fMethodStubsButtons.setSelection(2, createInherited);

		fMethodStubsButtons.setEnabled(canBeModified);
	}

	// ---- creation ----------------

	/*
	 * @see NewTypeWizardPage#createTypeMembers
	 */
	@Override
	protected void createTypeMembers(IType type, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
		boolean doMain = isCreateMain();
		boolean doConstr = isCreateConstructors();
		boolean doInherited = isCreateInherited();
		createInheritedMethods(type, doConstr, doInherited, imports, new SubProgressMonitor(monitor, 1));

		if (doMain) {
			createMatin(type, imports);
		}

		putDialogSettings();

		if (monitor != null) {
			monitor.done();
		}
	}

	protected void createMatin(IType type, ImportsManager imports) throws CoreException {
		StringBuffer buf = new StringBuffer();
		final String lineDelim = "\n"; // OK, since content is formatted afterwards //$NON-NLS-1$
		if (isAddComments()) {
			String comment = CodeGeneration
					.getMethodComment(
							type.getCompilationUnit(),
							type.getTypeQualifiedName('.'),
							"main", new String[] { "args" }, new String[0], Signature.createTypeSignature("void", true), null, lineDelim); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (comment != null) {
				buf.append(comment);
				buf.append(lineDelim);
			}
		}
		buf.append("public static void main("); //$NON-NLS-1$
		buf.append(imports.addImport("java.lang.String")); //$NON-NLS-1$
		buf.append("[] args) {"); //$NON-NLS-1$
		buf.append(lineDelim);
		final String content = CodeGeneration.getMethodBodyContent(type.getCompilationUnit(),
				type.getTypeQualifiedName('.'), "main", false, "", lineDelim); //$NON-NLS-1$ //$NON-NLS-2$
		if (content != null && content.length() != 0)
			buf.append(content);
		buf.append(lineDelim);
		buf.append("}"); //$NON-NLS-1$
		type.createMethod(buf.toString(), null, false, null);
	}

	protected void putDialogSettings() {
		IDialogSettings dialogSettings = getDialogSettings();
		if (dialogSettings != null) {
			IDialogSettings section = dialogSettings.getSection(SETTINGS_PAGE_NAME);
			if (section == null) {
				section = dialogSettings.addNewSection(SETTINGS_PAGE_NAME);
			}
			section.put(SETTINGS_CREATECONSTR, isCreateConstructors());
			section.put(SETTINGS_CREATEUNIMPLEMENTED, isCreateInherited());
		}
	}
}
