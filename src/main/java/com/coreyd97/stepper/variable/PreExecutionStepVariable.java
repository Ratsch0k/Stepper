package com.coreyd97.stepper.variable;

public abstract class PreExecutionStepVariable extends StepVariable {

    PreExecutionStepVariable(String identifier){
        super(identifier);
    }

    public abstract void updateVariableBeforeExecution();
}
