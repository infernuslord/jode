/* jode.bytecode.AttributeInfo Copyright (C) 1997-1998 Jochen Hoenicke.
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
import jode.decompiler.TabbedPrintWriter;
import java.io.*;
import java.lang.reflect.Modifier;

public class AttributeInfo {
    ClassInfo classinfo;

    String name;
    byte[] data;

    public AttributeInfo() {
    }

    public void read(ConstantPool constantPool, 
                     DataInputStream input, int howMuch) throws IOException {
	String attrName = constantPool.getUTF8(input.readUnsignedShort());
	int length = input.readInt();
        if ((howMuch & ClassInfo.ALL_ATTRIBUTES) != 0) {
            name = attrName;
            data = new byte[length];
            input.readFully(data);
        } else 
            input.skip(length);
    }

    public String getName() {
        return name;
    }

    public byte[] getContents() {
        return data;
    }

    static final char hex[] = { '0','1','2','3','4','5','6','7',
                                '8','9','a','b','c','d','e','f' };

    public void dumpSource(TabbedPrintWriter writer) throws IOException{
        if (data != null) {
            writer.println("/* Attribute "+name+" ["+data.length+"]");
            writer.tab();
            StringBuffer sb = new StringBuffer();
            for (int i=0; i< data.length; i++) {
                byte b = data[i];
                int  h = (b<0)?b+256:b;
                writer.print(" " + hex[h/16] + hex[h%16]);
                if (b >=32 && b < 127) {
                    sb.append((char)b); 
                } else {
                    sb.append(".");
                }
                if (i % 16 == 15) {
                    writer.println("   "+sb.toString());
                    sb.setLength(0);
                }
            }
            if ((data.length % 16) != 0) {
                for (int i=data.length % 16; i<16; i++)
                    writer.print("   ");
                writer.println("   "+sb.toString());
            }
            writer.untab();
            writer.println("*/");
        }
    }
}
