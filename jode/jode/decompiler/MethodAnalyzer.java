package jode;
import sun.tools.java.*;
import java.lang.reflect.Modifier;
import java.io.*;

public class MethodAnalyzer implements Analyzer, Constants {
    FieldDefinition mdef;
    JodeEnvironment env;
    CodeAnalyzer code = null;
    LocalVariableTable lvt;
    
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
            lvt = new LocalVariableTable(bc.getMaxLocals());
            readLVT(bc);
            code = new CodeAnalyzer(this, bc, env);
        }
    }

    public void readLVT(BinaryCode bc) {
        BinaryAttribute attr = bc.getAttributes();
        while (attr != null) {
            if (attr.getName() == idLocalVariableTable) {
                DataInputStream stream = 
                    new DataInputStream
                    (new ByteArrayInputStream(attr.getData()));
                try {
                    lvt.read(env, stream);
                } catch (IOException ex) {
                    throw new ClassFormatError(ex.toString());
                }
            }
            attr = attr.getNextAttribute();
        }
        if (!lvt.isReadFromClass()) {
            int offset = 0;
            if (!mdef.isStatic()) {
                LocalInfo li = lvt.getLocal(0);
                li.name = "this";
                li.type = mdef.getClassDefinition().getType();
                offset++;
            }                
            Type[] paramTypes = mdef.getType().getArgumentTypes();
            for (int i=0; i< paramTypes.length; i++) {
                LocalInfo li = lvt.getLocal(offset+i);
                li.type = paramTypes[i];
            }
        }
    }

    public void analyze() 
         throws ClassFormatError
    {
        code.analyze();
    }

    public LocalVariable getLocal(int i) {
        return lvt.getLocal(i);
    }

    public Identifier getLocalName(int i, int addr) {
        Identifier name = lvt.getLocal(i).getName(addr);
        if (name != null)
            return name;
        if (!mdef.isStatic() && i == 0)
            return idThis;
        return Identifier.lookup("local_"+i+"@"+addr);
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
                writer.print(mdef.getType().getReturnType().toString()+" "+
                             mdef.getName().toString());
            writer.print("(");
            Type[] paramTypes = mdef.getType().getArgumentTypes();
            int offset = mdef.isStatic()?0:1;
            for (int i=0; i<paramTypes.length; i++) {
                if (i>0)
                    writer.print(", ");
                writer.print(paramTypes[i].
                             typeString(getLocalName(i+offset, 0).toString(), 
                                        false, false));
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
                    writer.print(exceptions[i].getName().toString());
                }
            }
        }
        if (code != null) {
            writer.println(" {");
            writer.tab();
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
