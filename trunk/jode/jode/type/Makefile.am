## Input file for automake to generate the Makefile.in used by configure

JAR = @JAR@
JAVAC = top_srcdir=$(top_srcdir) top_builddir=$(top_builddir) $(top_builddir)/javacWithDeps.sh
CLASSPATH = @CLASSPATH@
CLASSLIB = @CLASSLIB@
BUILD_CLASSPATH = $(top_srcdir):$(top_builddir):$(CLASSPATH):$(CLASSLIB)

MY_JAVA_FILES = \
  ArrayType.java \
  ClassInterfacesType.java \
  IntegerType.java \
  MethodType.java \
  NullType.java \
  RangeType.java \
  ReferenceType.java \
  Type.java

noinst_DATA = $(MY_JAVA_FILES:.java=.class)
EXTRA_DIST = $(MY_JAVA_FILES)

@QUOTE@-include *.u

%.class: %.java
	$(JAVAC) -classpath $(BUILD_CLASSPATH):$(CLASSLIB) -d $(top_builddir) $<

clean-local:
	@rm -f *.class
	@rm -f *.u
