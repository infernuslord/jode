package jode;
import sun.tools.java.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.io.*;

public class CodeAnalyzer implements Analyzer, Constants {
    
    BinaryCode bincode;

    MethodInstructionHeader methodHeader;
    int[] references;
    Hashtable tryAddrs   = new Hashtable();
    Hashtable catchAddrs = new Hashtable();
    Hashtable labels     = new Hashtable();
    MethodAnalyzer method;
    public JodeEnvironment env;
    
    /**
     * Get the method.
     * @return The method to which this code belongs.
     */
    public MethodAnalyzer getMethod() {return method;}
    
    void readCode(byte[] code) 
         throws ClassFormatError
    {
        InstructionHeader[] instr = new InstructionHeader[code.length];
	int returnCount;
        try {
            DataInputStream stream = 
                new DataInputStream(new ByteArrayInputStream(code));
	    for (int addr = 0; addr < code.length; ) {
		instr[addr] = Opcodes.readOpcode(addr, stream, this);
		addr = instr[addr].getNextAddr();
	    }
        } catch (IOException ex) {
            throw new ClassFormatError(ex.toString());
        }
	methodHeader = new MethodInstructionHeader(instr);
    }

    void setExceptionHandler(BinaryExceptionHandler handler) {
	/*
        tryAddrs.put(new Integer(handler.startPC), handler);
        references[handler.startPC]++;
        catchAddrs.put(new Integer(handler.handlerPC), handler);
        references[handler.handlerPC]++;
	*/
    }

    /*
    Instruction getInstruction(int addr) {
        return instr[addr];
    }

    void setInstruction(int addr, Instruction i) {
        instr[addr] = i;
    }

    void removeInstruction(int addr) {
        instr[addr] = null;
    }
    */

    static int WRONG   = -3;
    static int SPECIAL = -2;
    static int FIRST   = -1;

    int getPreviousAddr(int addr) {
	/*
        int i;
        for (i = addr-1; i >= 0 && instr[i] == null; i--) {}
        return i;
	*/
	return addr;
    }
    
    int getNextAddr(int addr) {
	/*
        return addr + instr[addr].getLength();
	*/
	return addr;
    }
    
    int getPredecessor(int addr) {
	/*
        if (references[addr] != 1)
            return WRONG;
        if (addr == 0)
            return FIRST;
        if (catchAddrs.get(new Integer(addr)) != null)
            return SPECIAL;

        int i = getPreviousAddr(addr);
        if (instr[i].getLength() != addr-i)
            throw new RuntimeException("length mismatch");
        int[] successors = instr[i].getSuccessors();
        for (int j=0; j< successors.length; j++) {
            if (successors[j] == addr)
                return i;
        }
	*/
        return WRONG;
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
	InstructionHeader ih;
	for (ih = methodHeader.getNextInstruction(); ih != null;
	     ih = ih.getNextInstruction()) {
	    
	    ih.dumpSource(writer);
	}
    }

    public CodeAnalyzer(MethodAnalyzer ma, BinaryCode bc, JodeEnvironment e)
         throws ClassFormatError
    {
        method = ma;
        env  = e;
	bincode = bc;
        readCode(bincode.getCode());
        BinaryExceptionHandler[] handlers = bincode.getExceptionHandlers();
        for (int i=0; i<handlers.length; i++) {
            setExceptionHandler(handlers[i]);
        }
    }

//     public InstructionHeader convertIInc(InstructionHeader ih) {
// 		    Expression[] expr = { 
// 			new Expression
// 			(new ConstOperator(ALL_INT_TYPE, 
// 					   Integer.toString(value)),
// 			 new Expression[0])
// 		    };
// 		    return new InstructionHeader
// 			(addr, 6, new Expression
// 			 (new LocalStoreOperator
// 			  (ALL_INT_TYPE, local,
// 			   Operator.OPASSIGN_OP+operation),
// 			  expr));
//     }

