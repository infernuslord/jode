/* SearchPath - Copyright (C) 1997-1998 Jochen Hoenicke.
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.StringTokenizer;
import java.util.Enumeration;
import jode.Decompiler;

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
    File[] dirs;
    ZipFile[] zips;
    
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
        dirs = new File[length];
        zips = new ZipFile[length];
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
		    Decompiler.err.println("Warning: SecurityException while"
					   + " accessing " + token);
		    dirs[i] = null;
		}
	    }
	}
    }

    public boolean exists(String filename) {
        for (int i=0; i<dirs.length; i++) {
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
	    if (bases[i] != null) {
		try {
		    URL url = new URL(bases[i], filename);
		    URLConnection conn = url.openConnection();
		    return conn.getInputStream();
		} catch (SecurityException ex) {
		    Decompiler.err.println("Warning: SecurityException"
					   +" while accessing "
					   +bases[i]+filename);
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
		    Decompiler.err.println("Warning: SecurityException"
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
            if (zips[i] != null) {
//                 ZipEntry ze = zips[i].getEntry(filename);
//                 if (ze != null)
//                     return ze.isDirectory();
                String directoryname = filename+"/";
                Enumeration zipEnum = zips[i].entries();
                while (zipEnum.hasMoreElements()) {
                    ZipEntry ze = (ZipEntry) zipEnum.nextElement();
                    String name = ze.getName();
                    if (name.startsWith(directoryname))
                        return true;
                }
            } else {
                if (java.io.File.separatorChar != '/')
                    filename = filename
                        .replace('/', java.io.File.separatorChar);
		try {
		    File f = new File(dirs[i], filename);
		    if (f.exists())
			return f.isDirectory();
		} catch (SecurityException ex) {
		    Decompiler.err.println("Warning: SecurityException"
					   +" while accessing "
					   +dirs[i]+filename);
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
    public Enumeration listClassFiles(final String dirName) {
        return new Enumeration() {
            int pathNr;
            Enumeration zipEnum;
            int fileNr;
            String[] files;

            public String findNextFile() {
                while (true) {
                    if (zipEnum != null) {
                        while (zipEnum.hasMoreElements()) {
                            ZipEntry ze = (ZipEntry) zipEnum.nextElement();
                            String name = ze.getName();
                            if (name.startsWith(dirName)
                                && name.endsWith(".class")) {
                                name = name.substring(dirName.length()+1);
                                if (name.indexOf('/') == -1)
                                    return name;
                            }
                        }
                        zipEnum = null;
                    }
                    if (files != null) {
                        while (fileNr < files.length) {
                            String name = files[fileNr++];
                            if (name.endsWith(".class")) {
                                return name;
                            }
                        }
                        files = null;
                    }
                    if (pathNr == dirs.length)
                        return null;

                    if (zips[pathNr] != null) {
//                         ZipEntry ze = zips[pathNr].getEntry(dirName);
//                         if (ze != null && ze.isDirectory())
                        zipEnum = zips[pathNr].entries();
                    } else if (dirs[pathNr] != null) {
                        String localDirName = 
                            (java.io.File.separatorChar != '/')
                            ? dirName.replace('/', java.io.File.separatorChar)
                            : dirName;
			try {
			    File f = new File(dirs[pathNr], localDirName);
			    if (f.exists() && f.isDirectory())
				files = f.list();
			} catch (SecurityException ex) {
			    Decompiler.err.println("Warning: SecurityException"
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
