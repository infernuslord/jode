package jode;
import sun.tools.java.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.io.*;

public class CodeAnalyzer implements Analyzer, Constants, Opcodes {
    
    MethodAnalyzer method;
    JodeEnvironment env;
    BinaryCode bincode;

    Instruction[] instr;
    int[] references;
    Hashtable tryAddrs   = new Hashtable();
    Hashtable catchAddrs = new Hashtable();
    Hashtable labels     = new Hashtable();

    public void readOpcodes(DataInputStream stream) 
         throws IOException, ClassFormatError
    {
        int addr = 0;
        try {
        while (true) {
            int opcode;
            try {
                opcode = stream.readUnsignedByte();
            } catch (EOFException eof) {
                break;
            }
            switch (opcode) {
            case NOP_OP:
                instr[addr] = new NopOperator(addr, 1);
                break;
            case ACONST_NULL_OP:
                instr[addr] = new ConstOperator(addr, 1, OBJECT_TYPE, "null");
                break;
            case ICONST_M1_OP: 
            case ICONST_0_OP: case ICONST_1_OP: case ICONST_2_OP:
            case ICONST_3_OP: case ICONST_4_OP: case ICONST_5_OP:
                instr[addr] = 
                    new ConstOperator(addr, 1, ALL_INT_TYPE, 
                                    Integer.toString(opcode - ICONST_0_OP));
                break;
            case LCONST_0_OP: case LCONST_1_OP:
                instr[addr] = 
                    new ConstOperator(addr, 1, LONG_TYPE, 
                                    Integer.toString(opcode - LCONST_0_OP)+
                                    "L");
                break;
            case FCONST_0_OP: case FCONST_1_OP: case FCONST_2_OP:
                instr[addr] = 
                    new ConstOperator(addr, 1, FLOAT_TYPE,
                                    Integer.toString(opcode - FCONST_0_OP)+
                                    ".0F");
                break;
            case DCONST_0_OP: case DCONST_1_OP:
                instr[addr] = 
                    new ConstOperator(addr, 1, DOUBLE_TYPE,
                                    Integer.toString(opcode - DCONST_0_OP)+
                                    ".0");
                break;
            case BIPUSH_OP:
                instr[addr] = 
                    new ConstOperator(addr, 2, ALL_INT_TYPE, 
                                    Integer.toString(stream.readByte()));
                break;
            case SIPUSH_OP:
                instr[addr] = 
                    new ConstOperator(addr, 3, ALL_INT_TYPE, 
                                    Integer.toString(stream.readShort()));
                break;
            case LDC_OP: {
                int index = stream.readUnsignedByte();
                instr[addr] = 
                    new ConstOperator(addr, 2, env.getConstantType(index),
                                    env.getConstant(index).toString());
                break;
            }
            case LDC_W_OP:
            case LDC2_W_OP: {
                int index = stream.readUnsignedShort();
                instr[addr] = 
                    new ConstOperator(addr, 3, env.getConstantType(index),
                                    env.getConstant(index).toString());
                break;
            }
            case ILOAD_OP: case LLOAD_OP: 
            case FLOAD_OP: case DLOAD_OP: case ALOAD_OP:
                instr[addr] = 
                    new LoadOperator(addr, 2, types[0][opcode-ILOAD_OP],
                                   method.getLocal(stream.readUnsignedByte()));
                break;
            case ILOAD_0_OP: case ILOAD_1_OP: case ILOAD_2_OP: case ILOAD_3_OP:
            case LLOAD_0_OP: case LLOAD_1_OP: case LLOAD_2_OP: case LLOAD_3_OP:
            case FLOAD_0_OP: case FLOAD_1_OP: case FLOAD_2_OP: case FLOAD_3_OP:
            case DLOAD_0_OP: case DLOAD_1_OP: case DLOAD_2_OP: case DLOAD_3_OP:
            case ALOAD_0_OP: case ALOAD_1_OP: case ALOAD_2_OP: case ALOAD_3_OP:
                instr[addr] = 
                    new LoadOperator(addr, 1, types[0][(opcode-ILOAD_0_OP)/4],
                                   method.getLocal((opcode-ILOAD_0_OP) & 3));
                break;
            case IALOAD_OP: case LALOAD_OP: 
            case FALOAD_OP: case DALOAD_OP: case AALOAD_OP:
            case BALOAD_OP: case CALOAD_OP: case SALOAD_OP:
                instr[addr] = 
                    new ArrayLoadOperator(addr, 1, types[1][opcode - IALOAD_OP]);
                break;
            case ISTORE_OP: case LSTORE_OP: 
            case FSTORE_OP: case DSTORE_OP: case ASTORE_OP:
                instr[addr] = 
                    new StoreOperator(addr, 2, types[0][opcode-ISTORE_OP],
                                    method.getLocal(stream.readUnsignedByte()),
                                    Operator.ASSIGN_OP);
                break;
            case ISTORE_0_OP: case ISTORE_1_OP: 
            case ISTORE_2_OP: case ISTORE_3_OP:
            case LSTORE_0_OP: case LSTORE_1_OP: 
            case LSTORE_2_OP: case LSTORE_3_OP:
            case FSTORE_0_OP: case FSTORE_1_OP:
            case FSTORE_2_OP: case FSTORE_3_OP:
            case DSTORE_0_OP: case DSTORE_1_OP:
            case DSTORE_2_OP: case DSTORE_3_OP:
            case ASTORE_0_OP: case ASTORE_1_OP:
            case ASTORE_2_OP: case ASTORE_3_OP:
                instr[addr] = 
                    new StoreOperator(addr, 1, types[0][(opcode-ISTORE_0_OP)/4],
                                    method.getLocal((opcode-ISTORE_0_OP) & 3),
                                    Operator.ASSIGN_OP);
                break;
            case IASTORE_OP: case LASTORE_OP:
            case FASTORE_OP: case DASTORE_OP: case AASTORE_OP:
            case BASTORE_OP: case CASTORE_OP: case SASTORE_OP:
                instr[addr] = 
                    new ArrayStoreOperator(addr, 1, 
                                         types[1][opcode - IASTORE_OP]);
                break;
            case POP_OP: case POP2_OP:
                instr[addr] = 
                    new PopOperator(addr, 1, opcode - POP_OP + 1);
                break;
            case DUP_OP: case DUP_X1_OP: case DUP_X2_OP:
            case DUP2_OP: case DUP2_X1_OP: case DUP2_X2_OP:
                instr[addr] = 
                    new DupOperator(addr, 1, 
                                  (opcode - DUP_OP)%3, 
                                  (opcode - DUP_OP)/3+1);
                break;
            case SWAP_OP:
                instr[addr] = new SwapOperator(addr, 1);
                break;
            case IADD_OP: case LADD_OP: case FADD_OP: case DADD_OP:
            case ISUB_OP: case LSUB_OP: case FSUB_OP: case DSUB_OP:
            case IMUL_OP: case LMUL_OP: case FMUL_OP: case DMUL_OP:
            case IDIV_OP: case LDIV_OP: case FDIV_OP: case DDIV_OP:
            case IREM_OP: case LREM_OP: case FREM_OP: case DREM_OP:
                instr[addr] = 
                    new BinaryOperator(addr, 1, types[0][(opcode - IADD_OP)%4],
                                     (opcode - IADD_OP)/4+Operator.ADD_OP);
                break;
            case INEG_OP: case LNEG_OP: case FNEG_OP: case DNEG_OP:
                instr[addr] = 
                    new UnaryOperator(addr, 1, types[0][opcode - INEG_OP], 
                                    Operator.NEG_OP);
                break;
            case ISHL_OP: case LSHL_OP:
            case ISHR_OP: case LSHR_OP:
            case IUSHR_OP: case LUSHR_OP:
                instr[addr] = new ShiftOperator(addr, 1, 
                                              types[0][(opcode - ISHL_OP)%2],
                                              (opcode - ISHL_OP)/2 + 
                                              Operator.SHIFT_OP);
                break;
            case IAND_OP: case LAND_OP:
            case IOR_OP : case LOR_OP :
            case IXOR_OP: case LXOR_OP:
                instr[addr] = new BinaryOperator(addr, 1, 
                                               types[0][(opcode - ISHL_OP)%2],
                                               (opcode - ISHL_OP)/2 + 
                                               Operator.SHIFT_OP);
                break;
            case IINC_OP: {
                int local = stream.readUnsignedByte();
                int value = stream.readUnsignedByte();
                int operation = Operator.ADD_OP;
                if (value < 0) {
                    value = -value;
                    operation = Operator.NEG_OP;
                }
                instr[addr] = new ConstOperator(addr, 1, ALL_INT_TYPE, 
                                              Integer.toString(value));
                instr[addr+1] =
                    new StoreOperator(addr+1, 2, ALL_INT_TYPE, 
                                    method.getLocal(local),
                                    operation + Operator.OPASSIGN_OP);
                addr++;
            }
        
            break;
            case I2L_OP: case I2F_OP: case I2D_OP:
            case L2I_OP: case L2F_OP: case L2D_OP:
            case F2I_OP: case F2L_OP: case F2D_OP:
            case D2I_OP: case D2L_OP: case D2F_OP: {
                int from = (opcode-I2L_OP)/3;
                int to   = (opcode-I2L_OP)%3;
                if (to >= from)
                    to++;
                instr[addr] = new ConvertOperator(addr, 1, types[0][from], 
                                                types[1][to]);
                break;
            }
            case I2B_OP: case I2C_OP: case I2S_OP:
                instr[addr] = new ConvertOperator(addr, 1, ALL_INT_TYPE, 
                                                types[1][(opcode-I2B_OP)+5]);
                break;
            case LCMP_OP:
            case FCMPL_OP: case FCMPG_OP:
            case DCMPL_OP: case DCMPG_OP:
                instr[addr] = new CompareToIntOperator
                    (addr, 1, types[0][(opcode-LCMP_OP+3)/2], 
                     (opcode-LCMP_OP+3)%2);
                break;
            case IFEQ_OP: case IFNE_OP: 
            case IFLT_OP: case IFGE_OP: case IFGT_OP: case IFLE_OP: {
                instr[addr] = new CompareUnaryOperator
                    (addr, 1, ALL_INT_TYPE, 
                     opcode - IFEQ_OP+Operator.COMPARE_OP);
                instr[addr+1] = new IfGotoOperator(addr+1, 2, 
                                                 addr+stream.readShort());
                addr++;
                break;
            }                         
            case IF_ICMPEQ_OP: case IF_ICMPNE_OP: case IF_ICMPLT_OP: 
            case IF_ICMPGE_OP: case IF_ICMPGT_OP: case IF_ICMPLE_OP:
                instr[addr] = new CompareBinaryOperator
                    (addr, 1, ALL_INT_TYPE, 
                     opcode - IF_ICMPEQ_OP+Operator.COMPARE_OP);
                instr[addr+1] = new IfGotoOperator(addr+1, 2, 
                                                 addr+stream.readShort());
                addr++;
                break;
            case IF_ACMPEQ_OP: case IF_ACMPNE_OP:
                instr[addr] = new CompareBinaryOperator
                    (addr, 1, OBJECT_TYPE, 
                     opcode - IF_ACMPEQ_OP+Operator.COMPARE_OP);
                instr[addr+1] = new IfGotoOperator(addr+1, 2, 
                                                 addr+stream.readShort());
                addr++;
                break;
            case GOTO_OP:
                instr[addr] = new GotoOperator(addr, 3, addr+stream.readShort());
                break;
            case JSR_OP:
                instr[addr] = new JsrOperator(addr, 3, addr+stream.readShort());
                break;
            case RET_OP:
                instr[addr] = new RetOperator(addr, 3, stream.readUnsignedByte());
                break;
            case TABLESWITCH_OP: {
                int length = 3-(addr % 4);
                stream.skip(length);
                int def  = addr + stream.readInt();
                int low  = stream.readInt();
                int high = stream.readInt();
                int[] cases = new int[high-low+1];
                int[] dests = new int[high-low+2];
                for (int i=0; i+low <= high; i++) {
                    cases[i] = i+low;
                    dests[i] = addr + stream.readInt();
                }
                dests[cases.length] = def;
                length += 13 + 4 * cases.length;
                instr[addr] = new SwitchOperator(addr, length, cases, dests);
                break;
            }
            case LOOKUPSWITCH_OP: {
                int length = 3-(addr % 4);
                stream.skip(length);
                int def    = addr + stream.readInt();
                int npairs = stream.readInt();
                int[] cases = new int[npairs];
                int[] dests = new int[npairs+1];
                for (int i=0; i < npairs; i++) {
                    cases[i] = stream.readInt();
                    dests[i] = addr + stream.readInt();
                }
                dests[npairs] = def;
                length += 9 + 8 * npairs;
                instr[addr] = new SwitchOperator(addr, length, cases, dests);
                break;
            }
            case IRETURN_OP: case LRETURN_OP: 
            case FRETURN_OP: case DRETURN_OP: case ARETURN_OP: {
                Type retType = UnknownType.commonType
                    (method.mdef.getType().getReturnType(),
                     types[0][opcode-IRETURN_OP]);
                instr[addr] = 
                    new ReturnOperator(addr, 1, retType);
                break;
            }
            case RETURN_OP:
                instr[addr] = new ReturnOperator(addr, 1, Type.tVoid);
                break;
            case GETSTATIC_OP:
                instr[addr] = 
                    new GetFieldOperator(addr, 3, true, 
                                       (FieldDefinition)env.getConstant
                                       (stream.readUnsignedShort()));
                break;
            case PUTSTATIC_OP:
                instr[addr] = 
                    new PutFieldOperator(addr, 3, true, 
                                       (FieldDefinition)env.getConstant
                                       (stream.readUnsignedShort()));
                break;
            case GETFIELD_OP:
                instr[addr] = new GetFieldOperator(addr, 3, false,
                                       (FieldDefinition)env.getConstant
                                       (stream.readUnsignedShort()));
                break;
            case PUTFIELD_OP:
                instr[addr] = 
                    new PutFieldOperator(addr, 3, false,
                                       (FieldDefinition)env.getConstant
                                       (stream.readUnsignedShort()));
                break;
            case INVOKEVIRTUAL_OP:
            case INVOKESPECIAL_OP:
            case INVOKESTATIC_OP :
                instr[addr] = new InvokeOperator(addr, 3,
                                               opcode == INVOKESTATIC_OP, 
                                               opcode == INVOKESPECIAL_OP, 
                                               (FieldDefinition)env.getConstant
                                               (stream.readUnsignedShort()));
                break;
            case INVOKEINTERFACE_OP: {
                instr[addr] = new InvokeOperator(addr, 5,
                                               false, false,
                                               (FieldDefinition)env.getConstant
                                               (stream.readUnsignedShort()));
                int reserved = stream.readUnsignedShort();
                break;
            }
            case NEW_OP: {
                ClassDeclaration cldec = (ClassDeclaration) 
                    env.getConstant(stream.readUnsignedShort());
                instr[addr] = new NewOperator(addr, 3, 
                                            Type.tClass(cldec.getName()));
                break;
            }
            case NEWARRAY_OP: {
                Type type;
                switch (stream.readUnsignedByte()) {
                case  4: type = Type.tBoolean; break;
                case  5: type = Type.tChar   ; break;
                case  6: type = Type.tFloat  ; break;
                case  7: type = Type.tDouble ; break;
                case  8: type = Type.tByte   ; break;
                case  9: type = Type.tShort  ; break;
                case 10: type = Type.tInt    ; break;
                case 11: type = Type.tLong   ; break;
                default:
                    throw new ClassFormatError("Invalid newarray operand");
                }
                instr[addr] = new NewArrayOperator(addr, 2, type);
                break;
            }
            case ANEWARRAY_OP: {
                ClassDeclaration cldec = (ClassDeclaration) env.getConstant
                    (stream.readUnsignedShort());
                instr[addr] = new NewArrayOperator
                    (addr, 3, Type.tClass(cldec.getName()));
                break;
            }
            case ARRAYLENGTH_OP:
                instr[addr] = new ArrayLengthOperator(addr, 1);
                break;
            case ATHROW_OP:
                instr[addr] = new ThrowOperator(addr, 1);
                break;
            case CHECKCAST_OP: {
                ClassDeclaration cldec = (ClassDeclaration) env.getConstant
                    (stream.readUnsignedShort());
                instr[addr] = new CheckCastOperator
                    (addr, 3, Type.tClass(cldec.getName()));
                break;
            }
            case INSTANCEOF_OP: {
                ClassDeclaration cldec = (ClassDeclaration) env.getConstant
                    (stream.readUnsignedShort());
                instr[addr] = new InstanceOfOperator
                    (addr, 3, Type.tClass(cldec.getName()));
                break;
            }
            case MONITORENTER_OP:
                instr[addr] = new MonitorEnterOperator(addr, 1);
                break;
            case MONITOREXIT_OP:
                instr[addr] = new MonitorExitOperator(addr, 1);
                break;
            case WIDE_OP: {
                switch (opcode=stream.readUnsignedByte()) {
                case ILOAD_OP: case LLOAD_OP: 
                case FLOAD_OP: case DLOAD_OP: case ALOAD_OP:
                    instr[addr] = new LoadOperator(addr, 3, 
                                                 types[0][opcode-ILOAD_OP],
                                                 method.getLocal
                                                 (stream.readUnsignedShort()));
                    break;
                case ISTORE_OP: case LSTORE_OP: 
                case FSTORE_OP: case DSTORE_OP: case ASTORE_OP:
                    instr[addr] = new StoreOperator(addr, 3, 
                                                  types[0][opcode-ISTORE_OP],
                                                  method.getLocal
                                                  (stream.readUnsignedShort()),
                                                  Operator.ASSIGN_OP);
                    break;
                case IINC_OP: {
                    int local = stream.readUnsignedShort();
                    int value = stream.readUnsignedShort();
                    int operation = Operator.ADD_OP;
                    if (value < 0) {
                        value = -value;
                        operation = Operator.NEG_OP;
                    }
                    instr[addr] = new ConstOperator(addr, 1, ALL_INT_TYPE, 
                                                  Integer.toString(value));
                    instr[addr+1] =
                        new StoreOperator(addr+1, 4, ALL_INT_TYPE, 
                                        method.getLocal(local),
                                        operation + Operator.OPASSIGN_OP);
                    addr++;
                    break;
                }
                case RET_OP:
                    instr[addr] = 
                        new RetOperator(addr, 3, stream.readUnsignedShort());
                    break;
                default:
                    throw new ClassFormatError("Invalid wide opcode "+opcode);
                }
                break;
            }
            case MULTIANEWARRAY_OP: {
                ClassDeclaration cldec = (ClassDeclaration) env.getConstant
                    (stream.readUnsignedShort());
                int dimension = stream.readUnsignedByte();
                instr[addr] = new NewArrayOperator
                    (addr, 3, Type.tClass(cldec.getName()), dimension);
                break;
            }
            case IFNULL_OP: case IFNONNULL_OP:
                instr[addr] = new CompareUnaryOperator
                    (addr, 1, OBJECT_TYPE,
                     opcode - IFNULL_OP + Operator.COMPARE_OP);
                instr[addr+1] = new IfGotoOperator(addr+1, 2, 
                                                 addr+stream.readShort());
                addr++;
                break;
            case GOTO_W_OP:
                instr[addr] = new GotoOperator(addr, 5, addr + stream.readInt());
                break;
            case JSR_W_OP:
                instr[addr] = new JsrOperator(addr, 5, addr + stream.readInt());
                break;
            default:
                throw new ClassFormatError("Invalid opcode "+opcode);
            }
            addr += instr[addr].getLength();
            if (stream.available() != instr.length-addr) {
                throw new RuntimeException("invalid op size: "+opcode);
            }
        }
        } catch (ClassCastException ex) {
            ex.printStackTrace();
            throw new ClassFormatError("Constant has wrong type");
        }
    }

