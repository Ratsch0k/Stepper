package com.coreyd97.stepper.step;

import com.coreyd97.stepper.variable.*;

public class StepStateVariableManager extends VariableManager {

    private final StepState step;

    StepStateVariableManager(StepState step){
        super();
        this.step = step;
    }

    public void updateVariablesBeforeExecution(){
        for (StepVariable variable : this.variables) {
            if(variable instanceof PreExecutionStepVariable)
                ((PreExecutionStepVariable) variable).updateVariableBeforeExecution();
        }
    }

    public void updateVariablesAfterExecution(StepExecutionInfo executionInfo){
        for (StepVariable variable : this.variables) {
            if(variable instanceof PostExecutionStepVariable)
            ((PostExecutionStepVariable) variable).updateVariableAfterExecution(executionInfo);
        }
    }

    public void updateVariableWithPreviousExecutionResult(PostExecutionStepVariable variable){
        variable.updateVariableAfterExecution(this.step.getLastExecutionResult());
    }
}
