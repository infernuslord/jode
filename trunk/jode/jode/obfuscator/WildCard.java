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

    public static boolean matches(String wildcard, String test) {
	int indexWild = wildcard.indexOf('*');
	if (indexWild == -1)
	    return wildcard.equals(test);
	if (!test.startsWith(wildcard.substring(0, indexWild)))
	    return false;

	test = test.substring(indexWild);
	int nextWild;
	while ((nextWild = wildcard.indexOf('*', indexWild + 1)) != -1) {
	    String pattern = wildcard.substring(indexWild+1, nextWild);
	    while (!test.startsWith(pattern))
		test = test.substring(1);
	    test = test.substring(nextWild - indexWild);
	    indexWild = nextWild;
	}

	return test.endsWith(wildcard.substring(indexWild+1));
    }
}
