package jp.hishidama.eclipse_plugin.jdt.util;

import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.ui.CodeStyleConfiguration;

public class AstRewriteUtility {

	protected CompilationUnit astRoot;
	protected AST ast;

	private ASTRewrite astRewrite;
	private ImportRewrite importRewrite;

	protected void initializeAst(ICompilationUnit cu) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
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

	// Type

	protected final Type newType(String typeName) {
		Code typeCode = PrimitiveType.toCode(typeName);
		if (typeCode != null) {
			return ast.newPrimitiveType(typeCode);
		}

		String t = getImportRewrite().addImport(typeName);
		return ast.newSimpleType(ast.newSimpleName(t));
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
		MemberValuePair pair = ast.newMemberValuePair();
		pair.setName(ast.newSimpleName(name));

		StringLiteral s = ast.newStringLiteral();
		s.setLiteralValue(value);
		pair.setValue(s);

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
		ReturnStatement ret = ast.newReturnStatement();
		ret.setExpression(ast.newNullLiteral());

		Block block = ast.newBlock();
		List<Statement> slist = block.statements();
		slist.add(ret);
		return block;
	}

	// Javadoc

	@SuppressWarnings("unchecked")
	protected final Javadoc newJavadoc(String comment) {
		TextElement text = ast.newTextElement();
		text.setText(comment);

		TagElement tag = ast.newTagElement();
		List<TextElement> flist = tag.fragments();
		flist.add(text);

		Javadoc javadoc = ast.newJavadoc();
		List<TagElement> tlist = javadoc.tags();
		tlist.add(tag);

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
}
