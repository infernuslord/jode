/* WildCard Copyright (C) 1999 Jochen Hoenicke.
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

public class WildCard {

    String wildcard;
    int firstStar;

    public WildCard(String wild) {
	wildcard = wild;
	firstStar = wildcard.indexOf('*');
    }

    public String getNextComponent(String prefix) {
	int lastDot = prefix.length();
	if (lastDot < wildcard.length()
	    || !wildcard.startsWith(prefix)
	    || (lastDot > 0 && wildcard.charAt(lastDot) != '.'))
	    return null;
	if (lastDot > 0)
	    lastDot++;
	int nextDot = wildcard.indexOf('.', lastDot);
	if (firstStar == -1) {
	    if (nextDot == -1)
		return wildcard.substring(lastDot);
	    else
		return wildcard.substring(lastDot, nextDot);
	} else {
	    if (nextDot == -1 || nextDot > firstStar) {
		return null;
	    } else {
		return wildcard.substring(lastDot, nextDot);
	    }
	}
    }

    public boolean startsWith(String test) {
	if (firstStar == -1 || firstStar >= test.length())
	    return wildcard.startsWith(test);
	return test.startsWith(wildcard.substring(0, firstStar));
    }

    public boolean matches(String test) {
	if (firstStar == -1)
	    return wildcard.equals(test);
	if (!test.startsWith(wildcard.substring(0, firstStar)))
	    return false;

	test = test.substring(firstStar);
	int lastWild = firstStar;
	int nextWild;
	while ((nextWild = wildcard.indexOf('*', lastWild + 1)) != -1) {
	    String pattern = wildcard.substring(lastWild+1, nextWild);
	    while (!test.startsWith(pattern)) {
		if (test.length() == 0)
		    return false;
		test = test.substring(1);
	    }
	    test = test.substring(nextWild - lastWild - 1);
	    lastWild = nextWild;
	}

	return test.endsWith(wildcard.substring(lastWild+1));
    }

    public String toString() {
	return "Wildcard "+wildcard;
    }
}
