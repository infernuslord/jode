/* ClassPath Copyright (C) 1998-1999 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package jode.bytecode;

import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

///#def COLLECTIONS java.util
import java.util.Iterator;
///#enddef

import jode.GlobalOptions;
import jode.util.UnifyHash;

/**
 * A path in which class files are searched for.  
 *
 * Class files can be loaded from several different locations, these
 * locations can be:
 * <ul>
 * <li> A local directory.</li>
 * <li> A local jar or zip file </li>
 * <li> A URL (unified resource location), pointing to a directory </li>
 * <li> A URL pointing to a jar or zip file. </li>
 * <li> A Jar URL (see {@link java.net.JarURLConnection}), useful if
 * the jar file is not packed correctly</li>
 * <li> The reflection URL <code>reflection:/</code> This location can
 * only load declarations of classes.  If a security manager is
 * present, it can only load public declarations.</li>
 * </ul>
 *
 * We use standard java means to find a class file: package correspong
 * to directories and the class file must have the <code>.class</code>
 * extension.  For example if the class path points to
 * <code>/home/java</code>, the class <code>java.lang.Object</code> is
 * loaded from <code>/home/java/java/lang/Object.class</code>.
 *
 * A class path can have another ClassPath as fallback.  If
 * ClassInfo.loadInfo is called and the class isn't found the fallback
 * ClassPath is searched instead.  This repeats until there is no
 * further fallback.
 *
 * The main method for creating classes is {@link #getClassInfo}.  The
 * other available methods are useful to find other files in the
 * ClassPath and to get a listing of all available files and classes.
 *
 * A ClassPath handles some <code>IOException</code>s and
 * <code>SecurityException</code>s skipping the path that produced
 * them.
 * 
 * @author Jochen Hoenicke
 * @version 1.1 */
public class ClassPath  {

    /**
     * We need a different pathSeparatorChar, since ':' (used for most
     * UNIX System) is used a protocol separator in URLs.  
     *
     * We currently allow both pathSeparatorChar and
     * altPathSeparatorChar and decide if it is a protocol separator
     * by context.  This doesn't always work, so use
     * <code>altPathSeparator</code>, or the ClassPath(String[])
     * constructor.  
     */
    public static final char altPathSeparatorChar = ',';

    private class Path {
	public boolean exists(String file) {
	    return false;
	}
	public boolean isDirectory(String file) {
	    return false;
	}
	public InputStream getFile(String file) throws IOException {
	    return null;
	}
	public Enumeration listFiles(String directory) {
	    return null;
	}

	public boolean loadClass(ClassInfo clazz, int howMuch) 
	    throws IOException, ClassFormatException 
	{
	    String file = clazz.getName().replace('.', '/') + ".class";
	    if (!exists(file))
		return false;
	    DataInputStream input = new DataInputStream
		(new BufferedInputStream
		 (getFile(file)));
	    clazz.read(input, howMuch);
	    return true;
	}
    }

    private class ReflectionPath extends Path {
	public boolean loadClass(ClassInfo classinfo, int howMuch) 
	    throws IOException, ClassFormatException 
	{
	    if (howMuch > ClassInfo.DECLARATIONS)
		return false;

	    Class clazz = null;
	    try {
		clazz = Class.forName(classinfo.getName());
	    } catch (ClassNotFoundException ex) {
		return false;
	    } catch (NoClassDefFoundError ex) {
		return false;
	    }
	    try {
		classinfo.loadFromReflection(clazz, howMuch);
		return true;
	    } catch (SecurityException ex) {
		return false;
	    }
	}
    }

    private class LocalPath extends Path {
	private File dir;

	public LocalPath(File path) {
	    dir = path;
	}

	public boolean exists(String filename) {
	    if (java.io.File.separatorChar != '/')
		filename = filename
		    .replace('/', java.io.File.separatorChar);
	    try {
		return new File(dir, filename).exists();
	    } catch (SecurityException ex) {
		return false;
	    }
	}

	public boolean isDirectory(String filename) {
	    if (java.io.File.separatorChar != '/')
		filename = filename
		    .replace('/', java.io.File.separatorChar);
	    return new File(dir, filename).isDirectory();
	}

	public InputStream getFile(String filename) throws IOException {
	    if (java.io.File.separatorChar != '/')
		filename = filename
		    .replace('/', java.io.File.separatorChar);
	    File f = new File(dir, filename);
	    return new FileInputStream(f);
	}

