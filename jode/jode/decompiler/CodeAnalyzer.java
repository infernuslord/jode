package jode;
import sun.tools.java.*;
import java.io.*;

public class CodeAnalyzer implements Analyzer, Constants {
    
    BinaryCode bincode;

    MethodInstructionHeader methodHeader;
    MethodAnalyzer method;
    public JodeEnvironment env;
    
    /**
     * Get the method.
     * @return The method to which this code belongs.
     */
    public MethodAnalyzer getMethod() {return method;}
    
    void readCode() 
         throws ClassFormatError
    {
        byte[] code = bincode.getCode();
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
        BinaryExceptionHandler[] handlers = bincode.getExceptionHandlers();
	methodHeader = new MethodInstructionHeader(env, instr, handlers);
    }

	/*
        tryAddrs.put(new Integer(handler.startPC), handler);
        references[handler.startPC]++;
        catchAddrs.put(new Integer(handler.handlerPC), handler);
        references[handler.handlerPC]++;
	*/

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
        methodHeader.dumpSource(writer);
    }

    public CodeAnalyzer(MethodAnalyzer ma, BinaryCode bc, JodeEnvironment e)
         throws ClassFormatError
    {
        method = ma;
        env  = e;
	bincode = bc;
        readCode();
    }

    static Transformation[] exprTrafos = {
        new RemoveNop(),
        new CombineCatchLocal(),
        new CreateExpression(),
        new CreatePostIncExpression(),
        new CreateAssignExpression(),
        new CreateNewConstructor(),
        new CombineIfGotoExpressions(),
        new CreateIfThenElseOperator(),
        new CreateConstantArray()
    };

    static Transformation[] simplifyTrafos = { new SimplifyExpression() };
    static Transformation[] blockTrafos = { 
        new CreateTryCatchStatements(),
        new CreateBreakStatement(),
        new CreateIfStatements(),
        new CreateWhileStatements(),
        new CreateSwitchStatements()
    };

    public void analyze()
    {
        methodHeader.doTransformations(exprTrafos);
        methodHeader.doTransformations(simplifyTrafos);
        methodHeader.doTransformations(blockTrafos);
    }

    public String getTypeString(Type type) {
        return env.getTypeString(type);
    }

    public ClassDefinition getClassDefinition() {
        return env.getClassDefinition();
    }
}

