package netty.server.core;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

/**
 * 扫描包下所有类
 */
class WebServerScanner {
	
	private final String basePackage;
	private final ClassLoader cl;

	WebServerScanner(String basePackage) {
		this.basePackage = basePackage;
		this.cl = getClass().getClassLoader();
	}

	WebServerScanner(String basePackage, ClassLoader cl) {
		this.basePackage = basePackage;
		this.cl = cl;
	}

	List<String> getFullyQualifiedClassNameList() throws IOException {
		System.out.println("开始扫描包" + basePackage + "下的所有类");

		return doScan(basePackage, new ArrayList<String>());
	}
	
	Class<?> forClassName(final String name) throws Exception {
		URLClassLoader loader = null;
		try {
			final String splashPath = dotToSplash(basePackage);

			final URL url = cl.getResource(splashPath);
			final String filePath = getRootPath(url);

			if (isJarFile(filePath)) {
				URL[] urls = new URL[] { url };
				loader = new URLClassLoader(urls);

				return loader.loadClass(name);
			}

			return Class.forName(name);
		} finally {
			if (loader != null)
				loader.close();
		}
	}

	List<String> doScan(final String basePackage, final List<String> nameList) throws IOException {
		final String splashPath = dotToSplash(basePackage);

		final URL url = cl.getResource(splashPath);
		final String filePath = getRootPath(url);

		final List<String> names;
		if (isJarFile(filePath)) {
			System.out.println(filePath + "是一个JAR包");

			names = readFromJarFile(filePath, splashPath);
		} else {
			System.out.println(filePath + "是一个目录");

			names = readFromDirectory(filePath);
		}

		for (final String name : names)
			if (isClassFile(name))
				nameList.add(isJarFile(filePath) ? splashToDot(name) : toFullyQualifiedName(name, basePackage));
			else
				doScan(basePackage + "." + name, nameList);

		for (final String n : nameList)
			System.out.println("找到" + n);

		return nameList;
	}

	String toFullyQualifiedName(final String shortName, final String basePackage) {
		final StringBuilder sb = new StringBuilder(basePackage);
		sb.append('.');
		sb.append(trimExtension(shortName));

		return sb.toString();
	}

	List<String> readFromJarFile(final String jarPath, final String splashedPackageName) throws IOException {
		System.out.println("从JAR包中读取类:" + jarPath);

		final List<String> nameList = new ArrayList<String>();

		JarInputStream jarIn = null;
		try {
			jarIn = new JarInputStream(new FileInputStream(jarPath));
			JarEntry entry = null;

			while ((entry = jarIn.getNextJarEntry()) != null) {
				final String name = entry.getName();
				if (name.startsWith(splashedPackageName) && isClassFile(name))
					nameList.add(name);
			}
		} finally {
			if (jarIn != null)
				jarIn.close();
		}

		return nameList;
	}

	List<String> readFromDirectory(final String path) {
		final File file = new File(path);
		final String[] names = file.list();

		return null == names ? null : Arrays.asList(names);
	}

	boolean isClassFile(final String name) {
		return name.endsWith(".class");
	}

	boolean isJarFile(final String name) {
		return name.endsWith(".jar");
	}

	static String getRootPath(final URL url) {
		final String fileUrl = url.getFile();
		int pos = fileUrl.indexOf('!');

		return -1 == pos ? fileUrl : fileUrl.substring(5, pos);
	}

	static String dotToSplash(final String name) {
		return name.replaceAll("\\.", "/");
	}

	static String splashToDot(final String name) {
		return trimExtension(name).replaceAll("/", "\\.");
	}

	static String trimExtension(final String name) {
		final int pos = name.indexOf('.');
		return -1 == pos ? name : name.substring(0, pos);
	}
}