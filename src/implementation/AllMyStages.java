/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import implementation.AllMyLatches.*;
import utilitytypes.EnumOpcode;
import baseclasses.InstructionBase;
import baseclasses.PipelineRegister;
import baseclasses.PipelineStageBase;
import utilitytypes.Operand;
import voidtypes.VoidLatch;
import baseclasses.CpuCore;

/**
 * The AllMyStages class merely collects together all of the pipeline stage
 * classes into one place.  You are free to split them out into top-level
 * classes.
 * <p>
 * Each inner class here implements the logic for a pipeline stage.
 * <p>
 * It is recommended that the compute methods be idempotent.  This means
 * that if compute is called multiple times in a clock cycle, it should
 * compute the same output for the same input.
 * <p>
 * How might we make updating the program counter idempotent?
 *
 * @author
 */
public class AllMyStages {

    /*** Fetch Stage ***/
    static class Fetch extends PipelineStageBase<VoidLatch, FetchToDecode> {
        public Fetch(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public String getStatus() {
            // Generate a string that helps you debug.
            return null;
        }

        @Override
        public void compute(VoidLatch input, FetchToDecode output) {
            GlobalData globals = (GlobalData) core.getGlobalResources();
            globals.program_counter = globals.newPCAddr;
            int pc = globals.program_counter;
            // Fetch the instruction
            InstructionBase ins = globals.program.getInstructionAt(pc);
            if (ins.isNull()) return;

            // Do something idempotent to compute the next program counter.

            if (globals.insInPipeline) {
                return;
            }

            // Don't forget branches, which MUST be resolved in the Decode
            // stage.  You will make use of global resources to commmunicate
            // between stages.

            // Your code goes here...
            globals.newPCAddr++;

            output.setInstruction(ins);

            globals.insInPipeline = true;
        }

        @Override
        public boolean stageWaitingOnResource() {
            // Hint:  You will need to implement this for when branches
            // are being resolved.
            return false;
        }


        /**
         * This function is to advance state to the next clock cycle and
         * can be applied to any data that must be updated but which is
         * not stored in a pipeline register.
         */
        @Override
        public void advanceClock() {
            // Hint:  You will need to implement this help with waiting
            // for branch resolution and updating the program counter.
            // Don't forget to check for stall conditions, such as when
            // nextStageCanAcceptWork() returns false.
        }
    }


    /*** Decode Stage ***/
    static class Decode extends PipelineStageBase<FetchToDecode, DecodeToExecute> {
        public Decode(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public boolean stageWaitingOnResource() {
//            GlobalData globalData = (GlobalData) core.getGlobalResources();
//            return globalData.insInPipeline;
            // Hint:  You will need to implement this to deal with 
            // dependencies.
            return false;
        }


        @Override
        public void compute(FetchToDecode input, DecodeToExecute output) {
            InstructionBase ins = input.getInstruction();

            // These null instruction checks are mostly just to speed up
            // the simulation.  The Void types were created so that null
            // checks can be almost completely avoided.
            if (ins.isNull()) return;


            // Do what the decode stage does:
            // - Look up source operands
            // - Decode instruction
            // - Resolve branches            

            output.setInstruction(ins);
            // Set other data that's passed to the next stage.
        }
    }


