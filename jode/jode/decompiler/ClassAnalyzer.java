package jode;
import java.lang.reflect.Modifier;
import java.io.IOException;
import sun.tools.java.*;

public class ClassAnalyzer implements Analyzer {
    BinaryClass cdef;
    JodeEnvironment env;
    Analyzer fields[];
    
    public ClassAnalyzer(BinaryClass bc, JodeEnvironment e)
    {
        cdef = bc;
        env  = e;
    }
    
    public void analyze() {
        int numFields = 0, i=0;
        
        FieldDefinition f;
        for (f= cdef.getInnerClassField(); f != null; f = f.getNextField())
            numFields++;
        for (f= cdef.getFirstField(); f != null; f = f.getNextField())
            numFields++;
        fields = new Analyzer[numFields];
        for (f= cdef.getInnerClassField(); f != null; f = f.getNextField()) {
            System.err.println("analyzing inner: "+f.getName());
            fields[i] = new ClassAnalyzer((BinaryClass) f.getInnerClass(), env);
            fields[i++].analyze();
        }
        for (f= cdef.getFirstField(); f != null; f = f.getNextField()) {
            if (f.getType().getTypeCode() == Constants.TC_METHOD) {
                System.err.println("analyzing method: "+f.getName());
                fields[i] = new MethodAnalyzer(f, env);
            } else {
                System.err.println("analyzing field: "+f.getName());
                fields[i] = new FieldAnalyzer(f, env);
            }
            fields[i++].analyze();
        }
    }

    public void dumpSource(TabbedPrintWriter writer) throws IOException
    {
        if (cdef.getSource() != null)
            writer.println("/* Original source: "+cdef.getSource()+" */");
        String modif = Modifier.toString(cdef.getModifiers());
        if (modif.length() > 0)
            writer.print(modif + " ");
        writer.print((cdef.isInterface())?"interface ":"class ");
	writer.println(env.getNickName(cdef.getName().toString()));
	writer.tab();
	if (cdef.getSuperClass() != null)
	    writer.println("extends "+cdef.getSuperClass().getName().toString());
        ClassDeclaration interfaces[] = cdef.getInterfaces();
	if (interfaces.length > 0) {
	    writer.print("implements ");
	    for (int i=0; i < interfaces.length; i++) {
		if (i > 0)
		    writer.print(", ");
		writer.print(interfaces[i].getName().toString());
	    }
	}
	writer.untab();
	writer.println(" {");
	writer.tab();

	for (int i=0; i< fields.length; i++)
	    fields[i].dumpSource(writer);
	writer.untab();
	writer.println("}");
    }
}


