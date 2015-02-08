package jp.hishidama.eclipse_plugin.jdt.hyperlink;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.StringLiteral;
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
		if (element == null) {
			return null;
		}

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
				IMethod method = (IMethod) code;
				boolean c;
				try {
					c = method.isConstructor();
				} catch (JavaModelException e) {
					c = false;
				}
				if (c) {
					IHyperlink[] cr = detectConstructorHyperlinks(method, word);
					if (cr != null) {
						return cr;
					}
				} else {
					IHyperlink[] mr = detectMethodHyperlinks(method, word);
					if (mr != null) {
						return mr;
					}
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

		if (element instanceof ICompilationUnit) {
			ICompilationUnit unit = (ICompilationUnit) element;
			StringFinder finder = findString(unit, offset);
			if (finder.getWord() != null) {
				IHyperlink[] r = detectStringHyperlinks(finder.getWord(), finder.getRegion(),unit, finder.getNodeList());
				if (r != null) {
					return r;
				}
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

	protected StringFinder findString(ICompilationUnit unit, int offset) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		ASTNode node = parser.createAST(new NullProgressMonitor());

		StringFinder finder = new StringFinder(offset);
		node.accept(finder);

		return finder;
	}

	protected static class StringFinder extends ASTVisitor {
		private int offset;

		private String word = null;
		private IRegion region = null;
		private boolean found = false;
		private List<ASTNode> stack = null;

		public StringFinder(int offset) {
			this.offset = offset;
		}

		public String getWord() {
			return word;
		}

		public IRegion getRegion() {
			return region;
		}

		public List<ASTNode> getNodeList() {
			return stack;
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			if (found) {
				return false;
			}
			return include(node);
		}

		protected boolean include(ASTNode node) {
			int offset = node.getStartPosition();
			int length = node.getLength();
			return offset <= this.offset && this.offset <= offset + length;
		}

		@Override
		public void postVisit(ASTNode node) {
			if (found && include(node)) {
				if (stack == null) {
					stack = new ArrayList<ASTNode>();
				}
				stack.add(node);
			}
		}

		@Override
		public boolean visit(StringLiteral node) {
			word = node.getLiteralValue();
			region = new Region(node.getStartPosition() + 1, node.getLength() - 2);
			found = true;
			return false;
		}
	}

	protected IHyperlink[] detectTypeHyperlinks(IType type, IRegion word) {
		return null;
	}

	protected IHyperlink[] detectFieldHyperlinks(IField field, IRegion word) {
		return null;
	}

	protected IHyperlink[] detectConstructorHyperlinks(IMethod method, IRegion word) {
		return null;
	}

	protected IHyperlink[] detectMethodHyperlinks(IMethod method, IRegion word) {
		return null;
	}

	protected IHyperlink[] detectVariableHyperlinks(ILocalVariable variable, IRegion word) {
		return null;
	}

	protected IHyperlink[] detectStringHyperlinks(String text, IRegion word, ICompilationUnit unit, List<ASTNode> stack) {
		return null;
	}
}
