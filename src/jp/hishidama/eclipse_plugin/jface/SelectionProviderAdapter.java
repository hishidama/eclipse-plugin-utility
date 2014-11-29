package jp.hishidama.eclipse_plugin.jface;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

public class SelectionProviderAdapter implements ISelectionProvider {

	private final List<ISelectionChangedListener> listeners = new ArrayList<ISelectionChangedListener>();

	private ISelection theSelection = StructuredSelection.EMPTY;

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		theSelection = selection;

		final SelectionChangedEvent event = new SelectionChangedEvent(this, selection);

		for (final ISelectionChangedListener listener : listeners) {
			SafeRunner.run(new SafeRunnable() {
				@Override
				public void run() {
					listener.selectionChanged(event);
				}
			});
		}
	}

	@Override
	public ISelection getSelection() {
		return theSelection;
	}
}
