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
    final String signature;
    final Type[] parameterTypes;
    final Type returnType;
    final boolean staticFlag;

    public MethodType(boolean isStatic, String signature) {
        this.staticFlag = isStatic;
        this.signature = signature;
        int index = 1, types = 0;
        while (signature.charAt(index) != ')') {
            types++;
            while (signature.charAt(index) == '[')
                index++;
            if (signature.charAt(index) == 'L')
                index = signature.indexOf(';', index);
            index++;
        }
        parameterTypes = new Type[types];

        index = 1;
        types = 0;
        while (signature.charAt(index) != ')') {
            int lastindex = index;
            while (signature.charAt(index) == '[')
                index++;
            if (signature.charAt(index) == 'L')
                index = signature.indexOf(';', index);
            index++;
            parameterTypes[types++] 
                = Type.tType(signature.substring(lastindex,index));
        }
        returnType = Type.tType(signature.substring(index+1));
    }

    public boolean isStatic() {
        return staticFlag;
    }

    public Type[] getParameterTypes() {
        return parameterTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    public String getTypeSignature() {
        return signature;
    }

    public String toString() {
        return signature;
    }

    public boolean equals(Object o) {
        MethodType mt;
        return (o instanceof MethodType
                && signature.equals((mt = (MethodType)o).signature)
                && staticFlag == mt.staticFlag);
    }
}
