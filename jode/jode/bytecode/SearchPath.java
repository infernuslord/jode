/* SearchPath Copyright (C) 1998-1999 Jochen Hoenicke.
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
import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.util.*;
import jode.GlobalOptions;

/**
 * This class represents a path of multiple directories and/or zip files,
 * where we can search for file names.
 * 
 * @author Jochen Hoenicke
 */
public class SearchPath  {

    /**
     * Hack to allow URLs that use the same separator as the normal
     * path separator.  Use a very unusual character for this.
     */
    public static final char protocolSeparator = 31;
    URL[] bases;
    byte[][] urlzips;
    File[] dirs;
    ZipFile[] zips;
    Hashtable[] zipEntries;

    private static void addEntry(Hashtable entries, String name) {
	String dir = "";
	int pathsep = name.lastIndexOf("/");
	if (pathsep != -1) {
	    dir = name.substring(0, pathsep);
	    name = name.substring(pathsep+1);
	}

	Vector dirContent = (Vector) entries.get(dir);
	if (dirContent == null) {
	    dirContent = new Vector();
	    entries.put(dir, dirContent);
	    if (dir != "")
		addEntry(entries, dir);
	}
	dirContent.addElement(name);
    }

    private void fillZipEntries(int nr) {
	Enumeration zipEnum = zips[nr].entries();
	zipEntries[nr] = new Hashtable();
	while (zipEnum.hasMoreElements()) {
	    ZipEntry ze = (ZipEntry) zipEnum.nextElement();
	    String name = ze.getName();
	    if (name.endsWith("/"))
		name = name.substring(0, name.length()-1);
	    if (!ze.isDirectory() && name.endsWith(".class"))
		addEntry(zipEntries[nr], name);
	}
    }

    private void readURLZip(int nr, URLConnection conn) {
	int length = conn.getContentLength();
	if (length <= 0)
	    // Give a approximation if length is unknown
	    length = 10240;
	else
	    // Increase the length by one, so we hopefully don't need
	    // to grow the array later (we need a little overshot to
	    // know when the end is reached).
	    length++;

	urlzips[nr] = new byte[length];
	try {
	    InputStream is = conn.getInputStream();
	    int pos = 0;
	    for (;;) {
		// This is ugly, is.available() may return zero even
		// if there are more bytes.
		int avail = Math.max(is.available(), 1);
		if (pos + is.available() > urlzips[nr].length) {
		    // grow the byte array.
		    byte[] newarr = new byte 
			[Math.max(2*urlzips[nr].length, pos + is.available())];
		    System.arraycopy(urlzips[nr], 0, newarr, 0, pos);
		    urlzips[nr] = newarr;
		}
		int count = is.read(urlzips[nr], pos, urlzips[nr].length-pos);
		if (count == -1)
		    break;
		pos += count;
	    }
	    if (pos < urlzips[nr].length) {
		// shrink the byte array again.
		byte[] newarr = new byte[pos];
		System.arraycopy(urlzips[nr], 0, newarr, 0, pos);
		urlzips[nr] = newarr;
	    }
	} catch (IOException ex) {
	    GlobalOptions.err.println("IOException while reading "
				   +"remote zip file "+bases[nr]);
	    // disable entry
	    bases[nr] = null;
	    urlzips[nr] = null;
	    return;
	}
	try {
	    // fill entries into hash table
	    ZipInputStream zis = new ZipInputStream
		(new ByteArrayInputStream(urlzips[nr]));
	    zipEntries[nr] = new Hashtable();
	    ZipEntry ze;
	    while ((ze = zis.getNextEntry()) != null) {
		String name = ze.getName();
		if (name.endsWith("/"))
		    name = name.substring(0, name.length()-1);
		if (!ze.isDirectory() && name.endsWith(".class"))
		    addEntry(zipEntries[nr], name);
		zis.closeEntry();
	    }
	    zis.close();
	} catch (IOException ex) {
	    GlobalOptions.err.println("Remote zip file "+bases[nr]
				   +" is corrupted.");
	    // disable entry
	    bases[nr] = null;
	    urlzips[nr] = null;
	    zipEntries[nr] = null;
	    return;
	}
    }
    
