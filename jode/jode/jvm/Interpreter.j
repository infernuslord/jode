; Interpreter Copyright (C) 1999 Jochen Hoenicke.
;
; This program is free software; you can redistribute it and/or modify
; it under the terms of the GNU General Public License as published by
; the Free Software Foundation; either version 2, or (at your option)
; any later version.
;
; This program is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU General Public License for more details.
;
; You should have received a copy of the GNU General Public License
; along with this program; see the file COPYING.  If not, write to
; the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
;
; $Id$

; This class is a java virtual machine written in java :-).  Well not
; exactly.  It does only handle a subset of the opcodes and is mainly
; written do deobfuscate Strings.
;
; @author Jochen Hoenicke

.class public jode/jvm/Interpreter
.super java/lang/Object
.implements jode/bytecode/Opcodes

.method public static interpretMethod(Ljode/jvm/RuntimeEnvironment;Ljode/bytecode/BytecodeInfo;[Ljode/jvm/Value;)Ljava/lang/Object;
.throws jode/jvm/InterpreterException
.throws java/lang/reflect/InvocationTargetException
	.limit locals 9
	.limit stack 10
	.catch java/lang/RuntimeException from aload_start to aload_end using wrapexception_handler
	.catch java/lang/RuntimeException from aastore_start to aastore_end using wrapexception_handler
	.catch java/lang/RuntimeException from zastore_start to zastore_end using wrapexception_handler
	.catch java/lang/RuntimeException from bastore_start to bastore_end using wrapexception_handler
	.catch java/lang/RuntimeException from sastore_start to sastore_end using wrapexception_handler
	.catch java/lang/RuntimeException from castore_start to castore_end using wrapexception_handler

	.catch java/lang/ArithmeticException from idiv_start to idiv_end using wrapexception_handler
	.catch java/lang/ArithmeticException from irem_start to irem_end using wrapexception_handler
	.catch java/lang/ArithmeticException from ldiv_start to ldiv_end using wrapexception_handler
	.catch java/lang/ArithmeticException from lrem_start to lrem_end using wrapexception_handler
	.catch java/lang/NullPointerException from arrlength_start to arrlength_end using wrapexception_handler
	.catch java/lang/NegativeArraySizeException from newarray_start to newarray_end using wrapexception_handler
	.catch java/lang/reflect/InvocationTargetException from newinstance_start to newinstance_end using invocationtarget_handler
	.catch java/lang/reflect/InvocationTargetException from invoke_start to invoke_end using invocationtarget_handler

	aload_1
	dup
	invokevirtual jode/bytecode/BytecodeInfo/getMaxStack()I
	dup
	anewarray jode/jvm/Value
	dup_x1
	astore_3
	goto initstack_enter
initstack_loop:
	dup2
	new jode/jvm/Value
	dup
	invokenonvirtual jode/jvm/Value/<init>()V
	aastore
initstack_enter:
	iconst_1
	isub
	dup
	ifge initstack_loop
	pop2

	invokevirtual jode/bytecode/BytecodeInfo/getFirstInstr()Ljode/bytecode/Instruction;
	astore 4
	iconst_0
	istore 5

; 0 == env
; 1 == code
; 2 == stack
; 3 == locals
; 4 == pc
; 5 == stacktop
big_loop:


; ========= DEBUGGING OUTPUT ===============================
	getstatic jode/Decompiler/isDebugging Z
	ifeq skip_debugging
	getstatic jode/Decompiler/err Ljava/io/PrintStream;
	dup
	aload 4
	invokevirtual jode/bytecode/Instruction/getDescription()Ljava/lang/String;
	invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V
	dup
	new java/lang/StringBuffer
	dup
	ldc "stack: ["
	invokenonvirtual java/lang/StringBuffer/<init>(Ljava/lang/String;)V
; stack contains:
; addr+": ["
; System.err
; System.err
	iconst_0	
	istore 6
	iload 5		; stacktop
	ifgt stackenter_1
	goto stackdone_1	

; stringbuffer
; System.err
; System.err
stackloop_1:
	ldc ","
	invokevirtual java/lang/StringBuffer/append(Ljava/lang/String;)Ljava/lang/StringBuffer;
stackenter_1:
	aload_3
	iload 6
	aaload
	dup
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	instanceof [C
	ifeq add_object
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	checkcast [C
	invokevirtual java/lang/StringBuffer/append([C)Ljava/lang/StringBuffer;
	goto added_object
add_object:
	invokevirtual java/lang/StringBuffer/append(Ljava/lang/Object;)Ljava/lang/StringBuffer;
added_object:
	iinc 6 1
	iload 6		; stackindex
	iload 5		; stacktop
	if_icmplt stackloop_1

; stringbuffer
; System.err
stackdone_1:
	ldc "]"
	invokevirtual java/lang/StringBuffer/append(Ljava/lang/String;)Ljava/lang/StringBuffer;
	invokevirtual java/lang/StringBuffer/toString()Ljava/lang/String;
	invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V

	new java/lang/StringBuffer
	dup
	
	ldc "local: ["
	invokenonvirtual java/lang/StringBuffer/<init>(Ljava/lang/String;)V

; stack contains:
; addr+": ["
; System.err
	iconst_0	
	istore 6
	aload 2
	arraylength	; maxlocals
	ifgt localenter_2
	goto localdone_2	

; stringbuffer
; System.err
localloop_2:
	ldc ","
	invokevirtual java/lang/StringBuffer/append(Ljava/lang/String;)Ljava/lang/StringBuffer;
localenter_2:
	aload_2
	iload 6
	aaload
	dup
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	instanceof [C
	ifeq add_object2
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	checkcast [C
	invokevirtual java/lang/StringBuffer/append([C)Ljava/lang/StringBuffer;
	goto added_object2
add_object2:
	invokevirtual java/lang/StringBuffer/append(Ljava/lang/Object;)Ljava/lang/StringBuffer;
added_object2:
	iinc 6 1
	iload 6		; stackindex
	aload 2
	arraylength	; maxlocals
	if_icmplt localloop_2

; stringbuffer
; System.err
localdone_2:
	ldc "]"
	invokevirtual java/lang/StringBuffer/append(Ljava/lang/String;)Ljava/lang/StringBuffer;
	invokevirtual java/lang/StringBuffer/toString()Ljava/lang/String;
	invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V

; ========= DEBUGGING OUTPUT ENDS ===============================
skip_debugging:
	aload 4
	dup
	astore 6 
	dup
	getfield jode/bytecode/Instruction/nextByAddr Ljode/bytecode/Instruction;
	astore 4
	getfield jode/bytecode/Instruction/opcode I
; stack:
; instr.opcode	
	dup
	sipush 153
	if_icmpge if_or_special_instr
	dup
	bipush 87
	if_icmpge no_const_store_load_instr
	dup
	bipush 20
	if_icmpgt load_store_instr
	dup
	ifeq nop_instr

; ====== LDC / LDC2_W
	aload_3			; stack
	iload 5			; stacktop
	aaload	
	iinc 5 1
	aload 6
	getfield jode/bytecode/Instruction/objData Ljava/lang/Object;
; Stack:
;  instr.objData
;  stack_value
;  opcode
	invokevirtual jode/jvm/Value/setObject(Ljava/lang/Object;)V
	bipush 20		; opc_ldc2_w
	if_icmpne big_loop
	iinc 5 1
	goto big_loop

illegal_instr:
nop_instr:
popI_big_loop:
	pop
	goto big_loop

; ====== LOAD / STORE INSTRUCTIONS

load_store_instr:
	dup
	bipush 54
	if_icmpge store_instr
	dup
	bipush 46
	if_icmpge array_load_instr

; ====== LOAD INSTRUCTIONS

	aload_3			; stack
	iload 5			; stacktop
	aaload	
	iinc 5 1

	aload_2			; locals
	aload 6
	getfield jode/bytecode/Instruction/localSlot I
	aaload
	invokevirtual jode/jvm/Value/setValue(Ljode/jvm/Value;)V

	iconst_1
	iand			; opcode & 1
	ifne big_loop
	iinc 5 1
	goto big_loop

array_load_instr:
	iinc 5 -1
	aload_3
	iload 5
	iconst_m1
	iadd
	aaload
	dup_x1
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/intValue()I
aload_start:
	invokestatic java/lang/reflect/Array/get(Ljava/lang/Object;I)Ljava/lang/Object;
aload_end:
; Stack:
;   element (of wrong type)
;   opcode
;   value (for result)

	swap
	bipush 51
	if_icmplt set_object_big_loop
; Stack:
;   element (of wrong type)
;   value (for result)
	dup
	instanceof java/lang/Number
	ifne number_convert
	dup
	instanceof java/lang/Character
	ifne char_convert

boolean_convert:
	checkcast java/lang/Boolean
	invokevirtual java/lang/Boolean/booleanValue()Z
	goto pack_integer
char_convert:
	checkcast java/lang/Character
	invokevirtual java/lang/Character/charValue()C
	goto pack_integer

number_convert:
	checkcast java/lang/Number
	invokevirtual java/lang/Number/intValue()I
pack_integer:
	new java/lang/Integer
	dup_x1
	swap
	invokenonvirtual java/lang/Integer/<init>(I)V
set_object_big_loop:
	invokevirtual jode/jvm/Value/setObject(Ljava/lang/Object;)V
	goto big_loop

; ====== STORE INSTRUCTIONS

store_instr:
	dup
	bipush 79
	if_icmpge array_store_instr

	pop			; opcode

	aload_2			; locals
	aload 6
	getfield jode/bytecode/Instruction/localSlot I
	aaload
	iinc 5 -1
	aload_3			; stack
	iload 5			; stacktop
	aaload	
	invokevirtual jode/jvm/Value/setValue(Ljode/jvm/Value;)V
	goto big_loop

array_store_instr:

	istore 7
	iinc 5 -2
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/intValue()I
; stack:
;  index
	aload_3
	iload 5
	iconst_1
	iadd
	aaload
; stack:
;  store-value
;  index
;	swap
;	dup_x1
;	invokestatic java/lang/String/valueOf(I)Ljava/lang/String;
;	getstatic java/lang/System/err Ljava/io/PrintStream;
;	swap
;	invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V

	; work around for mysterious bug in virtual machine XXX
	astore 8
	istore 6

	iinc 5 -1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	dup
; stack:
;  array
;  store-value
;  index
;  array

	instanceof [Z
	ifne bool_array_store
	iload 6
	aload 8
	iload 7
	tableswitch 84
		byte_array_store
		char_array_store
		short_array_store
	default: normal_array_store

; stack:
;  store-value
;  index
;  array

normal_array_store:
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
aastore_start:
	invokestatic java/lang/reflect/Array/set(Ljava/lang/Object;ILjava/lang/Object;)V
aastore_end:
	goto big_loop

bool_array_store:
	iload 6
	aload 8
	invokevirtual jode/jvm/Value/intValue()I
zastore_start:
	invokestatic java/lang/reflect/Array/setBoolean(Ljava/lang/Object;IZ)V
zastore_end:
	goto big_loop
			
byte_array_store:
	invokevirtual jode/jvm/Value/intValue()I
bastore_start:
	invokestatic java/lang/reflect/Array/setByte(Ljava/lang/Object;IB)V
bastore_end:
	goto big_loop
			
char_array_store:
	invokevirtual jode/jvm/Value/intValue()I
castore_start:
	invokestatic java/lang/reflect/Array/setChar(Ljava/lang/Object;IC)V
castore_end:
	goto big_loop

short_array_store:
	invokevirtual jode/jvm/Value/intValue()I
sastore_start:
	invokestatic java/lang/reflect/Array/setShort(Ljava/lang/Object;IS)V
sastore_end:
	goto big_loop
			

; =================

no_const_store_load_instr:
	dup
	bipush 96		; opc_iadd
	if_icmpge arith_instr

	dup
	bipush 92
	if_icmpge dup2_swap_instr
	dup
	bipush 89
	if_icmpge dup_instr

; ==== POP INSTRUCTION
	bipush 86
	isub			; pop count
	ineg
	iload 5			; stacktop
	iadd
	istore 5		; stacktop
	goto big_loop

; ==== DUP INSTRUCTION
dup_instr:
	bipush 88
	isub			; dup depth+1
	istore 7
	aload_3
	iload 5
	aaload			; stack[stacktop]
	iload 5

next_loop_dup1:
; Stack:
;  stacktop-i
;  stack[stacktop]
	iinc 7 -1
	dup
	aload_3
	swap
	aaload
; Stack:
;  stack[stacktop-i]
;  stacktop-i
;  stack[stacktop]

	swap
	iconst_1
	isub
	dup_x1

; Stack:
;  stacktop-(i+1)
;  stack[stacktop-i]
;  stacktop-(i+1)
;  stack[stacktop]
	aload_3
	swap
	aaload

; Stack:
;  stack[stacktop-(i+1)]
;  stack[stacktop-i]
;  stacktop-(i+1)
;  stack[stacktop]
	invokevirtual jode/jvm/Value/setValue(Ljode/jvm/Value;)V
	iload 7
	ifne  next_loop_dup1

; Stack:
;  stacktop-(depth+1)
;  stack[stacktop]
	aload_3
	swap
	aaload
	swap
	invokevirtual jode/jvm/Value/setValue(Ljode/jvm/Value;)V
	iinc 5 1
	goto big_loop

dup2_swap_instr:
	dup
	bipush 95		; opc_swap
	if_icmpne dup2_instr

; ==== SWAP INSTRUCTION
	pop
	aload_3
	iload 5
	iconst_1
	isub
	dup2
	aaload
	astore 7

; Stack:
; stacktop-1
; stack
	dup2
	iconst_1
	isub
	dup2_x2
; stacktop-2
; stack
; stacktop-1
; stack
; stacktop-2
; stack
	aaload
	aastore
	aload 7
	aastore
	goto big_loop
	


; ==== DUP2 INSTRUCTION
dup2_instr:
	bipush 90
	isub			; dup depth+2
	istore 7
	aload_3
	iload 5
	dup2
	aaload			; stack[stacktop]
	astore 8
	iconst_1
	iadd
	aaload			; stack[stacktop+1]
	iload 5

next_loop_dup2:
; Stack:
;  stacktop-i
;  stack[stacktop+1]
	iinc 7 -1
	dup
	aload_3
	swap
	iconst_1
	iadd
	aaload
; Stack:
;  stack[stacktop-i+1]
;  stacktop-i
;  stack[stacktop+1]

	swap
	iconst_1
	isub
	dup_x1

; Stack:
;  stacktop-i-1
;  stack[stacktop-i+1]
;  stacktop-i-1
;  stack[stacktop+1]
	aload_3
	swap
	aaload

; Stack:
;  stack[stacktop-i-1]
;  stack[stacktop-i]
;  stacktop-i-1
;  stack[stacktop+1]
	invokevirtual jode/jvm/Value/setValue(Ljode/jvm/Value;)V
	iload 7
	ifne  next_loop_dup2

; Stack:
;  stacktop-depth-2
;  stack[stacktop+1]
	aload_3
	swap
	dup2
	aaload
 	aload 8

; Stack:
;  stack[stacktop]
;  stack[stacktop-depth-2]
;  stacktop-depth-2
;  stack
;  stack[stacktop+1]
	
	invokevirtual jode/jvm/Value/setValue(Ljode/jvm/Value;)V
	iconst_1
	iadd
	aaload
	swap
	invokevirtual jode/jvm/Value/setValue(Ljode/jvm/Value;)V
	iinc 5 2
	goto big_loop

; ========== ARITHMETICS

arith_instr:
	dup
	sipush 132	; opc_iinc
	if_icmpge convert_or_cmp_or_iinc_instr
	dup
	bipush 126	; opc_iand
	if_icmpge binary_op_instr
	dup
	bipush 116	; opc_ineg
	if_icmpge neg_or_shift_instr

	dup
	iconst_1
	iand
	ifeq binary_op_instr
	iinc 5 -1		; stacktop--;

binary_op_instr:
	iinc 5 -1		; stacktop--;
	dup
	iconst_1
	iand
	iconst_1
	iadd			; long ? 2 : 1
	aload_3
	iload 5
	dup2_x1
	aaload
	astore 7		; local_7 = stack[stacktop]
	isub
	goto arith_do_instr

iinc_instr:
	pop2
	aload_2
	aload 6
	getfield jode/bytecode/Instruction/localSlot I
	aaload
	dup
	invokevirtual jode/jvm/Value/intValue()I
	aload 6
	getfield jode/bytecode/Instruction/intData I
	iadd
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop

convert_or_cmp_or_iinc_instr:
	dup
	sipush 133
	isub
	dup
	iflt iinc_instr
	iconst_1
	swap
	ishl
	dup
	sipush 0x7FFF
	iand
	ifeq two_op_instr
	aconst_null
	goto second_op_done
two_op_instr:
	iinc 5 -1		; stacktop--;
	ldc  0x30000
	iand
	iconst_2
	swap
	ifeq second_op_single
	iinc 5 -1
	iconst_2
	ishl
second_op_single:
	aload_3
	iload 5
	aaload
second_op_done:
; Stack:
;  second_op
;  opcode mask
;  opcode
	astore 7
	dup
	bipush 0x0E38
	iand
	iinc 5 -1
	ifeq first_op_single
	iinc 5 -1
first_op_single:
;  opcode mask
;  opcode
	sipush 0x05A5
	iand

	aload_3
	iload 5
	aaload
	swap
	iinc 5 1
	ifeq result_single
	iinc 5 1
result_single:	
	goto arith_do_instr_no_aaload

neg_or_shift_instr:
	dup
	bipush 120		; opc_ishl
	if_icmpge binary_op_instr

unary_instr:
	aconst_null
	astore 7
	dup
	iconst_1
	iand
	iconst_1
	iadd			; long instr ? 2 : 1
	ineg
	iload 5
	iadd
	aload_3
	swap
	
; Local:
;  7 == stack[op2] / null
; Stack:
;  op/result1 stackindex
;  stack
;  opcode
arith_do_instr:
	aaload
arith_do_instr_no_aaload:
	dup_x1
	swap
	tableswitch 96
		iadd_instr
		ladd_instr
		fadd_instr
		dadd_instr
		isub_instr
		lsub_instr
		fsub_instr
		dsub_instr
		imul_instr
		lmul_instr
		fmul_instr
		dmul_instr
		idiv_instr
		ldiv_instr
		fdiv_instr
		ddiv_instr
		irem_instr
		lrem_instr
		frem_instr
		drem_instr
		ineg_instr
		fneg_instr
		lneg_instr
		dneg_instr
		ishl_instr
		lshl_instr
		ishr_instr
		lshr_instr
		iushr_instr
		lushr_instr
		iand_instr
		land_instr
		ior_instr
		lor_instr
		ixor_instr
		lxor_instr
		lxor_instr		; opc_iinc	
		i2l_instr
		i2f_instr
		i2d_instr
		l2i_instr
		l2f_instr
		l2d_instr
		f2i_instr
		f2l_instr
		f2d_instr
		d2i_instr
		d2l_instr
		d2f_instr
		i2b_instr
		i2c_instr
		i2s_instr
		lcmp_instr
		fcmpl_instr
		fcmpg_instr
		dcmpl_instr
		dcmpg_instr
	default: iadd_instr

iadd_instr:
	invokevirtual jode/jvm/Value/intValue()I
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	iadd
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
isub_instr:
	invokevirtual jode/jvm/Value/intValue()I
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	isub
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
imul_instr:
	invokevirtual jode/jvm/Value/intValue()I
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	imul
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
idiv_instr:
	invokevirtual jode/jvm/Value/intValue()I
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
idiv_start:
	idiv
idiv_end:
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
irem_instr:
	invokevirtual jode/jvm/Value/intValue()I
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
irem_start:
	irem
irem_end:
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
ineg_instr:
	invokevirtual jode/jvm/Value/intValue()I
	ineg
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
ishl_instr:
	invokevirtual jode/jvm/Value/intValue()I
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	ishl
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
ishr_instr:
	invokevirtual jode/jvm/Value/intValue()I
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	ishr
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
iushr_instr:
	invokevirtual jode/jvm/Value/intValue()I
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	iushr
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
iand_instr:
	invokevirtual jode/jvm/Value/intValue()I
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	iand
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
ior_instr:
	invokevirtual jode/jvm/Value/intValue()I
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	ior
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
ixor_instr:
	invokevirtual jode/jvm/Value/intValue()I
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	ixor
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop

ladd_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/longValue()J
	ladd
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
lsub_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/longValue()J
	lsub
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
lmul_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/longValue()J
	lmul
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
ldiv_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/longValue()J
ldiv_start:
	ldiv
ldiv_end:
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
lrem_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/longValue()J
lrem_start:
	lrem
lrem_end:
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
lneg_instr:
	invokevirtual jode/jvm/Value/longValue()J
	lneg
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
lshl_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	lshl
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
lshr_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	lshr
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
lushr_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/intValue()I
	lushr
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
land_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/longValue()J
	land
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
lor_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/longValue()J
	lor
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
lxor_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/longValue()J
	lxor
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop

fadd_instr:
	invokevirtual jode/jvm/Value/floatValue()F
	aload 7
	invokevirtual jode/jvm/Value/floatValue()F
	fadd
	invokevirtual jode/jvm/Value/setFloat(F)V
	goto big_loop
fsub_instr:
	invokevirtual jode/jvm/Value/floatValue()F
	aload 7
	invokevirtual jode/jvm/Value/floatValue()F
	fsub
	invokevirtual jode/jvm/Value/setFloat(F)V
	goto big_loop
fmul_instr:
	invokevirtual jode/jvm/Value/floatValue()F
	aload 7
	invokevirtual jode/jvm/Value/floatValue()F
	fmul
	invokevirtual jode/jvm/Value/setFloat(F)V
	goto big_loop
fdiv_instr:
	invokevirtual jode/jvm/Value/floatValue()F
	aload 7
	invokevirtual jode/jvm/Value/floatValue()F
	fdiv
	invokevirtual jode/jvm/Value/setFloat(F)V
	goto big_loop
frem_instr:
	invokevirtual jode/jvm/Value/floatValue()F
	aload 7
	invokevirtual jode/jvm/Value/floatValue()F
	frem
	invokevirtual jode/jvm/Value/setFloat(F)V
	goto big_loop
fneg_instr:
	invokevirtual jode/jvm/Value/floatValue()F
	fneg
	invokevirtual jode/jvm/Value/setFloat(F)V
	goto big_loop

dadd_instr:
	invokevirtual jode/jvm/Value/doubleValue()D
	aload 7
	invokevirtual jode/jvm/Value/doubleValue()D
	dadd
	invokevirtual jode/jvm/Value/setDouble(D)V
	goto big_loop
dsub_instr:
	invokevirtual jode/jvm/Value/doubleValue()D
	aload 7
	invokevirtual jode/jvm/Value/doubleValue()D
	dsub
	invokevirtual jode/jvm/Value/setDouble(D)V
	goto big_loop
dmul_instr:
	invokevirtual jode/jvm/Value/doubleValue()D
	aload 7
	invokevirtual jode/jvm/Value/doubleValue()D
	dmul
	invokevirtual jode/jvm/Value/setDouble(D)V
	goto big_loop
ddiv_instr:
	invokevirtual jode/jvm/Value/doubleValue()D
	aload 7
	invokevirtual jode/jvm/Value/doubleValue()D
	ddiv
	invokevirtual jode/jvm/Value/setDouble(D)V
	goto big_loop
drem_instr:
	invokevirtual jode/jvm/Value/doubleValue()D
	aload 7
	invokevirtual jode/jvm/Value/doubleValue()D
	drem
	invokevirtual jode/jvm/Value/setDouble(D)V
	goto big_loop
dneg_instr:
	invokevirtual jode/jvm/Value/doubleValue()D
	dneg
	invokevirtual jode/jvm/Value/setDouble(D)V
	goto big_loop


i2f_instr:
	invokevirtual jode/jvm/Value/intValue()I
	i2f
	invokevirtual jode/jvm/Value/setFloat(F)V
	goto big_loop
i2l_instr:
	invokevirtual jode/jvm/Value/intValue()I
	i2l
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
i2d_instr:
	invokevirtual jode/jvm/Value/intValue()I
	i2d
	invokevirtual jode/jvm/Value/setDouble(D)V
	goto big_loop
l2i_instr:
	invokevirtual jode/jvm/Value/longValue()J
	l2i
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
l2f_instr:
	invokevirtual jode/jvm/Value/longValue()J
	l2f
	invokevirtual jode/jvm/Value/setFloat(F)V
	goto big_loop
l2d_instr:
	invokevirtual jode/jvm/Value/longValue()J
	l2d
	invokevirtual jode/jvm/Value/setDouble(D)V
	goto big_loop
f2i_instr:
	invokevirtual jode/jvm/Value/floatValue()F
	f2i
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
f2l_instr:
	invokevirtual jode/jvm/Value/floatValue()F
	f2l
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
f2d_instr:
	invokevirtual jode/jvm/Value/floatValue()F
	f2d
	invokevirtual jode/jvm/Value/setDouble(D)V
	goto big_loop
d2i_instr:
	invokevirtual jode/jvm/Value/doubleValue()D
	d2i
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
d2f_instr:
	invokevirtual jode/jvm/Value/doubleValue()D
	d2f
	invokevirtual jode/jvm/Value/setFloat(F)V
	goto big_loop
d2l_instr:
	invokevirtual jode/jvm/Value/doubleValue()D
	d2l
	invokevirtual jode/jvm/Value/setLong(J)V
	goto big_loop
i2b_instr:
	invokevirtual jode/jvm/Value/intValue()I
	i2b
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
i2c_instr:
	invokevirtual jode/jvm/Value/intValue()I
	i2c
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
i2s_instr:
	invokevirtual jode/jvm/Value/intValue()I
	i2s
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop

lcmp_instr:
	invokevirtual jode/jvm/Value/longValue()J
	aload 7
	invokevirtual jode/jvm/Value/longValue()J
	lcmp
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
fcmpl_instr:
	invokevirtual jode/jvm/Value/floatValue()F
	aload 7
	invokevirtual jode/jvm/Value/floatValue()F
	fcmpl
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
fcmpg_instr:
	invokevirtual jode/jvm/Value/floatValue()F
	aload 7
	invokevirtual jode/jvm/Value/floatValue()F
	fcmpg
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
dcmpl_instr:
	invokevirtual jode/jvm/Value/doubleValue()D
	aload 7
	invokevirtual jode/jvm/Value/doubleValue()D
	dcmpl
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop
dcmpg_instr:
	invokevirtual jode/jvm/Value/doubleValue()D
	aload 7
	invokevirtual jode/jvm/Value/doubleValue()D
	dcmpg
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop

; ============================================
;   IF AND SPECIAL INSTR
; ============================================

if_or_special_instr:
	dup
	tableswitch 153 
		ifunary_instr
		ifunary_instr
		ifunary_instr
		ifunary_instr
		ifunary_instr
		ifunary_instr
		ifbinary_instr
		ifbinary_instr
		ifbinary_instr
		ifbinary_instr
		ifbinary_instr
		ifbinary_instr
		ifabinary_instr
		ifabinary_instr
		goto_instr
		jsr_instr
		ret_instr
		tableswitch_instr
		lookupswitch_instr
		areturn_instr
		lreturn_instr
		areturn_instr
		lreturn_instr
		areturn_instr
		return_instr
		getstatic_instr
		putstatic_instr
		getfield_instr
		putfield_instr
		invoke_instr
		invoke_instr
		invokestatic_instr
		invoke_instr
		illegal_instr
		new_instr
		illegal_instr
		illegal_instr
		arraylength_instr
		athrow_instr
		checkcast_instr
		instanceof_instr
		monitorenter_instr
		monitorexit_instr
		illegal_instr
		multianewarray_instr
		ifaunary_instr
		ifaunary_instr
	default: illegal_instr

ifabinary_instr:
	iconst_1
	iadd
	iconst_1
	iand	
	iinc 5 -1
	aload_3
	iload 5	
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	goto ifacmp
ifaunary_instr:
	iconst_1
	iand	
	aconst_null
ifacmp:
	iinc 5 -1
	aload_3
	iload 5	
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	if_acmpeq ifacmp_equal
	ifeq big_loop
	goto jump_succ
ifacmp_equal:
	ifne big_loop
	goto jump_succ

ifbinary_instr:
	sipush 133		; opc_if_icmple - 31
	isub
	iinc 5 -1
	aload_3
	iload 5	
	aaload
	invokevirtual jode/jvm/Value/intValue()I
	goto ificmp
ifunary_instr:
	bipush 127		; opc_ifle - 31
	isub
	iconst_0
ificmp:
	iinc 5 -1
	aload_3
	iload 5	
	aaload
	invokevirtual jode/jvm/Value/intValue()I
	dup2
	if_icmplt ificmp_greater
	if_icmpeq ificmp_equal
ificmp_less:
	bipush 0x09
	goto ificmp_final
ificmp_equal:
	bipush 0x25
	goto ificmp_final
ificmp_greater:
	pop2
	bipush 0x06
ificmp_final:
	swap
	ishl
	ifge big_loop
jump_succ:
	aload 6
	getfield jode/bytecode/Instruction/succs [Ljode/bytecode/Instruction;
	iconst_0
	aaload
	astore 4
	goto big_loop

jsr_instr:
	aload_3
	iload 5
	aaload
	aload 6
	invokevirtual jode/jvm/Value/setObject(Ljava/lang/Object;)V
	iinc 5 1

goto_instr:
	pop
	goto jump_succ

ret_instr:
	pop
	iinc 5 -1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	checkcast jode/bytecode/Instruction
	astore 4
	goto big_loop

tableswitch_instr:
	pop
	iinc 5 -1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/intValue()I
	aload 6
	getfield jode/bytecode/Instruction/intData I
	isub
	dup
	aload 6
	getfield jode/bytecode/Instruction/succs [Ljode/bytecode/Instruction;
	dup_x2
; Stack:
;  succs
;  value - low
;  value - low
;  succs
	arraylength
	if_icmpge default_dest
	dup
	ifge load_dest
default_dest:
; Stack:
;  value - low
;  succs
	pop
	dup
	arraylength
	iconst_1
	isub
load_dest:
	aaload
	astore 4
	goto big_loop
	
lookupswitch_instr:
	pop
	iinc 5 -1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/intValue()I
	aload 6
	getfield jode/bytecode/Instruction/objData Ljava/lang/Object;
	checkcast [I
	iconst_0
; Stack:
;   i
;   objData
;   value
lookup_loop:
	dup2_x1
	iaload
	istore 7
	dup_x2
	iload 7
	if_icmpeq lookup_found
	iconst_1
	iadd
	dup2
	swap
	arraylength
	if_icmplt lookup_loop
lookup_found:
; Stack:
;   i
;   objData
;   value
	istore 7
	pop2
	aload 6
	getfield jode/bytecode/Instruction/succs [Ljode/bytecode/Instruction;
	iload 7
	aaload
	astore 4
	goto big_loop

lreturn_instr:
	iinc 5 -1
areturn_instr:
	pop
	iinc 5 -1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	areturn
return_instr:
	pop
	aconst_null
	areturn

putfield_object:
	pop
	goto putfield_normal

putstatic_instr:
putfield_instr:
	istore 7
	aload_0
	aload 6
	getfield jode/bytecode/Instruction/objData Ljava/lang/Object;
	checkcast jode/bytecode/Reference
	dup
	invokevirtual jode/bytecode/Reference/getType()Ljava/lang/String;
	dup
	invokevirtual java/lang/String/length()I
	iconst_1
	if_icmpne putfield_object
	iconst_0
	invokevirtual java/lang/String/charAt(I)C
	dup
	bipush 74		; 'J'
	if_icmpeq putfield_long
	bipush 68		; 'D'
	if_icmpne putfield_normal
	iconst_0
putfield_long:
	iinc 5 -1
	pop
; Stack:
;   Reference
;   env
putfield_normal:
	iinc 5 -1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
; Stack:
;   value
;   Reference
;   env
	aconst_null
	iload 7
	sipush 179		; opc_putstatic
	if_icmpeq putfield_static
	pop
	iinc 5 -1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	dup
	ifnull popLLLL_create_nullexc
putfield_static:
	swap
	invokeinterface jode/jvm/RuntimeEnvironment/putField(Ljode/bytecode/Reference;Ljava/lang/Object;Ljava/lang/Object;)V 4
	goto big_loop

getstatic_instr:
	pop
	aload_3
	iload 5
	iinc 5 1
	aaload
	aconst_null
	astore 7
	goto getfield_do

getfield_instr:
	pop
	aload_3
	iload 5
	iconst_m1
	iadd
	aaload
	dup
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	dup
	ifnull popLL_create_nullexc
	astore 7
getfield_do:
	aload_0
	aload 6
	getfield jode/bytecode/Instruction/objData Ljava/lang/Object;
	checkcast jode/bytecode/Reference
	dup_x2
	aload 7
getfield_start:
	invokeinterface jode/jvm/RuntimeEnvironment/getField(Ljode/bytecode/Reference;Ljava/lang/Object;)Ljava/lang/Object; 3
getfield_end:
	invokevirtual jode/jvm/Value/setObject(Ljava/lang/Object;)V
	invokevirtual jode/bytecode/Reference/getType()Ljava/lang/String;
	dup
	invokevirtual java/lang/String/length()I
	iconst_1
	if_icmpne popL_big_loop
	iconst_0
	invokevirtual java/lang/String/charAt(I)C
	dup
	bipush 74		; 'J'
	if_icmpeq getfield_long
	bipush 68		; 'D'
	if_icmpne big_loop
	iconst_0
getfield_long:
	pop
	iinc 5 1
	goto big_loop


invokestatic_instr:
invoke_instr:
	istore 7
	aload_0
	aload 6
	getfield jode/bytecode/Instruction/objData Ljava/lang/Object;
	checkcast jode/bytecode/Reference
	dup
	invokevirtual jode/bytecode/Reference/getType()Ljava/lang/String;
	invokestatic jode/type/Type/tType(Ljava/lang/String;)Ljode/type/Type;
	checkcast jode/type/MethodType
; Stack:
;   methodType
;   ref
;   env
	dup_x2
; Stack:
;   methodType
;   ref
;   env
;   methodType
	invokevirtual jode/type/MethodType/getParameterTypes()[Ljode/type/Type;
	dup
	arraylength
	dup
	anewarray java/lang/Object
	dup_x2
	pop
	goto invoke_test
; Stack:
;   i
;   paramTypes
;   args
;   ref
;   env
;   methodType
invoke_loop:
;	dup
;	invokestatic java/lang/String/valueOf(I)Ljava/lang/String;
;	getstatic java/lang/System/err Ljava/io/PrintStream;
;	swap
;	invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V

	dup2_x1
	aaload
;   paramTypes[i]
;   args
;   i
;   paramTypes
;   ref
;   env
;   methodType
	invokevirtual jode/type/Type/stackSize()I
	ineg
	iload 5
	iadd
	istore 5
	dup_x2
	swap	
	dup_x1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	aastore
; Stack:
;   i
;   paramTypes
;   args
;   ref
;   env
;   methodType
invoke_test:
	iconst_1	
	isub
	dup
	ifge invoke_loop
	pop2
	astore 6
	dup
	invokevirtual jode/bytecode/Reference/getName()Ljava/lang/String;
; Stack:
;   name
;   ref
;   env
;   methodType
; Locals: 
;   6 = args
;   7 = opcode

	ldc "<init>"
	invokevirtual java/lang/String/equals(Ljava/lang/Object;)Z
	ifeq invoke_normalmethod

	iinc 5 -1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/getNewObject()Ljode/jvm/NewObject;
	dup_x2
	pop
	aload 6
newinstance_start:
	invokeinterface jode/jvm/RuntimeEnvironment/invokeConstructor(Ljode/bytecode/Reference;[Ljava/lang/Object;)Ljava/lang/Object; 3
newinstance_end:
	invokevirtual jode/jvm/NewObject/setObject(Ljava/lang/Object;)V
popL_big_loop:
	pop			; MethodType
	goto big_loop

invoke_normalmethod:
	iload 7
	sipush 184		; opc_invokestatic
	if_icmpne invoke_nonstatic
	iconst_0
	aconst_null
	goto invoke_do
invoke_nonstatic:
	iconst_1
	iload 7
	sipush 183		; opc_invokespecial
	if_icmpne invoke_nonspecial
	iconst_1
	ixor
invoke_nonspecial:
	iinc 5 -1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	dup
	ifnull popLILLL_create_nullexc
invoke_do:
	aload 6
invoke_start:
	invokeinterface jode/jvm/RuntimeEnvironment/invokeMethod(Ljode/bytecode/Reference;ZLjava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object; 5
invoke_end:
	astore 6

; Stack:
;   methodType
	invokevirtual jode/type/MethodType/getReturnType()Ljode/type/Type;
	invokevirtual jode/type/Type/stackSize()I
	dup
	ifeq popI_big_loop
	aload_3
	iload 5
	dup_x1
	aaload
	aload 6
	invokevirtual jode/jvm/Value/setObject(Ljava/lang/Object;)V
	iadd
	istore 5
	goto big_loop

new_instr:
	pop
	aload_3
	iload 5
	aaload
	new jode/jvm/NewObject
	dup
	aload 6
	getfield jode/bytecode/Instruction/objData Ljava/lang/Object;
	checkcast java/lang/String
	invokenonvirtual jode/jvm/NewObject/<init>(Ljava/lang/String;)V
	invokevirtual jode/jvm/Value/setNewObject(Ljode/jvm/NewObject;)V
	iinc 5 1
	goto big_loop

arraylength_instr:
	pop
	aload_3
	iload 5
	iconst_m1
	iadd
	aaload
	dup
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
arrlength_start:
	invokestatic java/lang/reflect/Array/getLength(Ljava/lang/Object;)I
arrlength_end:
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop

athrow_instr:
	pop
	iinc 5 -1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	dup
	ifnull popL_create_nullexc
	checkcast java/lang/Throwable
	goto throw_exception
popLILLL_create_nullexc:
	pop2
	pop
	goto popLL_create_nullexc
popLLLL_create_nullexc:
	pop2
popLL_create_nullexc:
	pop
popL_create_nullexc:
	pop
create_nullexc:
	new java/lang/NullPointerException
	dup
	invokenonvirtual java/lang/NullPointerException/<init>()V
	goto throw_exception

checkcast_instr:
	pop
	aload_0
	aload_3
	iload 5
	iconst_m1
	iadd
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	dup
	ifnull popLL_big_loop
	aload 6
	getfield jode/bytecode/Instruction/objData Ljava/lang/Object;
	checkcast java/lang/String
	invokeinterface jode/jvm/RuntimeEnvironment/instanceOf(Ljava/lang/Object;Ljava/lang/String;)Z 3
	ifne big_loop
	new java/lang/ClassCastException
	dup
	invokenonvirtual java/lang/ClassCastException/<init>()V
	goto throw_exception
popLL_big_loop:
	pop2
	goto big_loop
		
instanceof_instr:
	pop
	aload_0
	aload_3
	iload 5
	iconst_m1
	iadd
	aaload
	dup_x1
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	aload 6
	getfield jode/bytecode/Instruction/objData Ljava/lang/Object;
	checkcast java/lang/String
	invokeinterface jode/jvm/RuntimeEnvironment/instanceOf(Ljava/lang/Object;Ljava/lang/String;)Z 3
	invokevirtual jode/jvm/Value/setInt(I)V
	goto big_loop

monitorenter_instr:
	pop
	aload_0
	aload_3
	iinc 5 -1
	iload 5
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	invokeinterface jode/jvm/RuntimeEnvironment/enterMonitor(Ljava/lang/Object;)V 2
	goto big_loop
monitorexit_instr:
	pop
	aload_0
	aload_3
	iinc 5 -1
	iload 5
	aaload
	invokevirtual jode/jvm/Value/objectValue()Ljava/lang/Object;
	invokeinterface jode/jvm/RuntimeEnvironment/exitMonitor(Ljava/lang/Object;)V 2
	goto big_loop

multianewarray_instr:
	pop
	aload 6
	getfield jode/bytecode/Instruction/intData I
	dup
	istore 7
	newarray int
	goto newarray_test
newarray_loop:
	dup
	iload 7
	iinc 5 -1
	aload_3
	iload 5
	aaload
	invokevirtual jode/jvm/Value/intValue()I
	iastore
newarray_test:
	iinc 7 -1
	iload 7
	ifne newarray_loop
	astore 7
	aload_3
	iload 5
	aaload
	iinc 5 1
	aload_0
	aload 6
	getfield jode/bytecode/Instruction/objData Ljava/lang/Object;
	checkcast java/lang/String
	aload 7
newarray_start:
	invokeinterface jode/jvm/RuntimeEnvironment/newArray(Ljava/lang/String;[I)Ljava/lang/Object; 3
newarray_end:
	invokevirtual jode/jvm/Value/setObject(Ljava/lang/Object;)V
	goto big_loop

wrapexception_handler:
	goto throw_exception
throw_exception:
	aconst_null	
	astore 6
	goto handle_exception

invocationtarget_handler:
	dup
	astore 6
	invokevirtual java/lang/reflect/InvocationTargetException/getTargetException()Ljava/lang/Throwable;
handle_exception:
	astore 8
	aload_1
	invokevirtual jode/bytecode/BytecodeInfo/getExceptionHandlers()[Ljode/bytecode/Handler;
	dup
	arraylength
	dup
	ifeq nohandlers
	istore 7
	aload 4
	getfield jode/bytecode/Instruction/addr I
	istore 4
	iconst_0
; Stack:
;  index
;  handlers
handler_loop:
	dup2
	aaload
	dup
	getfield jode/bytecode/Handler/start Ljode/bytecode/Instruction;
	getfield jode/bytecode/Instruction/addr I
	iload 4
	if_icmplt wrong_handler_pop
	dup
	getfield jode/bytecode/Handler/end Ljode/bytecode/Instruction;
	getfield jode/bytecode/Instruction/addr I
	iload 4
	if_icmplt wrong_handler_pop
	dup
	getfield jode/bytecode/Handler/type Ljava/lang/String;
	aload_0
	swap
	aload 8
	swap
	invokeinterface jode/jvm/RuntimeEnvironment/instanceOf(Ljava/lang/Object;Ljava/lang/String;)Z 3
	ifne wrong_handler_pop

	getfield jode/bytecode/Handler/catcher Ljode/bytecode/Instruction;
	astore 4
	pop2
	iconst_1
	istore 5
	aload_3
	iconst_0
	aaload
	aload 8
	invokevirtual jode/jvm/Value/setObject(Ljava/lang/Object;)V
	goto big_loop
	
wrong_handler_pop:
	pop
	iconst_1
	iadd
	dup
	iload 7
	if_icmplt handler_loop
nohandlers:
	pop2
	aload 6
	ifnull wrap_exc
	aload 6
	athrow
wrap_exc:
	new java/lang/reflect/InvocationTargetException
	dup
	aload 8
	invokenonvirtual java/lang/reflect/InvocationTargetException/<init>(Ljava/lang/Throwable;)V
	athrow
.end method
