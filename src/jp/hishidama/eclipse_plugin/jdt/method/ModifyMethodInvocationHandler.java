package jp.hishidama.eclipse_plugin.jdt.method;

import java.util.List;

import jp.hishidama.eclipse_plugin.util.FileUtil;
import jp.hishidama.eclipse_plugin.util.JdtUtil;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

public class ModifyMethodInvocationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		if (editorPart == null || !(editorPart instanceof ITextEditor)) {
			return null;
		}
		ITextEditor editor = (ITextEditor) editorPart;

		MethodCallFinder finder = getMethod(editor);
		if (finder != null) {
			IMethod method = finder.getMethod();
			if (method != null) {
				IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());

				ModifyMethodInvocationWizard wizard = new ModifyMethodInvocationWizard();
				wizard.init(method, finder.getArguments(), document, finder.getNode());
				WizardDialog dialog = new WizardDialog(null, wizard);
				dialog.open();
			}
		}
		return null;
	}

	private MethodCallFinder getMethod(ITextEditor editor) {
		ISelectionProvider provider = editor.getSite().getSelectionProvider();
		if (provider == null) {
			return null;
		}
		ISelection s = provider.getSelection();
		if (s == null || !(s instanceof ITextSelection)) {
			return null;
		}
		ITextSelection selection = (ITextSelection) s;
		int offset = selection.getOffset();

		IFile file = FileUtil.getFile(editor);
		if (file == null) {
			return null;
		}
		ICompilationUnit unit = JdtUtil.getJavaUnit(file);
		if (unit == null) {
			return null;
		}

		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		ASTNode node = parser.createAST(new NullProgressMonitor());
		MethodCallFinder visitor = new MethodCallFinder(offset);
		node.accept(visitor);
		return visitor;
	}

	static class MethodCallFinder extends ASTVisitor {
		private final int offset;

		private ASTNode found;
		private IMethod method;
		private List<Expression> arguments;

		public MethodCallFinder(int offset) {
			this.offset = offset;
		}

		public ASTNode getNode() {
			return found;
		}

		public IMethod getMethod() {
			return method;
		}

		public List<Expression> getArguments() {
			return arguments;
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			int offset = node.getStartPosition();
			int length = node.getLength();
			return offset <= this.offset && this.offset <= offset + length;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean visit(ClassInstanceCreation node) {
			this.found = node;
			IMethodBinding bind = node.resolveConstructorBinding();
			this.method = (IMethod) bind.getJavaElement();
			this.arguments = node.arguments();
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean visit(MethodInvocation node) {
			this.found = node;
			IMethodBinding bind = node.resolveMethodBinding();
			this.method = (IMethod) bind.getJavaElement();
			this.arguments = node.arguments();
			return true;
		}
	}
}
