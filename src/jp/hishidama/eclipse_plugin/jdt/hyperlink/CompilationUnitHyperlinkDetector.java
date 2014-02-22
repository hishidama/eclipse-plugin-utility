package jp.hishidama.eclipse_plugin.jdt.hyperlink;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.WorkingCopyManager;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

@SuppressWarnings("restriction")
public abstract class CompilationUnitHyperlinkDetector extends AbstractHyperlinkDetector {

	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		ITextEditor editor = (ITextEditor) getAdapter(ITextEditor.class);
		if (editor == null) {
			return null;
		}
		ICompilationUnit unit = getCompilationUnit(editor);
		if (unit == null) {
			return null;
		}

		return detectHyperlinks(unit, region);
	}

	protected ICompilationUnit getCompilationUnit(ITextEditor editor) {
		IEditorInput input = editor.getEditorInput();
		WorkingCopyManager manager = JavaPlugin.getDefault().getWorkingCopyManager();
		return manager.getWorkingCopy(input, false);
	}

	protected abstract IHyperlink[] detectHyperlinks(ICompilationUnit unit, IRegion region);
}
