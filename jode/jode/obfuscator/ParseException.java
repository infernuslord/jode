package jode.obfuscator;

public class ParseException extends Exception {
    public ParseException(int linenr, String message) {
	super ("line "+linenr+": "+message);
    }
}
