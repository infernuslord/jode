package jode.bytecode;
import java.util.Vector;

/**
 * This class represents an instruction in the byte code.
 *
 * For simplicity currently most fields are public.  You shouldn't change
 * many of them, though.
 */
public class Instruction implements Opcodes{
    /**
     * The opcode of the instruction.  We map some opcodes, e.g.
     * <pre>
     * iload_[0-3] -> iload, ldc_w -> ldc, wide iinc -> iinc.
     * </pre>
     */
    public int opcode;
    /**
     * If this opcode uses a local this gives the slot.  This info is
     * used when swapping locals.  
     */
    public int localSlot = -1;
    /**
     * Optional object data for this opcode.  This is mostly used for
     * method/field/class references,  but also for a value array
     * in a lookupswitch.
     */
    public Object objData;
    /**
     * Optional integer data for this opcode.  There are various uses
     * for this.
     */
    public int intData;
    /**
     * The address of this opcode.
     */
    public int addr;
    /**
     * The length of this opcode.  You shouldn't touch it, nor rely on
     * it, since the length of some opcodes may change automagically
     * (e.g. when changing localSlot  iload_0 <-> iload 5)
     */
    public int length;
    /**
     * If this is true, the instruction will never flow into the nextByAddr.
     */
    public boolean alwaysJumps = false;
    /**
     * The successors of this opcodes, where flow may lead to
     * (except that nextByAddr is implicit if !alwaysJump).  The
     * value null is equivalent to an empty array.
     */
    public Instruction[] succs;
    /**
     * The predecessors of this opcode, including the prevByAddr, if
     * that does not alwaysJump.
     */
    public Vector preds = new Vector();
    /**
     * The next instruction in code order.
     */
    public Instruction nextByAddr;
    /**
     * The previous instruction in code order, useful when changing
     * the order.
     */
    public Instruction prevByAddr;

    /**
     * You can use this field to add some info to each instruction.
     */
    public Object tmpInfo;
}
