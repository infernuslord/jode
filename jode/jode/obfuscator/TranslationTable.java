package jode.obfuscator;

///#ifdef JDK12
///import java.util.Map;
///import java.util.TreeMap;
///import java.util.Iterator;
///#else
import jode.util.Comparator;
import jode.util.Map;
import jode.util.TreeMap;
import jode.util.Iterator;
///#endif

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;

public class TranslationTable extends TreeMap {

///#ifndef JDK12
    public TranslationTable() {
	super(createStringComparator());
    }

    private static Comparator createStringComparator() {
	return new Comparator() {
	    public int compare(Object o1, Object o2) {
		return ((String) o1).compareTo((String) o2);
	    }
	};
    }
///#endif

    public void load(InputStream in) throws IOException {
        BufferedReader reader = 
	  new BufferedReader(new InputStreamReader(in));

	String line;
        while ((line = reader.readLine()) != null) {
	    if (line.charAt(0) == '#')
		continue;
	    int delim = line.indexOf('=');
	    String key = line.substring(0, delim);
	    String value = line.substring(delim+1);
	    put(key, value);
	}
    }

    public void store(OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(out);
	for (Iterator i = entrySet().iterator(); i.hasNext(); ) {
	    Map.Entry e = (Map.Entry) i.next();
	    writer.println(e.getKey()+"="+e.getValue());
	}
	writer.flush();
    }
}
