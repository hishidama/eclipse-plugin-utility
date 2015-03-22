package jp.hishidama.eclipse_plugin.jdt.search;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchScope;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.SearchResultUpdater;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;

@SuppressWarnings("restriction")
public class StackTraceFileSearchQuery extends FileSearchQuery {

	private StackTraceFileSearchData fData;
	private StackTraceFileSearchResult fResult;

	public StackTraceFileSearchQuery(StackTraceFileSearchData data, FileTextSearchScope scope) {
		super("specified stack trace", false, true, false, scope);
		this.fData = data;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		AbstractTextSearchResult textResult = getSearchResult();
		textResult.removeAll();

		TextSearchScope scope = getSearchScope();
		StackTraceFileSearchRequestor requestor = new StackTraceFileSearchRequestor(fData, textResult);
		Pattern searchPattern = Pattern.compile(".*");

		return TextSearchEngine.create().search(scope, requestor, searchPattern, monitor);
	}

	@Override
	public AbstractTextSearchResult getSearchResult() {
		if (fResult == null) {
			fResult = new StackTraceFileSearchResult(this);
			new SearchResultUpdater(fResult);
		}
		return fResult;
	}
}
