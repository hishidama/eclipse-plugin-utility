package jp.hishidama.eclipse_plugin.jface;

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

public abstract class ModifiableTable<R> {
	private List<R> list = new ArrayList<R>();

	private boolean editOnly = false;
	private TableViewer viewer;
	private Table table;
	private List<Button> selectionButton = new ArrayList<Button>();

	public ModifiableTable(Composite parent, int style) {
		viewer = new TableViewer(parent, style);

		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setInput(list);

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

	public void addColumn(String text, int width, int style) {
		TableColumn column = new TableColumn(table, style);
		column.setText(text);
		column.setWidth(width);
	}

	public void addItem(R element) {
		list.add(element);
		// refresh();
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
		{
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
		{
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
		if (element == null) {
			return;
		}

		int index = table.getSelectionIndex();
		if (index < 0 || list.size() <= index) {
			list.add(element);
		} else {
			list.add(index, element);
		}
		refresh();
	}

	protected abstract R createElement();

	protected void doEdit() {
		int index = table.getSelectionIndex();
		if (index < 0 || list.size() <= index) {
			return;
		}
		editElement(list.get(index));
		refresh();
	}

	protected abstract void editElement(R element);

	protected void doMove(int z) {
		Set<R> set = new HashSet<R>();
		int[] index = table.getSelectionIndices();
		for (int i : index) {
			set.add(list.get(i));
		}

		if (z > 0) {
			for (int i = index.length - 1; i >= 0; i--) {
				int fr = index[i];
				int to = fr + 1;
				if (to < list.size() && !set.contains(list.get(to))) {
					swap(list, fr, to);
				}
			}
		} else {
			for (int i = 0; i < index.length; i++) {
				int fr = index[i];
				int to = fr - 1;
				if (to >= 0 && !set.contains(list.get(to))) {
					swap(list, fr, to);
				}
			}
		}
		refresh();
	}

	private void swap(List<R> list, int index1, int index2) {
		R r1 = list.get(index1);
		R r2 = list.get(index2);
		list.set(index1, r2);
		list.set(index2, r1);
	}

	protected void doDelete() {
		int[] index = table.getSelectionIndices();
		for (int i = index.length - 1; i >= 0; i--) {
			list.remove(index[i]);
		}
		refresh();
	}

	public List<R> getElementList() {
		return list;
	}
}
