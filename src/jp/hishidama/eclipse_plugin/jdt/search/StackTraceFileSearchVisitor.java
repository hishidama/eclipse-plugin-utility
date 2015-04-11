package jp.hishidama.eclipse_plugin.jdt.search;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.core.text.TextSearchScope;
import org.eclipse.search.internal.core.text.TextSearchVisitor;

@SuppressWarnings("restriction")
public class StackTraceFileSearchVisitor extends TextSearchVisitor {

	private IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

	public StackTraceFileSearchVisitor(TextSearchRequestor collector, Pattern searchPattern) {
		super(collector, searchPattern);
	}

	@Override
	public IStatus search(TextSearchScope scope, IProgressMonitor monitor) {
		Set<IFile> set = new LinkedHashSet<IFile>();

		IResource[] roots = scope.getRoots();
		for (IResource root : roots) {
			IPath location = root.getRawLocation();
			if (location == null) {
				location = root.getLocation();
			}
			if (location != null) {
				collect(location.toFile(), set);
			}
		}

		return search(set.toArray(new IFile[set.size()]), monitor);
	}

	private void collect(File file, Set<IFile> set) {
		if (exclude(file)) {
			return;
		}
		if (file.isFile()) {
			if (includeFile(file)) {
				IPath path = Path.fromOSString(file.getAbsolutePath());
				IFile ifile = root.getFileForLocation(path);
				if (ifile != null && ifile.isAccessible()) {
					set.add(ifile);
				}
			}
		} else {
			for (File f : file.listFiles()) {
				collect(f, set);
			}
		}
	}

	protected boolean exclude(File file) {
		String name = file.getName();
		return name.startsWith(".");
	}

	protected boolean includeFile(File file) {
		String name = file.getName();
		return name.endsWith(".java");
	}
}
