package jode;

public class CreateExpression implements Transformation {

    public InstructionHeader transform(InstructionHeader ih) {
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
                    ih = ih.combine(2, e);
		}
            }
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
        if(Decompiler.isVerbose && params > 0)
            System.err.print("x");
        return ih.combine(params+1, new Expression(op, exprs));
    }
}
