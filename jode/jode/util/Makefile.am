## Input file for automake to generate the Makefile.in used by configure

JAR = @JAR@
JAVAC = top_srcdir=$(top_srcdir) top_builddir=$(top_builddir) $(top_builddir)/javacWithDeps.sh
CLASSPATH = @CLASSPATH@
CLASSLIB = @CLASSLIB@
BUILD_CLASSPATH = $(top_srcdir):$(top_builddir):$(CLASSPATH):$(CLASSLIB)

MY_JAVA_FILES = \
  SimpleMap.java \
  SimpleSet.java \
  ArrayEnum.java \
  AbstractCollection.java \
  AbstractList.java \
  AbstractMap.java \
  AbstractSequentialList.java \
  AbstractSet.java \
  ArrayList.java \
  Arrays.java \
  BasicMapEntry.java \
  Bucket.java \
  Collection.java \
  Collections.java \
  Comparable.java \
  Comparator.java \
  ConcurrentModificationException.java \
  HashMap.java \
  HashSet.java \
  Iterator.java \
  LinkedList.java \
  List.java \
  ListIterator.java \
  Map.java \
  Set.java \
  SortedMap.java \
  SortedSet.java \
  TreeMap.java \
  TreeSet.java \
  UnsupportedOperationException.java

noinst_DATA = $(MY_JAVA_FILES:.java=.class)
EXTRA_DIST = $(MY_JAVA_FILES)

@QUOTE@-include *.u

%.class: %.java
	$(JAVAC) -classpath $(BUILD_CLASSPATH):$(CLASSLIB) -d $(top_builddir) $<

clean-local:
	@rm -f *.class
	@rm -f *.u
