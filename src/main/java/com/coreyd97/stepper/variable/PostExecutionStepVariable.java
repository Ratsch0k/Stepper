package com.coreyd97.stepper.variable;

import com.coreyd97.stepper.step.StepExecutionInfo;

public abstract class PostExecutionStepVariable extends StepVariable {

    PostExecutionStepVariable(String identifier){
        super(identifier);
    }

    public abstract void setCondition(String condition);

    public abstract String getConditionText();

    public abstract void updateVariableAfterExecution(StepExecutionInfo executionInfo);
}
