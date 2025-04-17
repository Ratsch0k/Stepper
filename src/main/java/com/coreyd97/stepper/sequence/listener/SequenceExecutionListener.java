package com.coreyd97.stepper.sequence.listener;

import com.coreyd97.stepper.step.Step;
import com.coreyd97.stepper.step.StepExecutionInfo;
import com.coreyd97.stepper.step.StepState;

import java.util.List;

public interface SequenceExecutionListener {
    void beforeSequenceStart(List<Step> steps);
    void beforeSequenceStateStart(List<StepState> steps);
    void sequenceStepExecuted(StepExecutionInfo executionInfo);
    void afterSequenceEnd(boolean success);
}
