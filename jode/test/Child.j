.class public jode/test/Child
.super jode/test/Base

.field private test I

.method <init>()V
	.limit locals 1
	.limit stack 2
	aload_0
	invokespecial jode/test/Base/<init>()V
	getstatic jode/test/Base/test I	
	pop
	aload_0
	getfield jode/test/Base/test J
	pop2
	aload_0
	getfield jode/test/Child/test I
	pop
	aload_0
	getfield jode/test/Child/test J
	pop2
	return
.end method

.method public static main([Ljava/lang/String;)V
	.limit locals 1
	.limit stack 0
	return
.end method