    void readCode(byte[] code) 
         throws ClassFormatError
    {
        instr      = new Instruction[code.length];
        references = new int[code.length];
        try {
            DataInputStream stream = 
                new DataInputStream(new ByteArrayInputStream(code));
            readOpcodes(stream);
        } catch (IOException ex) {
            throw new ClassFormatError(ex.toString());
        }
        references[0]++;
        for (int addr = 0; addr < code.length; 
             addr += instr[addr].getLength()) {
            int[] successors = instr[addr].getSuccessors();
            for (int i=0; i < successors.length; i++) {
                references[successors[i]]++;
            }
        }
    }

    void setExceptionHandler(BinaryExceptionHandler handler) {
        tryAddrs.put(new Integer(handler.startPC), handler);
        references[handler.startPC]++;
        catchAddrs.put(new Integer(handler.handlerPC), handler);
        references[handler.handlerPC]++;
    }

    Instruction getInstruction(int addr) {
        return instr[addr];
    }

    void setInstruction(int addr, Instruction i) {
        instr[addr] = i;
    }

    void removeInstruction(int addr) {
        instr[addr] = null;
    }

    static int WRONG   = -3;
    static int SPECIAL = -2;
    static int FIRST   = -1;

    int getPreviousAddr(int addr) {
        int i;
        for (i = addr-1; i >= 0 && instr[i] == null; i--) {}
        return i;
    }
    