	public Enumeration listFiles(String directory) {
	    if (File.separatorChar != '/')
		directory = directory
		    .replace('/', File.separatorChar);
	    File f = new File(dir, directory);
	    final String[] files = f.list();
	    if (files == null)
		return null;

	    if (!directory.endsWith(File.separator))
		directory += File.separator;
	    final String prefix = directory;
	    return new Enumeration() {
		int i = 0;
		public boolean hasMoreElements() {
		    return i < files.length;
		}
		public Object nextElement() {
		    try {
			return files[i++];
		    } catch (ArrayIndexOutOfBoundsException ex) {
			return new NoSuchElementException();
		    }
		}
	    };
	}
    }

    private class ZipPath extends Path {
	private Hashtable entries = new Hashtable();
	private ZipFile file;
	private byte[] contents;
	private String prefix;

	private void addEntry(ZipEntry ze) {
	    String name = ze.getName();
	    if (prefix != null) {
		if (!name.startsWith(prefix))
		    return;
		name = name.substring(prefix.length());
	    }
	    
	    if (ze.isDirectory()
		/* || !name.endsWith(".class")*/)
		return;

	    do {
		String dir = "";
		int pathsep = name.lastIndexOf("/");
		if (pathsep != -1) {
		    dir = name.substring(0, pathsep);
		    name = name.substring(pathsep+1);
		}
		
		Vector dirContent = (Vector) entries.get(dir);
		if (dirContent != null) {
		    dirContent.addElement(name);
		    return;
		}
		    
		dirContent = new Vector();
		dirContent.addElement(name);
		entries.put(dir, dirContent);
		name = dir;
	    } while (name.length() > 0);
	}

	public ZipPath(ZipFile zipfile, String prefix) {
	    this.file = zipfile;
	    this.prefix = prefix;

	    Enumeration zipEnum = file.entries();
	    entries = new Hashtable();
	    while (zipEnum.hasMoreElements()) {
		addEntry((ZipEntry) zipEnum.nextElement());
	    }
	}

	public ZipPath(byte[] zipcontents, String prefix) 
	    throws IOException
	{
	    this.contents = zipcontents;
	    this.prefix = prefix;

	    // fill entries into hash table
	    ZipInputStream zis = new ZipInputStream
		(new ByteArrayInputStream(zipcontents));
	    entries = new Hashtable();
	    ZipEntry ze;
	    while ((ze = zis.getNextEntry()) != null) {
		addEntry(ze);
		zis.closeEntry();
	    }
	    zis.close();
	}

	public boolean exists(String filename) {
	    if (entries.containsKey(filename))
		return true;

	    String dir = "";
	    String name = filename;
	    int index = filename.lastIndexOf('/');
	    if (index >= 0) {
		dir = filename.substring(0, index);
		name = filename.substring(index+1);
	    }
	    Vector directory = (Vector)entries.get(dir);
	    if (directory != null && directory.contains(name))
		return true;
	    return false;
	}

	public boolean isDirectory(String filename) {
	    return entries.containsKey(filename);
	}

	public InputStream getFile(String filename) throws IOException {
	    String fullname = prefix != null ? prefix + filename : filename;
	    if (contents != null) {
		ZipInputStream zis = new ZipInputStream
		    (new ByteArrayInputStream(contents));
		ZipEntry ze;
		while ((ze = zis.getNextEntry()) != null) {
		    if (ze.getName().equals(fullname)) {
///#ifdef JDK11
///			// The skip method in jdk1.1.7 ZipInputStream
///			// is buggy.  We return a wrapper that fixes
///			// this.
///			return new FilterInputStream(zis) {
///			    private byte[] tmpbuf = new byte[512];
///			    public long skip(long n) throws IOException {
///				long skipped = 0;
///				while (n > 0) {
///				    int count = read(tmpbuf, 0, 
///						     (int)Math.min(n, 512L));
///				    if (count == -1)
///					return skipped;
///				    skipped += count;
///				    n -= count;
///				}
///				return skipped;
///			    }
///			};
///#else
			return zis;
///#endif
		    }
		    zis.closeEntry();
		}
		zis.close();
	    } else {
                ZipEntry ze = file.getEntry(fullname);
                if (ze != null)
                    return file.getInputStream(ze);
	    }
	    return null;
	}

	public Enumeration listFiles(String directory) {
	    Vector direntries = (Vector) entries.get(directory);
	    if (direntries != null)
		return direntries.elements();
	    return null;
	}
    }

    private class URLPath extends Path {
	private URL base;

	public URLPath(URL base) {
	    this.base = base;
	}

