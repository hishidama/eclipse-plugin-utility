package jp.hishidama.eclipse_plugin.jdt.hyperlink;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

// @see org.eclipse.jdt.internal.ui.javaeditor.JavaElementHyperlinkDetector
public abstract class JdtHyperlinkDetector extends AbstractHyperlinkDetector {

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		ITextEditor editor = (ITextEditor) getAdapter(ITextEditor.class);
		if (editor == null) {
			return null;
		}
		int offset = region.getOffset();
		return detectHyperlinks(editor, offset);
	}

	public IHyperlink[] detectHyperlinks(ITextEditor editor, int offset) {
		IEditorInput input = editor.getEditorInput();
		IJavaElement element = (IJavaElement) input.getAdapter(IJavaElement.class);

		IDocument document = editor.getDocumentProvider().getDocument(input);
		IRegion word = findWord(document, offset);
		if (word == null || word.getLength() == 0) {
			return null;
		}
		IJavaElement[] codes;
		try {
			ITypeRoot root = (ITypeRoot) element.getAdapter(ITypeRoot.class);
			codes = root.codeSelect(word.getOffset(), word.getLength());
		} catch (JavaModelException e) {
			return null;
		}
		for (IJavaElement code : codes) {
			int elementType = code.getElementType();
			switch (elementType) {
			case IJavaElement.TYPE:
				IHyperlink[] tr = detectTypeHyperlinks((IType) code, word);
				if (tr != null) {
					return tr;
				}
				break;
			case IJavaElement.FIELD:
				IHyperlink[] fr = detectFieldHyperlinks((IField) code, word);
				if (fr != null) {
					return fr;
				}
				break;
			case IJavaElement.METHOD:
				IHyperlink[] mr = detectMethodHyperlinks((IMethod) code, word);
				if (mr != null) {
					return mr;
				}
				break;
			case IJavaElement.LOCAL_VARIABLE:
				IHyperlink[] vr = detectVariableHyperlinks((ILocalVariable) code, word);
				if (vr != null) {
					return vr;
				}
				break;
			}
		}
		return null;
	}

	private static IRegion findWord(IDocument document, int offset) {
		int start = -2;
		int end = -1;
		try {
			int pos;
			for (pos = offset; pos >= 0; pos--) {
				char c = document.getChar(pos);
				if (!Character.isJavaIdentifierPart(c)) {
					break;
				}
			}

			start = pos;
			pos = offset;
			for (int length = document.getLength(); pos < length; pos++) {
				char c = document.getChar(pos);
				if (!Character.isJavaIdentifierPart(c)) {
					break;
				}
			}

			end = pos;
		} catch (BadLocationException e) {
		}
		if (start >= -1 && end > -1) {
			if (start == offset && end == offset) {
				return new Region(offset, 0);
			}
			if (start == offset) {
				return new Region(start, end - start);
			} else {
				return new Region(start + 1, end - start - 1);
			}
		} else {
			return null;
		}
	}

	protected IHyperlink[] detectTypeHyperlinks(IType type, IRegion word) {
		return null;
	}

	protected IHyperlink[] detectFieldHyperlinks(IField field, IRegion word) {
		return null;
	}

	protected IHyperlink[] detectMethodHyperlinks(IMethod method, IRegion word) {
		return null;
	}

	protected IHyperlink[] detectVariableHyperlinks(ILocalVariable variable, IRegion word) {
		return null;
	}
}
