## Input file for automake to generate the Makefile.in used by configure

JAVADEP = $(PERL) -w -s $(top_srcdir)/scripts/javaDependencies.pl \
   -subdir=$(subdir) -dependdir=$(top_builddir) \
   -classpath=$(top_builddir):$(top_srcdir) -depfile=Makefile.dep
CLASSPATH = @CLASSPATH@
CLASSLIB = @CLASSLIB@
SUBSTCP = @SUBSTCP@
FULL_CLASSPATH := $(shell $(SUBSTCP) $(top_srcdir):$(top_builddir):$(CLASSPATH):$(CLASSLIB))

MY_JAVA_FILES = \
  ArrayEnum.java \
  SimpleMap.java \
  SimpleSet.java \
  StringQuoter.java \
  UnifyHash.java

noinst_DATA = $(MY_JAVA_FILES:.java=.class)
EXTRA_DIST = $(MY_JAVA_FILES)

@QUOTE@-include Makefile.dep

%.class: %.java
	$(JAVAC) -classpath $(FULL_CLASSPATH) -d $(top_builddir) $<

Makefile.dep: $(MY_JAVA_FILES:.java=.class)
	$(JAVADEP) $^

clean-local:
	@rm -f *.class
	@rm -f *.dep