    int getNextAddr(int addr) {
        return addr + instr[addr].getLength();
    }
    
    int getPredecessor(int addr) {
        if (references[addr] != 1)
            return WRONG;
        if (addr == 0)
            return FIRST;
        if (catchAddrs.get(new Integer(addr)) != null)
            return SPECIAL;

        int i = getPreviousAddr(addr);
        if (instr[i].getLength() != addr-i)
            throw new RuntimeException("length mismatch");
        int[] successors = instr[i].getSuccessors();
        for (int j=0; j< successors.length; j++) {
            if (successors[j] == addr)
                return i;
        }
        return WRONG;
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
        int[] successors = { 0};
        for (int addr = 0; addr < instr.length;) {
            if (writer.verbosity > 5) {
                writer.println("<"+addr + " - "+
                               (addr+instr[addr].getLength()-1)+
                               "> ["+references[addr]+"] : "+ 
                               instr[addr].getClass().getName());
                writer.tab();
            }
            int i;
            for (i=0; i< successors.length && successors[i] != addr; i++)
                {}
            if (references[addr] != 1 || i == successors.length)
                writer.print("addr_"+addr+": ");
            instr[addr].dumpSource(writer, this);

            if (writer.verbosity > 5)
                writer.untab();
            successors = instr[addr].getSuccessors();
            
            for (i = instr[addr++].getLength()-1; i>0; i--)
                if (instr[addr++] != null)
                    throw new AssertError ("dubious instr at addr "+addr);
        }
    }

