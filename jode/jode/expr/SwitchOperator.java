package jode;
import sun.tools.java.Type;

public class SwitchOperator extends JumpInstruction {
    int[] cases;
    int[] destinations;
    Type operandType;

    public SwitchOperator(int addr, int length, int[] cases, int[] dests) {
        super(addr,length);
        this.cases = cases;
        this.destinations = dests;
        this.operandType = UnknownType.tUInt;
    }

    public int[] getCases() {
        return cases;
    }

    public int[] getSuccessors() {
        return destinations;
    }

    public int getPriority() {
        return 0;
    }

    public int getOperandCount() {
        return 1;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public Type getOperandType(int i) {
        return operandType;
    }

    public void setOperandType(Type types[]) {
        operandType = UnknownType.commonType(operandType, types[0]);
    }

    public boolean setType(Type t) {
        super.setType(type);
        if (type != operandType) {
            operandType = type;
            return true;
        }
        return false;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return "switch ("+operands[0]+") ";
    }

    public void dumpSource(TabbedPrintWriter writer, CodeAnalyzer ca) 
         throws java.io.IOException
    {
        writer.println("switch(stack_0) {");
        writer.tab();
        for (int i=0; i< cases.length; i++) {
            writer.println("case "+cases[i]+
                           ": goto addr_"+destinations[i]+";");
        }
        writer.println("default: goto addr_"+destinations[cases.length]);
        writer.untab();
        writer.println("}");
    }
}
