## Input file for automake to generate the Makefile.in used by configure

JAR = @JAR@
JAVAC = @JAVAC@
JIKES = @JIKES@
CLASSPATH = @CLASSPATH@
CLASSLIB = @CLASSLIB@
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
  ConstructorOperator.java \
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