    public CodeAnalyzer(MethodAnalyzer ma, BinaryCode bc, JodeEnvironment e)
         throws ClassFormatError
    {
        method = ma;
        env  = e;
        readCode(bincode.getCode());
        BinaryExceptionHandler[] handlers = bincode.getExceptionHandlers();
        for (int i=0; i<handlers.length; i++) {
            setExceptionHandler(handlers[i]);
        }
    }

    public int createExpression(int addr) {
        try {
            Operator op = (Operator) instr[addr];
            int length  = instr[addr].getLength();
            int params  = op.getOperandCount();
            Expression exprs[] = new Expression[params];
            int addrs[] = new int[params+1];
            
            addrs[params] = addr;
            for (int i = params-1; i>=0; i--) {
                addrs[i] = getPredecessor(addrs[i+1]);
                if (addrs[i] < 0)
                    return -1;
                exprs[i] = (Expression) instr[addrs[i]];
                if (exprs[i].isVoid())
                    return -1;
            }
            length += addrs[params]-addrs[0];
            for (int i = 1; i <= params; i++) {
                instr[addrs[i]] = null;
            }
            addr = addrs[0];
            instr[addr] = new Expression(addr, length, op, exprs);
            return addr;
        } catch (ClassCastException ex) {
            return -1;
        }
    }

