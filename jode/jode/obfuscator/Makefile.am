## Input file for automake to generate the Makefile.in used by configure

JAR = @JAR@
JAVAC = @JAVAC@
JAVADEP = $(top_builddir)/javaDependencies.pl\
   -dependdir=$(top_builddir) -classpath=$(top_builddir):$(top_srcdir)
CLASSPATH = @CLASSPATH@
CLASSLIB = @CLASSLIB@
BUILD_CLASSPATH = $(top_srcdir):$(top_builddir):$(CLASSPATH):$(CLASSLIB)

MY_JAVA_FILES = \
  ClassBundle.java \
  ClassIdentifier.java \
  CodeAnalyzer.java \
  CodeTransformer.java \
  ConstantAnalyzer.java \
  ConstantRuntimeEnvironment.java \
  FieldIdentifier.java \
  Identifier.java \
  IdentifierMatcher.java \
  LocalIdentifier.java \
  LocalOptimizer.java \
  Main.java \
  MethodIdentifier.java \
  ModifierMatcher.java \
  NameSwapper.java \
  PackageIdentifier.java \
  RemovePopAnalyzer.java \
  Renamer.java \
  SimpleAnalyzer.java \
  StrongRenamer.java \
  TranslationTable.java \
  WildCard.java
#  LocalizeFieldTransformer.java

noinst_DATA = $(MY_JAVA_FILES:.java=.class)
EXTRA_DIST = $(MY_JAVA_FILES)

@QUOTE@-include $(MY_JAVA_FILES:.java=.dep)

%.class: %.java
	$(JAVAC) -classpath $(BUILD_CLASSPATH):$(CLASSLIB) -d $(top_builddir) $<

%.dep: %.class
	$(JAVADEP) $< >$@

clean-local:
	@rm -f *.class
	@rm -f *.dep
