package jp.hishidama.eclipse_plugin.jdt.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;

public class StackTraceFileSearchData {

	private static class TraceElement {
		public String packageDir;
		public String fileName;
		public int line;
	}

	private Set<String> fileNameSet = new HashSet<String>();

	private List<TraceElement> traceList = new ArrayList<TraceElement>();

	public void add(String qualifier, String fileName, int line) {
		fileNameSet.add(fileName);

		TraceElement element = new TraceElement();
		element.packageDir = qualifier.replace('.', '/');
		element.fileName = fileName;
		element.line = line;
		traceList.add(element);
	}

	public boolean acceptFile(IFile file) {
		String name = file.getName();
		boolean accept = fileNameSet.contains(name);
		return accept;
	}

	public Set<Integer> getLines(IFile file) {
		Set<Integer> lines = new TreeSet<Integer>();

		String path = file.getFullPath().toPortableString();
		for (TraceElement element : traceList) {
			String suffix = String.format("/%s/%s", element.packageDir, element.fileName);
			if (path.endsWith(suffix)) {
				lines.add(element.line);
			}
		}

		return lines;
	}

	public String getLabel() {
		if (traceList.isEmpty()) {
			return "specified stack trace";
		}

		StringBuilder sb = new StringBuilder();

		TraceElement first = traceList.get(0);
		sb.append(first.fileName);
		sb.append(":");
		sb.append(first.line);

		if (traceList.size() >= 2) {
			sb.append(" etc");
		}

		return sb.toString();
	}
}