    public int combineExpressions(int addr) {
        int count = 0;
        int start = addr;
        if (!(instr[addr] instanceof Expression) ||
            !((Expression)instr[addr]).isVoid())
            return -1;
        do {
            addr = getNextAddr(addr);
            count++;
        } while (addr < instr.length && 
                 references[addr] == 1 &&
                 instr[addr] instanceof Expression &&
                 ((Expression)instr[addr]).isVoid());
        Expression[] expr = new Expression[count];
        addr = start;
        for (int i=0; i < count; i++) {
            expr[i] = (Expression)instr[addr];
            int next = getNextAddr(addr);
            instr[addr] = null;
            addr = next;
        }
        instr[start] = new Block(start, addr-start, expr);
        return start;
    }

    public int createAssignExpression(int addr) {
        try {
            StoreInstruction store = (StoreInstruction) instr[addr];
            int dupAddr = getPreviousAddr(addr);
            if (dupAddr < 0 || references[dupAddr] != 1)
                return -1;
            DupOperator dup = (DupOperator) instr[dupAddr];
            if (dup.getDepth() != store.getLValueOperandCount() && 
                dup.getLength() != store.getLValueType().stackSize())
                return -1;
            int end = getNextAddr(addr);
            instr[dupAddr] = new AssignOperator(dupAddr, end-dupAddr,
                                                Operator.ASSIGN_OP, store);
            instr[addr] = null;
            return dupAddr;
        } catch (ClassCastException ex) {
            return -1;
        }
    }