	public boolean exists(String filename) {
	    try {
		URL url = new URL(base, filename);
		URLConnection conn = url.openConnection();
		conn.connect();
		conn.getInputStream().close();
		return true;
	    } catch (IOException ex) {
		return false;
	    }
	}

	public InputStream getFile(String filename) throws IOException {
	    try {
		URL url = new URL(base, filename);
		URLConnection conn = url.openConnection();
		conn.setAllowUserInteraction(true);
		return conn.getInputStream();
	    } catch (IOException ex) {
		return null;
	    }
	}

	public boolean loadClass(ClassInfo clazz, int howMuch) 
	    throws IOException, ClassFormatException 
	{
	    String file = clazz.getName().replace('.', '/') + ".class";
	    InputStream is = getFile(file);
	    if (is == null)
		return false;

	    DataInputStream input = new DataInputStream
		(new BufferedInputStream(is));
	    clazz.read(input, howMuch);
	    return true;
	}
    }

    private Path[] paths;
    private UnifyHash classes = new UnifyHash();
    
    ClassPath fallback = null;

    /**
     * Creates a new class path for the given path.  See the class
     * description for more information, which kind of paths are
     * supported.
     * @param path An array of paths.
     * @param fallback The fallback classpath.
     */
    public ClassPath(String[] paths, ClassPath fallback) {
	this.fallback = fallback;
	initPath(paths);
    }

    /**
     * Creates a new class path for the given path.  See the class
     * description for more information, which kind of paths are
     * supported.
     * @param path An array of paths.
     */
    public ClassPath(String[] paths) {
	initPath(paths);
    }

    /**
     * Creates a new class path for the given path.  See the class
     * description for more information, which kind of paths are
     * supported.
     * @param path One or more paths.  They should be separated by the
     * altPathSeparatorChar or pathSeparatorChar, but the latter is
     * deprecated since it may give problems for UNIX machines.  
     * @see #ClassPath(String[] paths)
     */
    public ClassPath(String path, ClassPath fallback) {
	this(path);
	this.fallback = fallback;
    }

    /**
     * Creates a new class path for the given path.  See the class
     * description for more information, which kind of paths are
     * supported.
     * @param path One or more paths.  They should be separated by the
     * altPathSeparatorChar or pathSeparatorChar, but the latter is
     * deprecated since it may give problems for UNIX machines.  
     * @see #ClassPath(String[] paths)
     */
    public ClassPath(String path) {
	// Calculate a good approximation (rounded upwards) of the tokens
	// in this path.
	int length = 1;
	for (int index=path.indexOf(File.pathSeparatorChar); 
	     index != -1; length++)
	    index = path.indexOf(File.pathSeparatorChar, index+1);
	if (File.pathSeparatorChar != altPathSeparatorChar) {
	    for (int index=path.indexOf(altPathSeparatorChar); 
		 index != -1; length++)
		index = path.indexOf(altPathSeparatorChar, index+1);
	}


	String[] tokens = new String[length];
	int i = 0;
        for (int ptr=0; ptr < path.length(); ptr++, i++) {
	    int next = ptr;
	    while (next < path.length()
		   && path.charAt(next) != File.pathSeparatorChar
		   && path.charAt(next) != altPathSeparatorChar)
		next++;

	    int index = ptr;
	colon_separator:
	    while (next > ptr
		   && next < path.length()
		   && path.charAt(next) == ':') {
		// Check if this is a URL instead of a pathSeparator
		// Since this is a while loop it allows nested urls like
		// jar:ftp://ftp.foo.org/pub/foo.jar!/
		
		while (index < next) {
		    char c = path.charAt(index);
		    // According to RFC 1738 letters, digits, '+', '-'
		    // and '.' are allowed SCHEMA characters.  We
		    // disallow '.' because it is a good marker that
		    // the user has specified a filename instead of a
		    // URL.
		    if ((c < 'A' || c > 'Z')
			&& (c < 'a' || c > 'z')
			&& (c < '0' || c > '9')
			&& "+-".indexOf(c) == -1) {
			break colon_separator;
		    }
		    index++;
		}
		next++;
		index++;
		while (next < path.length()
		       && path.charAt(next) != File.pathSeparatorChar
		       && path.charAt(next) != altPathSeparatorChar)
		    next++;
	    }
	    tokens[i] = path.substring(ptr, next);
	    ptr = next;
	}
	initPath(tokens);
    }

