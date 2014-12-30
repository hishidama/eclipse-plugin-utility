package jp.hishidama.eclipse_plugin.wizard.page;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class SelectMethodPage extends EditWizardPage {

	protected IType type;
	private IMethod firstSelectedMethod;
	private List<IMethod> methodList;
	private TreeViewer viewer;

	public SelectMethodPage() {
		super("SelectMethodPage");
		setTitle("Select Method");
		setDescription("メソッドを選択して下さい。");
	}

	public void initialize(IType type, IMethod method) {
		this.type = type;
		this.methodList = getMethodList(type);
		this.firstSelectedMethod = method;
	}

	protected List<IMethod> getMethodList(IType type) {
		List<IMethod> list = new ArrayList<IMethod>();
		try {
			IJavaElement[] children = type.getChildren();
			for (IJavaElement element : children) {
				if (element instanceof IMethod) {
					IMethod method = (IMethod) element;
					list.add(method);
				}
			}
		} catch (JavaModelException e) {
		}
		return list;
	}

	@Override
	protected Composite createComposite(Composite parent) {
		Font font = parent.getFont();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setFont(font);

		viewer = new TreeViewer(composite);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 128;
		viewer.getTree().setLayoutData(data);
		viewer.setContentProvider(createViewerContentProvider());
		viewer.setLabelProvider(createViewerLabelProvider());
		viewer.addSelectionChangedListener(SELECT_CHANGE_REFRESH_LISTENER);

		return composite;
	}

	protected ITreeContentProvider createViewerContentProvider() {
		return new ContentProvider();
	}

	protected static class ContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			@SuppressWarnings("unchecked")
			List<IMethod> list = (List<IMethod>) inputElement;
			return list.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	}

	protected ILabelProvider createViewerLabelProvider() {
		return new WorkbenchLabelProvider();
	}

	private boolean first = true;

	@Override
	public void setVisible(boolean visible) {
		if (visible && first) {
			first = false;
			viewer.setInput(methodList);
			StructuredSelection selection = new StructuredSelection(firstSelectedMethod);
			viewer.setSelection(selection, true);
		}
		super.setVisible(visible);
	}

	@Override
	protected String validate() {
		if (getSelectedMethod() == null) {
			return "メソッドを選択して下さい。";
		}
		return null;
	}

	public IMethod getSelectedMethod() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		return (IMethod) selection.getFirstElement();
	}
}