    public InstructionHeader removeNop(InstructionHeader ih) {
        Instruction pred;
        try {
            NopOperator op = (NopOperator) ih.getInstruction();
            ih = ih.getSimpleUniquePredecessor();
            pred = ih.getInstruction();
            if (pred == null)
                return null;
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
        ih.combine(2, pred);
        return ih;
    }

    public InstructionHeader createConstantArray(InstructionHeader ih) {
        Expression[] consts;
	int count;
        Type type;
        try {
            if (ih.getInstruction() instanceof DupOperator)
                /* this is not the end of the array assign */
                return null;
            ih = ih.getSimpleUniquePredecessor();
            ArrayStoreOperator store = 
                (ArrayStoreOperator) ih.getInstruction();
            ih = ih.getSimpleUniquePredecessor();
            Expression lastconst = (Expression) ih.getInstruction();
            ih = ih.getSimpleUniquePredecessor();
            Expression lastindexexpr = (Expression) ih.getInstruction();
            ConstOperator lastindexop = 
                (ConstOperator) lastindexexpr.getOperator();
            if (!MyType.isOfType(lastindexop.getType(), MyType.tInt))
                return null;
            int lastindex = Integer.parseInt(lastindexop.getValue());
            ih = ih.getSimpleUniquePredecessor();
            DupOperator dup = (DupOperator) ih.getInstruction();
            if (dup.getDepth() != 0 || 
                dup.getCount() != store.getLValueType().stackSize())
                return null;
            consts = new Expression[lastindex+1];
            consts[lastindex] = lastconst;
	    count = 1;
            while (lastindex-- > 0) {
                ih = ih.getSimpleUniquePredecessor();
                ArrayStoreOperator store2 = 
                    (ArrayStoreOperator) ih.getInstruction();
                ih = ih.getSimpleUniquePredecessor();
		lastconst = (Expression) ih.getInstruction();
                ih = ih.getSimpleUniquePredecessor();
                Expression indexexpr = (Expression) ih.getInstruction();
                ConstOperator indexop = 
                    (ConstOperator) indexexpr.getOperator();
                if (!MyType.isOfType(indexop.getType(), MyType.tUInt))
                    return null;
                int index = Integer.parseInt(indexop.getValue());
		if (index > lastindex)
                    return null;
		while (index < lastindex) {
		    consts[lastindex] = new Expression
			(new ConstOperator(MyType.tUnknown, ""), 
			 new Expression[0]);
		    lastindex--;
		}
                consts[lastindex] = lastconst;
                ih = ih.getSimpleUniquePredecessor();
                dup = (DupOperator) ih.getInstruction();
                if (dup.getDepth() != 0 || 
                    dup.getCount() != store.getLValueType().stackSize())
                    return null;
		count++;
            }
            ih = ih.getSimpleUniquePredecessor();
            Expression newArrayExpr = (Expression) ih.getInstruction();
            NewArrayOperator newArrayOp = 
                (NewArrayOperator) newArrayExpr.getOperator();
            type = newArrayOp.getType();
            if (newArrayOp.getOperandCount() != 1)
                return null;
            Expression countexpr = 
                (Expression) newArrayExpr.getSubExpressions()[0];
            ConstOperator countop = 
                (ConstOperator) countexpr.getOperator();
            if (!MyType.isOfType(countop.getType(), MyType.tUInt))
                return null;
            if (Integer.parseInt(countop.getValue()) != consts.length)
                return null;
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
        Operator op = new ConstantArrayOperator(type, consts.length);
        ih.combine(4*count+1, new Expression(op, consts));
        return ih;
    }

    public InstructionHeader createExpression(InstructionHeader ih) {
        Operator op;
        Expression exprs[];
        int params;
        try {
            op = (Operator) ih.getInstruction();
            params  = op.getOperandCount();
            exprs = new Expression[params];
            for (int i = params-1; i>=0; i--) {
                ih = ih.getSimpleUniquePredecessor();
                exprs[i] = (Expression) ih.getInstruction();
                if (exprs[i].isVoid()) {
		    if (i == params-1)
			return null;
		    Expression e = exprs[i+1].tryToCombine(exprs[i]);
		    if (e == null)
			return null;
		    i++;
		    exprs[i] = e;
                    ih.combine(2, e);
		}
            }
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
        ih.combine(params+1, new Expression(op, exprs));
        return ih;
    }

//     public int combineExpressions(int addr) {
//         int count = 0;
//         int start = addr;
//         if (!(instr[addr] instanceof Expression) ||
//             !((Expression)instr[addr]).isVoid())
//             return -1;
//         do {
//             addr = getNextAddr(addr);
//             count++;
//         } while (addr < instr.length && 
//                  references[addr] == 1 &&
//                  instr[addr] instanceof Expression &&
//                  ((Expression)instr[addr]).isVoid());
//         Expression[] expr = new Expression[count];
//         addr = start;
//         for (int i=0; i < count; i++) {
//             expr[i] = (Expression)instr[addr];
//             int next = getNextAddr(addr);
//             instr[addr] = null;
//             addr = next;
//         }
//         instr[start] = new Block(start, addr-start, expr);
//         return start;
//     }

    public InstructionHeader createAssignExpression(InstructionHeader ih) {
        StoreInstruction store;
        try {
            store = (StoreInstruction) ih.getInstruction();
            ih = ih.getSimpleUniquePredecessor();

            DupOperator dup = (DupOperator) ih.getInstruction();
            if (dup.getDepth() != store.getLValueOperandCount() && 
                dup.getCount() != store.getLValueType().stackSize())
                return null;
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
        ih.combine(2, new AssignOperator(Operator.ASSIGN_OP, store));
        return ih;
    }

    public InstructionHeader createLocalPostIncExpression(InstructionHeader ih)
    {
	IIncOperator iinc;
	int op;
	Type type;
        try {
	    Expression iincExpr = (Expression) ih.getInstruction();
	    iinc = (IIncOperator) iincExpr.getOperator();
	    if (iinc.getOperator() == iinc.ADD_OP + iinc.OPASSIGN_OP)
                op = Operator.INC_OP;
            else if (iinc.getOperator() == iinc.NEG_OP + iinc.OPASSIGN_OP)
                op = Operator.DEC_OP;
            else
                return null;
            if (!iinc.getValue().equals("1") &&
		!iinc.getValue().equals("-1"))
                return null;
            if (iinc.getValue().equals("-1"))
		op ^= 1;
            ih = ih.getSimpleUniquePredecessor();
	    Expression loadExpr = (Expression) ih.getInstruction();
	    LocalLoadOperator load = 
		(LocalLoadOperator)loadExpr.getOperator();
	    if (!iinc.matches(load))
		return null;

	    type = MyType.intersection(load.getType(), MyType.tUInt);
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
	Operator postop = new LocalPostFixOperator(type, op, iinc);
	ih.combine(2, postop);
	return ih;
    }

    public InstructionHeader createPostIncExpression(InstructionHeader ih) {
	StoreInstruction store;
	int op;
	Type type;
        try {
	    store = (StoreInstruction) ih.getInstruction();
	    if (store.getLValueOperandCount() == 0)
		return null;
            ih = ih.getSimpleUniquePredecessor();
            BinaryOperator binOp = (BinaryOperator) ih.getInstruction();
            if (binOp.getOperator() == store.ADD_OP)
                op = Operator.INC_OP;
            else if (store.getOperator() == store.NEG_OP)
                op = Operator.DEC_OP;
            else
                return null;
            ih = ih.getSimpleUniquePredecessor();
            Expression expr = (Expression) ih.getInstruction();
            ConstOperator constOp = (ConstOperator) expr.getOperator();
            if (!constOp.getValue().equals("1") &&
		!constOp.getValue().equals("-1"))
                return null;
            if (constOp.getValue().equals("-1"))
		op ^= 1;
            ih = ih.getSimpleUniquePredecessor();
            DupOperator dup = (DupOperator) ih.getInstruction();
            if (dup.getCount() != store.getLValueType().stackSize() ||
                dup.getDepth() != store.getLValueOperandCount())
                return null;
            ih = ih.getSimpleUniquePredecessor();
            Operator load = (Operator) ih.getInstruction();

	    if (!store.matches(load))
		return null;

	    ih = ih.getSimpleUniquePredecessor();
	    DupOperator dup2 = (DupOperator) ih.getInstruction();
	    if (dup2.getCount() != store.getLValueOperandCount() ||
		dup2.getDepth() != 0)
		return null;

	    type = MyType.intersection(load.getType(), store.getLValueType());
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
	Operator postop = new PostFixOperator(type, op, store);
	ih.combine(6, postop);
	return ih;
    }

    public InstructionHeader createAssignOp(InstructionHeader ih) {
        Expression rightHandSide;
        StoreInstruction store;
        BinaryOperator binop;
        try {
            store = (StoreInstruction) ih.getInstruction();
            ih = ih.getSimpleUniquePredecessor();
            binop = (BinaryOperator) ih.getInstruction();
            if (binop.getOperator() <  binop.ADD_OP ||
                binop.getOperator() >= binop.ASSIGN_OP)
                return null;
            ih = ih.getSimpleUniquePredecessor();
            rightHandSide = (Expression) ih.getInstruction();
            if (rightHandSide.isVoid())
                return null; /* XXX */
            ih = ih.getSimpleUniquePredecessor();
            Operator load = (Operator) ih.getInstruction();
            if (!store.matches(load))
                return null;
            ih = ih.getSimpleUniquePredecessor();
            DupOperator dup = (DupOperator) ih.getInstruction();
            if (dup.getDepth() != 0 && 
                dup.getCount() != store.getLValueOperandCount())
                return null;
        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }
        ih.combine(3, rightHandSide);
        InstructionHeader storeIH = ih.getNextInstruction();
        store.setOperator(store.OPASSIGN_OP+binop.getOperator());
        store.setLValueType(MyType.intersection(binop.getType(), 
                                              store.getLValueType()));
        storeIH.combine(2, store);
        return ih;
    }

    public InstructionHeader combineNewConstructor(InstructionHeader ih) {
        InvokeOperator constrCall;
        Expression exprs[];
        try {
            constrCall = (InvokeOperator) ih.getInstruction();
            if (!constrCall.isConstructor())
                return null;
            int params  = constrCall.getOperandCount();
            exprs = new Expression[params];
            for (int i = params-1; i>0; i--) {
                ih = ih.getSimpleUniquePredecessor();
                exprs[i] = (Expression) ih.getInstruction();
                if (exprs[i].isVoid())
                    return null; /* XXX */
            }
            ih = ih.getSimpleUniquePredecessor();
            DupOperator dup = (DupOperator) ih.getInstruction();
            if (dup.getCount() != 1 && dup.getDepth() != 0)
                return null;
            ih = ih.getSimpleUniquePredecessor();
            exprs[0] = (Expression) ih.getInstruction();
            if (exprs[0].isVoid())
                return null;
            NewOperator op = (NewOperator) exprs[0].getOperator();
            if (constrCall.getClassType() != op.getType())
                return null;
        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }
        ConstructorOperator conOp = 
            new ConstructorOperator(constrCall.getClassType(), 
                                    constrCall.getField());

        ih.combine(exprs.length+2, new Expression(conOp, exprs));
        return ih;
    }

    public InstructionHeader combineIfGotoExpressions(InstructionHeader ih2) {
        InstructionHeader ih1;
        Expression[] e;
        int operator;
        try {
            if (ih2.switchType != MyType.tBoolean)
                return null;
            InstructionHeader[] dests2 = ih2.getSuccessors();

            /* if ih2.getSimpleUniquePredecessor.getOperator().isVoid() XXX */

            Vector predec = ih2.getPredecessors();
            if (predec.size() != 1)
                return null;

            ih1 = (InstructionHeader) predec.elementAt(0);
            if (ih1.switchType != MyType.tBoolean)
                return null;
            InstructionHeader[] dests1 = ih1.getSuccessors();
            if (dests1[0] != ih2)
                return null;

            if (dests1[1] == dests2[0]) {
                e = new Expression[2];
                operator = Operator.LOG_AND_OP;
                e[1] = (Expression)ih2.getInstruction();
                e[0] = ((Expression)ih1.getInstruction()).negate();
            } else if (dests1[1] == dests2[1]) {
                e = new Expression[2];
                operator = Operator.LOG_OR_OP;
                e[1] = (Expression)ih2.getInstruction();
                e[0] = (Expression)ih1.getInstruction();
            } else
                return null;
        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }

        Expression cond = 
            new Expression(new BinaryOperator(Type.tBoolean, operator), e);

	ih1.combineConditional(cond);
        return ih1;
    }

    public InstructionHeader createFunnyIfThenElseOp(InstructionHeader ih) {
        Expression cond = null;
        try {
            InstructionHeader ifHeader= ih;
            if (ifHeader.switchType != MyType.tBoolean)
                return null;
            CompareUnaryOperator compare = 
                (CompareUnaryOperator) ifHeader.getInstruction();
            if ((compare.getOperator() & ~1) != compare.EQUALS_OP)
                return null;
            Enumeration enum = ih.getPredecessors().elements();
            while (enum.hasMoreElements()) {
                try {
                    ih = (InstructionHeader) enum.nextElement();
                    Expression zeroExpr = (Expression) ih.getInstruction();
                    ConstOperator zero = 
                        (ConstOperator) zeroExpr.getOperator();
                    if (!zero.getValue().equals("0"))
                        continue;

                    ih = ih.getUniquePredecessor();
                    if (ih.switchType != MyType.tBoolean)
                        continue;
                    
                    if (compare.getOperator() == compare.EQUALS_OP && 
                        ih.getSuccessors()[1] != ifHeader.getSuccessors()[0]
                        || compare.getOperator() == compare.NOTEQUALS_OP &&
                        ih.getSuccessors()[1] != ifHeader.getSuccessors()[1])
                        continue;
                    
                    cond = (Expression) ih.getInstruction();
                    break;
                } catch (ClassCastException ex) {
                } catch (NullPointerException ex) {
                }
            }

            if (cond == null)
                return null;

        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }

        InstructionHeader next = ih.nextInstruction;
        ih.nextInstruction = next.nextInstruction;
        ih.successors[1].predecessors.removeElement(ih);
        ih.switchType = MyType.tVoid;
        ih.successors = next.successors;
        ih.successors[0].predecessors.removeElement(next);
        ih.successors[0].predecessors.addElement(ih);
        ih.succs   = next.succs;
        ih.length += next.length;
        return ih;
    }

    public InstructionHeader createIfThenElseOperator(InstructionHeader ih) {
        InstructionHeader ifHeader;
        Expression e[] = new Expression[3];
        InstructionHeader[] succs;
        try {
            Vector predec = ih.getPredecessors();

            if (predec.size() != 1)
                return null;
            ifHeader = (InstructionHeader) predec.elementAt(0);
            if (ifHeader.switchType != Type.tBoolean)
                return null;
            succs = ifHeader.getSuccessors();
            if (succs[1] != ih ||
                succs[0].getNextInstruction() != succs[1] ||
                succs[0].getSuccessors().length != 1 ||
                succs[1].getSuccessors().length != 1 ||
                succs[0].getSuccessors()[0] != succs[1].getSuccessors()[0])
                return null;

            e[0] = ((Expression) ifHeader.getInstruction()).negate();
            e[1] = (Expression) succs[0].getInstruction();
            e[2] = (Expression) succs[1].getInstruction();
        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }
        IfThenElseOperator iteo = new IfThenElseOperator
            (MyType.intersection(e[1].getType(),e[2].getType()));
        ifHeader.instr           = new Expression(iteo, e);
        ifHeader.nextInstruction = ih.nextInstruction;
        ifHeader.length          = ih.addr + ih.length - ifHeader.addr;
        ifHeader.succs           = ih.succs;
        ifHeader.successors      = ih.successors;
        ifHeader.switchType      = Type.tVoid;
        ifHeader.successors[0].predecessors.removeElement(succs[0]);
        ifHeader.successors[0].predecessors.removeElement(succs[1]);
        ifHeader.successors[0].predecessors.addElement(ifHeader);
        return ifHeader;
    }

    public void analyze()
    {
        InstructionHeader ih, next;
        for (ih = methodHeader.getNextInstruction(); ih != null; 
             ih = next) {
	    if (env.isVerbose)
		System.err.print(".");
//             System.err.println(""+ih.getAddress());
            if ((next = removeNop(ih)) != null) continue;
            if ((next = createExpression(ih)) != null) continue;
            if ((next = createPostIncExpression(ih)) != null) continue;
            if ((next = createLocalPostIncExpression(ih)) != null) continue;
            if ((next = createAssignOp(ih)) != null) continue;
            if ((next = combineNewConstructor(ih)) != null) continue;
            if ((next = combineIfGotoExpressions(ih)) != null) continue;
            if ((next = createFunnyIfThenElseOp(ih)) != null) continue;
            if ((next = createIfThenElseOperator(ih)) != null) continue;
            if ((next = createAssignExpression(ih)) != null) continue;
            if ((next = createConstantArray(ih)) != null) continue;
            next = ih.getNextInstruction();
        }

        for (ih = methodHeader.getNextInstruction(); ih != null; 
             ih = ih.getNextInstruction()) {
            ih.instr = ih.getInstruction().simplify();
        }
//         for (int addr = 0; addr < instr.length; ) {
//             int nextAddr;
//             nextAddr = createExpression(addr);
//             if (nextAddr >= 0) {
//                 addr = nextAddr;
//                 continue;
//             }
//             nextAddr = createAssignExpression(addr);
//             if (nextAddr >= 0) {
//                 addr = nextAddr;
//                 continue;
//             }
//             nextAddr = createArrayOpAssign(addr);
//             if (nextAddr >= 0) {
//                 addr = nextAddr;
//                 continue;
//             }
//             nextAddr = createPostIncExpression(addr);
//             if (nextAddr >= 0) {
//                 addr = nextAddr;
//                 continue;
//             }
// //             nextAddr = createIfGotoStatement(addr);
// //             if (nextAddr >= 0) {
// //                 addr = nextAddr;
// //                 continue;
// //             }
//             nextAddr = combineIfGotoExpressions(addr);
//             if (nextAddr >= 0) {
//                 addr = nextAddr;
//                 continue;
//             }
//             nextAddr = combineConditionalExpr(addr);
//             if (nextAddr >= 0) {
//                 addr = nextAddr;
//                 continue;
//             }
//             addr += instr[addr].getLength();
//         }
//         for (int addr = 0; addr < instr.length; ) {
//             int nextAddr;
//             nextAddr = combineExpressions(addr);
//             if (nextAddr >= 0) {
//                 addr = nextAddr;
//                 continue;
//             }
//             addr += instr[addr].getLength();
//         }
    }

    public String getTypeString(Type type) {
        return type.toString();
    }

    public ClassDefinition getClassDefinition() {
        return env.getClassDefinition();
    }
}

