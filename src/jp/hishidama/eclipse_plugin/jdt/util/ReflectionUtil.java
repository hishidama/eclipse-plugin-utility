package jp.hishidama.eclipse_plugin.jdt.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class ReflectionUtil {

	public static Class<?> loadClass(IJavaProject javaProject, String className) throws JavaModelException,
			IOException, ClassNotFoundException {
		List<URL> list = new ArrayList<URL>(128);

		IProject project = javaProject.getProject();
		getURL(list, project, javaProject.getOutputLocation());
		IClasspathEntry[] classpath = javaProject.getRawClasspath();
		getURL(list, project, classpath);
		URL[] urls = list.toArray(new URL[list.size()]);
		ClassLoader loader = URLClassLoader.newInstance(urls);
		Class<?> clazz = loader.loadClass(className);

		return clazz;
	}

	private static void getURL(List<URL> list, IProject project, IClasspathEntry[] classpath) {
		for (IClasspathEntry entry : classpath) {
			IPath pp = entry.getOutputLocation();
			if (pp == null) {
				pp = entry.getPath();
			}
			getURL(list, project, pp);
		}
	}

	private static void getURL(List<URL> list, IProject project, IPath pp) {
		try {
			if (pp.toFile().exists()) {
				URL url = pp.toFile().toURI().toURL();
				list.add(url);
				return;
			}
			IPath vp = JavaCore.getResolvedVariablePath(pp);
			if (vp != null) {
				URL url = vp.toFile().toURI().toURL();
				list.add(url);
				return;
			}
			IFolder folder = project.getParent().getFolder(pp);
			if (folder != null) {
				URI uri = folder.getLocationURI();
				if (uri != null) {
					String s = uri.toURL().toExternalForm() + "/";
					list.add(new URL(s));
					return;
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Object object, String methodName) throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		Method method = object.getClass().getMethod(methodName);
		return (T) method.invoke(object);
	}
}
