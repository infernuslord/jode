<?php require("header.inc"); ?> 

<h1>FAQ - Frequently Asked Questions</h1>

This is a list of some questions that pop up from time to time.

<h2>Decompiler issues</h2>

<h3>The decompiler crashes with a VerifyException, what can I do?</h3>

<p>The class isn't verifiable, probably because there is not enough
information about used classes.  See the question about the
classpath.</p>

<p>This could also be caused by malicious bytecode, or because there
is a bug in Jode's verifier.</p>

<h3>What should be included in the classpath?</h3>

<p>Jode needs to know the full class hierarchie to guess the types.
This includes not only the classes in the program, but also the
libraries used by the java program, even the Java runtime library.
You should set the classpath to include all these classes.</p>

<p>If you don't specify the classpath on the command line, Jode uses
the same as your Java Virtual Machine.</p>

<p>As last resort, if Jode can't find a class in the classpath it uses
reflection to ask the Virtual Machine.  This works quite well, but
loading classes can have side effects, e.g. when AWT classes are
loaded, an AWT thread is created, even though Jode doesn't need
it.</p>

<h3>Why doesn't Jode decompile my inner class
<code>MyClass$Inner.class</code>?</h3>

<p>You should decompile the outermost class (<code>MyClass</code> in
this case).  The produced code contains the inner class.  </p>

<h2>Obfuscator issues</h2>

<h3>What should be included in the classpath?</h3>

<p>The program, all libraries, the Java runtime library.  Don't omit a
library even when you don't want to obfuscate it.</p>

<h3>What should I preserve</h3>

<p>The most common mistake is to preserve a class.  In most cases this
is not what you want.  This only makes sure the class won't be
renamed, it doesn't prevent it from being stripped.  Instead you
should preserve methods and constructors.  The constructor is just a
method with the special name <tt>&lt;init&gt;</tt>. </p>

<p> Another common mistake is to omit the type
signature, e.g. to preserve <tt>Class.main</tt> instead of
<tt>Class.main.([Ljava/lang/String;)V</tt>.  That doesn't work.  If
you don't want to care about the format of the type signature use a
wildcard as in <tt>Class.main.*</tt>. </p>

<h3>What is a type signature</h3>

<p>The type signature is a machine readable representation of a java
type that is used all over in java bytecode.  The JDK ships a command
named <tt>javap</tt>.  With <tt>java -s</tt> you can lists the fields
and methods of a class with their type signatures.</p>

<p> If you are interested in the format of type signatures read the
Java Virtual Machine Specification, Chapter 4.3 Descriptors</p>

<?php require("footer.inc"); ?>

