package com.coreyd97.stepper;

import com.coreyd97.stepper.step.StepExecutionInfo;

public interface StepStateChangeListener {
    public void onExecutionInfoChanged(StepExecutionInfo info);
}
