<?php require("header.inc"); ?>
<h1>Download</h1>

<p>Click <?php sflink("project/filelist.php")?>here</a> to download
the latest released source code of <i>JODE</i> .  You need several
other packages to build <i>JODE</i>, check the <?php selflink("links")
?>links page</a>. </p>

<p>The simplest way to get it, especially for non unix users, is in
precompiled form, though.  I have two jar archives at the <a
href="ftp://jode.sourceforge.net/pub/jode">ftp server</a>.  You may
need to press shift while clicking on the link, depending on your
browser.

<?php
function jarlink($what) {
  global $version;
  echo "<a href=\"ftp://jode.sourceforge.net/pub/jode/jode-".$version.$what;
  echo ".jar\">jode-".$version.$what.".jar</a>";
} ?>

<ul> <li><?php jarlink("-1.1") ?> is for JDK&nbsp;1.1.  It contains
the collection classes from the GNU Classpath project.  If you want to
use the swing interface, you have to download swing separately. </li>

<li> <?php jarlink("-1.2") ?> is for JDK&nbsp;1.2 or better. </li> </ul>
</p>

<h1>CVS Repository</h1>

<p>You can get the latest sources from the <?php sflink("cvs/") ?> CVS
repository</a>.  They may not always compile, though.  If you want an
older version you can use the <code>-r</code> option:</p>
<ul>
<li><code>-r jode_1_0_93</code>: gives you the version 1.0.93</li>
<li><code>-r branch_1_1</code>: gives you the latest version that will
become 1.1</li>
</ul>

<p>To build the sources from CVS change to the main directory where
the <code>configure.in</code> file resides and run

<pre>aclocal && automake -a && autoconf</pre>

Afterwards follow the instruction in the INSTALL file.  </p>

<?php require("footer.inc"); ?>
