<?php require("header.inc"); ?>
<h1>The <i>JODE</i> Applet</h1>

<p>Please be patience, loading the applet may take some time.</p>

<center>
<applet code="jode/Applet.class" archive="jode-applet.jar" width=540 height=400>
<param name=pagecolor value="ffffff">
<param name=classpath value="http://jode.sourceforge.net/plasma.jar">
<param name=class value="PlasmaApplet">
<p>Sorry you need a java enabled browser to test a java applet ;-)</p>
<p>Don't read the rest, it only contains information about the applet.</p>
</applet>
</center><br>

<p> Press the start button to decompile <a
href="http://www.informatik.uni-oldenburg.de/~mw/plasma.html">Michael's
Plasma applet</a> (and give the decompiler some time to download the
jar file).  </p>

You may change the classpath to point to a zip or jar file of your
choice, using a similar syntax.  Only http and ftp addresses are supported.
The file must be available from the world wide web. In fact you download it
from Sourceforge and Sourceforge gets it from the given address.  This hack
is necessary, because Java's security policy doesn't allow applets to contact
a different server.  You can also point the classpath to a directory containing
the class-files (include a slash `/' at the end in this case), but
this is not recommended, since it is <i>very</i> slow.  You may give
multiple entries in the class path field separated by a comma.<br><br>

You can't use this applet for local files.  You can try to give
local filenames directly without going through Sourceforge, but that is
probably forbidden by your browser.  Most browser only allow loading
files from the same server as the applet, and this is the reason why
you have to use such a weird URL.<br><br>

Save probably doesn't work, because it is forbidden by your browser.<br><br>
<?php require("footer.inc"); ?>
