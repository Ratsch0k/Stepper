package com.coreyd97.stepper;

import java.util.Map;

import com.coreyd97.stepper.sequence.StepSequence;

public class StepSequenceExecutor {
    public static void execute(StepSequence sequence, Map<String, String> variables) {
        Stepper.callbacks.printOutput("Copying sequence: " + sequence);
        StepSequence executionCopy = sequence.copy();
        Stepper.callbacks.printOutput("Copied: " + executionCopy);
        executionCopy.executeBlocking(variables);
    }
}
