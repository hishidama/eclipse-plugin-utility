package jp.hishidama.eclipse_plugin.jdt.util;

import java.util.List;

import jp.hishidama.eclipse_plugin.util.JdtUtil;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.CodeStyleConfiguration;

public class AstRewriteUtility {

	protected CompilationUnit astRoot;
	protected AST ast;

	private ASTRewrite astRewrite;
	private ImportRewrite importRewrite;

	protected void initializeAst(ICompilationUnit cu) {
		ASTParser parser = JdtUtil.newASTParser();
		parser.setSource(cu);
		astRoot = (CompilationUnit) parser.createAST(new NullProgressMonitor());
		astRoot.recordModifications();
		ast = astRoot.getAST();
	}

	protected ASTRewrite getAstRewrite() {
		if (astRewrite == null) {
			astRewrite = ASTRewrite.create(ast);
		}
		return astRewrite;
	}

	protected ImportRewrite getImportRewrite() {
		if (importRewrite == null) {
			importRewrite = CodeStyleConfiguration.createImportRewrite(astRoot, true);
		}
		return importRewrite;
	}

	protected final MethodDeclaration findConstructor(ListRewrite listRewrite) {
		for (Object object : listRewrite.getRewrittenList()) {
			if (object instanceof MethodDeclaration) {
				MethodDeclaration method = (MethodDeclaration) object;
				if (method.isConstructor()) {
					return method;
				}
			}
		}
		return null;
	}

	protected final FieldDeclaration findLastField(ListRewrite listRewrite) {
		FieldDeclaration found = null;
		for (Object object : listRewrite.getRewrittenList()) {
			if (object instanceof FieldDeclaration) {
				found = (FieldDeclaration) object;
			}
		}
		return found;
	}

