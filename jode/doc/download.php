<?php require("header.inc"); ?>
<h2>Download</h2> 

The latest source code of <i>JODE</i> is available at the <?php
sflink("project/") ?>project page</a>.
You need several other packages to build <i>JODE</i>, check the <?php
selflink("links") ?>links page</a>. <br><br>

The simplest way to get it, especially for non unix users, is in
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

<ul> <li><?php jarlink("-1.1") ?> is for JDK&nbsp;1.1.  It already
contains Getopt and the collection classes from the GNU Classpath
project.  If you want to use the swing interface, you have to download
swing separately. </li>

<li> <?php jarlink("-1.2") ?> is for JDK&nbsp;1.2.  It already
contains Getopt, so you don't need any other package.</li> </ul>

<h2>CVS Repository</h2>

You can get the latest sources from the <?php sflink("cvs/") ?>
CVS repository</a>.
Follow the instruction on that page; use <code>jode</code> as
<i>modulename</i>.  Then change to the directory jode and run

<pre>aclocal && automake -a && autoconf</pre>

Afterwards follow the instruction in the INSTALL file.  
<?php require("footer.inc"); ?>
