/* MethodType Copyright (C) 1997-1998 Jochen Hoenicke.
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
package jode;

/** 
 * This type represents an method type.
 *
 * @author Jochen Hoenicke 
 */
public class MethodType {
    Type[] argumentTypes;
    Type returnType;
    Type elementType;

    public MethodType(String signature) {
        int index = 1, types = 0;
        while (signature.charAt(index) != ')') {
            types++;
            while (signature.charAt(index) == '[')
                index++;
            if (signature.charAt(index) == 'L')
                index = signature.indexOf(';', index);
            index++;
        }
        argumentTypes = new Type[types];

        index = 1;
        types = 0;
        while (signature.charAt(index) != ')') {
            int lastindex = index;
            while (signature.charAt(index) == '[')
                index++;
            if (signature.charAt(index) == 'L')
                index = signature.indexOf(';', index);
            index++;
            argumentTypes[types++] 
                = Type.tType(signature.substring(lastindex,index));
        }
        returnType = Type.tType(signature.substring(index+1));
    }

    public Type[] getArgumentTypes() {
        return argumentTypes;
    }

    public Type getReturnType() {
        return returnType;
    }
}
