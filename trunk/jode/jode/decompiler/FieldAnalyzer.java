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

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException 
    {
	String modif = Modifier.toString(fdef.getModifiers());
	if (modif.length() > 0)
	    writer.print(modif+" ");

        writer.println(fdef.getType().
                     typeString(fdef.getName().toString(), false, false)+";");
//         writer.tab();
//         if (attributes.length > 0) {
//             writer.println("/* Attributes: "+attributes.length+" */");
//             for (int i=0; i < attributes.length; i++)
//                 attributes[i].dumpSource(writer);
//         }
//         writer.untab();
    }
}


