package jp.hishidama.eclipse_plugin.jdt.search;

import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.search.internal.core.text.PatternConstructor;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

// @see org.eclipse.search.internal.ui.text.TextSearchPage.SearchPatternData
@SuppressWarnings("restriction")
public class JavaStringSearchData {
	private boolean isCaseSensitive;
	private boolean isRegExSearch;
	public boolean isWholeWord;
	private String textPattern;

	private int scope;
	private IWorkingSet[] workingSets;

	public JavaStringSearchData(String textPattern, boolean isCaseSensitive, boolean isRegExSearch,
			boolean isWholeWord, int scope, IWorkingSet[] workingSets) {
		this.textPattern = textPattern;
		this.isCaseSensitive = isCaseSensitive;
		this.isRegExSearch = isRegExSearch;
		this.isWholeWord = isWholeWord;
		this.scope = scope;
		this.workingSets = workingSets;
	}

	public String getTextPattern() {
		return textPattern;
	}

	public boolean isCaseSensitive() {
		return isCaseSensitive;
	}

	public boolean isRegExSearch() {
		return isRegExSearch;
	}

	public boolean isWholeWord() {
		return isWholeWord;
	}

	public int getScope() {
		return scope;
	}

	public IWorkingSet[] getWorkingSets() {
		return workingSets;
	}

	public Pattern getSearchPattern() {
		return PatternConstructor.createPattern(textPattern, isRegExSearch, true, isCaseSensitive, isWholeWord);
	}

	public void store(IDialogSettings settings) {
		settings.put("isCaseSensitive", isCaseSensitive); //$NON-NLS-1$
		settings.put("isRegExSearch", isRegExSearch); //$NON-NLS-1$
		settings.put("isWholeWord", isWholeWord); //$NON-NLS-1$
		settings.put("textPattern", textPattern); //$NON-NLS-1$
		settings.put("scope", scope); //$NON-NLS-1$
		if (workingSets != null) {
			String[] wsIds = new String[workingSets.length];
			for (int i = 0; i < workingSets.length; i++) {
				wsIds[i] = workingSets[i].getLabel();
			}
			settings.put("workingSets", wsIds); //$NON-NLS-1$
		} else {
			settings.put("workingSets", new String[0]); //$NON-NLS-1$
		}
	}

	public static JavaStringSearchData create(IDialogSettings settings) {
		String textPattern = settings.get("textPattern"); //$NON-NLS-1$
		String[] wsIds = settings.getArray("workingSets"); //$NON-NLS-1$
		IWorkingSet[] workingSets = null;
		if (wsIds != null && wsIds.length > 0) {
			IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
			workingSets = new IWorkingSet[wsIds.length];
			for (int i = 0; workingSets != null && i < wsIds.length; i++) {
				workingSets[i] = workingSetManager.getWorkingSet(wsIds[i]);
				if (workingSets[i] == null) {
					workingSets = null;
				}
			}
		}
		try {
			int scope = settings.getInt("scope"); //$NON-NLS-1$
			boolean isRegExSearch = settings.getBoolean("isRegExSearch"); //$NON-NLS-1$
			boolean isCaseSensitive = settings.getBoolean("isCaseSensitive"); //$NON-NLS-1$
			boolean isWholeWord = settings.getBoolean("isWholeWord"); //$NON-NLS-1$

			return new JavaStringSearchData(textPattern, isCaseSensitive, isRegExSearch, isWholeWord, scope,
					workingSets);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
