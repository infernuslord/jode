/* FieldAnalyzer Copyright (C) 1998-1999 Jochen Hoenicke.
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
import java.lang.reflect.Modifier;
import jode.*;
import jode.bytecode.FieldInfo;
import jode.bytecode.AttributeInfo;
import jode.bytecode.ClassFormatException;
import jode.bytecode.ConstantPool;
import jode.expr.Expression;
import jode.expr.ConstOperator;

public class FieldAnalyzer implements Analyzer {
    ClassAnalyzer clazz;
    JodeEnvironment env;
    int modifiers;
    Type type;
    String fieldName;
    Expression constant;
    boolean isSynthetic;
    boolean analyzedSynthetic = false;
    
    public FieldAnalyzer(ClassAnalyzer cla, FieldInfo fd, 
                         JodeEnvironment e)
    {
        clazz = cla;
        env  = e;

        modifiers = fd.getModifiers();
        type = fd.getType();
        fieldName = fd.getName();
        constant = null;
	this.isSynthetic = (fd.findAttribute("Synthetic") != null);

        AttributeInfo attribute = fd.findAttribute("ConstantValue");

        if (fd.getConstant() != null) {
	    constant = new ConstOperator(fd.getConstant());
	    constant.setType(type);
	    constant.makeInitializer();
        }
    }

    public String getName() {
        return fieldName;
    }

    public Type getType() {
	return type;
    }

    public boolean isSynthetic() {
	return isSynthetic;
    }

    public void analyzedSynthetic() {
	analyzedSynthetic = true;
    }

    public boolean setInitializer(Expression expr) {
        expr.makeInitializer();
        if (constant != null)
            return constant.equals(expr);
        constant = expr;
        return true;
    }

    public void analyze() {
        type.useType();
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException 
    {
	if (analyzedSynthetic)
	    return; /*XXX*/
	if (isSynthetic)
	    writer.print("/*synthetic*/ ");
	String modif = Modifier.toString(modifiers);
	if (modif.length() > 0)
	    writer.print(modif+" ");

        writer.print(type.toString() + " " + fieldName);
        if (constant != null) {
            writer.print(" = " + constant.simplify().toString());
        }
        writer.println(";");
    }
}
