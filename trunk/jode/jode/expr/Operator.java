package jode;
import sun.tools.java.Type;

public abstract class Operator extends Instruction {
    public final static int ADD_OP     =  1;
    public final static int NEG_OP     =  2;
    public final static int SHIFT_OP   =  6;
    public final static int AND_OP     =  9;
    public final static int ASSIGN_OP  = 12;
    public final static int OPASSIGN_OP= 12;
    public final static int INC_OP     = 24; /* must be even! */
    public final static int DEC_OP     = 25;
    public final static int COMPARE_OP = 26; /* must be even! */
    public final static int LOG_AND_OP = 32; /* must be even! */
    public final static int LOG_OR_OP  = 33;
    public final static int LOG_NOT_OP = 34;
    static String opString[] = {
        "", " + ", " - ", " * ", " / ", " % ", 
	" << ", " >> ", " >>> ", " & ", " | ", " ^ ",
        " = ", " += ", " -= ", " *= ", " /= ", " %= ", 
	" <<= ", " >>= ", " >>>= ", " &= ", " |= ", " ^= ",
        "++", "--",
        " == "," != "," < "," >= "," > ", " <= ", " && ", " || ",
        "!", "~"
    };

    protected int operator;
    
    String casts;

    Operator (Type type, int op) {
        super(type);
        this.operator = op;
        if (type == null)
            throw new AssertError("type == null");
        casts = type.toString();
    }

    public int getOperator() {
        return operator;
    }
    public void setOperator(int op) {
        operator = op;
    }

    /**
     * Sets the return type of this operator.
     * @return true if the operand types changed
     */
    public boolean setType(Type type) {
//         if (!MyType.isOfType(type, this.type)) {
//             casts = type.toString()+"/*invalid*/ <- " + casts;
//         } else if (type != this.type) {
//             casts = type.toString()+" <- " + casts;
//         }
//         this.type = type;
        return false;
    }

    public String getOperatorString() {
        return opString[operator];
    }

    /**
     * Get priority of the operator.
     * Currently this priorities are known:
     * <ul><li> 1000 constant
     * </li><li> 950 new, .(field access), []
     * </li><li> 900 new[]
     * </li><li> 800 ++,-- (post)
     * </li><li> 700 ++,--(pre), +,-(unary), ~, !, cast
     * </li><li> 650 *,/, % 
     * </li><li> 610 +,-
     * </li><li> 600 <<, >>, >>> 
     * </li><li> 550 >, <, >=, <=, instanceof
     * </li><li> 500 ==, != 
     * </li><li> 450 & 
     * </li><li> 420 ^ 
     * </li><li> 410 | 
     * </li><li> 350 && 
     * </li><li> 310 || 
     * </li><li> 200 ?:
     * </li><li> 100 =, +=, -=, etc.
     * </li></ul>
     */
    public abstract int getPriority();

    /**
     * Get minimum priority of the nth operand.
     * @see getPriority
     */
    public abstract int getOperandPriority(int i);
    public abstract Type getOperandType(int i);
    public abstract int getOperandCount();
    public abstract void setOperandType(Type[] inputTypes);
    public abstract String toString(String[] operands);

    public String toString()
    {
        String[] operands = new String[getOperandCount()];
        for (int i=0; i< operands.length; i++) {
            operands[i] = "stack_"+(operands.length-i-1);
        }
        return toString(operands);
    }
}