    public int createPostIncExpression(int addr) {
        try {
            int op;
            Expression storeExpr = (Expression) instr[addr];
            StoreOperator store = (StoreOperator) storeExpr.getOperator();
            if (store.getOperator() == store.ADD_OP+store.OPASSIGN_OP)
                op = Operator.INC_OP;
            else if (store.getOperator() == store.NEG_OP+store.OPASSIGN_OP)
                op = Operator.INC_OP+1;
            else
                return -1;
            Expression expr = storeExpr.getSubExpressions()[0];
            ConstOperator constOp = (ConstOperator) expr.getOperator();
            if (!constOp.getValue().equals("1"))
                return -1;
            int loadAddr = getPreviousAddr(addr);
            if (loadAddr < 0 || references[loadAddr] != 1)
                return -1;
            Expression loadExpr = (Expression) instr[loadAddr];
            LoadOperator load = (LoadOperator) loadExpr.getOperator();
            if (load.getSlot() != store.getSlot())
                return -1;
            int end = getNextAddr(addr);
            Operator postop = 
                new PostFixOperator(loadAddr, end-loadAddr, 
                                    loadExpr.getType(), op);
            Expression [] exprs = { loadExpr };
            instr[loadAddr] = new Expression
                (loadAddr, end-loadAddr, postop, exprs);
            instr[addr] = null;
            return loadAddr;
        } catch (ClassCastException ex) {
            return -1;
        }
    }

    public int createArrayOpAssign(int addr) {
        try {
            StoreInstruction store = (StoreInstruction) instr[addr];
            int binOpAddr = getPreviousAddr(addr);
            if (binOpAddr < 0 || references[binOpAddr] != 1)
                return -1;
            BinaryOperator binop = (BinaryOperator) instr[binOpAddr];
            if (binop.getOperator() <  binop.ADD_OP ||
                binop.getOperator() >= binop.ASSIGN_OP)
                return -1;
            int exprAddr = getPreviousAddr(binOpAddr);
            if (exprAddr < 0 || references[exprAddr] != 1)
                return -1;
            Expression expr = (Expression) instr[exprAddr];
            int loadAddr = getPreviousAddr(exprAddr);
            if (loadAddr < 0 || references[loadAddr] != 1)
                return -1;
            Operator load = 
                (Operator) instr[loadAddr];
            if (load.getType() != store.getLValueType())
                return -1;
            if (!store.matches(load))
                return -1;
            int dupAddr = getPreviousAddr(loadAddr);
            if (dupAddr < 0)
                return -1;
            DupOperator dup = (DupOperator) instr[dupAddr];
            if (dup.getDepth() != 0 && 
                dup.getLength() != store.getLValueOperandCount())
                return -1;
            int end = getNextAddr(addr);
            instr[dupAddr] = new Expression
                (dupAddr, binOpAddr-dupAddr, 
                 expr.getOperator(), expr.getSubExpressions());
            instr[binOpAddr] = store;
            store.setAddr(binOpAddr);
            store.setLength(end-binOpAddr);
            store.setOperator(store.OPASSIGN_OP+binop.getOperator());
            store.setLValueType
                (UnknownType.commonType(binop.getType(), 
                                        store.getLValueType()));
            instr[loadAddr] = instr[exprAddr] = 
                instr[addr] = null;
            return dupAddr;
        } catch (ClassCastException ex) {
            return -1;
        }
    }

