dnl
dnl Add macros
dnl JODE_CHECK_JAVA
dnl

dnl JODE_CHECK_JAVA(path)
AC_DEFUN(JODE_CHECK_JAVA,
[
  AC_PATH_PROG(JAVA, java, "", $1/bin:$1/jre/bin:$PATH)
  AC_PATH_PROG(JAVAC, javac, "", $1/bin:$PATH)
  AC_PATH_PROG(JAR, jar, "", $1/bin:$PATH)
  for path in $1/lib $1/jre/lib $1/shared; do
    for classlib in classes.zip rt.jar; do
       AC_CHECK_FILES($path/$classlib, 
	[ CLASSLIB=$path/$classlib
	  break 3
	], [ true ])
    done
  done
  AC_SUBST(CLASSPATH)
  AC_SUBST(CLASSLIB)
])

AC_DEFUN(JODE_CHECK_CLASS,
[
  if (IFS=":"
    clazz=`echo $1 | sed -e 's/\./\//g' -e 's/\(.*\)/\1.class/'`
    myclasspath=$2;
    for path in $myclasspath; do
      if test -d $path; then
        if test -e $path/$clazz; then
	  exit 0
        fi
      elif CLASS_CHECK $path $clazz ; then
	exit 0
      fi
    done;
    exit 1)
  then
    $3
  else
    $4
  fi
])    
