## Input file for automake to generate the Makefile.in used by configure

JAR = @JAR@
JAVAC = @JAVAC@
JAVADEP = $(top_builddir)/javaDependencies.pl -subdir=$(subdir)\
   -dependdir=$(top_builddir) -classpath=$(top_builddir):$(top_srcdir)
CLASSPATH = @CLASSPATH@
CLASSLIB = @CLASSLIB@
SUBSTCP = @SUBSTCP@
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

@QUOTE@-include $(MY_JAVA_FILES:.java=.dep)

%.class: %.java
	$(JAVAC) -classpath `$(SUBSTCP) $(BUILD_CLASSPATH):$(CLASSLIB)` -d $(top_builddir) $<

%.dep: %.class
	$(JAVADEP) $< >$@

clean-local:
	@rm -f *.class
	@rm -f *.dep