    /**
     * Creates a new search path for the given path.
     * @param path The path where we should search for files.  They
     * should be separated by the system dependent pathSeparator.  The
     * entries may also be zip or jar files.
     */
    public SearchPath(String path) {
        StringTokenizer tokenizer = 
            new StringTokenizer(path, File.pathSeparator);
        int length = tokenizer.countTokens();
        
	bases = new URL[length];
	urlzips = new byte[length][];
        dirs = new File[length];
        zips = new ZipFile[length];
        zipEntries = new Hashtable[length];
        for (int i=0; i< length; i++) {
	    String token = tokenizer.nextToken()
		.replace(protocolSeparator, ':');
	    int index = token.indexOf(':');
	    if (index != -1 && index < token.length()-2
		&& token.charAt(index+1) == '/'
		&& token.charAt(index+2) == '/') {
		// This looks like an URL.
		try {
		    bases[i] = new URL(token);
		    try {
			URLConnection connection = bases[i].openConnection();
			if (token.endsWith(".zip") || token.endsWith(".jar")
			    || connection.getContentType().endsWith("/zip")) {
			    // This is a zip file.  Read it into memory.
			    readURLZip(i, connection);
			}
		    } catch (IOException ex) {
			// ignore
		    } catch (SecurityException ex) {
			GlobalOptions.err.println("Warning: Security exception "
					       +"while accessing "
					       +bases[i]+".");
		    }
		} catch (MalformedURLException ex) {
		    /* disable entry */
		    bases[i] = null;
		    dirs[i] = null;
		}
	    } else {
		try {
		    dirs[i] = new File(token);
		    if (!dirs[i].isDirectory()) {
			try {
			    zips[i] = new ZipFile(dirs[i]);
			} catch (java.io.IOException ex) {
			    /* disable this entry */
			    dirs[i] = null;
			}
		    }
		} catch (SecurityException ex) {
		    /* disable this entry */
		    GlobalOptions.err.println("Warning: SecurityException while"
					   + " accessing " + token);
		    dirs[i] = null;
		}
	    }
	}
    }

    public boolean exists(String filename) {
        for (int i=0; i<dirs.length; i++) {
	    if (zipEntries[i] != null) {
		if (zipEntries[i].get(filename) != null)
		    return true;

		String dir = "";
		String name = filename;
		int index = filename.lastIndexOf('/');
		if (index >= 0) {
		    dir = filename.substring(0, index);
		    name = filename.substring(index+1);
		}
		Vector directory = (Vector)zipEntries[i].get(dir);
		if (directory != null && directory.contains(name))
		    return true;
		continue;
	    }
	    if (bases[i] != null) {
		try {
		    URL url = new URL(bases[i], filename);
		    URLConnection conn = url.openConnection();
		    conn.connect();
		    conn.getInputStream().close();
		    return true;
		} catch (IOException ex) {
		    /* ignore */
		}
		continue;
	    }
            if (dirs[i] == null)
                continue;
            if (zips[i] != null) {
                ZipEntry ze = zips[i].getEntry(filename);
                if (ze != null)
                    return true;
            } else {
                if (java.io.File.separatorChar != '/')
                    filename = filename
                        .replace('/', java.io.File.separatorChar);
		try {
		    File f = new File(dirs[i], filename);
		    if (f.exists())
			return true;
		} catch (SecurityException ex) {
		    /* ignore and take next element */
		}
            }
        }
        return false;
    }

    /**
     * Searches for a file in the search path.
     * @param filename the filename. The path components should be separated
     * by <code>/</code>.
     * @return An InputStream for the file.
     */
    public InputStream getFile(String filename) throws IOException {
        for (int i=0; i<dirs.length; i++) {
	    if (urlzips[i] != null) {
		ZipInputStream zis = new ZipInputStream
		    (new ByteArrayInputStream(urlzips[i]));
		ZipEntry ze;
		while ((ze = zis.getNextEntry()) != null) {
		    if (ze.getName().equals(filename)) {
///#ifdef JDK11
			// The skip method in jdk1.1.7 ZipInputStream
			// is buggy.  We return a wrapper that fixes
			// this.
			return new FilterInputStream(zis) {
			    private byte[] tmpbuf = new byte[512];
			    public long skip(long n) throws IOException {
				long skipped = 0;
				while (n > 0) {
				    int count = read(tmpbuf, 0, 
						     (int)Math.min(n, 512L));
				    if (count == -1)
					return skipped;
				    skipped += count;
				    n -= count;
				}
				return skipped;
			    }
			};
///#else
///			return zis;
///#endif
		    }
		    zis.closeEntry();
		}
		continue;
	    }
	    if (bases[i] != null) {
		try {
		    URL url = new URL(bases[i], filename);
		    URLConnection conn = url.openConnection();
		    conn.setAllowUserInteraction(true);
		    return conn.getInputStream();
		} catch (SecurityException ex) {
		    GlobalOptions.err.println("Warning: SecurityException"
					   +" while accessing "
					   +bases[i]+filename);
		    ex.printStackTrace(GlobalOptions.err);
		    /* ignore and take next element */
		} catch (FileNotFoundException ex) {
		    /* ignore and take next element */
		}
		continue;
	    }
            if (dirs[i] == null)
                continue;
            if (zips[i] != null) {
                ZipEntry ze = zips[i].getEntry(filename);
                if (ze != null)
                    return zips[i].getInputStream(ze);
            } else {
                if (java.io.File.separatorChar != '/')
                    filename = filename
                        .replace('/', java.io.File.separatorChar);
		try {
		    File f = new File(dirs[i], filename);
		    if (f.exists())
			return new FileInputStream(f);
		} catch (SecurityException ex) {
		    GlobalOptions.err.println("Warning: SecurityException"
					   +" while accessing "
					   +dirs[i]+filename);
		    /* ignore and take next element */
		}
            }
        }
        throw new FileNotFoundException(filename);
    }

