package jp.hishidama.eclipse_plugin.jdt.search;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.search.ui.ISearchQuery;

public class JavaStringSearchQuery implements ISearchQuery {
	private final JavaStringSearchData data;
	private final IJavaSearchScope scope;
	private final String scopeDescription;

	private JavaStringSearchResult searchResult;

	public JavaStringSearchQuery(JavaStringSearchData data, IJavaSearchScope scope, String scopeDescription) {
		this.data = data;
		this.scope = scope;
		this.scopeDescription = scopeDescription;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		JavaStringSearchResult result = getSearchResult();
		result.removeAll();

		SearchPattern pattern = createSearchPattern();
		SearchParticipant[] participants = { SearchEngine.getDefaultSearchParticipant() };
		SearchRequestor requestor = new JavaStringSearchRequestor(data, result);
		try {
			new SearchEngine().search(pattern, participants, scope, requestor, monitor);
		} catch (CoreException e) {
			return e.getStatus();
		}

		return Status.OK_STATUS;
	}

	protected SearchPattern createSearchPattern() {
		String type = "*";
		int searchFor = IJavaSearchConstants.TYPE;
		int limitTo = IJavaSearchConstants.DECLARATIONS;
		int matchRule = SearchPattern.R_PATTERN_MATCH;
		return SearchPattern.createPattern(type, searchFor, limitTo, matchRule);
	}

	@Override
	public String getLabel() {
		return data.getTextPattern();
	}

	public String getResultLabel(int matchCount) {
		return MessageFormat.format("''{0}'' - {1} found in {2}", getLabel(), matchCount, scopeDescription);
	}

	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	@Override
	public JavaStringSearchResult getSearchResult() {
		if (searchResult == null) {
			searchResult = new JavaStringSearchResult(this);
		}
		return searchResult;
	}
}
