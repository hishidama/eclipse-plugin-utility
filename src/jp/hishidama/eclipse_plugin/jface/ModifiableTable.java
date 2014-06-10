package jp.hishidama.eclipse_plugin.jface;

import static jp.hishidama.eclipse_plugin.util.StringUtil.nonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public abstract class ModifiableTable<R> {
	private List<R> rowList = new ArrayList<R>();

	private boolean useAdd = true;
	private boolean useEdit = true;
	private boolean useMove = true;
	private boolean useDelete = true;

	private boolean visibleDup = false;

	protected final TableViewer viewer;
	protected final Table table;
	private int tableStyle;
	protected final List<Button> selectionButton = new ArrayList<Button>();

	public ModifiableTable(Composite parent, int style) {
		this.tableStyle = style;
		viewer = new TableViewer(parent, style);

		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setInput(rowList);

		table = viewer.getTable();
		table.setLayoutData(createGridData());
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshButtons();
				if (e.detail == SWT.CHECK) {
					widgetChecked(e);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				doEdit();
			}
		});
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				Point point = new Point(e.x, e.y);

				Table table = (Table) e.getSource();
				TableItem item = table.getItem(point);
				if (item == null) {
					table.deselectAll();
					refresh();
				}
			}
		});
	}

	protected GridData createGridData() {
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 96;
		return data;
	}

	public void setEditOnly(boolean editOnly) {
		if (editOnly) {
			setEnableButton(false, true, false, false);
		} else {
			setEnableButton(true, true, true, true);
		}
	}

	public void setEnableButton(boolean useAdd, boolean useEdit, boolean useMove, boolean useDelete) {
		this.useAdd = useAdd;
		this.useEdit = useEdit;
		this.useMove = useMove;
		this.useDelete = useDelete;
	}

	public void setVisibleDupButton(boolean use) {
		this.visibleDup = use;
	}

	public void addColumn(String text, int width, int style) {
		TableColumn column = new TableColumn(table, style);
		column.setText(nonNull(text));
		column.setWidth(width);
	}

	public void addItem(R element) {
		rowList.add(element);
		// refresh();
	}

	public void removeAll() {
		rowList.clear();
		// table.removeAll();
	}

	protected class ContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			@SuppressWarnings("unchecked")
			List<R> l = (List<R>) inputElement;
			return l.toArray();
		}
	}

	protected class LabelProvider extends BaseLabelProvider implements ITableLabelProvider {

		@SuppressWarnings("unchecked")
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return getImage((R) element, columnIndex);
		}

		@SuppressWarnings("unchecked")
		@Override
		public String getColumnText(Object element, int columnIndex) {
			return getText((R) element, columnIndex);
		}
	}

	protected Image getImage(R element, int columnIndex) {
		return null; // do override
	}

	protected abstract String getText(R element, int columnIndex);

	public void createButtonArea(Composite field) {
		createAddButton(field);
		createEditButton(field);
		{
			Button button = createPushButton(field, "up");
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					doMove(-1);
				}
			});
			if (useMove) {
				selectionButton.add(button);
			} else {
				button.setEnabled(false);
			}
		}
		{
			Button button = createPushButton(field, "down");
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					doMove(+1);
				}
			});
			if (useMove) {
				selectionButton.add(button);
			} else {
				button.setEnabled(false);
			}
		}
		if (visibleDup) {
			createDupButton(field);
		}
		createButtonAfterDup(field);
		createDeleteButton(field);
	}

	protected void createAddButton(Composite field) {
		Button button = createPushButton(field, "add");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});
		if (!useAdd) {
			button.setEnabled(false);
		}
	}

	protected void createEditButton(Composite field) {
		Button button = createPushButton(field, "edit");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doEdit();
			}
		});
		if (useEdit) {
			selectionButton.add(button);
		} else {
			button.setEnabled(false);
		}
	}

	protected void createDupButton(Composite field) {
		Button button = createPushButton(field, "dup");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doDuplicate();
			}
		});
		selectionButton.add(button);
	}

	protected void createButtonAfterDup(Composite field) {
		// do override
	}

	protected void createDeleteButton(Composite field) {
		Button button = createPushButton(field, "delete");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doDelete();
			}
		});
		if (useDelete) {
			selectionButton.add(button);
		} else {
			button.setEnabled(false);
		}
	}

	public void createCheckButtonArea(Composite field) {
		{
			Button button = createPushButton(field, "Check All");
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setCheckedAll(true);
				}
			});
		}
		{
			Button button = new Button(field, SWT.PUSH);
			button.setText("Uncheck All");
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setCheckedAll(false);
				}
			});
		}
	}

	protected Button createPushButton(Composite parent, String text) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		return button;
	}

	public void setSelection(int index) {
		table.setSelection(index);
		refreshButtons();
	}

	public void setCheckedAll(boolean checked) {
		for (TableItem item : table.getItems()) {
			item.setChecked(checked);
		}
		widgetChecked(null);
	}

	protected void widgetChecked(SelectionEvent e) {
		// do override
	}

	public void refresh() {
		viewer.refresh();
		refreshButtons();
	}

	protected void refreshButtons() {
		boolean enable = table.getSelectionCount() > 0;
		for (Button button : selectionButton) {
			button.setEnabled(enable);
		}
	}

	protected void doAdd() {
		R element = createElement();
		doAdd(element);
	}

	protected void doAdd(R element) {
		if (element == null) {
			return;
		}

		int index = table.getSelectionIndex();
		if (index < 0 || rowList.size() <= index) {
			rowList.add(element);
		} else {
			rowList.add(index, element);
		}
		refresh();
	}

	protected abstract R createElement();

	protected void doEdit() {
		int index = table.getSelectionIndex();
		if (index < 0 || rowList.size() <= index) {
			return;
		}
		editElement(rowList.get(index));
		refresh();
	}

	protected abstract void editElement(R element);

	protected void doMove(int z) {
		List<Boolean> checkList = null;
		if ((tableStyle & SWT.CHECK) != 0) {
			checkList = new ArrayList<Boolean>(table.getItemCount());
			for (TableItem item : table.getItems()) {
				checkList.add(item.getChecked());
			}
		}

		Set<R> set = new HashSet<R>();
		int[] index = table.getSelectionIndices();
		for (int i : index) {
			set.add(rowList.get(i));
		}

		if (z > 0) {
			for (int i = index.length - 1; i >= 0; i--) {
				int fr = index[i];
				int to = fr + 1;
				if (to < rowList.size() && !set.contains(rowList.get(to))) {
					swap(rowList, fr, to);
					if (checkList != null) {
						swap(checkList, fr, to);
					}
				}
			}
		} else {
			for (int i = 0; i < index.length; i++) {
				int fr = index[i];
				int to = fr - 1;
				if (to >= 0 && !set.contains(rowList.get(to))) {
					swap(rowList, fr, to);
					if (checkList != null) {
						swap(checkList, fr, to);
					}
				}
			}
		}
		refresh();

		if (checkList != null) {
			int i = 0;
			for (TableItem item : table.getItems()) {
				item.setChecked(checkList.get(i++));
			}
		}
	}

	private <T> void swap(List<T> list, int index1, int index2) {
		T r1 = list.get(index1);
		T r2 = list.get(index2);
		list.set(index1, r2);
		list.set(index2, r1);
	}

	protected void doDuplicate() {
		int[] index = table.getSelectionIndices();
		for (int i = index.length - 1; i >= 0; i--) {
			R row = dupElement(rowList.get(index[i]));
			rowList.add(index[i] + 1, row);
		}
		refresh();
	}

	protected R dupElement(R element) {
		throw new UnsupportedOperationException("not implements");
	}

	protected void doDelete() {
		int[] index = table.getSelectionIndices();
		for (int i = index.length - 1; i >= 0; i--) {
			rowList.remove(index[i]);
		}
		refresh();
	}

	public final Table getTable() {
		return table;
	}

	public final TableViewer getTableViewer() {
		return viewer;
	}

	public int getIndex(R element) {
		int i = 0;
		for (R row : rowList) {
			if (row == element) {
				return i;
			}
			i++;
		}
		return -1;
	}

	public void setEnabled(boolean enabled) {
		table.setEnabled(enabled);
		// TODO ボタンもsetEnabledしたい
	}

	public void setChecked(int i, boolean checked) {
		table.getItem(i).setChecked(checked);
	}

	public boolean getChecked(int i) {
		return table.getItem(i).getChecked();
	}

	public int getSelectionIndex() {
		return table.getSelectionIndex();
	}

	public int[] getSelectionIndices() {
		return table.getSelectionIndices();
	}

	public TableItem[] getItems() {
		return table.getItems();
	}

	public List<R> getElementList() {
		return rowList;
	}

	public List<R> getCheckedElementList() {
		List<R> list = new ArrayList<R>(rowList.size());
		for (int i = 0; i < rowList.size(); i++) {
			if (getChecked(i)) {
				list.add(rowList.get(i));
			}
		}
		return list;
	}
}
