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
import jode.type.*;
import jode.AssertError;
import jode.Decompiler;
import jode.GlobalOptions;

import java.lang.reflect.Modifier;
import java.io.*;

public class MethodAnalyzer implements Analyzer {
    ImportHandler imports;
    CodeAnalyzer code = null;
    ClassAnalyzer classAnalyzer;
    boolean isConstructor;
    boolean isStatic;
    boolean isSynthetic;
    boolean isDeprecated;
    int modifiers;
    String methodName;
    MethodType methodType;
    Type[] exceptions;

    boolean analyzed = false;
    SyntheticAnalyzer synth;
    
    public MethodAnalyzer(ClassAnalyzer cla, MethodInfo minfo,
                          ImportHandler imports) {
        this.classAnalyzer = cla;
        this.imports = imports;
        this.modifiers = minfo.getModifiers();
        this.methodType = Type.tMethod(minfo.getType());
        this.methodName = minfo.getName();
        this.isStatic = minfo.isStatic();
        this.isConstructor = 
            methodName.equals("<init>") || methodName.equals("<clinit>");
	this.isSynthetic = minfo.isSynthetic();
	this.isDeprecated = minfo.isDeprecated();
        
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
        return isStatic;
    }

    public final boolean isSynthetic() {
	return isSynthetic;
    }

    public final boolean isGetClass() {
	if (synth == null) 
	    analyzeSynthetic();
	return synth.type == SyntheticAnalyzer.GETCLASS;
    }

    public Type getReturnType() {
        return methodType.getReturnType();
    }

    public void analyze() 
      throws ClassFormatError
    {
	if (code == null)
	    return;

	analyzed = true;
	int offset = 0;
	if (!isStatic()) {
	    LocalInfo clazz = code.getParamInfo(0);
	    clazz.setType
                (Type.tClass(this.classAnalyzer.getClazz().getName()));
	    clazz.setName("this");
	    offset++;
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

	if (!Decompiler.immediateOutput) {
	    if (GlobalOptions.verboseLevel > 0)
		GlobalOptions.err.print(methodName+": ");
	    code.analyze();
	    if (GlobalOptions.verboseLevel > 0)
		GlobalOptions.err.println("");
	}
    }
    
    public void analyzeSynthetic() {
	if (!analyzed)
	    analyze();
	synth = new SyntheticAnalyzer(this);
    }
	    
    public void dumpSource(TabbedPrintWriter writer) 
         throws IOException
    {
	if (synth != null && synth.type == synth.GETCLASS)
	    // We don't need this class anymore (hopefully?)
	    return;
	
	if (isConstructor && classAnalyzer.constructors.length == 1
	    && methodType.getParameterTypes().length == 0
	    && getMethodHeader() != null
	    && getMethodHeader().getBlock() instanceof jode.flow.EmptyBlock
	    && getMethodHeader().hasNoJumps())
	    // If this is the only constructor and it is empty and
	    // takes no parameters, this is the default constructor.
	    return;

	if (Decompiler.immediateOutput && code != null) {
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

	if (isDeprecated) {
	    writer.println("/**");
	    writer.println(" * @deprecated");
	    writer.println(" */");
	}
	String modif = Modifier.toString(modifiers);
	if (modif.length() > 0)
	    writer.print(modif+" ");
        if (isConstructor && isStatic())
            writer.print(""); /* static block */
        else { 
            if (isConstructor)
                writer.print(imports.getClassString(classAnalyzer.getClazz()));
            else
                writer.print(getReturnType().toString()
			     + " " + methodName);
            writer.print("(");
            Type[] paramTypes = methodType.getParameterTypes();
            int offset = isStatic()?0:1;
            for (int i=0; i<paramTypes.length; i++) {
                if (i>0)
                    writer.print(", ");
                LocalInfo li;
                if (code == null) {
                    li = new LocalInfo(offset);
                    li.setType(paramTypes[i]);
                    li.makeNameUnique();
                } else
                    li = code.getParamInfo(offset);
                writer.print(li.getType().toString()+" "+li.getName());
                offset++;
            }
            writer.print(")");
        }
        if (exceptions.length > 0) {
            writer.println("");
            writer.print("throws ");
            for (int i= 0; i< exceptions.length; i++) {
                if (i > 0)
                    writer.print(", ");
                writer.print(exceptions[i].toString());
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
}
