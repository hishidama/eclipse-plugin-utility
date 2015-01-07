package jp.hishidama.eclipse_plugin.jdt.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
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
		IType type = (IType) element;
		if (type.isMember()) {
			return;
		}
		ICompilationUnit unit = type.getCompilationUnit();
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
		private final int offset;
		private final StringVisitor visitor = new StringVisitor();

		public AcceptVisitor(SearchMatch match) {
			this.match = match;
			this.offset = match.getOffset();
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			return include(node);
		}

		protected boolean include(ASTNode node) {
			int offset = node.getStartPosition();
			int length = node.getLength();
			return offset <= this.offset && this.offset <= offset + length;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			return visitDeclaration(node);
		}

		@Override
		public boolean visit(EnumDeclaration node) {
			return visitDeclaration(node);
		}

		@Override
		public boolean visit(AnnotationTypeDeclaration node) {
			return visitDeclaration(node);
		}

		private boolean visitDeclaration(AbstractTypeDeclaration node) {
			SimpleName name = node.getName();
			if (include(name)) {
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

			private Object getElement(ASTNode node0) {
				for (ASTNode node = node0; node != null; node = node.getParent()) {
					IBinding bind = null;
					IJavaElement element = null;
					if (node instanceof MethodDeclaration) {
						bind = ((MethodDeclaration) node).resolveBinding();
					} else if (node instanceof VariableDeclaration) {
						bind = ((VariableDeclaration) node).resolveBinding();
					} else if (node instanceof AbstractTypeDeclaration) {
						bind = ((AbstractTypeDeclaration) node).resolveBinding();
					} else if (node instanceof Annotation) {
						bind = ((Annotation) node).resolveAnnotationBinding();
					} else if (node instanceof MemberValuePair) {
						bind = ((MemberValuePair) node).resolveMemberValuePairBinding();
					} else if (node instanceof EnumConstantDeclaration) {
						bind = ((EnumConstantDeclaration) node).resolveVariable();
					} else if (node instanceof AnnotationTypeMemberDeclaration) {
						bind = ((AnnotationTypeMemberDeclaration) node).resolveBinding();
					} else if (node instanceof Initializer) {
						element = findElement(node);
					}
					if (bind != null) {
						element = bind.getJavaElement();
					}
					if (element != null) {
						return element;
					}
				}

				IJavaElement element = findElement(node0);
				if (element != null) {
					return element;
				}

				return match.getElement();
			}

			private IJavaElement findElement(ASTNode node) {
				IType type = findType(node);
				if (type == null) {
					return null;
				}
				try {
					for (IJavaElement child : type.getChildren()) {
						if (child instanceof ISourceReference) {
							ISourceRange range = ((ISourceReference) child).getSourceRange();
							int offset = range.getOffset();
							int length = range.getLength();
							if (offset <= node.getStartPosition() && node.getStartPosition() < offset + length) {
								return child;
							}
						}
					}
				} catch (JavaModelException e) {
				}
				return null;
			}

			private IType findType(ASTNode node) {
				for (; node != null; node = node.getParent()) {
					if (node instanceof AbstractTypeDeclaration) {
						ITypeBinding bind = ((AbstractTypeDeclaration) node).resolveBinding();
						return (IType) bind.getJavaElement();
					}
				}
				return null;
			}
		}
	}
}