    private byte[] readURLZip(URLConnection conn) {
	int length = conn.getContentLength();
	if (length <= 0)
	    // Give a approximation if length is unknown
	    length = 10240;
	else
	    // Increase the length by one, so we hopefully don't need
	    // to grow the array later (we need a little overshot to
	    // know when the end is reached).
	    length++;

	byte[] contents = new byte[length];

	try {
	    InputStream is = conn.getInputStream();
	    int pos = 0;
	    for (;;) {
		// This is ugly, is.available() may return zero even
		// if there are more bytes.
		int avail = Math.max(is.available(), 1);
		if (pos + is.available() > contents.length) {
		    // grow the byte array.
		    byte[] newarr = new byte 
			[Math.max(2*contents.length, pos + is.available())];
		    System.arraycopy(contents, 0, newarr, 0, pos);
		    contents = newarr;
		}
		int count = is.read(contents, pos, contents.length-pos);
		if (count == -1)
		    break;
		pos += count;
	    }
	    if (pos < contents.length) {
		// shrink the byte array again.
		byte[] newarr = new byte[pos];
		System.arraycopy(contents, 0, newarr, 0, pos);
		contents = newarr;
	    }
	    return contents;
	} catch (IOException ex) {
	    return null;
	}
    }

    private void initPath(String[] tokens) {
	int length = tokens.length;
	paths = new Path[length];

        for (int i = 0; i < length; i++) {
	    String path = tokens[i];
	    if (path == null)
		continue;

	    String zipPrefix = null;
	    // The special reflection URL
	    if (path.startsWith("reflection:")) {
		paths[i] = new ReflectionPath();
		continue;
	    }

	    // We handle jar URL's ourself.
	    if (path.startsWith("jar:")) {
		int index = 0;
		do {
		    index = path.indexOf('!', index);
		} while (index != -1 && index != path.length()-1
			 && path.charAt(index+1) != '/');
		
		if (index == -1 || index == path.length() - 1) {
		    GlobalOptions.err.println("Warning: Illegal jar url "
+ path + ".");
		    continue;
		}
		zipPrefix = path.substring(index+2);
		if (!zipPrefix.endsWith("/"))
		    zipPrefix += "/";
		path = path.substring(4, index);
	    }
	    int index = path.indexOf(':');
	    if (index != -1 && index < path.length()-2
		&& path.charAt(index+1) == '/'
		&& path.charAt(index+2) == '/') {
		// This looks like an URL.
		try {
		    URL base = new URL(path);
		    try {
			URLConnection connection = base.openConnection();
			if (zipPrefix != null
			    || path.endsWith(".zip") || path.endsWith(".jar")
			    || connection.getContentType().endsWith("/zip")) {
			    // This is a zip file.  Read it into memory.
			    byte[] contents = readURLZip(connection);
			    if (contents != null)
				paths[i] = new ZipPath(contents, zipPrefix);
			} else
			    paths[i] = new URLPath(base);
		    } catch (IOException ex) {
			GlobalOptions.err.println
			    ("Warning: IO exception while accessing "
			     +path+".");
		    } catch (SecurityException ex) {
			GlobalOptions.err.println
			    ("Warning: Security exception while accessing "
			     +path+".");
		    }
		} catch (MalformedURLException ex) {
		    GlobalOptions.err.println
			("Warning: Malformed URL "+ path + ".");
		}
	    } else {
		try {
		    File dir = new File(path);
		    if (zipPrefix != null || !dir.isDirectory()) {
			try {
			    paths[i] = new ZipPath(new ZipFile(dir), 
						   zipPrefix);
			} catch (java.io.IOException ex) {
			    GlobalOptions.err.println
				("Warning: Can't read "+ path + ".");
			}
		    } else
			paths[i] = new LocalPath(dir);
		} catch (SecurityException ex) {
		    GlobalOptions.err.println
			("Warning: SecurityException while accessing "
			 + path + ".");
		}
	    }
	}
    }


    /** 
     * Creates a new class info for a class residing in this search
     * path.  This doesn't load the class immediately, this is done by
     * ClassInfo.loadInfo.  It is no error if class doesn't exists.
     * @param classname the dot-separated full qualified name of the class.  
     *        For inner classes you must use the bytecode name with $,
     *        e.g. <code>java.util.Map$Entry.</code> 
     * @exception IllegalArgumentException if class name isn't valid.
     */
    public ClassInfo getClassInfo(String classname) 
    {
	checkClassName(classname);
	int hash = classname.hashCode();
	Iterator iter = classes.iterateHashCode(hash);
	while (iter.hasNext()) {
	    ClassInfo clazz = (ClassInfo) iter.next();
	    if (clazz.getName().equals(classname))
		return clazz;
	}
	ClassInfo clazz = new ClassInfo(classname, this);
	classes.put(hash, clazz);
        return clazz;
    }

