package jp.hishidama.eclipse_plugin.wizard.page;

import java.util.regex.Pattern;

import jp.hishidama.eclipse_plugin.util.internal.LogUtil;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.corext.util.JavaConventionsUtil;
import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.IJUnitHelpContextIds;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;
import org.eclipse.jdt.internal.junit.wizards.WizardMessages;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.ControlContentAssistHelper;
import org.eclipse.jdt.internal.ui.refactoring.contentassist.JavaTypeCompletionProcessor;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

/**
 * @see org.eclipse.jdt.junit.wizards.NewTestCaseWizardPageOne
 */
@SuppressWarnings("restriction")
public class NewTestClassWizardPage extends NewClassWizardPage {

	private final static String SETTINGS_PAGE_NAME = "NewTestClassWizardPage"; //$NON-NLS-1$

	/** Field ID of the class under test field. */
	public final static String CLASS_UNDER_TEST = SETTINGS_PAGE_NAME + ".classundertest"; //$NON-NLS-1$

	private final static String TEST_SUFFIX = "Test"; //$NON-NLS-1$

	private String fClassUnderTestText; // model
	private IType fClassUnderTest; // resolved model, can be null
	private Text fClassUnderTestControl; // control
	private IStatus fClassUnderTestStatus; // status

	private Button fClassUnderTestButton;
	private JavaTypeCompletionProcessor fClassToTestCompletionProcessor;

	/**
	 * Creates a new <code>NewClassWizardPage</code>
	 */
	public NewTestClassWizardPage(String pageName) {
		super(pageName);

		fClassToTestCompletionProcessor = new JavaTypeCompletionProcessor(false, false, true);

		fClassUnderTestStatus = new JUnitStatus();

		fClassUnderTestText = ""; //$NON-NLS-1$
	}

	// -------- Initialization ---------

	/**
	 * Initialized the page with the current selection
	 * 
	 * @param selection
	 *            The selection
	 */
	@Override
	public void init(IStructuredSelection selection) {
		IJavaElement element = getInitialJavaElement(selection);

		initContainerPage(element);
		initTypePage(element);
		// put default class to test
		if (element != null) {
			IType classToTest = null;
			// evaluate the enclosing type
			IType typeInCompUnit = (IType) element.getAncestor(IJavaElement.TYPE);
			if (typeInCompUnit != null) {
				if (typeInCompUnit.getCompilationUnit() != null) {
					classToTest = typeInCompUnit;
				}
			} else {
				ICompilationUnit cu = (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
				if (cu != null)
					classToTest = cu.findPrimaryType();
				else {
					if (element instanceof IClassFile) {
						try {
							IClassFile cf = (IClassFile) element;
							if (cf.isStructureKnown()) {
								classToTest = cf.getType();
							}
						} catch (JavaModelException e) {
							LogUtil.logWarn(getClass().getSimpleName() + "#init()", e);
						}
					}
				}
			}
			if (classToTest != null) {
				try {
					if (!CoreTestSearchEngine.isTestImplementor(classToTest)) {
						setClassUnderTest(classToTest.getFullyQualifiedName('.'));
					}
				} catch (JavaModelException e) {
					LogUtil.logWarn(getClass().getSimpleName() + "#init()", e);
				}
			}
		}

		// restoreWidgetValues();

		doStatusUpdate();

		init();
	}

	private boolean initializedPackageFragmentRoot = false;

	@Override
	public void setPackageFragmentRoot(IPackageFragmentRoot root, boolean canBeModified) {
		if (!initializedPackageFragmentRoot) {
			initializedPackageFragmentRoot = true;
			String s = (root == null) ? "" : root.getPath().makeRelative().toString(); //$NON-NLS-1$
			int n = s.indexOf("/main/");
			if (n >= 0) {
				s = s.replaceFirst(Pattern.quote("/main/"), "/test/");
				root = root.getJavaProject().getPackageFragmentRoot(s);
			}
		}
		super.setPackageFragmentRoot(root, canBeModified);
	}

	@Override
	protected void handleFieldChanged(String fieldName) {
		super.handleFieldChanged(fieldName);
		if (fieldName.equals(CONTAINER)) {
			fClassUnderTestStatus = classUnderTestChanged();
			if (fClassUnderTestButton != null && !fClassUnderTestButton.isDisposed()) {
				fClassUnderTestButton.setEnabled(getPackageFragmentRoot() != null);
			}

			// updateBuildPathMessage();
		}
		doStatusUpdate();
	}

	@Override
	protected IStatus[] getAllComponentStatus() {
		// status of all used components
		IStatus[] status = { fContainerStatus, fPackageStatus, fTypeNameStatus, fClassUnderTestStatus, fModifierStatus,
				fSuperClassStatus };
		return status;
	}

	// ------ UI --------

	/*
	 * @see WizardPage#createControl
	 */
	@Override
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

		createSeparator(composite, nColumns);
		createClassUnderTestControls(composite, nColumns);

		setControl(composite);

		// set default and focus
		initDefaultTypeName();

		Dialog.applyDialogFont(composite);
		initHelp(composite);
	}