	protected final MethodDeclaration findMethodDeclaration(final int offset) {
		final MethodDeclaration[] found = new MethodDeclaration[1];
		astRoot.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getStartPosition() <= offset && offset < node.getStartPosition() + node.getLength()) {
					found[0] = node;
				}
				return false;
			}
		});
		return found[0];
	}

	@SuppressWarnings("unchecked")
	protected final String getFieldName(FieldDeclaration field) {
		List<VariableDeclarationFragment> flist = field.fragments();
		if (flist.size() <= 0) {
			return null;
		}
		return flist.get(0).getName().getIdentifier();
	}

	protected final String getJavadocParamName(TagElement tag) {
		List<?> flist = tag.fragments();
		if (flist.size() < 1) {
			return null;
		}
		Object obj = flist.get(0);
		if (obj instanceof SimpleName) {
			SimpleName name = (SimpleName) obj;
			return name.getIdentifier();
		}
		return null;
	}

	// Type

	protected final Type newType(String typeName) {
		if (isTypeParameter(typeName)) {
			return ast.newSimpleType(ast.newSimpleName(typeName));
		}

		Code typeCode = PrimitiveType.toCode(typeName);
		if (typeCode != null) {
			return ast.newPrimitiveType(typeCode);
		}

		String t = getImportRewrite().addImport(typeName);
		if (t.contains(".")) {
			return ast.newSimpleType(ast.newName(t));
		}
		return ast.newSimpleType(ast.newSimpleName(t));
	}

	protected boolean isTypeParameter(String typeName) {
		return false;
	}

	@SuppressWarnings("unchecked")
	protected final Type newType(String typeName, String parameterTypeName) {
		ParameterizedType t = ast.newParameterizedType(newType(typeName));
		List<Type> tlist = t.typeArguments();
		tlist.add(newType(parameterTypeName));
		return t;
	}

	// Annotation

	protected final MarkerAnnotation newMarkerAnnotation(String name) {
		MarkerAnnotation a = ast.newMarkerAnnotation();
		a.setTypeName(ast.newName(getImportRewrite().addImport(name)));
		return a;
	}

	protected final SingleMemberAnnotation newSingleMemberAnnotation(String name, String value) {
		SingleMemberAnnotation a = ast.newSingleMemberAnnotation();
		a.setTypeName(ast.newName(getImportRewrite().addImport(name)));
		a.setValue(ast.newName(value));
		return a;
	}

	protected final NormalAnnotation newNormalAnnotation(String name) {
		NormalAnnotation a = ast.newNormalAnnotation();
		a.setTypeName(ast.newName(getImportRewrite().addImport(name)));
		return a;
	}

	@SuppressWarnings("unchecked")
	protected final void addTo(NormalAnnotation annotation, MemberValuePair member) {
		List<MemberValuePair> vlist = annotation.values();
		vlist.add(member);
	}

	protected final MemberValuePair newMemberValuePair(String name, String value) {
		StringLiteral s = ast.newStringLiteral();
		s.setLiteralValue(value);
		return newMemberValuePair(name, s);
	}

	protected final MemberValuePair newMemberValuePair(String name, Expression value) {
		MemberValuePair pair = ast.newMemberValuePair();
		pair.setName(ast.newSimpleName(name));

		pair.setValue(value);

		return pair;
	}

	@SuppressWarnings("unchecked")
	protected final MemberValuePair newMemberValuePair(String name, List<String> values) {
		MemberValuePair pair = ast.newMemberValuePair();
		pair.setName(ast.newSimpleName(name));

		ArrayInitializer array = ast.newArrayInitializer();
		List<Expression> alist = array.expressions();
		for (String value : values) {
			StringLiteral s = ast.newStringLiteral();
			s.setLiteralValue(value);
			alist.add(s);
		}
		pair.setValue(array);

		return pair;
	}

	// Field

	protected final FieldDeclaration newFieldDeclaration(String typeName, String varName, boolean instanceCreation) {
		ClassInstanceCreation creation = null;
		if (instanceCreation) {
			creation = ast.newClassInstanceCreation();
			creation.setType(newType(typeName));
		}

		return newFieldDeclaration(typeName, varName, creation);
	}

	@SuppressWarnings("unchecked")
	protected final FieldDeclaration newFieldDeclaration(String typeName, String varName, Expression initializer) {
		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(varName));
		if (initializer != null) {
			fragment.setInitializer(initializer);
		}

		FieldDeclaration field = ast.newFieldDeclaration(fragment);
		field.setType(newType(typeName));

		field.modifiers().addAll(ast.newModifiers(Modifier.PRIVATE | Modifier.FINAL));
		return field;
	}

	protected final FieldAccess newThisFieldAccess(String fieldName) {
		FieldAccess a = ast.newFieldAccess();
		a.setExpression(ast.newThisExpression());
		a.setName(ast.newSimpleName(fieldName));
		return a;
	}

	// Method

	protected final MethodDeclaration newMethodDeclaration(String name) {
		MethodDeclaration m = ast.newMethodDeclaration();
		m.setName(ast.newSimpleName(name));
		return m;
	}

	// Block

	protected final Block newEmptyBlock() {
		return ast.newBlock();
	}

	@SuppressWarnings("unchecked")
	protected final Block newReturnNullBlock() {
		Block block = ast.newBlock();
		List<Statement> slist = block.statements();
		slist.add(newReturnStatement(ast.newNullLiteral()));
		return block;
	}

	// Statement

	/**
	 * typeName varName = new typeName();
	 * 
	 * @param typeName
	 *            クラス名
	 * @param varName
	 *            変数名
	 * @return VariableDeclarationStatement
	 */
	protected final VariableDeclarationStatement newVariableDeclarationStatement(String typeName, String varName) {
		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(newType(typeName));

		return newVariableDeclarationStatement(typeName, varName, creation);
	}

	protected final VariableDeclarationStatement newVariableDeclarationStatement(String typeName, String varName, Expression initializer) {
		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(varName));
		fragment.setInitializer(initializer);

		VariableDeclarationStatement statement = ast.newVariableDeclarationStatement(fragment);
		statement.setType(newType(typeName));
		return statement;
	}

	protected final ExpressionStatement newMethodInvocationStatement(String objectName, String methodName, Expression... arguments) {
		return ast.newExpressionStatement(newMethodInvocation(objectName, methodName, arguments));
	}

	protected final ReturnStatement newReturnStatement(Expression expression) {
		ReturnStatement statement = ast.newReturnStatement();
		statement.setExpression(expression);
		return statement;
	}

	// Expression

	@SuppressWarnings("unchecked")
	protected final MethodInvocation newMethodInvocation(String objectName, String methodName, Expression... arguments) {
		MethodInvocation method = ast.newMethodInvocation();
		if (objectName != null) {
			method.setExpression(ast.newSimpleName(objectName));
		}
		method.setName(ast.newSimpleName(methodName));

		List<Expression> alist = method.arguments();
		for (Expression arg : arguments) {
			alist.add(arg);
		}

		return method;
	}

	// Javadoc

	@SuppressWarnings("unchecked")
	protected final Javadoc newJavadoc(String comment) {
		Javadoc javadoc = ast.newJavadoc();
		if (comment != null) {
			TextElement text = ast.newTextElement();
			text.setText(comment);

			TagElement tag = ast.newTagElement();
			List<TextElement> flist = tag.fragments();
			flist.add(text);

			List<TagElement> tlist = javadoc.tags();
			tlist.add(tag);
		}
		return javadoc;
	}

	@SuppressWarnings("unchecked")
	protected final void addJavadocParam(Javadoc javadoc, String name, String description) {
		TagElement tag = ast.newTagElement();
		tag.setTagName("@param");

		tag.fragments().add(ast.newSimpleName(name));

		TextElement text = ast.newTextElement();
		text.setText(description);
		tag.fragments().add(text);

		javadoc.tags().add(tag);
	}

	@SuppressWarnings("unchecked")
	protected final void addJavadocReturn(Javadoc javadoc, String description) {
		TagElement tag = ast.newTagElement();
		tag.setTagName("@return");

		TextElement text = ast.newTextElement();
		text.setText(description);
		tag.fragments().add(text);

		javadoc.tags().add(tag);
	}

	protected final TextElement newTextElement(String text) {
		TextElement elem = ast.newTextElement();
		if (text != null) {
			elem.setText(text);
		}
		return elem;
	}
}
