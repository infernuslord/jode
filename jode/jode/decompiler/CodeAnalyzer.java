/* CodeAnalyzer Copyright (C) 1998-1999 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package jode.decompiler;
import jode.Decompiler;
import jode.type.Type;
import jode.bytecode.*;
import jode.flow.FlowBlock;
import jode.flow.TransformExceptionHandlers;
import jode.jvm.CodeVerifier;
import jode.jvm.VerifyException;

import java.util.BitSet;
import java.util.Stack;
import java.util.Vector;
import java.util.Enumeration;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CodeAnalyzer implements Analyzer {
    
    FlowBlock methodHeader;
    BytecodeInfo code;
    MethodAnalyzer method;
    ImportHandler imports;

    Vector allLocals = new Vector();
    LocalInfo[] param;
    LocalVariableTable lvt;
    
    public CodeAnalyzer(MethodAnalyzer ma, MethodInfo minfo,
			ImportHandler i)
    {
        method = ma;
	imports = i;
	code = minfo.getBytecode();
	CodeVerifier verifier = new CodeVerifier(getClazz(), minfo, code);
	try {
	    verifier.verify();
	} catch (VerifyException ex) {
	    ex.printStackTrace();
	    throw new jode.AssertError("Verification error");
	}
	
	if (Decompiler.useLVT) {
	    LocalVariableInfo[] localvars = code.getLocalVariableTable();
	    if (localvars != null)
		lvt = new LocalVariableTable(code.getMaxLocals(), 
					     localvars);
	}
	initParams();
    }

    public void initParams() {
        Type[] paramTypes = method.getType().getParameterTypes();
	int paramCount = (method.isStatic() ? 0 : 1) + paramTypes.length;
	param = new LocalInfo[paramCount];
	int offset = 0;
	int slot = 0;
	if (!method.isStatic())
	    param[offset++] = getLocalInfo(0, slot++);
	for (int i=0; i < paramTypes.length; i++) {
	    param[offset++] = getLocalInfo(0, slot);
	    slot += paramTypes[i].stackSize();
	}
	
    }

    public BytecodeInfo getBytecodeInfo() {
	return code;
    }

    public FlowBlock getMethodHeader() {
        return methodHeader;
    }

    void readCode() {
	/* The adjacent analyzation relies on this */
	DeadCodeAnalysis.removeDeadCode(code);
	Handler[] handlers = code.getExceptionHandlers();
	int returnCount;
        TransformExceptionHandlers excHandlers; 
	{
	    /* First create a FlowBlock for every block that has a 
	     * predecessor other than the previous instruction.
	     */
	    for (Instruction instr = code.getFirstInstr();
		 instr != null; instr = instr.nextByAddr) {
		if (instr.prevByAddr == null
		    || instr.prevByAddr.alwaysJumps
		    || instr.preds != null)
		    instr.tmpInfo = new FlowBlock
			(this, instr.addr, instr.length);
	    }

	    for (int i=0; i < handlers.length; i++) {
		Instruction instr = handlers[i].start;
		if (instr.tmpInfo == null)
		    instr.tmpInfo 
			= new FlowBlock(this, instr.addr, instr.length);
		instr = handlers[i].catcher;
		if (instr.tmpInfo == null)
		    instr.tmpInfo 
			= new FlowBlock(this, instr.addr, instr.length);
	    }

            /* While we read the opcodes into FlowBlocks 
             * we try to combine sequential blocks, as soon as we
             * find two sequential instructions in a row, where the
             * second has no predecessors.
             */
            int mark = 1000;
            FlowBlock lastBlock = null;
	    boolean   lastSequential = false;
	    for (Instruction instr = code.getFirstInstr();
		 instr != null; instr = instr.nextByAddr) {

		jode.flow.StructuredBlock block
		    = Opcodes.readOpcode(instr, this);

                if (jode.Decompiler.isVerbose && instr.addr > mark) {
                    Decompiler.err.print('.');
                    mark += 1000;
                }

                if (lastSequential && instr.tmpInfo == null
		    /* Only merge with previous block, if this is sequential, 
		     * too.  
		     * Why?  doSequentialT1 does only handle sequential blocks.
		     */
		    && !instr.alwaysJumps && instr.succs == null) {
		    
                    lastBlock.doSequentialT1(block, instr.length);

                } else {

		    if (instr.tmpInfo == null)
			instr.tmpInfo = new FlowBlock
			    (this, instr.addr, instr.length);
		    FlowBlock flowBlock = (FlowBlock) instr.tmpInfo;
		    flowBlock.setBlock(block);

		    if (lastBlock != null)
			lastBlock.setNextByAddr(flowBlock);

                    instr.tmpInfo = lastBlock = flowBlock;
		    lastSequential = !instr.alwaysJumps && instr.succs == null;
                }
	    }

	    methodHeader = (FlowBlock) code.getFirstInstr().tmpInfo;
            excHandlers = new TransformExceptionHandlers();
            for (int i=0; i<handlers.length; i++) {
                Type type = null;
                FlowBlock start 
		    = (FlowBlock) handlers[i].start.tmpInfo;
                int endAddr = handlers[i].end.nextByAddr.addr;
                FlowBlock handler
		    = (FlowBlock) handlers[i].catcher.tmpInfo;
                if (handlers[i].type != null)
                    type = Type.tClass(handlers[i].type);
                
                excHandlers.addHandler(start, endAddr, handler, type);
            }
        }
	for (Instruction instr = code.getFirstInstr();
	     instr != null; instr = instr.nextByAddr)
	    instr.tmpInfo = null;

        if (Decompiler.isVerbose)
            Decompiler.err.print('-');
            
