/* JodeApplet Copyright (C) 1999 Jochen Hoenicke.
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
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class JodeApplet extends Applet {
    JodeWindow jodeWin = new JodeWindow(this);

///#ifdef AWT10
///    public boolean action(Event e, Object arg) {
///	jodeWin.action(e);
///	return true;
///    }
///#endif
    	
    public void init() {
	String cp = getParameter("classpath");
	if (cp != null)
	    jodeWin.setClasspath(cp);
	String cls = getParameter("class");
	if (cls != null)
	    jodeWin.setClass(cls);
	setFont(new Font("dialog", Font.PLAIN, 10));
    }
}

