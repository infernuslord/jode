/* GlobalOptions Copyright (C) 1999 Jochen Hoenicke.
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
import java.io.PrintStream;

public class GlobalOptions {
    public final static String version = "1.0";
    public final static String email = "jochen@gnu.org";
    public final static String copyright = 
	"Jode (c) 1998,1999 Jochen Hoenicke <"+email+">";

    public static PrintStream err = System.err;
    public static int verboseLevel   = 0;
    public static int debuggingFlags = 0;

    public static final int DEBUG_BYTECODE = 0x01;
    public static final int DEBUG_VERIFIER = 0x02;
    public static final int DEBUG_TYPES    = 0x04;
    public static final int DEBUG_FLOW     = 0x08;
    public static final int DEBUG_INOUT    = 0x10;
    public static final int DEBUG_ANALYZE  = 0x20;
    public static final int DEBUG_LVT      = 0x40;

    public static boolean sanityChecks = false;
}
