package jode;
import sun.tools.java.*;
import java.lang.reflect.Modifier;
import java.io.*;

public class MethodAnalyzer implements Analyzer, Constants {
    FieldDefinition mdef;
    JodeEnvironment env;
    CodeAnalyzer code = null;
    LocalVariableAnalyzer lva;
    
    public MethodAnalyzer(FieldDefinition fd, JodeEnvironment e)
    {
        mdef = fd;
        env  = e;
        byte bytecode[] = ((BinaryField) mdef).getAttribute(Constants.idCode);
        if (bytecode != null) {
            BinaryCode bc = 
                new BinaryCode(bytecode, 
                               env.getConstantPool(),
                               env);
            lva = new LocalVariableAnalyzer(env, mdef, bc.getMaxLocals());
            lva.read(bc);
            code = new CodeAnalyzer(this, bc, env);
        }
    }

    public int getParamCount() {
	return (mdef.isStatic()?0:1)+
	    mdef.getType().getArgumentTypes().length;
    }

    public void analyze() 
         throws ClassFormatError
    {
	if (code == null)
	    return;
	if (env.isVerbose)
	    System.err.print(mdef.getName().toString()+": locals ");
        lva.createLocalInfo(code);
	if (env.isVerbose) {
	    System.err.println("");
	    System.err.print("code ");
	}
        code.analyze();
	if (env.isVerbose)
	    System.err.println("");
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
        writer.println("");
	String modif = Modifier.toString(mdef.getModifiers());
	if (modif.length() > 0)
	    writer.print(modif+" ");
        if (mdef.isInitializer()) {
            writer.print(""); /* static block */
        } else { 
            if (mdef.isConstructor())
                writer.print(mdef.getClassDeclaration().getName().toString());
            else
                writer.print(env.getTypeString(mdef.getType().getReturnType())+
			     " "+ mdef.getName().toString());
            writer.print("(");
            Type[] paramTypes = mdef.getType().getArgumentTypes();
            int offset = mdef.isStatic()?0:1;
            for (int i=0; i<paramTypes.length; i++) {
                if (i>0)
                    writer.print(", ");
		writer.print
		    ((code == null)?
		     env.getTypeString(paramTypes[i]):
		     env.getTypeString
		     (paramTypes[i], lva.getLocal(i+offset).getName()));
            }
            writer.print(")");
        }
        IdentifierToken[] exceptions = mdef.getExceptionIds();
        if (exceptions != null && exceptions.length > 0) {
            writer.println("");
            writer.print("throws ");
            for (int i= 0; i< exceptions.length; i++) {
                if (exceptions[i] != null) {
                    if (i > 0)
                        writer.print(", ");
                    writer.print(env.getTypeString(exceptions[i].getName()));
                }
            }
        }
        if (code != null) {
            writer.println(" {");
            writer.tab();
	    lva.dumpSource(writer);
            code.dumpSource(writer);
            writer.untab();
            writer.println("}");
        } else
            writer.println(";");
    }

    /*
    public byte[] getAttribute(Identifier identifier)
    {
        if (mdef instanceof BinaryField)
            return ((BinaryField)mdef).getAttribute(identifier);
        return null;
    }
    */
}
