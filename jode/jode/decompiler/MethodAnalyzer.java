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
import jode.bytecode.MethodInfo;
import jode.jvm.SyntheticAnalyzer;
import jode.type.*;
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
	    LocalInfo clazz = code.getParamInfo(0);
	    clazz.setType(Type.tClass(classAnalyzer.getClazz()));
	    clazz.setName("this");
	    offset++;
	}

	if (isConstructor()
	    && classAnalyzer.getParent() instanceof ClassAnalyzer
	    && !classAnalyzer.isStatic()) {
	    ClassAnalyzer parent = (ClassAnalyzer) classAnalyzer.getParent();
	    LocalInfo clazz = code.getParamInfo(1);
	    clazz.setType(Type.tClass(parent.getClazz()));
	    clazz.setName("this$-1");
	}
        
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
    
    public void dumpSource(TabbedPrintWriter writer) 
         throws IOException
    {
	if (synth != null) {
	    // We don't need this class anymore (hopefully?)
	    if (synth.getKind() == synth.GETCLASS)
		return;
	    if (synth.getKind() >= synth.ACCESSGETFIELD
		&& synth.getKind() <= synth.ACCESSSTATICMETHOD
		&& (Decompiler.options & Decompiler.OPTION_INNER) != 0
		&& (Decompiler.options & Decompiler.OPTION_ANON) != 0)
		return;
	}
	
	if (isConstructor && classAnalyzer.constructors.length == 1
	    && (methodType.getParameterTypes().length == 0
		|| (methodType.getParameterTypes().length == 1
		    && classAnalyzer.parent instanceof ClassAnalyzer))
	    && getMethodHeader() != null
	    && getMethodHeader().getBlock() instanceof jode.flow.EmptyBlock
	    && getMethodHeader().hasNoJumps())
	    // If this is the only constructor and it is empty and
	    // takes no parameters, this is the default constructor.
	    return;

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

        writer.println();

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
            if (isConstructor)
		writer.print(classAnalyzer.getName());
            else {
                writer.printType(getReturnType());
		writer.print(" " + methodName);
	    }
            writer.print("(");
            Type[] paramTypes = methodType.getParameterTypes();
            int offset = isStatic()?0:1;

	    int start = 0;
	    if (isConstructor()
		&& classAnalyzer.getParent() instanceof ClassAnalyzer) {
		start++;
		offset++;
	    }

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
