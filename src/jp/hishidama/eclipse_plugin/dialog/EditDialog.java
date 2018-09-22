package jp.hishidama.eclipse_plugin.dialog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

public abstract class EditDialog extends Dialog {

	private final String windowTitle;
	protected final int numColumns;

	protected final ModifyListener MODIFY_REFRESH_LISTENER = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			refreshOkButton();
		}
	};

	protected final SelectionListener SELECT_REFRESH_LISTENER = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			refreshOkButton();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			refreshOkButton();
		}
	};

	public EditDialog(Shell parentShell, String windowTitle) {
		this(parentShell, windowTitle, 2);
	}

	public EditDialog(Shell parentShell, String windowTitle, int numColumns) {
		super(parentShell);
		this.windowTitle = windowTitle;
		this.numColumns = numColumns;

		initializeShellStyle();
	}

	protected void initializeShellStyle() {
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	public void create() {
		super.create();

		getShell().setText(windowTitle);

		refresh();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = createDialogAreaComposite(parent);

		createFields(composite);

		return composite;
	}

	private Composite createDialogAreaComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(numColumns, false);
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayout(layout);
		applyDialogFont(composite);

		return composite;
	}

	protected abstract void createFields(Composite composite);

	protected abstract void refresh();

	protected List<Button> createRadioField(Composite composite, String label, String... labels) {
		if (label != null) {
			createLabel(composite, label);
		}

		Composite field = createRowLayout(composite);

		List<Button> list = new ArrayList<Button>(labels.length);
		for (String radioLabel : labels) {
			final Button button = new Button(field, SWT.RADIO);
			button.setText(radioLabel);
			button.addSelectionListener(SELECT_REFRESH_LISTENER);

			list.add(button);
		}

		createDummyColumn(composite, 2);

		return list;
	}

	protected Text createTextField(Composite composite, String label) {
		createLabel(composite, label);
		Text text = createText(composite);
		return text;
	}

	protected Combo createComboField(Composite composite, String label, String[] list) {
		return createComboField(composite, label, list, false);
	}

	protected Combo createComboFieldFull(Composite composite, String label, String[] list) {
		return createComboField(composite, label, list, true);
	}

	private Combo createComboField(Composite composite, String label, String[] list, boolean full) {
		createLabel(composite, label);

		Combo combo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		if (full) {
			data.horizontalSpan = getCompositeNumColumns(composite) - 1;
		}
		combo.setLayoutData(data);

		for (String s : list) {
			combo.add(s);
		}
		combo.addSelectionListener(SELECT_REFRESH_LISTENER);

		if (!full) {
			createDummyColumn(composite, 2);
		}
		return combo;
	}

	protected Tree createTreeField(Composite composite, String label) {
		createLabel(composite, label);

		Tree tree = createTree(composite);

		createDummyColumn(composite, 2);
		return tree;
	}

	protected Table createCheckedTable(Composite composite, String label) {
		createLabel(composite, label);

		Table table = createCheckedTable(composite);

		createDummyColumn(composite, 2);
		return table;
	}

	public static class TextButtonPair {
		public Label label;
		public Text text;
		public Button button;
	}

	protected TextButtonPair createTextButtonField(Composite composite, String label, String buttonLabel) {
		TextButtonPair pair = new TextButtonPair();
		pair.label = createLabel(composite, label);
		pair.text = createText(composite, SWT.SINGLE | SWT.BORDER, 256, 1);
		pair.button = createPushButton(composite, buttonLabel);

		createDummyColumn(composite, 3);
		return pair;
	}

	protected Button createCheckButtonField(Composite composite, String label, String text) {
		createLabel(composite, label);

		Button button = new Button(composite, SWT.CHECK);
		button.setText(text);

		createDummyColumn(composite, 2);
		return button;
	}

	protected Composite createFillLayout(Composite composite, int span) {
		Composite field = new Composite(composite, SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = span;
		field.setLayoutData(data);
		field.setLayout(new FillLayout(SWT.HORIZONTAL));
		return field;
	}

	protected Composite createRowLayout(Composite composite) {
		Composite field = new Composite(composite, SWT.NONE);
		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.marginTop = 0;
		layout.marginBottom = 0;
		field.setLayout(layout);
		return field;
	}

	protected Label createLabel(Composite composite, String text) {
		Label label = new Label(composite, SWT.LEFT);
		label.setText(text);
		return label;
	}

	protected Text createText(Composite composite) {
		return createText(composite, SWT.SINGLE | SWT.BORDER);
	}

	protected Text createReadOnlyText(Composite composite) {
		return createText(composite, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
	}

	protected Text createText(Composite composite, int style) {
		int span = getCompositeNumColumns(composite) - 1;
		return createText(composite, style, 128 * 3, span);
	}

	protected Text createText(Composite composite, int style, int widthHint, int span) {
		final Text text = new Text(composite, style);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = widthHint;
		data.horizontalSpan = span;
		text.setLayoutData(data);
		text.addModifyListener(MODIFY_REFRESH_LISTENER);
		return text;
	}

	protected Button createPushButton(Composite composite, String text) {
		Button button = new Button(composite, SWT.PUSH);
		button.setText(text);
		return button;
	}

	protected Button createCheckButton(Composite composite, String text) {
		Button button = new Button(composite, SWT.CHECK);
		button.setText(text);
		return button;
	}

	protected Tree createTree(Composite composite) {
		Tree tree = new Tree(composite, SWT.BORDER | SWT.SINGLE);
		initializeTree(tree);

		return tree;
	}

	protected void initializeTree(Tree tree) {
		initializeTree(tree, true);
	}

	protected void initializeTree(Tree tree, boolean addSelectionListener) {
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 128 * 3;
		data.heightHint = 128 * 2;
		data.horizontalSpan = 2;
		data.verticalSpan = 1;
		tree.setLayoutData(data);

		if (addSelectionListener) {
			tree.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					refreshOkButton();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					if (refreshOkButton()) {
						okPressed();
					}
				}
			});
		}
	}

	protected Table createCheckedTable(Composite composite) {
		Table table = new Table(composite, SWT.SINGLE | SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 128 * 3;
		data.heightHint = 128 * 2;
		table.setLayoutData(data);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		table.addSelectionListener(SELECT_REFRESH_LISTENER);

		return table;
	}

	protected void createDummyColumn(Composite composite, int existsColumn) {
		int size = getCompositeNumColumns(composite);
		for (int i = existsColumn; i < size; i++) {
			new Label(composite, SWT.NONE);
		}
	}

	private int getCompositeNumColumns(Composite composite) {
		int n = this.numColumns;
		Layout layout = composite.getLayout();
		if (layout instanceof GridLayout) {
			n = ((GridLayout) layout).numColumns;
		}
		return n;
	}

	protected boolean refreshOkButton() {
		Button ok = getButton(IDialogConstants.OK_ID);
		if (ok == null) {
			return false;
		}
		if (validate()) {
			ok.setEnabled(true);
			return true;
		} else {
			ok.setEnabled(false);
			return false;
		}
	}

	protected abstract boolean validate();

	protected static String nonNull(String s) {
		return (s != null) ? s : "";
	}
}
