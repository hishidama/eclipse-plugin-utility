package jp.hishidama.eclipse_plugin.jdt.method;

import java.util.ArrayList;
import java.util.List;

import jp.hishidama.eclipse_plugin.dialog.EditDialog;
import jp.hishidama.eclipse_plugin.jdt.util.TypeUtil;
import jp.hishidama.eclipse_plugin.jface.ModifiableTable;
import jp.hishidama.eclipse_plugin.util.StringUtil;
import jp.hishidama.eclipse_plugin.wizard.page.EditWizardPage;

import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ModifyMethodArgumentPage extends EditWizardPage {

	private IMethod method;
	private List<Expression> arguments;

	private Text methodNameText;
	private ArgumentTable table;

	public ModifyMethodArgumentPage() {
		super("ModifyMethodArgumentPage");
		setTitle("Set Argument");
		setDescription("メソッドの引数を指定して下さい。");
	}

	public void initialize(IMethod method, List<Expression> arguments) {
		this.method = method;
		this.arguments = arguments;
	}

	@Override
	protected Composite createComposite(Composite parent) {
		Font font = parent.getFont();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(font);

		createLabel(composite, "Method:");
		methodNameText = createReadOnlyText(composite, 1);

		createLabel(composite, "Argument:");
		table = new ArgumentTable(composite);
		table.addColumn("parameter type", 128 + 96, SWT.NONE);
		table.addColumn("parameter name", 128 + 64, SWT.NONE);
		table.addColumn("argument type", 128 + 64, SWT.NONE);
		table.addColumn("argument value", 256 + 32, SWT.NONE);

		createLabel(composite, "");
		Composite field = new Composite(composite, SWT.NONE);
		field.setLayout(new FillLayout(SWT.HORIZONTAL));
		table.createButtonArea(field);

		return composite;
	}

	protected static class ArgumentRow {
		public String type;
		public String name;
		public String valueType;
		public String value;
	}

	protected class ArgumentTable extends ModifiableTable<ArgumentRow> {

		public ArgumentTable(Composite parent) {
			super(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		}

		@Override
		public void createButtonArea(Composite field) {
			createEditButton(field);
			createMoveButton(field);
		}

		@Override
		protected String getText(ArgumentRow element, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return element.type;
			case 1:
				return element.name;
			case 2:
				return element.valueType;
			default:
				return element.value;
			}
		}

		@Override
		protected ArgumentRow createElement() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void editElement(ArgumentRow row) {
			ArgumentDialog dialog = new ArgumentDialog(getShell(), row);
			dialog.open();
		}

		protected class ArgumentDialog extends EditDialog {
			private ArgumentRow row;

			private Text paramTypeText;
			private Text paramNameText;
			private Text argTypeText;
			private Text argValueText;

			public ArgumentDialog(Shell parentShell, ArgumentRow row) {
				super(parentShell, "Set Argument", 2);
				this.row = row;
			}

			@Override
			protected void createFields(Composite composite) {
				createLabel(composite, "parameter type:");
				paramTypeText = createReadOnlyText(composite);
				paramTypeText.setText(nonNull(row.type));
				createLabel(composite, "parameter name:");
				paramNameText = createReadOnlyText(composite);
				paramNameText.setText(nonNull(row.name));
				createLabel(composite, "argument type:");
				argTypeText = createText(composite);
				argTypeText.setText(nonNull(row.valueType));
				createLabel(composite, "argument value:");
				argValueText = createText(composite);
				argValueText.setText(nonNull(row.value));
				argValueText.setFocus();
			}

			@Override
			protected void refresh() {
				refreshOkButton();
			}

			@Override
			protected boolean validate() {
				return true;
			}

			@Override
			protected void okPressed() {
				row.valueType = argTypeText.getText();
				row.value = argValueText.getText();

				super.okPressed();
			}
		}

		@Override
		protected void doMove(int z) {
			boolean[] selected = new boolean[table.getItemCount()];
			int[] indices = table.getSelectionIndices();
			for (int index : indices) {
				selected[index] = true;
			}

			List<ArgumentRow> rowList = getElementList();

			if (z > 0) {
				for (int i = indices.length - 1; i >= 0; i--) {
					int fr = indices[i];
					int to = fr + 1;
					if (to < rowList.size() && !selected[to]) {
						swap(rowList, selected, fr, to);
					}
				}
			} else {
				for (int i = 0; i < indices.length; i++) {
					int fr = indices[i];
					int to = fr - 1;
					if (to >= 0 && !selected[to]) {
						swap(rowList, selected, fr, to);
					}
				}
			}

			List<Integer> list = new ArrayList<Integer>();
			for (int index = 0; index < selected.length; index++) {
				if (selected[index]) {
					list.add(index);
				}
			}
			int[] newIndices = new int[list.size()];
			for (int i = 0; i < list.size(); i++) {
				newIndices[i] = list.get(i);
			}
			table.setSelection(newIndices);

			refresh();
		}

		private void swap(List<ArgumentRow> list, boolean[] selected, int index1, int index2) {
			ArgumentRow row1 = list.get(index1);
			ArgumentRow row2 = list.get(index2);
			String valueType1 = row1.valueType;
			String valueType2 = row2.valueType;
			row1.valueType = valueType2;
			row2.valueType = valueType1;
			String value1 = row1.value;
			String value2 = row2.value;
			row1.value = value2;
			row2.value = value1;

			boolean sel1 = selected[index1];
			boolean sel2 = selected[index2];
			selected[index1] = sel2;
			selected[index2] = sel1;
		}
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			methodNameText.setText(method.getElementName());
			initializeTable();
		}
		super.setVisible(visible);
	}

	protected void initializeTable() {
		table.removeAll();
		try {
			for (ILocalVariable parameter : method.getParameters()) {
				ArgumentRow row = new ArgumentRow();
				row.type = TypeUtil.getVariableTypeSimpleName(parameter);
				row.name = parameter.getElementName();
				table.addItem(row);
			}
		} catch (JavaModelException e) {
		}

		if (arguments != null) {
			List<ArgumentRow> list = table.getElementList();
			int firstSize = list.size();
			int i = 0;
			for (Expression arg : arguments) {
				if (i >= firstSize) {
					table.addItem(new ArgumentRow());
					list = table.getElementList();
				}
				ArgumentRow row = list.get(i++);
				ITypeBinding type = arg.resolveTypeBinding();
				if (type != null) {
					row.valueType = type.getName();
				}
				row.value = arg.toString();
			}
		}

		table.refresh();
	}

	@Override
	protected String validate() {
		return null;
	}

	@Override
	public boolean isPageComplete() {
		return super.isPageComplete() && isCurrentPage();
	}

	public List<String> getArguments() {
		List<ArgumentRow> rowList = table.getElementList();
		List<String> list = new ArrayList<String>(rowList.size());
		for (ArgumentRow row : rowList) {
			if (StringUtil.nonEmpty(row.name)) {
				list.add(row.value);
			}
		}
		return list;
	}
}
