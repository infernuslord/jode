<?php require("header.inc"); ?> 

<P><i>JODE</i> is a java package containing a decompiler and an
optimizer for java.  This package is freely available under the GPL
(see <?php selflink("license") ?>license</a>).<p>

<P>The decompiler takes <tt>class</tt> files as input and produces
something similar to the original <tt>java</tt> file.  Of course this
can't be perfect: There is no way to produce the comments or the names
of local variables (except when compiled with debuging) and there are
often more ways to write the same thing.  But <i>JODE</i> does its job
quite well, so you should give it a try: <? selflink("applet") ?>start
the applet</a>.</P>

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
<li><i>JODE</i> is now hosted by <a href="http://sourceforge.net/">SourceForge</a>.</li>
<li>The latest <?php sflink("cvs/") ?>CVS</a> version breaks long lines</li>
<li>I can now decompile <b>inner and anonymous</b> classes.</li>
<li>The optimizer (aka obfuscator) can be customized via a small
config file</li>
<li>Jode is <tt>autoconf</tt>igured.</li>
</ul>

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

<p>Sometimes this program may exit with an <code>Exception</code> or
produce incorrect code.  Most time the code can't be compiled, so that
it can be easily spotted.  If you have one of these problems (except
those that occur on some of the <code>jode.test</code> files, I would
be very interested in a bug report (including the <code>class</code>
file, if possible). </p>

<p>Sometimes <i>JODE</i> generates some GOTO expression and labels.
This shouldn't happen any more with code produced by javac or jikes.
But some flow obfuscator may provoke this.  In that case you can run
the Obfuscator first (to optimize away the flow obfuscation ;-).</p>
<?php require("footer.inc"); ?>
