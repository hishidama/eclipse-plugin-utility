package jp.hishidama.eclipse_plugin.wizard.page;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;

public class ExcelFileSelectionWizardPage extends ProjectFileSelectionWizardPage {

	public ExcelFileSelectionWizardPage(IStructuredSelection selection) {
		super("ExcelFileSelectionWizardPage", "Excel File",
				"Select a existing excel file or input a new excel file name.", selection);
	}

	@Override
	protected boolean filterFile(IFile file) {
		String ext = file.getFileExtension();
		return "xls".equals(ext) || "xlsx".equals(ext);
	}

	@Override
	protected String validateFile(IFile file) {
		if (!filterFile(file)) {
			return "File extension is not an Excel file.";
		}
		return null;
	}
}