//          try {
//              TabbedPrintWriter writer = new TabbedPrintWriter(System.err);
//  	    methodHeader.dumpSource(writer);
//          } catch (java.io.IOException ex) {
//          }
        excHandlers.analyze();
        methodHeader.analyze();
    } 

    public void analyze()
    {
	if (code == null)
	    return;
        readCode();
	if (!Decompiler.usePUSH && methodHeader.mapStackToLocal())
	    methodHeader.removePush();
	if (Decompiler.removeOnetimeLocals)
	    methodHeader.removeOnetimeLocals();

        Enumeration enum = allLocals.elements();
        while (enum.hasMoreElements()) {
            LocalInfo li = (LocalInfo)enum.nextElement();
            if (!li.isShadow())
                imports.useType(li.getType());
        }
	for (int i=0; i < param.length; i++) {
	    for (int j=0; j < i; j++) {
		if (param[j].getName().equals(param[i].getName())) {
		    /* A name conflict happened. */
		    param[i].makeNameUnique();
		    break; /* j */
		}
	    }
	}
	methodHeader.makeDeclaration(new jode.flow.VariableSet(param));
	methodHeader.simplify();
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
	if (methodHeader != null)
	    methodHeader.dumpSource(writer);
	else
	    writer.println("COULDN'T DECOMPILE METHOD!");
    }

    public LocalInfo getLocalInfo(int addr, int slot) {
        LocalInfo li = (lvt != null)
            ? lvt.getLocal(slot).getInfo(addr)
            : new LocalInfo(slot);
        if (!allLocals.contains(li))
            allLocals.addElement(li);
        return li;
    }

    /**
     * Checks if the variable set contains a local with the given name.
     */
    public LocalInfo findLocal(String name) {
        Enumeration enum = allLocals.elements();
        while (enum.hasMoreElements()) {
            LocalInfo li = (LocalInfo) enum.nextElement();
            if (li.getName().equals(name))
                return li;
        }
        return null;
    }

    public LocalInfo getParamInfo(int nr) {
	return param[nr];
    }

    public String getTypeString(Type type) {
        return method.classAnalyzer.getTypeString(type);
    }

    public ClassAnalyzer getClassAnalyzer() {
	return method.classAnalyzer;
    }

    public ClassInfo getClazz() {
        return method.classAnalyzer.clazz;
    }

    /**
     * Get the method.
     * @return The method to which this code belongs.
     */
    public MethodAnalyzer getMethod() {return method;}

    public void useType(Type type) {
	imports.useType(type);
    }
}