    /**
     * Searches for a filename in the search path and tells if it is a
     * directory.
     * @param filename the filename. The path components should be separated
     * by <code>/</code>.
     * @return true, if filename exists and is a directory, false otherwise.
     */
    public boolean isDirectory(String filename) {
        for (int i=0; i<dirs.length; i++) {
            if (dirs[i] == null)
                continue;
            if (zips[i] != null && zipEntries[i] == null)
		fillZipEntries(i);

	    if (zipEntries[i] != null) {
		if (zipEntries[i].containsKey(filename))
		    return true;
            } else {
                if (java.io.File.separatorChar != '/')
                    filename = filename
                        .replace('/', java.io.File.separatorChar);
		try {
		    File f = new File(dirs[i], filename);
		    if (f.exists())
			return f.isDirectory();
		} catch (SecurityException ex) {
		    GlobalOptions.err.println("Warning: SecurityException"
					   +" while accessing "
					   +dirs[i]+filename);
		}
            }
        }
        return false;
    }

    /**
     * Searches for all files in the given directory.
     * @param dirName the directory name. The path components should
     * be separated by <code>/</code>.
     * @return An enumeration with all files/directories in the given
     * directory.  */
    public Enumeration listFiles(final String dirName) {
        return new Enumeration() {
            int pathNr;
            Enumeration zipEnum;
            int fileNr;
	    String localDirName = 
		(java.io.File.separatorChar != '/')
		? dirName.replace('/', java.io.File.separatorChar)
		: dirName;
	    File currentDir;
            String[] files;

            public String findNextFile() {
                while (true) {
                    if (zipEnum != null) {
                        while (zipEnum.hasMoreElements()) {
			    return (String) zipEnum.nextElement();
                        }
                        zipEnum = null;
                    }
                    if (files != null) {
                        while (fileNr < files.length) {
                            String name = files[fileNr++];
                            if (name.endsWith(".class")) {
                                return name;
                            } else {
				File f = new File(currentDir, name);
				if (f.exists() && f.isDirectory())
				    return name;
			    }
                        }
                        files = null;
                    }
                    if (pathNr == dirs.length)
                        return null;

                    if (zips[pathNr] != null && zipEntries[pathNr] == null)
			fillZipEntries(pathNr);

		    if (zipEntries[pathNr] != null) {
			Vector entries = 
			    (Vector) zipEntries[pathNr].get(dirName);
			if (entries != null)
			    zipEnum = entries.elements();
                    } else if (dirs[pathNr] != null) {
			try {
			    File f = new File(dirs[pathNr], localDirName);
			    if (f.exists() && f.isDirectory()) {
				currentDir = f;
				files = f.list();
			    }
			} catch (SecurityException ex) {
			    GlobalOptions.err.println("Warning: SecurityException"
						   +" while accessing "
						   +dirs[pathNr]+localDirName);
			    /* ignore and take next element */
			}
                    }
                    pathNr++;
                }
            }

            String nextName;

            public boolean hasMoreElements() {
                return (nextName != null
                        || (nextName = findNextFile()) != null);
            }

            public Object nextElement() {
                if (nextName == null)
                    return findNextFile();
                else {
                    String result = nextName;
                    nextName = null;
                    return result;
                }
            }
        };
    }
}
