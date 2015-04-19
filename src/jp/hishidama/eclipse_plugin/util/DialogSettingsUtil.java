package jp.hishidama.eclipse_plugin.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Combo;

public class DialogSettingsUtil {

	public static void load(IDialogSettings settings, Combo combo, String key, String defaultValue) {
		List<String> list = getList(settings, key);
		combo.setItems(list.toArray(new String[list.size()]));

		String value = getString(settings, key, defaultValue);
		combo.setText(value);
	}

	public static void save(IDialogSettings settings, Combo combo, String key, int limit) {
		String text = combo.getText();
		put(settings, key, text);

		String[] items = combo.getItems();
		List<String> list = new ArrayList<String>(items.length + 1);
		if (!text.isEmpty()) {
			list.add(text);
		}
		for (String value : items) {
			if (!text.equals(value)) {
				list.add(value);
			}
		}
		put(settings, key, list, limit);
	}

	public static void refreshComboItems(Combo combo) {
		String text = combo.getText();

		String[] items = combo.getItems();
		List<String> list = new ArrayList<String>(items.length + 1);
		if (!text.isEmpty()) {
			list.add(text);
		}
		for (String value : items) {
			if (!text.equals(value)) {
				list.add(value);
			}
		}
		combo.setItems(list.toArray(new String[list.size()]));
		combo.setText(text);
	}

	public static String getString(IDialogSettings settings, String key, String defaultValue) {
		String value = settings.get(key);
		return (value != null) ? value : defaultValue;
	}

	public static void put(IDialogSettings settings, String key, String value) {
		settings.put(key, value);
	}

	public static int getInt(IDialogSettings settings, String key, int defaultValue) {
		String value = settings.get(key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static void put(IDialogSettings settings, String key, int value) {
		settings.put(key, value);
	}

	public static List<String> getList(IDialogSettings settings, String key) {
		List<String> list = new ArrayList<String>();

		int count = getInt(settings, key + "#list_count", 0);
		for (int i = 0; i < count; i++) {
			String value = settings.get(String.format("%s[%d]", key, i));
			if (value != null) {
				list.add(value);
			}
		}

		return list;
	}

	public static void put(IDialogSettings settings, String key, Collection<String> list, int limit) {
		int i = 0;
		for (String value : list) {
			if (value != null) {
				put(settings, String.format("%s[%d]", key, i++), value);
				if (i == limit) {
					break;
				}
			}
		}
		put(settings, key + "#list_count", i);
	}
}
