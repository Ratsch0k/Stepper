package com.coreyd97.stepper;

import java.util.HashMap;
import java.util.Map;

import com.coreyd97.stepper.exception.SequenceExecutionException;
import com.coreyd97.stepper.sequence.StepSequence;
import com.coreyd97.stepper.sequence.StepSequenceExecutionable;
import com.coreyd97.stepper.sequence.StepSequenceState;

public class StepSequenceExecutor {
    public static void execute(StepSequence sequence, Map<String, String> variables) {
        StepSequence executionCopy = sequence.copy();        executionCopy.executeBlocking(variables);
    }

    public static void execute(StepSequenceState sequence) {
        StepSequenceExecutor.execute(sequence, new HashMap<>(), false);
    }

    public static void execute(StepSequenceState sequence, boolean updateState) {
        StepSequenceExecutor.execute(sequence, new HashMap<>(), updateState);
    }
    
    public static void execute(StepSequenceState sequence, Map<String, String> variables, boolean updateState) {
        StepSequenceExecutionable executionable = new StepSequenceExecutionable(sequence, updateState);

        try {
            executionable.execute(variables);
        } catch (SequenceExecutionException e) {
            Stepper.callbacks.printError("Error occurred while executing sequence: " + e);
        }
    }
}
