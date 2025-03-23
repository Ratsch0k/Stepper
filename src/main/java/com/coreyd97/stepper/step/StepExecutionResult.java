package com.coreyd97.stepper.step;

import java.util.List;

import com.coreyd97.stepper.variable.StepVariable;

public class StepExecutionResult {
    private final StepExecutionInfo info;
    private final List<StepVariable> variables;
    
    public StepExecutionResult(StepExecutionInfo info, List<StepVariable> variables) {
        this.info = info;
        this.variables = variables;
    }

    public StepExecutionInfo getInfo() {
        return this.info;
    }

    public List<StepVariable> getVariables() {
        return this.variables;
    }
}
