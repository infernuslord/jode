/* TabbedPrintWriter Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.decompiler;
import java.io.*;
import java.util.Stack;
import jode.Decompiler;
import jode.GlobalOptions;
import jode.bytecode.ClassInfo;
import jode.bytecode.InnerClassInfo;
import jode.type.*;

public class TabbedPrintWriter {
    boolean atbol;
    int indentsize;
    int currentIndent = 0;
    String indentStr = "";
    PrintWriter pw;
    ImportHandler imports;
    Stack scopes = new Stack();

    public TabbedPrintWriter (OutputStream os, ImportHandler imports,
			      boolean autoFlush) {
	pw = new PrintWriter(os, autoFlush);
	this.imports = imports;
	this.indentsize = (Decompiler.outputStyle & Decompiler.TAB_SIZE_MASK);
	atbol = true;
    }

    public TabbedPrintWriter (Writer os, ImportHandler imports, 
			      boolean autoFlush) {
	pw = new PrintWriter(os, autoFlush);
	this.imports = imports;
	this.indentsize = (Decompiler.outputStyle & Decompiler.TAB_SIZE_MASK);
	atbol = true;
    }

    public TabbedPrintWriter (OutputStream os, ImportHandler imports) {
	this(os, imports, true);
    }

    public TabbedPrintWriter (Writer os, ImportHandler imports) {
	this(os, imports, true);
    }

    public TabbedPrintWriter (OutputStream os) {
	this(os, null);
    }

    public TabbedPrintWriter (Writer os) {
	this(os, null);
    }

    /**
     * Convert the numeric indentation to a string.
     */
    protected void makeIndentStr() {
	int tabs = (currentIndent >> 3);
	// This is a very fast implementation.
	if (tabs <= 20)
	    indentStr = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t       "
		.substring(20 - tabs, 20 + (currentIndent&7));
	else {
	    /* the slow way */
	    StringBuffer sb = new StringBuffer(tabs+7);
	    while (tabs > 20) {
		sb.append("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t       "
			  .substring(0,20));
		tabs -= 20;
	    }
	    sb.append("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t       "
		      .substring(20 - tabs, 20 + (currentIndent&7)));
	    indentStr = sb.toString();
	} 
    }

    public void tab() {
	currentIndent += indentsize;
	makeIndentStr();
    }

    public void untab() {
	currentIndent -= indentsize;
	makeIndentStr();
    }

    public void println(String str) {
	if (atbol)
	    pw.print(indentStr);
	pw.println(str);
	atbol = true;
    }

    public void println() {
	pw.println();
	atbol = true;
    }

    public void print(String str) {
	if (atbol)
	    pw.print(indentStr);
	pw.print(str);
	atbol = false;
    }

    public void printType(Type type) {
	print(getTypeString(type));
    }

    /**
     * Print a opening brace with the current indentation style.
     * Called at the end of the line of the instance that opens the
     * brace.  It doesn't do a tab stop after opening the brace.
     */
    public void openBrace() {
	if ((Decompiler.outputStyle & Decompiler.BRACE_AT_EOL) != 0)
	    if (atbol)
		println("{");
	    else
		println(" {");
	else {
	    if (!atbol)
		println();
	    if (currentIndent > 0)
		tab();
	    println("{");
	}
    }

    public void pushScope(Scope scope) {
	scopes.push(scope);
    }

    public void popScope() {
	scopes.pop();
    }

    /**
     * Checks if the name in inScope conflicts with an identifier in a
     * higher scope.
     */
    public boolean conflicts(String name, Scope inScope, int context) {
	int dot = name.indexOf('.');
	if (dot >= 0)
	    name = name.substring(0, dot);
	int count = scopes.size();
	for (int ptr = count; ptr-- > 0; ) {
	    Scope scope = (Scope) scopes.elementAt(ptr);
	    if (scope == inScope)
		return false;
	    if (scope.conflicts(name, context)) {
		return true;
	    }
	}
	return false;
    }

    public Scope getScope(Object obj, int scopeType) {
	int count = scopes.size();
	for (int ptr = count; ptr-- > 0; ) {
	    Scope scope = (Scope) scopes.elementAt(ptr);
	    if (scope.isScopeOf(obj, scopeType))
		return scope;
	}
	return null;
    }

    public String getInnerClassString(ClassInfo info, int scopeType) {
	InnerClassInfo[] outers = info.getOuterClasses();
	if (outers == null)
	    return null;
	for (int i=0; i< outers.length; i++) {
	    if (outers[i].name == null || outers[i].outer == null)
		return null;
	    Scope scope = getScope(ClassInfo.forName(outers[i].outer), 
				   Scope.CLASSSCOPE);
	    if (scope != null && 
		!conflicts(outers[i].name, scope, scopeType)) {
 		StringBuffer sb = new StringBuffer(outers[i].name);
		for (int j = i; j-- > 0;) {
		    sb.append('.').append(outers[j].name);
		}
		return sb.toString();
	    }
	}
	String name = getClassString
	    (ClassInfo.forName(outers[outers.length-1].outer), scopeType);
	StringBuffer sb = new StringBuffer(name);
	for (int j = outers.length; j-- > 0;)
	    sb.append('.').append(outers[j].name);
	return sb.toString();
    }
    
    public String getAnonymousClassString(ClassInfo info, int scopeType) {
	InnerClassInfo[] outers = info.getOuterClasses();
	if (outers == null)
	    return null;
	for (int i=0; i< outers.length; i++) {
	    if (outers[i].name == null)
		return "ANONYMOUS CLASS";
	    Scope scope = getScope(info, Scope.METHODSCOPE);
	    if (scope != null && 
		!conflicts(outers[i].name, scope, scopeType)) {
 		StringBuffer sb = new StringBuffer(outers[i].name);
		for (int j = i; j-- > 0;) {
		    sb.append('.').append(outers[j].name);
		}
		return sb.toString();
	    } else if (outers[i].outer == null) {
		StringBuffer sb;
		if (scope != null)
		    sb = new StringBuffer("NAME CONFLICT ");
		else
		    sb = new StringBuffer("UNREACHABLE ");
		
		sb.append(outers[i].name);
		for (int j = i; j-- > 0;) {
		    sb.append('.').append(outers[j].name);
		}
		return sb.toString();
	    }
	}
	String name = getClassString
	    (ClassInfo.forName(outers[outers.length-1].outer), scopeType);
	StringBuffer sb = new StringBuffer(name);
	for (int j = outers.length; j-- > 0;)
	    sb.append('.').append(outers[j].name);
	return sb.toString();
    }
    
    public String getClassString(ClassInfo clazz, int scopeType) {
	String name = clazz.getName();
	if (name.indexOf('$') >= 0) {
	    if ((Decompiler.options & Decompiler.OPTION_INNER) != 0) {
		String innerClassName
		    = getInnerClassString(clazz, scopeType);
		if (innerClassName != null)
		    return innerClassName;
	    }
	    if ((Decompiler.options
		 & Decompiler.OPTION_ANON) != 0) {
		String innerClassName
		    = getAnonymousClassString(clazz, scopeType);
		if (innerClassName != null)
		    return innerClassName;
	    }
	}
	if (imports != null) {
	    String importedName = imports.getClassString(clazz);
	    if (!conflicts(importedName, null, scopeType))
		return importedName;
	}
	if (conflicts(name, null, Scope.AMBIGUOUSNAME))
	    return "PKGNAMECONFLICT "+ name;
	return name;
    }

    public String getTypeString(Type type) {
	if (type instanceof ArrayType)
	    return getTypeString(((ArrayType) type).getElementType()) + "[]";
	else if (type instanceof ClassInterfacesType) {
	    ClassInfo clazz = ((ClassInterfacesType) type).getClassInfo();
	    return getClassString(clazz, Scope.CLASSNAME);
	} else if (type instanceof NullType)
	    return "Object";
	else
	    return type.toString();
    }

    /**
     * Print a opening brace with the current indentation style.
     * Called at the end of the line of the instance that opens the
     * brace.  It doesn't do a tab stop after opening the brace.
     */
    public void openBraceNoSpace() {
	if ((Decompiler.outputStyle & Decompiler.BRACE_AT_EOL) != 0)
	    println("{");
	else {
	    if (!atbol)
		println();
	    if (currentIndent > 0)
		tab();
	    println("{");
	}
    }

    public void closeBraceContinue() {
	if ((Decompiler.outputStyle & Decompiler.BRACE_AT_EOL) != 0)
	    print("} ");
	else {
	    println("}");
	    if (currentIndent > 0)
		untab();
	}
    }

    public void closeBraceNoSpace() {
	if ((Decompiler.outputStyle & Decompiler.BRACE_AT_EOL) != 0)
	    print("}");
	else {
	    println("}");
	    if (currentIndent > 0)
		untab();
	}
    }

    public void closeBrace() {
	if ((Decompiler.outputStyle & Decompiler.BRACE_AT_EOL) != 0)
	    println("}");
	else {
	    println("}");
	    if (currentIndent > 0)
		untab();
	}
    }

    public void flush() {
	pw.flush();
    }

    public void close() {
	pw.close();
    }
}
