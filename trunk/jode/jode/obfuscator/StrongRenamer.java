/* StrongRenamer Copyright (C) 1999 Jochen Hoenicke.
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

public class StrongRenamer implements Renamer {
    String charSetStart;
    String charSetCont;

    public StrongRenamer(String charSetStart, String charSetCont) {
	this.charSetStart = charSetStart;
	this.charSetCont = charSetCont;
    }

    public String generateName(Identifier ident, String lastName) {
	if (lastName == null)
	    return charSetStart.substring(0,1);

	char firstCont = charSetCont.charAt(0);
	int pos = lastName.length() - 1;
	StringBuffer sb = new StringBuffer(lastName.length() + 1);
	while (pos > 0) {
	    int index = charSetCont.indexOf(lastName.charAt(pos)) + 1;
	    if (index < charSetCont.length()) {
		sb.append(lastName.substring(0, pos));
		sb.append(charSetCont.charAt(index));
		for (int i = lastName.length() - pos - 1; i-- > 0; )
		    sb.append(firstCont);
		return sb.toString();
	    }
	    pos --;
	}
	    
	int index = charSetStart.indexOf(lastName.charAt(pos)) + 1;
	if (index < charSetStart.length()) {
	    sb.append(charSetStart.charAt(index));
	    for (int i = lastName.length() - 1; i-- > 0; )
		sb.append(firstCont);
	    return sb.toString();
	} else {
	    sb.append(charSetStart.charAt(0));
	    for (int i = lastName.length(); i-- > 0; )
		sb.append(firstCont);
	}
	return sb.toString();
    }
}
