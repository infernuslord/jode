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
  ArrayLengthOperator.java \
  ArrayLoadOperator.java \
  ArrayStoreOperator.java \
  BinaryOperator.java \
  CheckCastOperator.java \
  CheckNullOperator.java \
  ClassFieldOperator.java \
  CombineableOperator.java \
  CompareBinaryOperator.java \
  CompareToIntOperator.java \
  CompareUnaryOperator.java \
  ConstOperator.java \
  ConstantArrayOperator.java \
  ConvertOperator.java \
  Expression.java \
  GetFieldOperator.java \
  IIncOperator.java \
  IfThenElseOperator.java \
  InstanceOfOperator.java \
  InvokeOperator.java \
  LValueExpression.java \
  LocalLoadOperator.java \
  LocalStoreOperator.java \
  LocalVarOperator.java \
  MatchableOperator.java \
  MonitorEnterOperator.java \
  MonitorExitOperator.java \
  NewArrayOperator.java \
  NewOperator.java \
  NoArgOperator.java \
  NopOperator.java \
  Operator.java \
  OuterLocalOperator.java \
  PopOperator.java \
  PrePostFixOperator.java \
  PutFieldOperator.java \
  ShiftOperator.java \
  SimpleOperator.java \
  StoreInstruction.java \
  StringAddOperator.java \
  ThisOperator.java \
  UnaryOperator.java

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
