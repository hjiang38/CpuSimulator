/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.CpuCore;
import baseclasses.InstructionBase;
import utilitytypes.EnumOpcode;

/**
 * The code that implements the ALU has been separates out into a static
 * method in its own class.  However, this is just a design choice, and you
 * are not required to do this.
 *
 * @author
 */
public class MyALU {

    static int execute(InstructionBase ins, CpuCore core) {
        GlobalData globalData = (GlobalData) core.getGlobalResources();
        if (ins.getOpcode() == EnumOpcode.JMP) {
            globalData.newPCAddr = ins.getLabelTarget().getAddress();
            return ins.getLabelTarget().getAddress();
        }
        int cmpRes;
        cmpRes = globalData.register_file[ins.getOper0().getRegisterNumber()];
        boolean jmp = false;
        switch (ins.getComparison()) {
            case EQ: { jmp = (cmpRes == 0); break; }
            case NE: { jmp = (cmpRes != 0); break; }
            case GT: { jmp = (cmpRes > 0); break; }
            case GE: { jmp = (cmpRes >= 0); break; }
            case LT: { jmp = (cmpRes < 0); break; }
            case LE: { jmp = (cmpRes <= 0); break; }
        }
        if (jmp) {
            globalData.newPCAddr = ins.getLabelTarget().getAddress();
            return ins.getLabelTarget().getAddress();
        } else {
            //globalData.newPCAddr = globalData.program_counter + 1;
            return ins.getPCAddress();
        }
    }

    static int execute(EnumOpcode opcode, int src1, int src2, int oper0) {
        int result = 0;

        // Implement code here that performs appropriate computations for
        // any instruction that requires an ALU operation.  See
        // EnumOpcode.
        switch (opcode) {
            case ADD: { result = src1 + src2; break; }
            case SUB: { result = src1 - src2; break; }
            case AND: { result = src1 & src2; break; }
            case OR: { result = src1 | src2; break; }
            case SHL: { result = src1 << src2; break; }
            case ASR: { result = src1 >>> src2; break; }
            case LSR: { result = src1 >> src2; break; }
            case XOR: { result = src1 ^ src2; break; }
            case MOVC: { result = src1; break; }
            case STORE: { result = src1 + src2; break; }
            case LOAD: { result = src1 + src2; break; }
            case CMP: { result = src1 - src2; break; }
        }

        return result;
    }
}
