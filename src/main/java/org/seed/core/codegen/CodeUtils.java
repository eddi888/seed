/**
 * Seed
 * Copyright (C) 2021 EUU⛰ROCKS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.seed.core.codegen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.tools.JavaFileObject.Kind;

import org.seed.C;
import org.seed.core.util.Assert;

public abstract class CodeUtils {
	
	private static final String[] CLASS_TYPES = { "class ", "interface ", "enum " };
	
	private CodeUtils() { }
	
	public static URI createJarURI(URL packageURL, String classFileName) {
		Assert.notNull(packageURL, "packageURL");
		Assert.notNull(classFileName, "classFileName");
		
		final String packageURLString = packageURL.toExternalForm();
		final String jarURI = packageURLString.substring(0, packageURLString.lastIndexOf('!'));
		return URI.create(jarURI + "!/" + classFileName);
	}
	
	public static URI createSourceURI(String className) {
		return className != null 
				? URI.create(className + Kind.SOURCE.extension) 
				: null;
	}
	
	public static ZipInputStream createZipStream(byte[] bytes) {
		Assert.notNull(bytes, "bytes");
		
		return new ZipInputStream(new ByteArrayInputStream(bytes));
	}
	
	public static boolean isClassFile(String fileName) {
		return isKind(fileName, Kind.CLASS);
	}
	
	public static boolean isSourceFile(String fileName) {
		return isKind(fileName, Kind.SOURCE);
	}
	
	public static boolean isJarFile(String fileName) {
		return fileName != null && fileName.endsWith(".jar");
	}
	
	public static boolean isSubPackage(String path, String packagePath) {
		Assert.notNull(path, C.PATH);
		Assert.notNull(packagePath, "packagePath");
		
		return path.indexOf('/', packagePath.length() + 1) != -1;
	}
	
	public static String getPackagePath(String packageName) {
		Assert.notNull(packageName, C.PACKAGENAME);
		
		return packageName.replace('.', '/');
	}
	
	public static String getQualifiedName(String entryName) {
		Assert.notNull(entryName, "entryName");
		
		return removeClassExtension(entryName).replace('/', '.');
	}
	
	public static String getQualifiedName(String packageName, String className) {
		Assert.notNull(packageName, C.PACKAGENAME);
		Assert.notNull(className, C.CLASSNAME);
		
		return packageName + '.' + className;
	}
	
	public static String extractClassName(String qualifiedName) {
		Assert.notNull(qualifiedName, C.QUALIFIEDNAME);
		
		final int idx = qualifiedName.lastIndexOf('.');
		return idx >= 0 ? qualifiedName.substring(idx + 1) : qualifiedName; 
	}
	
	public static String extractPackageName(String qualifiedName) {
		Assert.notNull(qualifiedName, C.QUALIFIEDNAME);
		
		final int idx = qualifiedName.lastIndexOf('.');
		return idx >= 0 ? qualifiedName.substring(0, idx) : "java.lang"; // primitives have no package
	}
	
	public static String extractQualifiedName(String code) {
		Assert.notNull(code, "code");
		
		// extract package name
		int startIdx = code.indexOf("package ");
		Assert.state(startIdx >= 0, "package not found");
		startIdx += 8;
		final int endIdx = code.indexOf(';', startIdx);
		Assert.state(endIdx >= 0, "; after package not found");
		final String packageName = code.substring(startIdx, endIdx).trim();
		
		// determine class / interface / enum
		for (String classType : CLASS_TYPES) {
			startIdx = code.indexOf(classType, endIdx);
			if (startIdx >= 0) {
				startIdx += classType.length();
				break;
			}
		}
		Assert.state(startIdx > 0, "class, interface or enum not found");
		
		// extract class / interface / enum name
		final StringBuilder nameBuf = new StringBuilder();
		while (startIdx < code.length()) {
			final char ch = code.charAt(startIdx++);
			if (Character.isLetterOrDigit(ch) || ch == '_') {
				nameBuf.append(ch);
			}
			else if (nameBuf.length() > 0) {
				return getQualifiedName(packageName, nameBuf.toString());
			}
		}
		Assert.state(nameBuf.length() > 0, "class name not found");
		Assert.state(false, "class name end not found");
		return null;
	}
	
	public static String removeClassExtension(String classFileName) {
		Assert.notNull(classFileName, "classFile");
		Assert.state(isClassFile(classFileName), "file is not a class file:" + classFileName);
		
		return classFileName.substring(0, classFileName.length() - Kind.CLASS.extension.length());
	}
	
	public static String getPackageName(File baseDir, File dir) {
		Assert.notNull(baseDir, "baseDir");
		Assert.notNull(dir, "dir");
		
		return dir.getAbsolutePath()
				  .substring(baseDir.getAbsolutePath().length() + 1)
				  .replace(File.separator, ".");
	}
	
	public static String getClassName(File file) {
		Assert.notNull(file, "file");
		
		return file.getName().substring(0, file.getName().indexOf(Kind.SOURCE.extension));
	}
	
	public static String getSourceFileName(String qualifiedName) {
		Assert.notNull(qualifiedName, "qualifiedName");
		
		return qualifiedName.replace(".", File.separator) + Kind.SOURCE.extension;
	}
	
	public static int getLineNumber(String code, Pattern pattern) {
		Assert.notNull(code, "code");
		Assert.notNull(pattern, "pattern");
		
		int line = 0;
		final Scanner scanner = new Scanner(code);
		while (scanner.hasNextLine()) {
			if (pattern.matcher(scanner.nextLine()).matches()) {
				line = -line;
				break;
			}
			line--;
		}
		scanner.close();
		return line;
	}
	
	private static boolean isKind(String fileName, Kind kind) {
		Assert.notNull(fileName, "fileName");
		
		return fileName != null && fileName.endsWith(kind.extension);
	}
	
}
