## Input file for automake to generate the Makefile.in used by configure

JAR = @JAR@
JAVAC = @JAVAC@
JAVADEP = $(top_builddir)/javaDependencies.pl\
   -dependdir=$(top_builddir) -classpath=$(top_builddir):$(top_srcdir)
CLASSPATH = @CLASSPATH@
CLASSLIB = @CLASSLIB@
BUILD_CLASSPATH = $(top_srcdir):$(top_builddir):$(CLASSPATH):$(CLASSLIB)

MY_JAVA_FILES = \
  BinaryInfo.java \
  BytecodeInfo.java \
  ClassFormatException.java \
  ClassInfo.java \
  ConstantPool.java \
  FieldInfo.java \
  GrowableConstantPool.java \
  Handler.java \
  InnerClassInfo.java \
  Instruction.java \
  LineNumber.java \
  LocalVariableInfo.java \
  MethodInfo.java \
  Opcodes.java \
  Reference.java \
  SearchPath.java

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