	protected void initDefaultTypeName() {
		String classUnderTest = getClassUnderTestText();
		if (classUnderTest.length() > 0) {
			setTypeName(Signature.getSimpleName(classUnderTest) + TEST_SUFFIX, true);
		}
	}

	@Override
	protected void initHelp(Composite composite) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJUnitHelpContextIds.NEW_TESTCASE_WIZARD_PAGE);
	}

	/**
	 * Creates the controls for the 'class under test' field. Expects a
	 * <code>GridLayout</code> with at least 3 columns.
	 * 
	 * @param composite
	 *            the parent composite
	 * @param nColumns
	 *            number of columns to span
	 */
	protected void createClassUnderTestControls(Composite composite, int nColumns) {
		Label classUnderTestLabel = new Label(composite, SWT.LEFT | SWT.WRAP);
		classUnderTestLabel.setFont(composite.getFont());
		classUnderTestLabel.setText(WizardMessages.NewTestCaseWizardPageOne_class_to_test_label);
		classUnderTestLabel.setLayoutData(new GridData());

		fClassUnderTestControl = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fClassUnderTestControl.setEnabled(true);
		fClassUnderTestControl.setFont(composite.getFont());
		fClassUnderTestControl.setText(fClassUnderTestText);
		fClassUnderTestControl.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				internalSetClassUnderText(((Text) e.widget).getText());
			}
		});
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = nColumns - 2;
		fClassUnderTestControl.setLayoutData(gd);

		fClassUnderTestButton = new Button(composite, SWT.PUSH);
		fClassUnderTestButton.setText(WizardMessages.NewTestCaseWizardPageOne_class_to_test_browse);
		fClassUnderTestButton.setEnabled(getPackageFragmentRoot() != null);
		fClassUnderTestButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				classToTestButtonPressed();
			}

			public void widgetSelected(SelectionEvent e) {
				classToTestButtonPressed();
			}
		});
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = false;
		gd.horizontalSpan = 1;
		gd.widthHint = LayoutUtil.getButtonWidthHint(fClassUnderTestButton);
		fClassUnderTestButton.setLayoutData(gd);

		ControlContentAssistHelper.createTextContentAssistant(fClassUnderTestControl, fClassToTestCompletionProcessor);
	}

	private void classToTestButtonPressed() {
		IType type = chooseClassToTestType();
		if (type != null) {
			setClassUnderTest(type.getFullyQualifiedName('.'));
		}
	}

	private IType chooseClassToTestType() {
		IPackageFragmentRoot root = getPackageFragmentRoot();
		if (root == null)
			return null;

		IJavaElement[] elements = new IJavaElement[] { root.getJavaProject() };
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(elements);

		try {
			SelectionDialog dialog = JavaUI.createTypeDialog(getShell(), getWizard().getContainer(), scope,
					IJavaElementSearchConstants.CONSIDER_CLASSES_AND_ENUMS, false, getClassUnderTestText());
			dialog.setTitle(WizardMessages.NewTestCaseWizardPageOne_class_to_test_dialog_title);
			dialog.setMessage(WizardMessages.NewTestCaseWizardPageOne_class_to_test_dialog_message);
			if (dialog.open() == Window.OK) {
				Object[] resultArray = dialog.getResult();
				if (resultArray != null && resultArray.length > 0)
					return (IType) resultArray[0];
			}
		} catch (JavaModelException e) {
			JUnitPlugin.log(e);
		}
		return null;
	}

	@Override
	protected IStatus packageChanged() {
		IStatus status = super.packageChanged();
		fClassToTestCompletionProcessor.setPackageFragment(getPackageFragment());
		return status;
	}

	/**
	 * Hook method that gets called when the class under test has changed. The
	 * method class under test returns the status of the validation.
	 * <p>
	 * Subclasses may extend this method to perform their own validation.
	 * </p>
	 * 
	 * @return the status of the validation
	 */
	protected IStatus classUnderTestChanged() {
		JUnitStatus status = new JUnitStatus();

		fClassUnderTest = null;

		IPackageFragmentRoot root = getPackageFragmentRoot();
		if (root == null) {
			return status;
		}

		String classToTestName = getClassUnderTestText();
		if (classToTestName.length() == 0) {
			return status;
		}

		IStatus val = JavaConventionsUtil.validateJavaTypeName(classToTestName, root);
		if (val.getSeverity() == IStatus.ERROR) {
			status.setError(WizardMessages.NewTestCaseWizardPageOne_error_class_to_test_not_valid);
			return status;
		}

		IPackageFragment pack = getPackageFragment(); // can be null
		try {
			IType type = resolveClassNameToType(root.getJavaProject(), pack, classToTestName);
			if (type == null) {
				status.setError(WizardMessages.NewTestCaseWizardPageOne_error_class_to_test_not_exist);
				return status;
			}
			if (type.isInterface()) {
				status.setWarning(Messages.format(
						WizardMessages.NewTestCaseWizardPageOne_warning_class_to_test_is_interface,
						BasicElementLabels.getJavaElementName(classToTestName)));
			}

			if (pack != null && !JUnitStubUtility.isVisible(type, pack)) {
				status.setWarning(Messages.format(
						WizardMessages.NewTestCaseWizardPageOne_warning_class_to_test_not_visible,
						BasicElementLabels.getJavaElementName(classToTestName)));
			}
			fClassUnderTest = type;
			// fPage2.setClassUnderTest(fClassUnderTest);
		} catch (JavaModelException e) {
			status.setError(WizardMessages.NewTestCaseWizardPageOne_error_class_to_test_not_valid);
		}
		return status;
	}

	private IType resolveClassNameToType(IJavaProject jproject, IPackageFragment pack, String classToTestName)
			throws JavaModelException {
		if (!jproject.exists()) {
			return null;
		}

		IType type = jproject.findType(classToTestName);

		// search in current package
		if (type == null && pack != null && !pack.isDefaultPackage()) {
			type = jproject.findType(pack.getElementName(), classToTestName);
		}

		// search in java.lang
		if (type == null) {
			type = jproject.findType("java.lang", classToTestName); //$NON-NLS-1$
		}
		return type;
	}

	/**
	 * Returns the content of the class to test text field.
	 * 
	 * @return the name of the class to test
	 */
	public String getClassUnderTestText() {
		return fClassUnderTestText;
	}

	/**
	 * Returns the class to be tested.
	 * 
	 * @return the class under test or <code>null</code> if the entered values
	 *         are not valid
	 */
	public IType getClassUnderTest() {
		return fClassUnderTest;
	}

	/**
	 * Sets the name of the class under test.
	 * 
	 * @param name
	 *            The name to set
	 */
	public void setClassUnderTest(String name) {
		if (fClassUnderTestControl != null && !fClassUnderTestControl.isDisposed()) {
			fClassUnderTestControl.setText(name);
		}
		internalSetClassUnderText(name);
	}

	private void internalSetClassUnderText(String name) {
		fClassUnderTestText = name;
		fClassUnderTestStatus = classUnderTestChanged();
		handleFieldChanged(CLASS_UNDER_TEST);
	}
}