    public int combineNewConstructor(int addr) {
        try {
            InvokeOperator constrCall = (InvokeOperator) instr[addr];
            if (!constrCall.isConstructor())
                return -1;
            int length  = instr[addr].getLength();
            int params  = constrCall.getOperandCount();
            Expression exprs[] = new Expression[params];
            int addrs[] = new int[params+1];
            addrs[params] = addr;
            for (int i = params-1; i>0; i--) {
                addrs[i] = getPredecessor(addrs[i+1]);
                if (addrs[i] < 0)
                    return -1;
                exprs[i] = (Expression) instr[addrs[i]];
                if (exprs[i].isVoid())
                    return -1;
            }
            addrs[0] = getPredecessor(addrs[1]);
            DupOperator dup = (DupOperator) instr[addrs[0]];
            if (dup.getCount() != 1 && dup.getDepth() != 0)
                return -1;
            addr = getPredecessor(addrs[0]);
            exprs[0] = (Expression) instr[addr];
            if (exprs[0].isVoid())
                return -1;
            NewOperator op = (NewOperator) exprs[0].getOperator();
            if (constrCall.getClassType() != op.getType())
                return -1;
            length += addrs[params]-addr;
            for (int i = 0; i <= params; i++) {
                instr[addrs[i]] = null;
            }
            ConstructorOperator conOp = 
                new ConstructorOperator(addr, length, 
                                        constrCall.getClassType(), 
                                        constrCall.getField());
            instr[addr] = new Expression(addr, length, conOp, exprs);
            return addr;
        } catch (ClassCastException ex) {
            return -1;
        }
    }

//     public int createIfGotoStatement(int addr) {
//         if (references[addr] != 1)
//             return -1;
//         Expression e;
//         int condAddr, dest;
//         try {
//             IfGotoOperator igo = (IfGotoOperator) instr[addr];
//             condAddr = getPreviousAddr(addr);
//             if (condAddr < 0)
//                 return -1;
//             e = (Expression) instr[condAddr];
//             if (e.isVoid())
//                 return -1;
//             dest = igo.getDestination();
//         } catch (ClassCastException ex) {
//             return -1;
//         }
//         int end = getNextAddr(addr);
//         instr[condAddr] = new IfGotoStatement(condAddr, end-condAddr, dest, e);
//         instr[addr] = null;
//         return condAddr;
//     }

    public int combineIfGotoExpressions(int addr) {
        Expression e[];
        int if1Addr;
        int end = getNextAddr(addr);
        int operator;
        IfGotoOperator igo2;
        try {
            igo2 = (IfGotoOperator) 
                ((Expression)instr[addr]).getOperator();
            int dest = igo2.getDestination();
            if1Addr = getPreviousAddr(addr);
            if (if1Addr < 0 || references[if1Addr] != 1)
                return -1;
            IfGotoOperator igo1 = (IfGotoOperator) 
                ((Expression)instr[if1Addr]).getOperator();
            if (igo1.getDestination() == end) {
                e = new Expression[2];
                operator = Operator.LOG_AND_OP;
                e[1] = ((Expression)instr[addr]).getSubExpressions()[0];
                e[0] = ((Expression)instr[if1Addr]).
                    getSubExpressions()[0].negate();
                references[end]--;
            } else if (igo1.getDestination() == dest) {
                e = new Expression[2];
                operator = Operator.LOG_OR_OP;
                e[1] = ((Expression)instr[addr]).getSubExpressions()[0];
                e[0] = ((Expression)instr[if1Addr]).getSubExpressions()[0];
                references[dest]--;
            } else
                return -1;
        } catch (ClassCastException ex) {
            return -1;
        }
        Expression[] cond = 
        { new Expression(if1Addr, end-if1Addr,
                         new BinaryOperator(if1Addr, end-if1Addr,
                                          Type.tBoolean, operator), e) };
        instr[if1Addr] = new Expression(if1Addr, end-if1Addr, igo2, cond);
        instr[addr] = null;
        return if1Addr;
    }

