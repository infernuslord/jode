## Input file for automake to generate the Makefile.in used by configure

SUBDIRS = util bytecode type jvm expr flow decompiler obfuscator @SWINGUI@

JAR = @JAR@
JAVAC = top_srcdir=$(top_srcdir) top_builddir=$(top_builddir) $(top_builddir)/javacWithDeps.sh
CLASSPATH = @CLASSPATH@
CLASSLIB = @CLASSLIB@
BUILD_CLASSPATH = $(top_srcdir):$(top_builddir):.:$(CLASSPATH):$(CLASSLIB)
VPATH=$(srcdir):$(top_srcdir):$(top_builddir)

MY_JAVA_FILES = \
  AssertError.java \
  Decompiler.java  \
  GlobalOptions.java  \
  JodeApplet.java  \
  JodeWindow.java

noinst_DATA = $(MY_JAVA_FILES:.java=.class)
EXTRA_DIST = $(MY_JAVA_FILES)


JARFILE = jode-@VERSION@.jar
data_DATA = $(JARFILE)

@QUOTE@-include *.u

%.class: %.java
	$(JAVAC) -classpath $(BUILD_CLASSPATH):$(CLASSLIB) -d $(top_builddir) $<

clean-local:
	@rm -f *.class
	@rm -f *.u

$(JARFILE): $(noinst_DATA)
	CLASSPATH=$(top_builddir):$(CLASSPATH) $(JAVA) -mx80m \
	    jode.obfuscator.Main --dest $(JARFILE) \
	    --revtable rename.table \
	    --rename=none --breakserial --strip=unreach -v -v \
	    --preserve 'jode.Decompiler.main.*' \
	    --preserve 'jode.JodeApplet.<init>.*' \
	    --preserve 'jode.JodeWindow.main.*' \
	    --preserve 'jode.obfuscator.Main.main.*' \
	    --preserve 'jode.swingui.Main.main.*'  jode