    /**
     * Checks, if a class with the given name exists somewhere in this
     * path.
     * @param classname the class name.
     * @exception IllegalArgumentException if class name isn't valid.
     */
    public boolean existsClass(String classname) {
	checkClassName(classname);
	return existsFile(classname.replace('.', '/') + ".class");
    }

    /**
     * Checks, if a file with the given name exists somewhere in this
     * path.
     * @param filename the file name.
     * @see #existsClass
     */
    public boolean existsFile(String filename) {
        for (int i=0; i<paths.length; i++) {
	    if (paths[i] != null && paths[i].exists(filename))
		return true;
	}
	return false;
    }

    private void checkClassName(String name) {
	if (name == null
	    || name.indexOf(';') != -1
	    || name.indexOf('[') != -1
	    || name.indexOf('/') != -1)
	    throw new IllegalArgumentException("Illegal class name: "+name);
    }

    /**
     * Searches for a file in the class path.
     * @param filename the filename. The path components should be separated
     * by <code>/</code>.
     * @return An InputStream for the file.
     */
    public InputStream getFile(String filename) throws IOException {
        for (int i=0; i < paths.length; i++) {
	    if (paths[i] != null && paths[i].exists(filename))
		return paths[i].getFile(filename);
	}
	throw new FileNotFoundException(filename);
    }

    /**
     * Searches for a filename in the class path and tells if it is a
     * directory.
     * @param filename the filename. The path components should be separated
     * by <code>/</code>.
     * @return true, if filename exists and is a directory, false otherwise.
     */
    public boolean isDirectory(String filename) {
        for (int i=0; i < paths.length; i++) {
	    if (paths[i] != null && paths[i].exists(filename))
		return paths[i].isDirectory(filename);
	}
	return false;
    }

    /**
     * Searches for a filename in the class path and tells if it is a
     * package.  This is the same as isDirectory.
     * @param fqn the full qualified name. The components should be dot
     * separated.
     * @return true, if filename exists and is a package, false otherwise.
     * @see isDirectory
     */
    public boolean isPackage(String fqn) {
	return isDirectory(fqn.replace('.', '/'));
    }

    /**
     * Get a list of all files in a given directory.
     * @param dirName the directory name. The path components must
     * be separated by <code>/</code>.
     * @return An enumeration with all files/directories in the given
     * directory.  If dirName doesn't denote a directory it returns null.
     */
    public Enumeration listFiles(final String dirName) {
	return new Enumeration() {
	    int i = 0;
	    Enumeration enum;

	    public boolean hasMoreElements() {
		while (true) {
		    while (enum == null && i < paths.length) {
			if (paths[i] != null && paths[i].exists(dirName)
			    && paths[i].isDirectory(dirName))
			    enum = paths[i].listFiles(dirName);
			i++;
		    }

		    if (enum == null)
			return false;
		    if (enum.hasMoreElements())
			return true;
		    enum = null;
		}
	    }

	    public Object nextElement() {
		if (!hasMoreElements())
		    return new NoSuchElementException();
		return enum.nextElement();
	    }
	};
    }

    /**
     * Get a list of all classes and packages in the given package.
     * @param package a dot-separated package name.
     * @return An enumeration with all class/subpackages in the given
     * package.  If package doesn't denote a package it returns null.
     */
    public Enumeration listClassesAndPackages(String packageName) {
	String dir = packageName.replace('.','/');
        final Enumeration enum = listFiles(dir);
	final String prefix = dir.length() > 0 ? dir + "/" : dir;
        return new Enumeration() {
	    String next = getNext();
	    
	    private String getNext() {
		while (enum.hasMoreElements()) {
		    String name = (String) enum.nextElement();
		    if (name.indexOf('.') == -1
			&& isDirectory(prefix + name))
			// This is a package
			return name;
		    if (name.endsWith(".class"))
			// This is a class
			return name.substring(0, name.length()-6);
		}
		return null;
	    }

            public boolean hasMoreElements() {
                return next != null;
            }
            public Object nextElement() {
		if (next == null)
		    throw new NoSuchElementException();
		String result = next;
		next = getNext();
		return result;
            }
        };
    }

    boolean loadClass(ClassInfo clazz, int howMuch) 
	throws IOException, ClassFormatException
    {
	for (int i = 0; i < paths.length; i++) {
	    if (paths[i] != null && paths[i].loadClass(clazz, howMuch))
		return true;
	}
	if (fallback != null)
	    return fallback.loadClass(clazz, howMuch);
	return false;
    }
}