    public int combineConditionalExpr(int addr) {
        if (references[addr] != 1)
            return -1;
        Expression e[] = new Expression[3];
        int ifAddr, gotoAddr, e1Addr;
        int end = getNextAddr(addr);
        try {
            e[2] = (Expression) instr[addr];
            if (e[2].isVoid())
                return -1;
            gotoAddr = getPreviousAddr(addr);
            if (gotoAddr < 0 || references[gotoAddr] != 1)
                return -1;
            if (((GotoOperator)
                 ((Expression)instr[gotoAddr]).getOperator())
                .getDestination() != end)
                return -1;
            e1Addr = getPreviousAddr(gotoAddr);
            if (e1Addr <0 || references[e1Addr] != 1)
                return -1;
            e[1] = (Expression) instr[e1Addr];
            if (e[1].isVoid())
                return -1;
            ifAddr = getPreviousAddr(e1Addr);
            if (ifAddr < 0)
                return -1;
            e[0] = (Expression)instr[ifAddr];
            IfGotoOperator igo = (IfGotoOperator) e[0].getOperator();
            if (igo.getDestination() != addr)
                return -1;
            e[0] = e[0].getSubExpressions()[0].negate();
        } catch (ClassCastException ex) {
            return -1;
        }
        IfThenElseOperator iteo = new IfThenElseOperator
            (ifAddr, end-ifAddr, 
             UnknownType.commonType(e[1].getType(),e[2].getType()));
        instr[ifAddr] = new Expression(ifAddr, end-ifAddr, iteo, e);
        instr[e1Addr] = instr[gotoAddr] = instr[addr] = null;
        references[end]--;
        return ifAddr;
    }

    public void analyzeLocals()
    {
        for (int slot=0; slot< bincode.maxLocals(); slot++) {
            LocalVariable localVar = method.getLocal(slot);
            int storeAddr[] = new int[instr.size];
            for (int i=0; i< instr.size; i++) {
                storeAddr[i] = -1;

            Stack addrStack = new Stack();
            addrStack.push(new Integer(0));
            storeAddr[0] = 0;

            while (!addrStack.isEmpty()) {
                int[] successors;
                int addr = ((Integer)addrStack.pop()).intValue();

                int store = infos[addr];
                if (instr[addr] instanceof StoreOperator) {
                    store = addr;
                } else if (instr[addr] instanceof LoadOperator) {
                    localVar.combine(store, addr);
                }
                successors = instr[addr].getSuccessors();
                addr = (successors.length==1)?successor[0];

                /* XXX try - catch blocks */

                LocalInfo nextInfo = localVar.getInfo(store);
                for (int i=0; i< successors.length; i++) {
                    if (info[successors[i]] != -1) {
                        if (nextInfo == localVar.getInfo(info[successors[i]]))
                            continue;
                        localVar.combine(info[successors[i]], store);
                    } else
                        info[successors[i]] = store;
                    addrStack.push(successors[i]);
                }
            }
        }
    }

    public void analyze() 
    {
        for (int addr = 0; addr < instr.length; ) {
            int nextAddr;
            nextAddr = createExpression(addr);
            if (nextAddr >= 0) {
                addr = nextAddr;
                continue;
            }
            nextAddr = createAssignExpression(addr);
            if (nextAddr >= 0) {
                addr = nextAddr;
                continue;
            }
            nextAddr = createArrayOpAssign(addr);
            if (nextAddr >= 0) {
                addr = nextAddr;
                continue;
            }
            nextAddr = createPostIncExpression(addr);
            if (nextAddr >= 0) {
                addr = nextAddr;
                continue;
            }
            nextAddr = combineNewConstructor(addr);
            if (nextAddr >= 0) {
                addr = nextAddr;
                continue;
            }
//             nextAddr = createIfGotoStatement(addr);
//             if (nextAddr >= 0) {
//                 addr = nextAddr;
//                 continue;
//             }
            nextAddr = combineIfGotoExpressions(addr);
            if (nextAddr >= 0) {
                addr = nextAddr;
                continue;
            }
            nextAddr = combineConditionalExpr(addr);
            if (nextAddr >= 0) {
                addr = nextAddr;
                continue;
            }
            addr += instr[addr].getLength();
        }
        for (int addr = 0; addr < instr.length; ) {
            int nextAddr;
            nextAddr = combineExpressions(addr);
            if (nextAddr >= 0) {
                addr = nextAddr;
                continue;
            }
            addr += instr[addr].getLength();
        }
    }

    public String getLocalName(int i, int addr) {
        return method.getLocalName(i, addr).toString();
    }

    public String getTypeString(Type type) {
        return type.toString();
    }

    public ClassDefinition getClassDefinition() {
        return env.getClassDefinition();
    }
}

