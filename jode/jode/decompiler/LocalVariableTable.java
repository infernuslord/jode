/* LocalVariableTable Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.Decompiler;
import jode.type.Type;
import jode.bytecode.AttributeInfo;
import jode.bytecode.ConstantPool;

public class LocalVariableTable {
    LocalVariableRangeList[] locals;

    public LocalVariableTable(int size, ConstantPool constantPool, 
			      AttributeInfo attr) {

        locals = new LocalVariableRangeList[size];
        for (int i=0; i<size; i++)
            locals[i] = new LocalVariableRangeList(i);

        DataInputStream stream = new DataInputStream
            (new ByteArrayInputStream(attr.getContents()));
        try {
            int count = stream.readUnsignedShort();
            for (int i=0; i < count; i++) {
                int start  = stream.readUnsignedShort();
                int end    = start + stream.readUnsignedShort();
		int nameIndex = stream.readUnsignedShort();
		int typeIndex = stream.readUnsignedShort();
		if (nameIndex == 0 || typeIndex == 0
		    || constantPool.getTag(nameIndex) != constantPool.UTF8
		    || constantPool.getTag(typeIndex) != constantPool.UTF8) {
		    // This is probably an evil lvt as created by HashJava
		    // simply ignore it.
		    if (Decompiler.showLVT) 
			Decompiler.err.println("Illegal entry, ignoring LVT");
		    return;
		}
                String name = constantPool.getUTF8(nameIndex);
                Type type = Type.tType(constantPool.getUTF8(typeIndex));
                int slot   = stream.readUnsignedShort();
                locals[slot].addLocal(start, end-start, name, type);
                if (Decompiler.showLVT)
                    Decompiler.err.println("\t"+name + ": " + type
                                       +" range "+start+" - "+end
                                       +" slot "+slot);
            }
        } catch (IOException ex) {
            ex.printStackTrace(Decompiler.err);
        }
    }

    public LocalVariableRangeList getLocal(int slot) 
         throws ArrayIndexOutOfBoundsException
    {
        return locals[slot];
    }
}
