<?php require("header.inc"); ?> 

<P><i>JODE</i> is a java package containing a decompiler and an
optimizer for java.  This package is <?php selflink("license")
?>freely available</a> under the GNU GPL.
<b>New:</b> The bytecode package and the core decompiler is now under
GNU Lesser General Public License, so you can integrate it in your 
project.</p>

<P>The decompiler reads in <tt>class</tt> files and produces something
similar to the original <tt>java</tt> file.  Of course this can't be
perfect: There is no way to produce the comments or the names of local
variables (except when compiled with debuging) and there are often
more ways to write the same thing.  However, <i>JODE</i> does its job quite
well, so you should give it a try and <? selflink("applet") ?>start the
applet</a>.  Jode has support for all constructs of JDK-1.3 including
inner and anonymous classes.</P>

<P>The optimizer transforms <tt>class</tt> files in various ways with
can be controlled by a script file. It supports the following
operations:</p>
<ul>
<li>Renaming class, method, field and local names to shorter,
obfuscated, or unique names or according to a given translation
table</li>
<li>Removing debugging information</li>
<li>Removing dead code (classes, fields, methods) and constant
fields</li>
<li>Optimizing local variable allocation</li>
</ul>

<h2>News</h2>

<ul>
<li><i>JODE</i> 1.1.1 is out. With support for javac v8 (jdk 1.3). </li>
<li>The license changed to LGPL for the bytecode interface and decompiler.</li>
</ul>

<h2>Known bugs of the decompiler</h2>

<p>Some jdk1.3 synthetic access functions aren't understood.  The
   produced source contains access$xxx functions, but it still compiles.</p>

<p>There may be other bugs, that cause Exceptions or invalid code.
   If you have such a problems don't hesitate to issue a bug report.
   Please include the <code>class</code> file if possible.</p>

<h2>Limitations</h2>

<p>If not all dependent classes can be found, the verifier (which is
   run before decompilation starts) may exit with a type error.  You
   can decompile it with <tt>--verify=off</tt>, but take the warning
   serious, that types may be incorrect.  There's sometimes no way to
   guess the right type, if you don't have access the full class
   hierarchie.<br>

   This is not a bug in the verifier: java will complain the same way,
   if it is run with bytecode verification turned on. And if you don't
   have the dependent classes, you can't compile the code again.</p>

<p>There may be situations, where the code doesn't understand complex
expressions. In this case many ugly temporary variables are used, but
the code should still be compileable.  This does especially happen
when you compile with <tt>`-O'</tt> flag and javac has inlined some
methods. </p>

<?php require("footer.inc"); ?>
