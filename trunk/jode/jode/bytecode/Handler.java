package jode.bytecode;

/**
 * A simple class containing the info about an exception handler
 */
public class Handler {
    public Instruction start, end, catcher;
    public String type;
}

