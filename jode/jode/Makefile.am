## Input file for automake to generate the Makefile.in used by configure

SUBDIRS = bytecode type util jvm expr flow decompiler obfuscator @SWINGUI@

JAR = @JAR@
JAVAC = @JAVAC@
JIKES = @JIKES@
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

if HAVE_JIKES

@QUOTE@-include $(top_builddir)/jode/.java.deps

%.class: %.java
	$(JIKES) -classpath $(BUILD_CLASSPATH):$(CLASSLIB) +M=$(top_builddir)/jode/.java.deps -d $(top_builddir) $<

else

%.class: %.java
	$(JAVAC) -classpath $(BUILD_CLASSPATH):$(CLASSLIB) -depend -d $(top_builddir) $<

endif

clean-local:
	@rm -f *.class
	@rm -f $(JARFILE) .java.deps

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
