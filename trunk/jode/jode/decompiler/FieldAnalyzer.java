package jode;
import sun.tools.java.*;
import java.lang.reflect.Modifier;

public class FieldAnalyzer implements Analyzer {
    FieldDefinition fdef;
    JodeEnvironment env;
    
    public FieldAnalyzer(FieldDefinition fd, JodeEnvironment e)
    {
        fdef = fd;
        env  = e;
    }

    public void analyze() {
    }

    public int unsigned(byte value) {
        if (value < 0)
            return value + 256;
        else
            return value;
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException 
    {
	String modif = Modifier.toString(fdef.getModifiers());
	if (modif.length() > 0)
	    writer.print(modif+" ");

        writer.print(env.getTypeString(fdef.getType(), fdef.getName()));
        byte[] attrib = 
            ((BinaryField) fdef).getAttribute(Constants.idConstantValue);
        if (attrib != null) {
            int index = (unsigned(attrib[0]) << 8) | unsigned(attrib[1]);
            writer.print(" = "+env.getConstant(index).toString());
        }
        writer.println(";");
    }
}


