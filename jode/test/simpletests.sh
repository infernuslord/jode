#!/bin/sh

TEMP=`mktemp -d tmp.XXXXXX`

if echo $JAVAC | grep jikes >/dev/null; then
    compiler=JIKES;
elif echo $JAVAC | grep javac  >/dev/null; then
    compiler=`$JAVAC -J-version 2>&1 | grep version | \
             perl -pe's/^.*version \"?([0-9]+)\.([0-9]+).*$/JAVAC\1\2/'`
else
    compiler=UNKNOWN
fi

echo "detected compiler $compiler"

error=""

EXPECT_FAIL="ResolveConflicts.java AnonymousClass.java InnerClass.java"

for testclass in \
ArrayCloneTest.java \
ArrayTest.java \
AssignOp.java \
ClassOpTest.java \
ConstantTypes.java \
Expressions.java \
Flow.java \
For.java \
HintTypeTest.java \
IfCombine.java \
LocalTypes.java \
ResolveConflicts.java \
TriadicExpr.java \
TryCatch.java \
Unreach.java \
AnonymousClass.java \
InnerClass.java \
InnerCompat.java \
NestedAnon.java
do
    cp $srcdir/$testclass $TEMP
    $top_srcdir/jcpp -D$compiler $TEMP/$testclass
    $JAVAC $JFLAGS -d $TEMP $TEMP/$testclass
    CLASSPATH=$top_builddir:$CLASSPATH $JAVA jode.Decompiler \
         --classpath=$TEMP --dest=$TEMP ${testclass%.java} &> $testclass.log
    if ! CLASSPATH=$TEMP:$CLASSPATH $JAVAC $JFLAGS -d $TEMP $TEMP/$testclass >> $testclass.log 2>&1 ; then
       cat $TEMP/$testclass >> $testclass.log
       CLASSPATH=$TEMP:$CLASSPATH javap -c ${testclass%.java} >> $testclass.log
       if ! echo $EXPECT_FAIL | grep $testclass >/dev/null ; then
         error="$error $testclass";
         echo "FAIL: $testclass"
       else
         echo "EXPECTED FAIL: $testclass"
       fi
    else
       echo "PASS: $testclass"
       rm $testclass.log
    fi
    #rm -rf $TEMP/*
done

rm -rf $TEMP;

if [ -n "$error" ]; then
    exit 1;
fi
