/* SimpleAnalyzer Copyright (C) 1999 Jochen Hoenicke.
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

package jode.obfuscator;
import jode.bytecode.Handler;
import jode.bytecode.Opcodes;
import jode.bytecode.ClassInfo;
import jode.bytecode.BytecodeInfo;
import jode.bytecode.Instruction;
import jode.bytecode.Reference;
import jode.GlobalOptions;
import jode.type.Type;

public class SimpleAnalyzer implements CodeAnalyzer, Opcodes {

    public Identifier canonizeReference(Instruction instr) {
	Reference ref = instr.getReference();
	Identifier ident = Main.getClassBundle().getIdentifier(ref);
	String clName = ref.getClazz();
	String realClazzName;
	if (ident != null) {
	    ClassIdentifier clazz = (ClassIdentifier)ident.getParent();
	    realClazzName = "L" + (clazz.getFullName()
				   .replace('.', '/')) + ";";
	} else {
	    /* We have to look at the ClassInfo's instead, to
	     * point to the right method.
	     */
	    ClassInfo clazz;
	    if (clName.charAt(0) == '[') {
		/* Arrays don't define new methods (well clone(),
		 * but that can be ignored).
		 */
		clazz = ClassInfo.javaLangObject;
	    } else {
		clazz = ClassInfo.forName
		    (clName.substring(1, clName.length()-1)
		     .replace('/','.'));
	    }
	    if (instr.getOpcode() >= opc_invokevirtual) {
		while (clazz != null
		       && clazz.findMethod(ref.getName(), 
					   ref.getType()) == null)
		    clazz = clazz.getSuperclass();
	    } else {
		while (clazz != null
		       && clazz.findField(ref.getName(), 
					  ref.getType()) == null)
		    clazz = clazz.getSuperclass();
	    }

	    if (clazz == null) {
		GlobalOptions.err.println("WARNING: Can't find reference: "
					  +ref);
		realClazzName = clName;
	    } else
		realClazzName = "L" + clazz.getName().replace('.', '/') + ";";
	}
	if (!realClazzName.equals(ref.getClazz())) {
	    ref = Reference.getReference(realClazzName, 
					 ref.getName(), ref.getType());
	    instr.setReference(ref);
	}
	return ident;
    }


    /**
     * Reads the opcodes out of the code info and determine its 
     * references
     * @return an enumeration of the references.
     */
    public void analyzeCode(MethodIdentifier m, BytecodeInfo bytecode) {
	for (Instruction instr = bytecode.getFirstInstr();
	     instr != null; instr = instr.getNextByAddr()) {
	    switch (instr.getOpcode()) {
	    case opc_checkcast:
	    case opc_instanceof:
	    case opc_multianewarray: {
		String clName = instr.getClazzType();
		int i = 0;
		while (i < clName.length() && clName.charAt(i) == '[')
		    i++;
		if (i < clName.length() && clName.charAt(i) == 'L') {
		    clName = clName.substring(i+1, clName.length()-1);
		    Main.getClassBundle().reachableIdentifier(clName, false);
		}
		break;
	    }
	    case opc_invokespecial:
	    case opc_invokestatic:
	    case opc_invokeinterface:
	    case opc_invokevirtual:
	    case opc_putstatic:
	    case opc_putfield:
		m.setGlobalSideEffects();
		/* fall through */
	    case opc_getstatic:
	    case opc_getfield: {
		Identifier ident = canonizeReference(instr);
		if (ident != null) {
		    if (instr.getOpcode() == opc_putstatic
			|| instr.getOpcode() == opc_putfield) {
			FieldIdentifier fi = (FieldIdentifier) ident;
			if (fi != null && !fi.isNotConstant())
			    fi.setNotConstant();
		    } else if (instr.getOpcode() == opc_invokevirtual
			       || instr.getOpcode() == opc_invokeinterface) {
			((ClassIdentifier) ident.getParent())
			    .reachableIdentifier(ident.getName(), 
						 ident.getType(), true);
		    } else {
			ident.setReachable();
		    }
		}
		break;
	    }
	    }
	}

	Handler[] handlers = bytecode.getExceptionHandlers();
	for (int i=0; i< handlers.length; i++) {
	    if (handlers[i].type != null)
		Main.getClassBundle()
		    .reachableIdentifier(handlers[i].type, false);
	}
    }

    public void transformCode(BytecodeInfo bytecode) {
	for (Instruction instr = bytecode.getFirstInstr(); 
	     instr != null; instr = instr.getNextByAddr()) {
	    if (instr.getOpcode() == opc_putstatic
		|| instr.getOpcode() == opc_putfield) {
		Reference ref = instr.getReference();
		FieldIdentifier fi = (FieldIdentifier)
		    Main.getClassBundle().getIdentifier(ref);
		if (fi != null
		    && (Main.stripping & Main.STRIP_UNREACH) != 0
		    && !fi.isReachable()) {
		    /* Replace instruction with pop opcodes. */
		    int stacksize = 
			(instr.getOpcode() 
			 == Instruction.opc_putstatic) ? 0 : 1;
		    stacksize += Type.tType(ref.getType()).stackSize();
		    if (stacksize == 3) {
			/* Add a pop instruction after this opcode. */
			instr.appendInstruction(Instruction.opc_pop);
			stacksize--;
		    }
		    instr.replaceInstruction(Instruction.opc_pop - 1
					     + stacksize);
		}
	    }
	}
    }
}
