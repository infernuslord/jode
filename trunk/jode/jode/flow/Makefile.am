## Input file for automake to generate the Makefile.in used by configure

JAR = @JAR@
JAVAC = @JAVAC@
JAVADEP = $(top_builddir)/javaDependencies.pl -subdir=$(subdir)\
   -dependdir=$(top_builddir) -classpath=$(top_builddir):$(top_srcdir)
CLASSPATH = @CLASSPATH@
CLASSLIB = @CLASSLIB@
BUILD_CLASSPATH = $(top_srcdir):$(top_builddir):$(CLASSPATH):$(CLASSLIB)

MY_JAVA_FILES = \
  BreakBlock.java \
  BreakableBlock.java \
  CaseBlock.java \
  CatchBlock.java \
  CombineIfGotoExpressions.java \
  CompleteSynchronized.java \
  ConditionalBlock.java \
  ContinueBlock.java \
  CreateAssignExpression.java \
  CreateCheckNull.java \
  CreateClassField.java \
  CreateConstantArray.java \
  CreateExpression.java \
  CreateForInitializer.java \
  CreateIfThenElseOperator.java \
  CreateNewConstructor.java \
  CreatePrePostIncExpression.java \
  DescriptionBlock.java \
  EmptyBlock.java \
  FinallyBlock.java \
  FlowBlock.java \
  IfThenElseBlock.java \
  InstructionBlock.java \
  InstructionContainer.java \
  JsrBlock.java \
  Jump.java \
  LoopBlock.java \
  RetBlock.java \
  ReturnBlock.java \
  SequentialBlock.java \
  SpecialBlock.java \
  StructuredBlock.java \
  SwitchBlock.java \
  SynchronizedBlock.java \
  ThrowBlock.java \
  TransformConstructors.java \
  TransformExceptionHandlers.java \
  TryBlock.java \
  VariableSet.java \
  VariableStack.java

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
