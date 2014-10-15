package jp.hishidama.eclipse_plugin.wizard.page;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class SelectExcelFilePage extends WizardPage {
	private IResource resource;
	private boolean check;
	private boolean multi;

	private ExcelFileTreeViewer viewer;

	public SelectExcelFilePage(String title, IResource resource, boolean check, boolean multi) {
		super("SelectExcelFilePage");
		setTitle(title);
		this.resource = resource;
		this.check = check;
		this.multi = multi;

		setPageComplete(false);
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout(1, false));

		{
			int style = SWT.BORDER | SWT.FULL_SELECTION;
			if (check) {
				style |= SWT.CHECK;
			}
			viewer = new ExcelFileTreeViewer(composite, style, 512 + 128);
			GridData grid = new GridData(GridData.FILL_BOTH);
			grid.heightHint = 192;
			viewer.setLayoutData(grid);
			rebuild(viewer);

			if (check) {
				viewer.addCheckStateListener(new ICheckStateListener() {
					public void checkStateChanged(CheckStateChangedEvent event) {
						refreshChecked();
					}
				});
				viewer.getTree().addMouseListener(new MouseAdapter() {
					@Override
					public void mouseDown(MouseEvent e) {
						Point point = new Point(e.x, e.y);
						Tree tree = (Tree) e.widget;
						TreeItem item = tree.getItem(point);
						if (item != null && item.getBounds(0).contains(point)) {
							item.setChecked(!item.getChecked());
							refreshChecked();
						}
					}
				});
			} else {
				viewer.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						refreshChecked();
					}
				});
			}
		}

		setControl(composite);
	}

	private void rebuild(ExcelFileTreeViewer viewer) {
		viewer.setInput(resource);
	}

	private void refreshChecked() {
		List<IFile> list;
		if (check) {
			list = viewer.getCheckedFilelList();
		} else {
			list = viewer.getSelectedFileList();
		}
		if (multi) {
			setPageComplete(!list.isEmpty());
		} else {
			setPageComplete(list.size() == 1);
		}
	}

	public List<IFile> getFileList() {
		List<IFile> list;
		if (check) {
			list = viewer.getCheckedFilelList();
		} else {
			list = viewer.getSelectedFileList();
		}
		return list;
	}
}
