package jode;
import sun.tools.java.Type;

public class CreateConstantArray implements Transformation {

    public InstructionHeader transform(InstructionHeader ih) {
        Expression[] consts = null;
	int count = 0;
        Type type;
        try {
            if (ih.getInstruction() instanceof DupOperator)
                /* this is not the end of the array assign */
                return null;
            ih = ih.getSimpleUniquePredecessor();
            int lastindex = -1;
            while (ih.getInstruction() instanceof ArrayStoreOperator) {
                ArrayStoreOperator store = 
                    (ArrayStoreOperator) ih.getInstruction();
                ih = ih.getSimpleUniquePredecessor();
		Expression lastconst = (Expression) ih.getInstruction();
                ih = ih.getSimpleUniquePredecessor();
                Expression indexexpr = (Expression) ih.getInstruction();
                ConstOperator indexop = 
                    (ConstOperator) indexexpr.getOperator();
                if (!MyType.isOfType(indexop.getType(), MyType.tUInt))
                    return null;
                int index = Integer.parseInt(indexop.getValue());
                if (index >= 0 && consts == null) {
                    lastindex = index;
                    consts = new Expression[lastindex+1];
                } else if (index < 0 || index > lastindex)
                    return null;
		else { 
                    while (index < lastindex) {
                        consts[lastindex--] = new Expression
                            (new ConstOperator(MyType.tUnknown, "0"), 
                             new Expression[0]);
                    }
                }
                consts[lastindex--] = lastconst;
                ih = ih.getSimpleUniquePredecessor();
                DupOperator dup = (DupOperator) ih.getInstruction();
                if (dup.getDepth() != 0 || 
                    dup.getCount() != store.getLValueType().stackSize())
                    return null;
		count++;
                ih = ih.getSimpleUniquePredecessor();
            }
            if (count == 0)
                return null;
            while (lastindex >= 0) {
                consts[lastindex--] = new Expression
                    (new ConstOperator(MyType.tUnknown, "0"), 
                     new Expression[0]);
            }
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
            int arraylength = Integer.parseInt(countop.getValue());
            if (arraylength != consts.length) {
                if (arraylength < consts.length)
                    return null;
                Expression[] newConsts = new Expression[arraylength];
                System.arraycopy(consts, 0, newConsts, 0, consts.length);
                for (int i=consts.length; i<arraylength; i++)
                    newConsts[i] = new Expression
                        (new ConstOperator(MyType.tUnknown, "0"), 
                         new Expression[0]);
                consts = newConsts;
            }
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
        if (Decompiler.isVerbose)
            System.err.print("a");
        Operator op = new ConstantArrayOperator(type, consts.length);
        return ih.combine(4*count+1, new Expression(op, consts));
    }
}
