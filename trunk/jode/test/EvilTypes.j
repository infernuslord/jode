; This class converts between boolean and ints without type casts.
; You can't decompile this directly, the decompiler probably gives type errors.

.class public jode/test/EvilTypes
.super java/lang/Object

.method public static boolToInt(Z)I
	.limit locals 1
	.limit stack 1
	iload_0
	ireturn
.end method

.method public static intToBool(I)Z
	.limit locals 1
	.limit stack 1
	iload_0
	ireturn
.end method

.method public static overrideParam(I)[I
	.limit locals 1
	.limit stack 1
	aconst_null
	astore_0
	aload_0
	areturn
.end method

.method public static test()V
	.limit locals 2
	.limit stack 2
	iconst_1
	invokestatic jode/test/EvilTypes/intToBool(I)Z
	istore 0
	iconst_2
	istore 1
	iload  0
	iload  1
	ixor
	pop
	return
.end method

.method private static useSerial(Ljava/io/Serializable;)V
	.limit locals 1
	.limit stack 0
	return
.end method

; This is a test where a type error occurs, because there is no Type
; that implements Cloneable, Serializable and is assignable form int
; array and java/lang/Date (though both objects are Cloneable and
; Serializable).  We can't find any correct type for local 2.

.method public static referenceCast(Ljava/util/Date;[I)Ljava/lang/Cloneable;
	.limit locals 3
	.limit stack 2
	aload_0
	ifnonnull second
	aload_0
	goto done
second:
	aload_1
done:
	dup
	astore_2
	invokestatic jode/test/EvilTypes/useSerial(Ljava/io/Serializable;)V
	aload_2
	areturn
.end method

; This shows that the bytecode verifier doesn't catch every type error.
.method public static test(Ljava/lang/String;)Ljava/lang/Runnable;
	.limit locals 1
	.limit stack 1
	aload_0
	areturn
.end method

.method public static main([Ljava/lang/String;)V
	.limit locals 1
	.limit stack 2
	aload_0
	iconst_0
	aaload
	invokestatic jode/test/EvilTypes/test(Ljava/lang/String;)Ljava/lang/Runnable;
	invokeinterface java/lang/Runnable/run()V 1
	return
.end method
