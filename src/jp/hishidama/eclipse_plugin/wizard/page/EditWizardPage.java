package jp.hishidama.eclipse_plugin.wizard.page;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public abstract class EditWizardPage extends WizardPage {

	protected final ModifyListener MODIFY_REFRESH_LISTENER = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			validate(visible);
		}
	};

	protected final SelectionListener SELECT_REFRESH_LISTENER = new SelectionListener() {
		@Override
		public void widgetSelected(SelectionEvent e) {
			validate(visible);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			validate(visible);
		}
	};

	private boolean visible;

	public EditWizardPage(String pageName) {
		super(pageName);
	}

	// @Override
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent);
		setControl(composite);
		validate(false);
	}

	protected abstract Composite createComposite(Composite parent);

	protected final Label createLabel(Composite composite, String labelText) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(labelText);
		return label;
	}

	protected final Text createText(Composite composite, int span) {
		return createText(composite, span, MODIFY_REFRESH_LISTENER);
	}

	protected final Text createText(Composite composite, int span, ModifyListener listener) {
		Text text = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		// data.widthHint = widthHint;
		data.horizontalSpan = span;
		text.setLayoutData(data);
		if (listener != null) {
			text.addModifyListener(listener);
		}
		return text;
	}

	protected final Text createTextField(Composite composite, int span, String label) {
		createLabel(composite, label);
		return createText(composite, span);
	}

	protected final List<Button> createPushButtonField(Composite composite, int span, String label, String... text) {
		createLabel(composite, label);

		Composite field = new Composite(composite, SWT.NONE);
		{
			GridData data = new GridData();
			data.horizontalSpan = span;
			field.setLayoutData(data);
		}
		field.setLayout(new GridLayout(text.length, true));

		List<Button> list = new ArrayList<Button>(text.length);
		for (String t : text) {
			Button button = new Button(field, SWT.PUSH);
			button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			button.setText(t);
			list.add(button);
		}
		return list;
	}

	protected final Button createCheckButtonField(Composite composite, String label, String buttonLabel) {
		createLabel(composite, label);

		Button button = new Button(composite, SWT.CHECK);
		button.setText(buttonLabel);
		return button;
	}

	protected final Combo createComboField(Composite composite, int span, String label) {
		createLabel(composite, label);
		return createCombo(composite, span);
	}

	protected final Combo createCombo(Composite composite, int span) {
		Combo combo = new Combo(composite, SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = span;
		combo.setLayoutData(data);
		combo.addModifyListener(MODIFY_REFRESH_LISTENER);
		return combo;
	}

	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
		super.setVisible(visible);
	}

	protected final boolean isVisible() {
		return visible;
	}

	protected final void validate(boolean putMessage) {
		String message = validate();
		if (message != null) {
			setPageComplete(false);
			if (putMessage) {
				setErrorMessage(message);
			}
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}

	protected abstract String validate();
}
