<?php require("header.inc"); ?>
<h1>Download</h1>

<p>Jode is available in the <?
sflink("project/showfiles.php")?>download area</a> in source or
binary form.  For compiling the source code, you need several other
packages, check the <?php selflink("links") ?>links page</a>.  You
need a unix like environment for compilation.</p>

<p>The simplest way to get it, especially for non unix users, is in
precompiled form, though.  There are two jar archives in the download
area:</P>

<ul> <li>jode-1.1-JDK1.1.jar is for JDK&nbsp;1.1.  If you want to use
the swing interface, you have to download swing separately, all other
packages are already included in the archive. </li>

<li>jode-1.1.jar is for JDK&nbsp;1.2 or better.  It should run
without any other package.</li> </ul>

<h1>CVS Repository</h1>

<p>You can get the latest sources from the <?php sflink("cvs/") ?> CVS
repository</a>.  Follow the instruction on that page; use
<code>jode</code> as <i>modulename</i>.  If you want to checkout a
specific version you can use the <code>-r</code> option:</p> 

<ul>
<li><code>-r jode_1_0_93</code>: checks out the version 1.0.93</li>
<li><code>-r branch_1_1</code>: checks out the latest version in the
1.1 series.</li> </ul>

<p>To build the sources from CVS change to the main directory where
the <code>configure.in</code> file resides and run

<pre>aclocal && automake -a && autoconf</pre>

<p>Afterwards follow the instruction in the INSTALL file.  </p>
<?php require("footer.inc"); ?>

