/* MethodAnalyzer Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.bytecode.ClassInfo;
import jode.bytecode.MethodInfo;
import jode.jvm.SyntheticAnalyzer;
import jode.type.*;
import jode.expr.Expression;
import jode.expr.ThisOperator;
import jode.AssertError;
import jode.Decompiler;
import jode.GlobalOptions;

import java.lang.reflect.Modifier;
import java.io.*;

public class MethodAnalyzer implements Analyzer {
    ImportHandler imports;
    ClassAnalyzer classAnalyzer;
    MethodInfo minfo;

    String methodName;
    MethodType methodType;
    boolean isConstructor;

    CodeAnalyzer code = null;
    Type[] exceptions;

    SyntheticAnalyzer synth;

    boolean isJikesConstructor;
    boolean isImplicitAnonymousConstructor;

    public MethodAnalyzer(ClassAnalyzer cla, MethodInfo minfo,
                          ImportHandler imports) {
        this.classAnalyzer = cla;
        this.imports = imports;
	this.minfo = minfo;
        this.methodName = minfo.getName();
        this.methodType = Type.tMethod(minfo.getType());
        this.isConstructor = 
            methodName.equals("<init>") || methodName.equals("<clinit>");
        
	if (minfo.getBytecode() != null)
	    code = new CodeAnalyzer(this, minfo, imports);
        String[] excattr = minfo.getExceptions();
        if (excattr == null) {
            exceptions = new Type[0];
        } else {
	    int excCount = excattr.length;
	    this.exceptions = new Type[excCount];
	    for (int i=0; i< excCount; i++)
		exceptions[i] = Type.tClass(excattr[i]);
        }
	if (minfo.isSynthetic() || methodName.indexOf('$') != -1)
	    synth = new SyntheticAnalyzer(minfo, true);
    }

    public String getName() {
	return methodName;
    }

    public MethodType getType() {
	return methodType;
    }

    public jode.flow.FlowBlock getMethodHeader() {
        return code != null ? code.getMethodHeader() : null;
    }

    public CodeAnalyzer getCode() {
	return code;
    }

    public final boolean isConstructor() {
        return isConstructor;
    }

    public final boolean isStatic() {
        return minfo.isStatic();
    }

    public final boolean isSynthetic() {
	return minfo.isSynthetic();
    }

    public final void setJikesConstructor(boolean value) {
	isJikesConstructor = value;
    }

    public final void setAnonymousConstructor(boolean value) {
	isImplicitAnonymousConstructor = value;
    }

    public final boolean isAnonymousConstructor() {
	return isImplicitAnonymousConstructor;
    }

    public final SyntheticAnalyzer getSynthetic() {
	return synth;
    }

    public Type getReturnType() {
        return methodType.getReturnType();
    }

    public void analyze() 
      throws ClassFormatError
    {
	if (code == null)
	    return;

	int offset = 0;
	if (!isStatic()) {
	    ClassInfo classInfo = classAnalyzer.getClazz();
	    LocalInfo thisLocal = code.getParamInfo(0);
	    thisLocal.setExpression(new ThisOperator(classInfo, true));
	    offset++;
	}

//  	if (isConstructor() && !isStatic()
//  	    && classAnalyzer.outerValues != null) {
//  	    Expression[] outerValues = classAnalyzer.outerValues;
//  	    for (int i=0; i< outerValues.length; i++) {
//  		LocalInfo local = code.getParamInfo(offset+i);
//  		local.setExpression(outerValues[i]);
//  	    }
//  	}
        
	Type[] paramTypes = methodType.getParameterTypes();
	for (int i=0; i< paramTypes.length; i++) {
	    code.getParamInfo(offset).setType(paramTypes[i]);
            offset++;
        }

        for (int i= 0; i< exceptions.length; i++)
            imports.useType(exceptions[i]);
    
        if (!isConstructor)
            imports.useType(methodType.getReturnType());

	if ((Decompiler.options & Decompiler.OPTION_IMMEDIATE) == 0) {
	    if (GlobalOptions.verboseLevel > 0)
		GlobalOptions.err.print(methodName+": ");
	    code.analyze();
	    if (GlobalOptions.verboseLevel > 0)
		GlobalOptions.err.println("");
	}
    }

    public LocalInfo getParamInfo(int i) {
	if (code == null)
	    return null;
	return code.getParamInfo(i);
    }

    public void analyzeAnonymousClasses() 
      throws ClassFormatError
    {
	if (code == null)
	    return;

	code.analyzeAnonymousClasses();
    }

    public boolean skipWriting() {
	if (synth != null) {
	    // We don't need this class anymore (hopefully?)
	    if (synth.getKind() == synth.GETCLASS)
		return true;
	    if (synth.getKind() >= synth.ACCESSGETFIELD
		&& synth.getKind() <= synth.ACCESSSTATICMETHOD
		&& (Decompiler.options & Decompiler.OPTION_INNER) != 0
		&& (Decompiler.options & Decompiler.OPTION_ANON) != 0)
		return true;
	}

	if (isConstructor && isJikesConstructor) {
	    // This is the first empty part of a jikes constructor
	    return true;
	}

	boolean declareAsConstructor = isConstructor;
	int skipParams = 0;
	if (isConstructor() && !isStatic()
	    && classAnalyzer.outerValues != null)
	    skipParams = classAnalyzer.outerValues.length;

	if (isJikesConstructor) {
	    // This is the real part of a jikes constructor
	    declareAsConstructor = true;
	    skipParams = 1;
	}

	if (declareAsConstructor
	    && classAnalyzer.constructors.length == 1) {

	    // If this is the only constructor and it is empty and
	    // takes no parameters, this is the default constructor.
	    if (methodType.getParameterTypes().length == skipParams
		&& getMethodHeader() != null
		&& getMethodHeader().getBlock() instanceof jode.flow.EmptyBlock
		&& getMethodHeader().hasNoJumps())
		return true;

	    // if this is an anonymous class and this is the only
	    // constructor and it only does a super call with the given
	    // parameters, this is constructor is implicit.
	    if (isImplicitAnonymousConstructor)
		return true;
	}
	return false;
    }
    
    public void dumpSource(TabbedPrintWriter writer) 
         throws IOException
    {
	boolean declareAsConstructor = isConstructor;
	int skipParams = 0;
	if (isConstructor() && !isStatic()
	    && (Decompiler.options & Decompiler.OPTION_CONTRAFO) != 0
	    && classAnalyzer.outerValues != null)
	    skipParams = classAnalyzer.outerValues.length;

	if (isJikesConstructor) {
	    // This is the real part of a jikes constructor
	    declareAsConstructor = true;
	    skipParams = 1;
	}

	if ((Decompiler.options & Decompiler.OPTION_IMMEDIATE) != 0
	    && code != null) {
            // We do the code.analyze() here, to get 
            // immediate output.

	    if (GlobalOptions.verboseLevel > 0)
		GlobalOptions.err.print(methodName+": ");
	    code.analyze();
	    if (GlobalOptions.verboseLevel > 0)
		GlobalOptions.err.println("");
	}

        if (isConstructor() && isStatic() 
            && getMethodHeader().getBlock() instanceof jode.flow.EmptyBlock)
            return;

	if (minfo.isDeprecated()) {
	    writer.println("/**");
	    writer.println(" * @deprecated");
	    writer.println(" */");
	}
	if (minfo.isSynthetic())
	    writer.print("/*synthetic*/ ");
	String modif = Modifier.toString(minfo.getModifiers());
	if (modif.length() > 0)
	    writer.print(modif+" ");
        if (isConstructor && isStatic())
            writer.print(""); /* static block */
        else { 
            if (declareAsConstructor)
		writer.print(classAnalyzer.getName());
            else {
                writer.printType(getReturnType());
		writer.print(" " + methodName);
	    }
            writer.print("(");
            Type[] paramTypes = methodType.getParameterTypes();
            int offset = skipParams + (isStatic() ? 0 : 1);
	    int start = skipParams;

	    LocalInfo[] param = new LocalInfo[paramTypes.length];
            for (int i=start; i<paramTypes.length; i++) {
                if (code == null) {
                    param[i] = new LocalInfo(offset);
                    param[i].setType(paramTypes[i]);
		    param[i].guessName();
		    for (int j=0; j < i; j++) {
			if (param[j].getName().equals(param[i].getName())) {
			    /* A name conflict happened. */
			    param[i].makeNameUnique();
			    break; /* j */
			}
		    }
                } else {
                    param[i] = code.getParamInfo(offset);
		    offset++;
		}
            }

            for (int i=start; i<paramTypes.length; i++) {
                if (i>start)
                    writer.print(", ");
                writer.printType(param[i].getType());
		writer.print(" "+param[i].getName());
            }
            writer.print(")");
        }
        if (exceptions.length > 0) {
            writer.println("");
            writer.print("throws ");
            for (int i= 0; i< exceptions.length; i++) {
                if (i > 0)
                    writer.print(", ");
                writer.printType(exceptions[i]);
            }
        }
        if (code != null) {
	    writer.openBrace();
            writer.tab();
            code.dumpSource(writer);
            writer.untab();
	    writer.closeBrace();
        } else
            writer.println(";");
    }

    public ClassAnalyzer getClassAnalyzer() {
	return classAnalyzer;
    }
}
