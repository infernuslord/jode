## Input file for automake to generate the Makefile.in used by configure

JAR = @JAR@
JAVAC = @JAVAC@
JIKES = @JIKES@
CLASSPATH = @CLASSPATH@
CLASSLIB = @CLASSLIB@
BUILD_CLASSPATH = $(top_srcdir):$(top_builddir):$(CLASSPATH):$(CLASSLIB)

MY_JAVA_FILES = \
  Analyzer.java \
  ClassAnalyzer.java \
  ClassDeclarer.java \
  DeadCodeAnalysis.java \
  Declarable.java \
  FieldAnalyzer.java \
  ImportHandler.java \
  LocalInfo.java \
  LocalVarEntry.java \
  LocalVariableRangeList.java \
  LocalVariableTable.java \
  MethodAnalyzer.java \
  Opcodes.java \
  OuterValueListener.java \
  Scope.java \
  TabbedPrintWriter.java

noinst_DATA = $(MY_JAVA_FILES:.java=.class)
EXTRA_DIST = $(MY_JAVA_FILES)

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
