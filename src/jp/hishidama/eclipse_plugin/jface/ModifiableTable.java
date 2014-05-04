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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public abstract class ModifiableTable<R> {
	private List<R> rowList = new ArrayList<R>();

	private boolean editOnly = false;
	private TableViewer viewer;
	private Table table;
	private int tableStyle;
	private List<Button> selectionButton = new ArrayList<Button>();

	public ModifiableTable(Composite parent, int style) {
		this.tableStyle = style;
		viewer = new TableViewer(parent, style);

		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setInput(rowList);

		table = viewer.getTable();
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshButtons();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				doEdit();
			}
		});
	}

	public void setEditOnly(boolean editOnly) {
		this.editOnly = editOnly;
	}

	public boolean isEditOnly() {
		return editOnly;
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
		{
			Button button = new Button(field, SWT.PUSH);
			button.setText("edit");
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					doEdit();
				}
			});
			selectionButton.add(button);
		}
		{
			Button button = new Button(field, SWT.PUSH);
			button.setText("up");
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					doMove(-1);
				}
			});
			if (editOnly) {
				button.setEnabled(false);
			} else {
				selectionButton.add(button);
			}
		}
		{
			Button button = new Button(field, SWT.PUSH);
			button.setText("down");
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					doMove(+1);
				}
			});
			if (editOnly) {
				button.setEnabled(false);
			} else {
				selectionButton.add(button);
			}
		}
		createDeleteButton(field);
	}

	protected void createAddButton(Composite field) {
		Button button = new Button(field, SWT.PUSH);
		button.setText("add");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});
		if (editOnly) {
			button.setEnabled(false);
		}
	}

	protected void createDeleteButton(Composite field) {
		Button button = new Button(field, SWT.PUSH);
		button.setText("delete");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doDelete();
			}
		});
		if (editOnly) {
			button.setEnabled(false);
		} else {
			selectionButton.add(button);
		}
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

	protected void doDelete() {
		int[] index = table.getSelectionIndices();
		for (int i = index.length - 1; i >= 0; i--) {
			rowList.remove(index[i]);
		}
		refresh();
	}

	public void setChecked(int i, boolean checked) {
		table.getItem(i).setChecked(checked);
	}

	public boolean getChecked(int i) {
		return table.getItem(i).getChecked();
	}

	public List<R> getElementList() {
		return rowList;
	}
}
