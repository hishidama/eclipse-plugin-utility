package jp.hishidama.eclipse_plugin.jdt.method;

import java.util.List;

import jp.hishidama.eclipse_plugin.util.internal.LogUtil;
import jp.hishidama.eclipse_plugin.wizard.page.SelectMethodPage;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

/**
 * メソッド呼び出しの引数を修正するウィザード.
 */
public class ModifyMethodInvocationWizard extends Wizard {

	private IMethod method;
	private List<Expression> arguments;
	private IDocument document;
	private ASTNode node;

	private SelectMethodPage methodPage;
	private ModifyMethodArgumentPage argumentPage;

	public ModifyMethodInvocationWizard() {
		setWindowTitle("Modify Method Call");
	}

	public void init(IMethod method, List<Expression> arguments, IDocument document, ASTNode node) {
		this.method = method;
		this.arguments = arguments;
		this.document = document;
		this.node = node;
	}

	@Override
	public void addPages() {
		methodPage = new SelectMethodPage();
		methodPage.initialize(method.getDeclaringType(), method);
		addPage(methodPage);

		argumentPage = new ModifyMethodArgumentPage();
		addPage(argumentPage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage nextPage = super.getNextPage(page);
		if (nextPage == argumentPage) {
			method = methodPage.getSelectedMethod();
			argumentPage.initialize(method, arguments);
		}
		return nextPage;
	}

	@Override
	public boolean canFinish() {
		return argumentPage.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		try {
			if (node instanceof MethodInvocation) {
				replaceMethodInvocation((MethodInvocation) node);
			} else {
				replaceClassInstanceCreation((ClassInstanceCreation) node);
			}
		} catch (BadLocationException e) {
			LogUtil.logError("ModifyMethodInvocationWizard#performFinish() error.", e);
			return false;
		}
		return true;
	}

	protected void replaceMethodInvocation(MethodInvocation node) throws BadLocationException {
		SimpleName name = node.getName();
		int offset = name.getStartPosition();
		int end = node.getStartPosition() + node.getLength();
		replace(offset, end);
	}

	protected void replaceClassInstanceCreation(ClassInstanceCreation node) throws BadLocationException {
		int offset = node.getStartPosition();
		int end = node.getStartPosition() + node.getLength();

		Expression expression = node.getExpression();
		if (expression != null) {
			offset = expression.getStartPosition() + expression.getLength();
			for (int i = offset; i < end; i++) {
				char c = document.getChar(i);
				if (c == '.') {
					offset = i + 1;
					break;
				}
			}
		}
		replace(offset, end);
	}

	protected void replace(int offset, int end) throws BadLocationException {
		StringBuilder sb = new StringBuilder(128);

		IMethod method = methodPage.getSelectedMethod();
		try {
			if (method.isConstructor()) {
				if (!this.method.isConstructor()) {
					offset = node.getStartPosition();
				}
				sb.append("new ");
			}
		} catch (JavaModelException e) {
			// fall through
		}
		sb.append(method.getElementName());
		sb.append("(");
		List<String> list = argumentPage.getArguments();
		boolean first = true;
		for (String arg : list) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(arg);
		}
		sb.append(")");

		document.replace(offset, end - offset, sb.toString());
	}
}
