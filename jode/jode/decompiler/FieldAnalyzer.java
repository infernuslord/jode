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
import jode.type.*;
import jode.bytecode.FieldInfo;
import jode.expr.Expression;
import jode.expr.ThisOperator;
import jode.expr.LocalLoadOperator;
import jode.expr.ConstOperator;

public class FieldAnalyzer implements Analyzer {
    ClassAnalyzer clazz;
    ImportHandler imports;
    int modifiers;
    Type type;
    String fieldName;
    Expression constant;
    boolean isSynthetic;
    boolean isDeprecated;
    boolean analyzedSynthetic = false;
    
    public FieldAnalyzer(ClassAnalyzer cla, FieldInfo fd, 
                         ImportHandler i)
    {
        clazz = cla;
        imports = i;

        modifiers = fd.getModifiers();
        type = Type.tType(fd.getType());
        fieldName = fd.getName();
        constant = null;
	this.isSynthetic = fd.isSynthetic();
	this.isDeprecated = fd.isDeprecated();
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

    public Expression getConstant() {
	return constant;
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
    
    public boolean setSpecial(Expression expr) {
	if (!isSynthetic || !Modifier.isFinal(modifiers))
	    return false;
	if (!fieldName.startsWith("this$")) {
	    if (!(expr instanceof LocalLoadOperator)
		|| !fieldName.startsWith("val$"))
		return false;
	}
	if (constant != null)
	    return constant.equals(expr);
	analyzedSynthetic();
	constant = expr;
	return true;
    }

    public boolean setClassConstant(String clazzName) {
	if (constant != null)
	    return false;
	if (fieldName.equals("class$" + clazzName.replace('.', '$'))
	    || fieldName.equals("class$L" + clazzName.replace('.', '$'))) {
	    analyzedSynthetic();
	    return true;
	}
	return false;
    }

    public void analyze() {
        imports.useType(type);
	if (constant != null)
	    constant = constant.simplify();
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException 
    {
	if (analyzedSynthetic)
	    return; /*XXX*/
	if (isDeprecated) {
	    writer.println("/**");
	    writer.println(" * @deprecated");
	    writer.println(" */");
	}
	if (isSynthetic)
	    writer.print("/*synthetic*/ ");
	String modif = Modifier.toString(modifiers);
	if (modif.length() > 0)
	    writer.print(modif+" ");

	writer.printType(type);
        writer.print(" " + fieldName);
        if (constant != null) {
            writer.print(" = ");
	    constant.dumpExpression(writer);
        }
        writer.println(";");
    }
}

