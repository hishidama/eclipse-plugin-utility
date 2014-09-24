package jp.hishidama.eclipse_plugin.jdt.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.hishidama.eclipse_plugin.util.StringUtil;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class JavadocUtil {

	public static Javadoc getJavadoc(final IType type) {
		if (type == null) {
			return null;
		}
		JavadocFinder visitor = new JavadocFinder(type) {
			@Override
			public boolean visit(TypeDeclaration node) {
				try {
					if (!type.isEnum()) {
						if (StringUtil.equals(node.getName().getIdentifier(), type.getElementName())) {
							this.javadoc = node.getJavadoc();
							return false;
						}
					}
				} catch (JavaModelException e) {
				}
				return true;
			}

			@Override
			public boolean visit(EnumDeclaration node) {
				try {
					if (type.isEnum()) {
						if (StringUtil.equals(node.getName().getIdentifier(), type.getElementName())) {
							this.javadoc = node.getJavadoc();
							return false;
						}
					}
				} catch (JavaModelException e) {
				}
				return true;
			}
		};
		return getJavadoc(type.getCompilationUnit(), visitor);
	}

	public static Javadoc getJavadoc(IField field) {
		if (field == null) {
			return null;
		}
		JavadocFinder visitor = new JavadocFinder(field) {
			@Override
			public boolean visit(FieldDeclaration node) {
				this.javadoc = node.getJavadoc();
				return false;
			}
		};
		return getJavadoc(field.getCompilationUnit(), visitor);
	}

	public static Javadoc getJavadoc(IMethod method) {
		if (method == null) {
			return null;
		}
		JavadocFinder visitor = new JavadocFinder(method) {
			@Override
			public boolean visit(MethodDeclaration node) {
				this.javadoc = node.getJavadoc();
				return false;
			}
		};
		return getJavadoc(method.getCompilationUnit(), visitor);
	}

	private static Javadoc getJavadoc(ICompilationUnit cu, JavadocFinder visitor) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(cu);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(new NullProgressMonitor());

		astRoot.accept(visitor);

		return visitor.javadoc;
	}

	private static class JavadocFinder extends ASTVisitor {

		private int offset;
		public Javadoc javadoc;

		public JavadocFinder(ISourceReference element) {
			try {
				this.offset = element.getSourceRange().getOffset();
			} catch (JavaModelException e) {
				this.offset = -1;
			}
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			int offset = node.getStartPosition();
			int length = node.getLength();
			return (offset <= this.offset) && (this.offset < offset + length);
		}
	}

	@SuppressWarnings("unchecked")
	public static String getHeader(Javadoc javadoc) {
		if (javadoc == null) {
			return null;
		}

		List<TagElement> tlist = javadoc.tags();
		for (TagElement tag : tlist) {
			if (tag.getTagName() == null) {
				return toString(tag);
			}
		}
		return null;
	}

	private static String toString(TagElement tag) {
		StringBuilder sb = new StringBuilder(128);

		List<?> flist = tag.fragments();
		for (Object obj : flist) {
			if (obj instanceof Name) {
				sb.append(((Name) obj).getFullyQualifiedName());
			} else if (obj instanceof TextElement) {
				sb.append(((TextElement) obj).getText());
			} else {
				sb.append(obj);
			}
		}

		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> getParamMap(Javadoc javadoc) {
		if (javadoc == null) {
			return Collections.emptyMap();
		}

		List<TagElement> tlist = javadoc.tags();
		Map<String, String> map = new LinkedHashMap<String, String>(tlist.size());
		for (TagElement tag : tlist) {
			if (TagElement.TAG_PARAM.equals(tag.getTagName())) {
				List<?> flist = tag.fragments();
				String name = null, value = null;
				try {
					name = ((SimpleName) flist.get(0)).getIdentifier();
					value = ((TextElement) flist.get(1)).getText();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (name != null) {
					map.put(name, value);
				}
			}
		}
		return map;
	}
}
