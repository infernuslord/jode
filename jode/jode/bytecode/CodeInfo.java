/* jode.bytecode.CodeInfo Copyright (C) 1997-1998 Jochen Hoenicke.
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
package jode.bytecode;
import java.io.*;
import java.lang.reflect.Modifier;

public class CodeInfo extends BinaryInfo {

    int maxStack, maxLocals;
    byte[] code;
    int[] exceptionTable;

    public int getMaxStack() {
        return maxStack;
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public byte[] getCode() {
        return code;
    }

    public int[] getExceptionHandlers() {
        return exceptionTable;
    }

    public void read(ConstantPool constantPool, 
                     DataInputStream input) throws IOException {
        maxStack = input.readUnsignedShort();
        maxLocals = input.readUnsignedShort();
        int codeLength = input.readInt();
        code = new byte[codeLength];
        input.readFully(code);
        int count = 4*input.readUnsignedShort();
        exceptionTable = new int[count];
        for (int i = 0; i< count; i+=4) {
            exceptionTable[i+0] = input.readUnsignedShort();
            exceptionTable[i+1] = input.readUnsignedShort();
            exceptionTable[i+2] = input.readUnsignedShort();
            exceptionTable[i+3] = input.readUnsignedShort();
        }
        readAttributes(constantPool, input, FULLINFO);
    }
}