    /*** Execute Stage ***/
    static class Execute extends PipelineStageBase<DecodeToExecute, ExecuteToMemory> {
        public Execute(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public void compute(DecodeToExecute input, ExecuteToMemory output) {
            InstructionBase ins = input.getInstruction();
            if (ins.isNull()) return;

            GlobalData globals = (GlobalData) core.getGlobalResources();
            Operand src1, src2, oper0;
            src1 = ins.getSrc1();
            src2 = ins.getSrc2();
            oper0 = ins.getOper0();

            int valueSrc1, valueSrc2, valueOper0, result = 0;
            EnumOpcode operation;
            valueSrc1 = src1.hasValue() ? src1.getValue() :
                    src1.getRegisterNumber() >= 0 ? globals.register_file[src1.getRegisterNumber()] : 0;
            valueSrc2 = src2.hasValue() ? src2.getValue() :
                    src2.getRegisterNumber() >= 0 ? globals.register_file[src2.getRegisterNumber()] : 0;
            valueOper0 = oper0.hasValue() ? oper0.getValue() :
                    oper0.getRegisterNumber() >= 0 ? globals.register_file[oper0.getRegisterNumber()] : 0;
            operation = ins.getOpcode();

            if (operation == EnumOpcode.OUT) {
                System.out.print(valueOper0 + " ");
            } else if (operation == EnumOpcode.BRA || operation == EnumOpcode.JMP) {
                result = MyALU.execute(ins, core);
            } else {
                result = MyALU.execute(operation, valueSrc1, valueSrc2, valueOper0);
            }

            // Fill output with what passes to Memory stage...
            output.setInstruction(ins);
            // Set other data that's passed to the next stage.
            output.setOutput(result);
        }
    }


    /*** Memory Stage ***/
    static class Memory extends PipelineStageBase<ExecuteToMemory, MemoryToWriteback> {
        public Memory(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public void compute(ExecuteToMemory input, MemoryToWriteback output) {
            InstructionBase ins = input.getInstruction();
            if (ins.isNull()) return;

            // Access memory...

            GlobalData globals = (GlobalData) core.getGlobalResources();
            Operand src1, src2, oper0;
            src1 = ins.getSrc1();
            src2 = ins.getSrc2();
            oper0 = ins.getOper0();

            int valueSrc1, valueSrc2, valueOper0, result = input.exeResult;
            EnumOpcode operation;
            valueSrc1 = src1.hasValue() ? src1.getValue() :
                    src1.getRegisterNumber() >= 0 ? globals.register_file[src1.getRegisterNumber()] : 0;
            valueSrc2 = src2.hasValue() ? src2.getValue() :
                    src2.getRegisterNumber() >= 0 ? globals.register_file[src2.getRegisterNumber()] : 0;
            valueOper0 = oper0.hasValue() ? oper0.getValue() :
                    oper0.getRegisterNumber() >= 0 ? globals.register_file[oper0.getRegisterNumber()] : 0;
            operation = ins.getOpcode();


            if (ins.getOpcode() == EnumOpcode.STORE) {
                int writeData = ins.getOper0().hasValue() ?
                        ins.getOper0().getValue() : globals.register_file[ins.getOper0().getRegisterNumber()];
                int writeAddr = input.exeResult;
                globals.memory[writeAddr] = writeData;
                result = input.exeResult;
            } else if (ins.getOpcode() == EnumOpcode.LOAD) {
                result = globals.memory[globals.register_file[src1.getRegisterNumber()]];
            }

            output.setInstruction(ins);
            // Set other data that's passed to the next stage.
            output.setOutput(result);
        }
    }


    /*** Writeback Stage ***/
    static class Writeback extends PipelineStageBase<MemoryToWriteback, VoidLatch> {
        public Writeback(CpuCore core, PipelineRegister input, PipelineRegister output) {
            super(core, input, output);
        }

        @Override
        public void compute(MemoryToWriteback input, VoidLatch output) {
            InstructionBase ins = input.getInstruction();
            if (ins.isNull()) return;

            // Write back result to register file

            GlobalData globalData = (GlobalData) core.getGlobalResources();


            //if (ins.getOpcode() == EnumOpcode.BRA) {
            //    globalData.program_counter = input.exeResult;
            if (EnumOpcode.needsWriteback(ins.getOpcode())) {
                // oper0 <- exeresult
                globalData.register_file[ins.getOper0().getRegisterNumber()] = input.exeResult;
            }


            globalData.insInPipeline = false;
            if (input.getInstruction().getOpcode() == EnumOpcode.HALT) {
                globalData.halt = true;
            }
        }
    }
}
