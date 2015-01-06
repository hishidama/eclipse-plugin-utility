package jp.hishidama.eclipse_plugin.jdt.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;

public class JavaStringSearchRequestor extends SearchRequestor {

	private JavaStringSearchResult result;
	private Matcher matcher;

	public JavaStringSearchRequestor(JavaStringSearchData data, JavaStringSearchResult result) {
		this.result = result;

		Pattern pattern = data.getSearchPattern();
		this.matcher = pattern.matcher("");
	}

	@Override
	public void acceptSearchMatch(SearchMatch match) throws CoreException {
		IJavaElement element = (IJavaElement) match.getElement();
		ICompilationUnit unit = ((IType) element).getCompilationUnit();
		accept(unit, match);
	}

	private void accept(ICompilationUnit unit, SearchMatch match) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		ASTNode node = parser.createAST(new NullProgressMonitor());
		AcceptVisitor visitor = new AcceptVisitor(match);
		node.accept(visitor);
	}

	private class AcceptVisitor extends ASTVisitor {

		private final SearchMatch match;
		private final StringVisitor visitor = new StringVisitor();

		public AcceptVisitor(SearchMatch match) {
			this.match = match;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			SimpleName name = node.getName();
			if (name.getStartPosition() <= match.getOffset()
					&& match.getOffset() < name.getStartPosition() + name.getLength()) {
				node.accept(visitor);
			}
			return true;
		}

		private class StringVisitor extends ASTVisitor {

			@Override
			public boolean visit(StringLiteral node) {
				String literal = node.getLiteralValue();
				matcher.reset(literal);
				while (matcher.find()) {
					int start = matcher.start();
					int end = matcher.end();
					int length = end - start;
					result.addMatch(getElement(node), node.getStartPosition() + 1 + start, length);
				}
				return false;
			}

			private Object getElement(ASTNode node) {
				for (; node != null; node = node.getParent()) {
					if (node instanceof MethodDeclaration) {
						IMethodBinding bind = ((MethodDeclaration) node).resolveBinding();
						IJavaElement element = bind.getJavaElement();
						if (element != null) {
							return element;
						}
					}
				}
				return match.getElement();
			}
		}
	}
}
