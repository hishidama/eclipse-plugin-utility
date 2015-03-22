package jp.hishidama.eclipse_plugin.jdt.search;

import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.ui.ISearchQuery;

@SuppressWarnings("restriction")
public class StackTraceFileSearchResult extends FileSearchResult {

	private StackTraceFileSearchQuery fQuery;

	public StackTraceFileSearchResult(StackTraceFileSearchQuery job) {
		super(null);
		fQuery = job;
	}

	public String getLabel() {
		return fQuery.getResultLabel(getMatchCount());
	}

	public ISearchQuery getQuery() {
		return fQuery;
	}
}
