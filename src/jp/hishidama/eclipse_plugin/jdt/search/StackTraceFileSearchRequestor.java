package jp.hishidama.eclipse_plugin.jdt.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.internal.ui.text.FileMatch;
import org.eclipse.search.internal.ui.text.LineElement;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

// @see org.eclipse.search.internal.ui.text.FileSearchQuery.TextSearchResultCollector
@SuppressWarnings("restriction")
public class StackTraceFileSearchRequestor extends TextSearchRequestor {

	private final StackTraceFileSearchData fData;
	private final AbstractTextSearchResult fResult;
	private List<FileMatch> fCachedMatches;

	public StackTraceFileSearchRequestor(StackTraceFileSearchData data, AbstractTextSearchResult result) {
		this.fData = data;
		this.fResult = result;
	}

	@Override
	public boolean acceptFile(IFile file) throws CoreException {
		if (fData.acceptFile(file)) {
			flushMatches();
			return true;
		}
		return false;
	}

	@Override
	public boolean acceptPatternMatch(TextSearchMatchAccess matchRequestor) throws CoreException {
		int matchOffset = matchRequestor.getMatchOffset();

		LineElement lineElement = getLineElement(matchOffset, matchRequestor);
		if (lineElement != null) {
			Set<Integer> lines = fData.getLines(matchRequestor.getFile());
			if (lines.contains(lineElement.getLine())) {
				FileMatch fileMatch = new FileMatch(matchRequestor.getFile(), matchOffset,
						matchRequestor.getMatchLength(), lineElement);
				fCachedMatches.add(fileMatch);
			}
		}
		return true;
	}

	private LineElement getLineElement(int offset, TextSearchMatchAccess matchRequestor) {
		int lineNumber = 1;
		int lineStart = 0;
		if (!fCachedMatches.isEmpty()) {
			// match on same line as last?
			FileMatch last = (FileMatch) fCachedMatches.get(fCachedMatches.size() - 1);
			LineElement lineElement = last.getLineElement();
			if (lineElement.contains(offset)) {
				return lineElement;
			}
			// start with the offset and line information from the last match
			lineStart = lineElement.getOffset() + lineElement.getLength();
			lineNumber = lineElement.getLine() + 1;
		}
		if (offset < lineStart) {
			return null; // offset before the last line
		}

		int i = lineStart;
		int contentLength = matchRequestor.getFileContentLength();
		while (i < contentLength) {
			char ch = matchRequestor.getFileContentChar(i++);
			if (ch == '\n' || ch == '\r') {
				if (ch == '\r' && i < contentLength && matchRequestor.getFileContentChar(i) == '\n') {
					i++;
				}
				if (offset < i) {
					// include line delimiter
					String lineContent = getContents(matchRequestor, lineStart, i);
					return new LineElement(matchRequestor.getFile(), lineNumber, lineStart, lineContent);
				}
				lineNumber++;
				lineStart = i;
			}
		}
		if (offset < i) {
			// until end of file
			String lineContent = getContents(matchRequestor, lineStart, i);
			return new LineElement(matchRequestor.getFile(), lineNumber, lineStart, lineContent);
		}
		return null; // offset outside of range
	}

	private static String getContents(TextSearchMatchAccess matchRequestor, int start, int end) {
		StringBuilder buf = new StringBuilder(end - start);
		for (int i = start; i < end; i++) {
			char ch = matchRequestor.getFileContentChar(i);
			if (Character.isWhitespace(ch) || Character.isISOControl(ch)) {
				buf.append(' ');
			} else {
				buf.append(ch);
			}
		}
		return buf.toString();
	}

	@Override
	public void beginReporting() {
		fCachedMatches = new ArrayList<FileMatch>();
	}

	@Override
	public void endReporting() {
		flushMatches();
		fCachedMatches = null;
	}

	private void flushMatches() {
		if (!fCachedMatches.isEmpty()) {
			fResult.addMatches((Match[]) fCachedMatches.toArray(new Match[fCachedMatches.size()]));
			fCachedMatches.clear();
		}
	}
}
