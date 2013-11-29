package jp.hishidama.eclipse_plugin.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class DocumentUtil {

	public static IDocument findEditorDocument(IFile file) {
		ITextEditor editor = FileUtil.findTextEditor(file);
		if (editor != null) {
			IDocumentProvider provider = editor.getDocumentProvider();
			return provider.getDocument(editor.getEditorInput());
		}
		return null;
	}
}
