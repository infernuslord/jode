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
import jode.bytecode.AttributeInfo;
import jode.*;

import java.lang.reflect.Modifier;
import java.io.*;

public class MethodAnalyzer implements Analyzer {
    JodeEnvironment env;
    CodeAnalyzer code = null;
    ClassAnalyzer classAnalyzer;
    boolean isConstructor;
    boolean isStatic;
    boolean isSynthetic;
    boolean isDeprecated;
    SyntheticAnalyzer synth;
    int modifiers;
    String methodName;
    MethodType methodType;
    Type[] exceptions;
    
    public MethodAnalyzer(ClassAnalyzer cla, MethodInfo minfo,
                          JodeEnvironment env) {
        this.classAnalyzer = cla;
        this.env = env;
        this.modifiers = minfo.getModifiers();
        this.methodType = minfo.getType();
        this.methodName = minfo.getName();
        this.isStatic = minfo.isStatic();
        this.isConstructor = 
            methodName.equals("<init>") || methodName.equals("<clinit>");
	this.isSynthetic = (minfo.findAttribute("Synthetic") != null);
	this.isDeprecated = (minfo.findAttribute("Deprecated") != null);
        
        AttributeInfo codeattr = minfo.findAttribute("Code");
        if (codeattr != null) {
	    code = new CodeAnalyzer(this, codeattr, env);
        }
        
        AttributeInfo excattr = minfo.findAttribute("Exceptions");
        if (excattr == null) {
            exceptions = new Type[0];
        } else {
            DataInputStream stream = new DataInputStream
                (new ByteArrayInputStream(excattr.getContents()));
            try {
                int throwCount = stream.readUnsignedShort();
                this.exceptions = new Type[throwCount];
                for (int t=0; t< throwCount; t++) {
                    int idx = stream.readUnsignedShort();
                    exceptions[t] = Type.tClass(classAnalyzer.getConstantPool()
                                                .getClassName(idx));
                }
            } catch (IOException ex) {
                throw new AssertError("exception attribute too long?");
            }
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
	return synth.type == SyntheticAnalyzer.GETCLASS;
    }

    public int getParamCount() {
        int count = isStatic() ? 0 : 1;
        Type[] paramTypes = methodType.getParameterTypes();
        for (int i=0; i< paramTypes.length; i++)
            count += paramTypes[i].stackSize();
	return count;
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
	    clazz.setType
                (Type.tClass(this.classAnalyzer.getClazz().getName()));
	    clazz.setName("this");
	    offset++;
	}
        
	Type[] paramTypes = methodType.getParameterTypes();
	for (int i=0; i< paramTypes.length; i++) {
	    code.getParamInfo(offset).setType(paramTypes[i]);
            offset += paramTypes[i].stackSize();
        }

        for (int i= 0; i< exceptions.length; i++)
            exceptions[i].useType();
    
        if (!isConstructor)
            methodType.getReturnType().useType();

	if (!Decompiler.immediateOutput || isSynthetic) {
	    if (Decompiler.isVerbose)
		Decompiler.err.print(methodName+": ");
	    code.analyze();
	    if (isSynthetic)
		synth = new SyntheticAnalyzer(this);
	    if (Decompiler.isVerbose)
		Decompiler.err.println("");
	}
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

	    if (Decompiler.isVerbose)
		Decompiler.err.print(methodName+": ");
	    code.analyze();
	    if (Decompiler.isVerbose)
		Decompiler.err.println("");
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
                writer.print(env.classString(classAnalyzer.
                                             getClazz().getName()));
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
                offset += paramTypes[i].stackSize();
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
