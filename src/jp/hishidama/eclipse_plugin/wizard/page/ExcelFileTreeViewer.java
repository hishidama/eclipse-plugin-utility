package jp.hishidama.eclipse_plugin.wizard.page;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckable;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ExcelFileTreeViewer extends TreeViewer implements ICheckable {
	/**
	 * List of check state listeners (element type:
	 * <code>ICheckStateListener</code>).
	 */
	private ListenerList checkStateListeners = new ListenerList();

	/**
	 * Last item clicked on, or <code>null</code> if none.
	 */
	private TreeItem lastClickedItem = null;

	public ExcelFileTreeViewer(Composite parent, int style, int nameWidth) {
		super(parent, style);
		setLabelProvider(new WorkbenchLabelProvider());
		setContentProvider(new WorkbenchContentProvider());

		Tree tree = getTree();
		tree.setHeaderVisible(true);
		{
			TreeColumn column = new TreeColumn(tree, SWT.NONE);
			column.setText("name");
			column.setWidth(nameWidth);
		}

		if ((style & SWT.CHECK) != 0) {
			addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(CheckStateChangedEvent event) {
					refreshChecked(event.getElement());
				}
			});
			tree.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent e) {
					Point point = new Point(e.x, e.y);
					Tree tree = (Tree) e.widget;
					TreeItem item = tree.getItem(point);
					if (item != null && item.getBounds(0).contains(point)) {
						item.setChecked(!item.getChecked());
						refreshChecked(item.getData());
					}
				}
			});
		}
	}

	public void setLayoutData(Object layoutData) {
		getTree().setLayoutData(layoutData);
	}

	// CheckboxTreeViewer

	public void addCheckStateListener(ICheckStateListener listener) {
		checkStateListeners.add(listener);
	}

	public void removeCheckStateListener(ICheckStateListener listener) {
		checkStateListeners.remove(listener);
	}

	public boolean getChecked(Object element) {
		Widget widget = findItem(element);
		if (widget instanceof TreeItem) {
			return ((TreeItem) widget).getChecked();
		}
		return false;
	}

	public boolean setChecked(Object element, boolean state) {
		Assert.isNotNull(element);
		Widget widget = internalExpand(element, false);
		if (widget instanceof TreeItem) {
			((TreeItem) widget).setChecked(state);
			return true;
		}
		return false;
	}

	@Override
	protected void handleDoubleSelect(SelectionEvent event) {

		if (lastClickedItem != null) {
			TreeItem item = lastClickedItem;
			Object data = item.getData();
			if (data != null) {
				boolean state = item.getChecked();
				setChecked(data, !state);
				fireCheckStateChanged(new CheckStateChangedEvent(this, data, !state));
			}
			lastClickedItem = null;
		} else {
			super.handleDoubleSelect(event);
		}
	}

	@Override
	protected void handleSelect(SelectionEvent event) {

		lastClickedItem = null;
		if (event.detail == SWT.CHECK) {
			TreeItem item = (TreeItem) event.item;
			lastClickedItem = item;
			super.handleSelect(event);

			Object data = item.getData();
			if (data != null) {
				fireCheckStateChanged(new CheckStateChangedEvent(this, data, item.getChecked()));
			}
		} else {
			super.handleSelect(event);
		}
	}

	/**
	 * Notifies any check state listeners that the check state of an element has
	 * changed. Only listeners registered at the time this method is called are
	 * notified.
	 * 
	 * @param event
	 *            a check state changed event
	 * 
	 * @see ICheckStateListener#checkStateChanged
	 */
	protected void fireCheckStateChanged(final CheckStateChangedEvent event) {
		Object[] array = checkStateListeners.getListeners();
		for (int i = 0; i < array.length; i++) {
			final ICheckStateListener l = (ICheckStateListener) array[i];
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					l.checkStateChanged(event);
				}
			});
		}

	}

	private void refreshChecked(Object obj) {
		// TODO
	}

	@Override
	public ITreeSelection getSelection() {
		return (ITreeSelection) super.getSelection();
	}

	public List<IFile> getSelectedFileList() {
		ITreeSelection selection = getSelection();
		TreePath[] paths = selection.getPaths();
		List<IFile> list = new ArrayList<IFile>();
		for (TreePath path : paths) {
			Object object = path.getLastSegment();
			if (object instanceof IFile) {
				IFile file = (IFile) object;
				if (isTargetFile(file)) {
					list.add(file);
				}
			}
		}
		return list;
	}

	public List<IFile> getCheckedFilelList() {
		List<IFile> list = new ArrayList<IFile>();
		for (TreeItem row : getTree().getItems()) {
			for (TreeItem item : row.getItems()) {
				if (item.getChecked()) {
					IResource data = (IResource) item.getData();
					if (data instanceof IFile) {
						IFile file = (IFile) data;
						if (isTargetFile(file)) {
							list.add(file);
						}
					}
				}
			}
		}
		return list;
	}

	protected boolean isTargetFile(IFile file) {
		String ext = file.getFullPath().getFileExtension();
		return "xls".equals(ext) || "xlsx".equals(ext);
	}
}
